package com.codex.api.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reflection-based event bus. Modules register themselves, and methods
 * annotated with {@link EventTarget} are automatically discovered and
 * invoked when matching events are fired via {@link #call(Event)}.
 */
public class EventManager {
    private static final Logger LOGGER = Logger.getLogger(EventManager.class.getName());
    private static final Map<Object, List<Listener>> listenerMap = new ConcurrentHashMap<>();
    private static final Map<Class<?>, CopyOnWriteArrayList<Listener>> listenersByType = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Listener>> dispatchCache = new ConcurrentHashMap<>();

    /**
     * Scans the object for methods annotated with {@link EventTarget} and
     * registers them as event listeners. Each annotated method must accept
     * exactly one parameter that extends {@link Event}.
     */
    public static void register(Object obj) {
        if (listenerMap.containsKey(obj)) {
            return;
        }

        List<Listener> found = new ArrayList<>();
        for (Method method : obj.getClass().getDeclaredMethods()) {
            EventTarget annotation = method.getAnnotation(EventTarget.class);
            if (annotation == null) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                continue;
            }

            Class<?> paramType = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(paramType)) {
                continue;
            }

            method.setAccessible(true);
            found.add(new Listener(obj, method, paramType, annotation.value()));
        }

        if (!found.isEmpty()) {
            listenerMap.put(obj, found);
            for (Listener listener : found) {
                CopyOnWriteArrayList<Listener> listeners = listenersByType.computeIfAbsent(
                    listener.eventType,
                    ignored -> new CopyOnWriteArrayList<Listener>()
                );
                listeners.add(listener);
                listeners.sort(Comparator.comparingInt(l -> l.priority));
            }
            dispatchCache.clear();
        }
    }

    /**
     * Removes all listeners associated with the given object.
     */
    public static void unregister(Object obj) {
        List<Listener> removed = listenerMap.remove(obj);
        if (removed != null) {
            for (Listener listener : removed) {
                CopyOnWriteArrayList<Listener> listeners = listenersByType.get(listener.eventType);
                if (listeners != null) {
                    listeners.remove(listener);
                    if (listeners.isEmpty()) {
                        listenersByType.remove(listener.eventType, listeners);
                    }
                }
            }
            dispatchCache.clear();
        }
    }

    /**
     * Dispatches the event to all registered listeners whose parameter type
     * matches the event's class. Listeners are invoked in priority order
     * (lower value = higher priority). Cancelled events are still delivered
     * to allow listeners to check cancellation state.
     */
    public static void call(Event event) {
        Class<?> eventClass = event.getClass();
        List<Listener> listeners = dispatchCache.computeIfAbsent(eventClass, EventManager::resolveListeners);

        for (Listener listener : listeners) {
            try {
                listener.method.invoke(listener.instance, event);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE,
                    "Error dispatching event to " + listener.instance.getClass().getSimpleName()
                        + "." + listener.method.getName(),
                    e);
            }
        }
    }

    private static List<Listener> resolveListeners(Class<?> eventClass) {
        List<Listener> resolved = new ArrayList<>();
        for (Map.Entry<Class<?>, CopyOnWriteArrayList<Listener>> entry : listenersByType.entrySet()) {
            if (entry.getKey().isAssignableFrom(eventClass)) {
                resolved.addAll(entry.getValue());
            }
        }

        resolved.sort(Comparator.comparingInt(l -> l.priority));
        return Collections.unmodifiableList(resolved);
    }

    public static void clear() {
        listenerMap.clear();
        listenersByType.clear();
        dispatchCache.clear();
    }

    /**
     * Wraps a listener method reference with its metadata.
     */
    public static class Listener {
        final Object instance;
        final Method method;
        final Class<?> eventType;
        final byte priority;

        Listener(Object instance, Method method, Class<?> eventType, byte priority) {
            this.instance = instance;
            this.method = method;
            this.eventType = eventType;
            this.priority = priority;
        }
    }
}
