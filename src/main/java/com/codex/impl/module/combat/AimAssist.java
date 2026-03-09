package com.codex.impl.module.combat;

import com.codex.api.event.EventTarget;
import com.codex.api.event.events.UpdateEvent;
import com.codex.api.event.events.MouseUpdateEvent;
import com.codex.api.module.Category;
import com.codex.api.module.Module;
import com.codex.api.value.BoolValue;
import com.codex.api.value.ColorValue;
import com.codex.api.value.ModeValue;
import com.codex.api.value.NumberValue;
import com.codex.client.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.stream.StreamSupport;

public class AimAssist extends Module {

    private final BoolValue targetPlayers = (BoolValue) new BoolValue("Target Players", true).setGroup("Targets");
    private final BoolValue targetHostiles = (BoolValue) new BoolValue("Target Hostiles", false).setGroup("Targets");
    private final BoolValue targetPassives = (BoolValue) new BoolValue("Target Passives", false).setGroup("Targets");
    private final BoolValue invisible = (BoolValue) new BoolValue("Target Invisible", false).setGroup("Targets");

    // Limited tracking distance for smoother, non-invasive targeting behavior.
    private final NumberValue range = (NumberValue) new NumberValue("Tracking Range", 4.5, 1.0, 6.0, 0.1).setGroup("Tracking");
    private final NumberValue fov = (NumberValue) new NumberValue("FOV", 90.0, 10.0, 360.0, 5.0).setGroup("Tracking");
    private final ModeValue targetPart = (ModeValue) new ModeValue("Aim Part", "Dynamic", "Head", "Chest", "Legs", "Random", "Dynamic").setGroup("Tracking");
    private final NumberValue dynamicSpeed = (NumberValue) new NumberValue("Dynamic Speed", 10.0, 1.0, 20.0, 1.0).setGroup("Tracking");

    private final NumberValue rotationSpeed = (NumberValue) new NumberValue("Rotation Speed", 120.0, 1.0, 720.0, 1.0).setGroup("Smoothness");
    private final NumberValue ignoreMouseInput = (NumberValue) new NumberValue("Ignore Mouse %", 0.0, 0.0, 100.0, 1.0).setGroup("Smoothness");

    private final BoolValue clickOnly = (BoolValue) new BoolValue("Click To Aim", false).setGroup("Humanize");
    private final BoolValue weaponOnly = (BoolValue) new BoolValue("Weapon Only", false).setGroup("Humanize");
    private final NumberValue humanizeNoise = (NumberValue) new NumberValue("Humanize Noise", 0.5, 0.0, 3.0, 0.1).setGroup("Humanize");
    
    private final BoolValue aimLock = (BoolValue) new BoolValue("Aim Lock (Focus 1 Target)", false).setGroup("Aim Lock");

    private final BoolValue showTargetEsp = (BoolValue) new BoolValue("Show Target ESP", false).setGroup("Visuals");
    private final ColorValue targetColor = (ColorValue) new ColorValue("ESP Color", 0xFFFF0000).setGroup("Visuals");

    private Entity currentTarget = null;
    private Entity lockedTarget = null;
    
    private float nextYaw;
    private float nextPitch;
    
    // Noise state for humanization
    private double noiseX = 0;
    private double noiseY = 0;
    private double noiseZ = 0;
    private long lastNoiseUpdate = 0;
    
    // Dynamic part state
    private String currentDynamicPart = "Chest";
    private long lastDynamicSwitch = 0;
    
    // Interpolation state
    private double interpolatedAimY = -1;
    private Entity lastInterpolatedEntity = null;

    public AimAssist() {
        super("Aim Assist", "Smooth, legitimate-looking combat tracking.", Category.COMBAT, false);

        addValue(targetPlayers);
        addValue(targetHostiles);
        addValue(targetPassives);
        addValue(invisible);

        addValue(range);
        addValue(fov);
        addValue(targetPart);
        addValue(dynamicSpeed);

        addValue(rotationSpeed);
        addValue(ignoreMouseInput);

        addValue(clickOnly);
        addValue(weaponOnly);
        addValue(humanizeNoise);
        
        addValue(aimLock);
        
        addValue(showTargetEsp);
        addValue(targetColor);

        WorldRenderEvents.AFTER_ENTITIES.register(this::onWorldRender);
    }

