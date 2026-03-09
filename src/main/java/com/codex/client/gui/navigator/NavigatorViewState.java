package com.codex.client.gui.navigator;

public final class NavigatorViewState {
    private static NavigatorSection activeSection = NavigatorSection.MODULES;

    private NavigatorViewState() {}

    public static NavigatorSection getActiveSection() {
        return activeSection;
    }

    public static void setActiveSection(NavigatorSection section) {
        activeSection = section == null ? NavigatorSection.MODULES : section;
    }
}