    @Override
    public void onEnable() {
        currentTarget = null;
        lockedTarget = null;
        lastInterpolatedEntity = null;
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        lockedTarget = null;
        lastInterpolatedEntity = null;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        currentTarget = null;
        
        if (mc.world == null || mc.player == null) return;
        if (mc.currentScreen != null) return;
        
        if (clickOnly.get() && !mc.options.attackKey.isPressed()) {
            return;
        }
        
        if (weaponOnly.get()) {
            net.minecraft.item.Item mainHand = mc.player.getMainHandStack().getItem();
            if (!(mainHand instanceof net.minecraft.item.SwordItem) && !(mainHand instanceof net.minecraft.item.AxeItem)) {
                lockedTarget = null; // Losing the weapon breaks the lock
                return;
            }
        }
        
        double maxRangeSq = range.asDouble() * range.asDouble();
        double fovLimit = fov.asDouble() / 2.0;

        // Process Lock On Hit (Only initiates when aiming at an entity and clicking)
        if (aimLock.get() && mc.crosshairTarget != null && mc.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.ENTITY) {
            Entity hitEnt = ((net.minecraft.util.hit.EntityHitResult) mc.crosshairTarget).getEntity();
            if (mc.options.attackKey.isPressed() && isValidTarget(hitEnt)) {
                lockedTarget = hitEnt;
            }
        }
        
        // Validate current lock
        if (lockedTarget != null) {
            if (!isValidTarget(lockedTarget) || lockedTarget.isRemoved()) {
                lockedTarget = null;
            }
        }

        // Target Selection
        if (aimLock.get()) {
            if (lockedTarget != null) {
                double distSq = mc.player.squaredDistanceTo(lockedTarget);
                double angle = getAngleToLookVec(getAimPoint(lockedTarget));
                if (distSq <= maxRangeSq && angle <= fovLimit && mc.player.canSee(lockedTarget)) {
                    currentTarget = lockedTarget;
                }
            }
        } else {
            double closestAngle = Double.MAX_VALUE;
            
            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player) continue;
                if (!isValidTarget(e)) continue;
                
                double distSq = mc.player.squaredDistanceTo(e);
                if (distSq > maxRangeSq) continue;
                
                Vec3d aimPoint = getAimPoint(e);
                double angle = getAngleToLookVec(aimPoint);
                
                if (angle <= fovLimit && angle < closestAngle) {
                    if (mc.player.canSee(e)) {
                        closestAngle = angle;
                        currentTarget = e;
                    }
                }
            }
        }

        if (currentTarget != null) {
            Vec3d aimPoint = getAimPoint(currentTarget);
            
            // Apply Humanize Noise
            double noiseLvl = humanizeNoise.asDouble();
            if (noiseLvl > 0) {
                long timeNow = System.currentTimeMillis();
                if (timeNow - lastNoiseUpdate > 50) {
                    noiseX = (Math.random() - 0.5) * noiseLvl * 0.2;
                    noiseY = (Math.random() - 0.5) * noiseLvl * 0.2;
                    noiseZ = (Math.random() - 0.5) * noiseLvl * 0.2;
                    lastNoiseUpdate = timeNow;
                }
                aimPoint = aimPoint.add(noiseX, noiseY, noiseZ);
            }
            
            float[] needed = getNeededRotations(aimPoint);
            float currentYaw = mc.player.getYaw();
            float currentPitch = mc.player.getPitch();
            
            // Use a slightly more responsive turn rate without introducing jitter.
            float speed = (float) rotationSpeed.asDouble() / 10f;
            
            nextYaw = smoothlyTurn(currentYaw, needed[0], speed);
            nextPitch = smoothlyTurn(currentPitch, needed[1], speed);
        } else {
            lastInterpolatedEntity = null; // Reset interpolator when we lose target
        }
    }
    
    @EventTarget
    public void onMouseUpdate(MouseUpdateEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (currentTarget == null || mc.player == null) {
            return;
        }

        float curYaw = mc.player.getYaw();
        float curPitch = mc.player.getPitch();
        
        int diffYaw = (int)(nextYaw - curYaw);
        int diffPitch = (int)(nextPitch - curPitch);

        // Precise hitbox intersection check for target validation.
        boolean facing = false;
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.ENTITY) {
            if (((net.minecraft.util.hit.EntityHitResult) mc.crosshairTarget).getEntity() == currentTarget) {
                facing = true;
            }
        }
        
        if (diffYaw == 0 && diffPitch == 0 && !facing) {
            diffYaw = nextYaw < curYaw ? -1 : 1;
            diffPitch = nextPitch < curPitch ? -1 : 1;
        }

        double inputFactor = 1.0 - (ignoreMouseInput.asDouble() / 100.0);
        int mouseInputX = (int)(event.getDeltaX() * inputFactor);
        int mouseInputY = (int)(event.getDeltaY() * inputFactor);

        event.setDeltaX(mouseInputX + diffYaw);
        event.setDeltaY(mouseInputY + diffPitch);
    }
    
    private boolean isValidTarget(Entity e) {
        if (!e.isAlive()) return false;
        if (e.isInvisible() && !invisible.get()) return false;
        if (!(e instanceof LivingEntity)) return false;
        
        if (e instanceof PlayerEntity) return targetPlayers.get();
        if (e instanceof HostileEntity) return targetHostiles.get();
        if (e instanceof PassiveEntity) return targetPassives.get();
        
        return false;
    }
    
    private Vec3d getAimPoint(Entity e) {
        Box box = e.getBoundingBox();
        String part = targetPart.get();
        
        long timeNow = System.currentTimeMillis();
        
        if ("Random".equals(part)) {
            // Pick a random part every 2 seconds
            if ((timeNow / 2000) % 3 == 0) part = "Head";
            else if ((timeNow / 2000) % 3 == 1) part = "Chest";
            else part = "Legs";
        } else if ("Dynamic".equals(part)) {
            // Switch speed scaled by dynamicSpeed slider
            double switchDelayMs = 2500.0 - (dynamicSpeed.asDouble() * 100.0);
            if (timeNow - lastDynamicSwitch > switchDelayMs + Math.random() * 500) { 
                double rand = Math.random();
                if (rand < 0.70) currentDynamicPart = "Chest"; 
                else if (rand < 0.90) currentDynamicPart = "Head"; 
                else currentDynamicPart = "Legs"; 
                lastDynamicSwitch = timeNow;
            }
            part = currentDynamicPart;
        }
        
        double x = box.minX + (box.maxX - box.minX) / 2.0;
        double z = box.minZ + (box.maxZ - box.minZ) / 2.0;
        double targetY;
        
        if ("Head".equals(part)) {
            targetY = e.getEyeY() - 0.1;
        } else if ("Legs".equals(part)) {
            targetY = box.minY + 0.2;
        } else {
            // Chest / Center
            targetY = box.minY + (box.maxY - box.minY) / 2.0;
        }
        
        // Smooth interpolation for the Y coordinate
        if (lastInterpolatedEntity != e || interpolatedAimY == -1) {
            interpolatedAimY = targetY;
            lastInterpolatedEntity = e;
        } else {
            // Transition speed based on slider
            double transitionFactor = dynamicSpeed.asDouble() / 40.0;
            double diff = targetY - interpolatedAimY;
            interpolatedAimY += diff * transitionFactor;
            
            // Snap to prevent infinite micro-adjustments
            if (Math.abs(diff) < 0.02) {
                interpolatedAimY = targetY;
            }
        }
        
        return new Vec3d(x, interpolatedAimY, z);
    }

    private void onWorldRender(WorldRenderContext context) {
        if (!isEnabled() || !showTargetEsp.get() || currentTarget == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

        net.minecraft.client.render.Tessellator tessellator = net.minecraft.client.render.Tessellator.getInstance();
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.RENDERTYPE_LINES);
        com.mojang.blaze3d.systems.RenderSystem.lineWidth(2.0f);
        
        net.minecraft.client.render.BufferBuilder buffer = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.DEBUG_LINES, net.minecraft.client.render.VertexFormats.LINES);
        
        int color = targetColor.get();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        Box box = currentTarget.getBoundingBox().offset(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);
        // Inflate slightly
        box = box.expand(0.05);
        
        addLine(buffer, matrices.peek(), box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, 255);
        addLine(buffer, matrices.peek(), box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, r, g, b, 255);
        addLine(buffer, matrices.peek(), box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, 255);
        addLine(buffer, matrices.peek(), box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, r, g, b, 255);
        
        addLine(buffer, matrices.peek(), box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, 255);
        addLine(buffer, matrices.peek(), box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, 255);
        addLine(buffer, matrices.peek(), box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, 255);
        addLine(buffer, matrices.peek(), box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, r, g, b, 255);
        
        addLine(buffer, matrices.peek(), box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, r, g, b, 255);
        addLine(buffer, matrices.peek(), box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, 255);
        addLine(buffer, matrices.peek(), box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, r, g, b, 255);
        addLine(buffer, matrices.peek(), box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, 255);
        
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buffer.end());
        com.mojang.blaze3d.systems.RenderSystem.lineWidth(1.0f);

        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private void addLine(VertexConsumer consumer, MatrixStack.Entry entry, double x1, double y1, double z1, double x2, double y2, double z2, int r, int g, int b, int a) {
        consumer.vertex(entry, (float)x1, (float)y1, (float)z1).color(r, g, b, a).normal(entry, (float)(x2 - x1), (float)(y2 - y1), (float)(z2 - z1));
        consumer.vertex(entry, (float)x2, (float)y2, (float)z2).color(r, g, b, a).normal(entry, (float)(x2 - x1), (float)(y2 - y1), (float)(z2 - z1));
    }

    private float[] getNeededRotations(Vec3d vec) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d eyes = mc.player.getPos().add(0, mc.player.getStandingEyeHeight(), 0);

        double diffX = vec.x - eyes.x;
        double diffZ = vec.z - eyes.z;
        double yaw = Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0;

        double diffY = vec.y - eyes.y;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        double pitch = -Math.toDegrees(Math.atan2(diffY, diffXZ));

        return new float[] { MathHelper.wrapDegrees((float) yaw), MathHelper.wrapDegrees((float) pitch) };
    }

    private double getAngleToLookVec(Vec3d vec) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float[] needed = getNeededRotations(vec);
        float currentYaw = MathHelper.wrapDegrees(mc.player.getYaw());
        float currentPitch = MathHelper.wrapDegrees(mc.player.getPitch());
        
        float yawDiff = Math.abs(currentYaw - needed[0]);
        if (yawDiff > 180) yawDiff = 360 - yawDiff;
        
        float pitchDiff = Math.abs(currentPitch - needed[1]);
        
        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    private float smoothlyTurn(float current, float target, float speed) {
        float diff = MathHelper.wrapDegrees(target - current);
        if (Math.abs(diff) < speed) {
            return current + diff;
        }
        return current + (diff > 0 ? speed : -speed);
    }
}
