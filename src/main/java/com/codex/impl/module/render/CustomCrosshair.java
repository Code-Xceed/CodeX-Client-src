package com.codex.impl.module.render;

import com.codex.api.event.EventTarget;
import com.codex.api.event.events.UpdateEvent;
import com.codex.api.module.Category;
import com.codex.api.module.Module;
import com.codex.api.value.BoolValue;
import com.codex.api.value.ColorValue;
import com.codex.api.value.ModeValue;
import com.codex.api.value.NumberValue;
import com.codex.client.gui.navigator.IPreviewable;
import com.codex.client.utils.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;

public class CustomCrosshair extends Module implements IPreviewable {

    private final ModeValue shape = (ModeValue) new ModeValue("Shape", "Cross", "Cross", "Dot", "Circle", "Plus", "Gap Cross").setGroup("Shape");
    
    private final NumberValue length = (NumberValue) new NumberValue("Length", 5.0, 1.0, 20.0, 0.5).setGroup("Size");
    private final NumberValue gapSize = (NumberValue) new NumberValue("Gap Size", 3.0, 0.0, 15.0, 0.5).setGroup("Size");

    private final ModeValue colorMode = (ModeValue) new ModeValue("Color Mode", "Static", "Static", "Rainbow", "Health-Based", "Target Hover").setGroup("Color");
    private final ColorValue staticColor = (ColorValue) new ColorValue("Static Color", 0xFFFFFFFF).setGroup("Color");
    private final ColorValue hoverColor = (ColorValue) new ColorValue("Hover Color", 0xFFFF5555).setGroup("Color");
    private final BoolValue dynamicInvert = (BoolValue) new BoolValue("Vanilla Blend (Invert)", false).setGroup("Color");

    private final BoolValue outline = (BoolValue) new BoolValue("Outline", true).setGroup("Dynamic");
    private final NumberValue outlineThickness = (NumberValue) new NumberValue("Outline Thickness", 1.0, 0.5, 3.0, 0.5).setGroup("Dynamic");
    private final BoolValue expandOnMove = (BoolValue) new BoolValue("Expand on Move", false).setGroup("Dynamic");
    private final BoolValue expandOnClick = (BoolValue) new BoolValue("Expand on Click", false).setGroup("Dynamic");
    private final ModeValue targetShapeShift = (ModeValue) new ModeValue("Target ShapeShift", "None", "None", "Bracket", "Dot", "Circle").setGroup("Dynamic");
    private final BoolValue animateShapeShift = (BoolValue) new BoolValue("Animate ShapeShift", true).setGroup("Dynamic");

    private final BoolValue showHitmarker = (BoolValue) new BoolValue("Show Hitmarker", true).setGroup("Advanced");
    private final BoolValue hitmarkerSound = (BoolValue) new BoolValue("Hitmarker Sound", true).setGroup("Advanced");
    private final BoolValue disableInGui = (BoolValue) new BoolValue("Disable in GUI", true).setGroup("Advanced");
    private final BoolValue disableInThirdPerson = (BoolValue) new BoolValue("Hide in 3rd Person", true).setGroup("Advanced");

    private float currentExpand = 0f;
    private float hitmarkerTime = 0f;
    private float targetShiftAnim = 0f;
    private long lastFrameTime = 0;

    public CustomCrosshair() {
        super("Custom Crosshair", "Highly customizable crosshair system.", Category.RENDER, false, false);

        addValue(shape);
        addValue(length);
        addValue(gapSize);
        
        addValue(colorMode);
        addValue(staticColor);
        addValue(hoverColor);
        addValue(dynamicInvert);
        
        addValue(outline);
        addValue(outlineThickness);
        addValue(expandOnMove);
        addValue(expandOnClick);
        addValue(targetShapeShift);
        addValue(animateShapeShift);
        
        addValue(showHitmarker);
        addValue(hitmarkerSound);
        addValue(disableInGui);
        addValue(disableInThirdPerson);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        // Handled in render to ensure perfectly smooth animations decoupled from 20 TPS tick rate
    }

    public void onAttackEntity(Entity entity) {
        if (showHitmarker.get()) {
            hitmarkerTime = 1.0f;
        }
        if (hitmarkerSound.get()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.2f);
            }
        }
    }

    public boolean shouldCancelVanillaCrosshair() {
        if (!isEnabled()) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (disableInGui.get() && mc.currentScreen != null) return false;
        if (disableInThirdPerson.get() && mc.options != null && !mc.options.getPerspective().isFirstPerson()) return false;
        return true;
    }
    
    @Override
    public void renderPreview(DrawContext context, float tickDelta, int previewX, int previewY, int previewWidth, int previewHeight) {
        float cx = previewX + (previewWidth / 2.0f);
        float cy = previewY + (previewHeight / 2.0f);
        renderInternal(context, tickDelta, cx, cy, true);
    }

    public void render(DrawContext context, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        
        int scaledWidth = mc.getWindow().getScaledWidth();
        int scaledHeight = mc.getWindow().getScaledHeight();

        float centerX = scaledWidth / 2.0f;
        float centerY = scaledHeight / 2.0f;
        
        renderInternal(context, tickDelta, centerX, centerY, false);
    }
    
    private void renderInternal(DrawContext context, float tickDelta, float centerX, float centerY, boolean isPreview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isPreview && mc.player == null) return;
        
        long timeNow = System.currentTimeMillis();
        if (lastFrameTime == 0) lastFrameTime = timeNow;
        float frameDelta = (timeNow - lastFrameTime) / 50.0f;
        lastFrameTime = timeNow;

        float targetExpand = 0f;
        if (!isPreview && mc.player != null) {
            if (expandOnMove.get() && (Math.abs(mc.player.getVelocity().x) > 0.05 || Math.abs(mc.player.getVelocity().z) > 0.05)) {
                targetExpand += 4.0f;
            }
            if (expandOnClick.get() && mc.options.attackKey.isPressed()) {
                targetExpand += 3.0f;
            }
        }
        
        float smoothFactor = 1.0f - (float)Math.pow(1.0 - 0.4, frameDelta);
        currentExpand += (targetExpand - currentExpand) * Math.clamp(smoothFactor, 0.01f, 1.0f);

        if (hitmarkerTime > 0) {
            hitmarkerTime -= 0.03f * frameDelta; 
            if (hitmarkerTime < 0) hitmarkerTime = 0;
        }

        boolean isHoveringEntity = !isPreview && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY;
        float shiftTarget = isHoveringEntity ? 1.0f : 0.0f;
        if (animateShapeShift.get()) {
            targetShiftAnim += (shiftTarget - targetShiftAnim) * Math.clamp(smoothFactor * 1.5f, 0.01f, 1.0f);
        } else {
            targetShiftAnim = shiftTarget;
        }

        if (disableInGui.get() && mc.currentScreen != null && !isPreview) {
            return;
        }
        
        if (disableInThirdPerson.get() && mc.options != null && !mc.options.getPerspective().isFirstPerson() && !isPreview) {
            return;
        }

        context.getMatrices().push();
        // Translate to exact center coordinate to ensure symmetrical expansion regardless of resolution parity
        context.getMatrices().translate(centerX, centerY, 0);

        boolean doInvert = dynamicInvert.get();
        if (doInvert) {
            context.draw(); 
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                com.mojang.blaze3d.platform.GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR, 
                com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR, 
                com.mojang.blaze3d.platform.GlStateManager.SrcFactor.ONE, 
                com.mojang.blaze3d.platform.GlStateManager.DstFactor.ZERO
            );
        }

        int color = doInvert ? 0xFFFFFFFF : getColor(mc, isHoveringEntity, isPreview);
        float len = (float) length.asDouble();
        float thick = 1.0f; // Fixed thickness for professional PvP standard
        float gap = (float) gapSize.asDouble() + currentExpand;
        boolean hasOutline = outline.get() && !doInvert; 
        float outThick = (float) outlineThickness.asDouble();
        
        String s = shape.get();
        String shiftMode = targetShapeShift.get();
        
        if (!"None".equals(shiftMode) && targetShiftAnim > 0.01f) {
            drawShapeShift(context, len, thick, gap, outThick, color, hasOutline, shiftMode, targetShiftAnim, doInvert);
            int alpha = (color >> 24) & 0xFF;
            int newAlpha = (int) (alpha * (1.0f - targetShiftAnim));
            if (doInvert) {
                color = (0xFF << 24) | (newAlpha << 16) | (newAlpha << 8) | newAlpha;
            } else {
                color = (newAlpha << 24) | (color & 0x00FFFFFF);
            }
        }

        if ((color >> 24 & 0xFF) > 5 || (doInvert && (color & 0xFF) > 5)) {
            drawBaseShape(context, s, len, thick, gap, color, hasOutline, outThick, doInvert);
        }
        
        if (hitmarkerTime > 0 || (isPreview && showHitmarker.get() && (System.currentTimeMillis() % 2000 > 1000))) {
            float ease = isPreview ? 1.0f : hitmarkerTime * hitmarkerTime;
            int alpha = (int) (ease * 255.0f);
            int hmColor = doInvert ? ((0xFF << 24) | (alpha << 16) | (alpha << 8) | alpha) : ((alpha << 24) | 0xFFFFFF);
            float hmSize = 4.0f;
            float hmGap = gap + 4.0f;
            
            context.getMatrices().push();
            context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(45));
            
            drawCenteredFloatRect(context, -hmGap - (hmSize/2f), 0, hmSize, 1.0f, hmColor, false, 0, doInvert); // Left
            drawCenteredFloatRect(context, hmGap + (hmSize/2f), 0, hmSize, 1.0f, hmColor, false, 0, doInvert); // Right
            drawCenteredFloatRect(context, 0, -hmGap - (hmSize/2f), 1.0f, hmSize, hmColor, false, 0, doInvert); // Top
            drawCenteredFloatRect(context, 0, hmGap + (hmSize/2f), 1.0f, hmSize, hmColor, false, 0, doInvert); // Bottom
            
            context.getMatrices().pop();
        }

        if (doInvert) {
            context.draw(); 
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }

        context.getMatrices().pop();
    }

    private void drawBaseShape(DrawContext context, String s, float len, float thick, float gap, int color, boolean hasOutline, float outThick, boolean doInvert) {
        if ("Cross".equals(s) || "Gap Cross".equals(s) || "Plus".equals(s)) {
            boolean isPlus = "Plus".equals(s);
            float actualGap = isPlus ? 0 : gap;
            
            drawCenteredFloatRect(context, -actualGap - (len/2f), 0, len, thick, color, hasOutline, outThick, doInvert); // Left
            drawCenteredFloatRect(context, actualGap + (len/2f), 0, len, thick, color, hasOutline, outThick, doInvert); // Right
            drawCenteredFloatRect(context, 0, -actualGap - (len/2f), thick, len, color, hasOutline, outThick, doInvert); // Top
            drawCenteredFloatRect(context, 0, actualGap + (len/2f), thick, len, color, hasOutline, outThick, doInvert); // Bottom
            
            if ("Cross".equals(s)) {
                drawCenteredFloatRect(context, 0, 0, thick, thick, color, hasOutline, outThick, doInvert); // Center
            }
        } else if ("Dot".equals(s)) {
            drawCenteredFloatRect(context, 0, 0, thick, thick, color, hasOutline, outThick, doInvert);
        } else if ("Circle".equals(s)) {
            // True geometric circle
            float radius = len + gap;
            drawHollowCircle(context, 0, 0, radius, thick, color, hasOutline, outThick, doInvert);
        }
    }

    private void drawShapeShift(DrawContext context, float len, float thick, float gap, float outThick, int color, boolean hasOutline, String mode, float animProgress, boolean doInvert) {
        int alpha = (int) (animProgress * 255.0f);
        int fadeColor = doInvert ? ((0xFF << 24) | (alpha << 16) | (alpha << 8) | alpha) : ((alpha << 24) | (color & 0x00FFFFFF));

        if ("Bracket".equals(mode)) {
            float brLen = len * 0.8f;
            float brGap = gap + (1.0f - animProgress) * 5.0f;
            
            // Left Bracket
            drawCenteredFloatRect(context, -brGap - brLen/2f, -brLen, brLen, thick, fadeColor, hasOutline, outThick, doInvert); // Top
            drawCenteredFloatRect(context, -brGap - brLen/2f, brLen, brLen, thick, fadeColor, hasOutline, outThick, doInvert); // Bottom
            drawCenteredFloatRect(context, -brGap - brLen, 0, thick, brLen * 2 + thick, fadeColor, hasOutline, outThick, doInvert); // Spine
            
            // Right Bracket
            drawCenteredFloatRect(context, brGap + brLen/2f, -brLen, brLen, thick, fadeColor, hasOutline, outThick, doInvert); // Top
            drawCenteredFloatRect(context, brGap + brLen/2f, brLen, brLen, thick, fadeColor, hasOutline, outThick, doInvert); // Bottom
            drawCenteredFloatRect(context, brGap + brLen, 0, thick, brLen * 2 + thick, fadeColor, hasOutline, outThick, doInvert); // Spine
        } else if ("Dot".equals(mode)) {
            float dotSize = thick + 1.0f; // Slightly larger for emphasis
            drawCenteredFloatRect(context, 0, 0, dotSize, dotSize, fadeColor, hasOutline, outThick, doInvert); // Center dot
        } else if ("Circle".equals(mode)) {
            // Morphing circle effect
            float radius = (len + gap) * animProgress;
            drawHollowCircle(context, 0, 0, radius, thick, fadeColor, hasOutline, outThick, doInvert);
        }
    }
    
    private void drawCenteredFloatRect(DrawContext context, float cx, float cy, float w, float h, int color, boolean outline, float outThick, boolean doInvert) {
        if (outline && !doInvert) {
            drawSolidFloatRect(context, cx, cy, w + outThick * 2, h + outThick * 2, 0xFF000000);
        }
        drawSolidFloatRect(context, cx, cy, w, h, color);
    }
    
    private void drawSolidFloatRect(DrawContext context, float cx, float cy, float w, float h, int color) {
        float minX = cx - (w / 2.0f);
        float minY = cy - (h / 2.0f);
        float maxX = cx + (w / 2.0f);
        float maxY = cy + (h / 2.0f);
        
        // Render float geometry precisely using matrices scaling by a large factor to avoid integer casting issues in native context.fill()
        context.getMatrices().push();
        context.getMatrices().scale(1/1000f, 1/1000f, 1f);
        context.fill((int)(minX * 1000), (int)(minY * 1000), (int)(maxX * 1000), (int)(maxY * 1000), color);
        context.getMatrices().pop();
    }
    
    private void drawHollowCircle(DrawContext context, float cx, float cy, float radius, float thickness, int color, boolean outline, float outThick, boolean doInvert) {
        if (radius <= 0) return;
        
        if (outline && !doInvert) {
            drawTorus(context, cx, cy, radius - outThick, radius + thickness + outThick, 0xFF000000);
        }
        drawTorus(context, cx, cy, radius, radius + thickness, color);
    }
    
    private void drawTorus(DrawContext context, float cx, float cy, float innerRadius, float outerRadius, int color) {
        int segments = 120; // Maximum Quality Smooth Circle
        float angleStep = (float) (2 * Math.PI / segments);
        float avgRad = (innerRadius + outerRadius) / 2.0f;
        float actualThick = outerRadius - innerRadius;
        
        // Draw precise rotated rectangles to simulate a perfectly smooth circle torus
        for (int i = 0; i < segments; i++) {
            float angle = i * angleStep;
            float length = (float) (2 * Math.PI * avgRad / segments) + 0.5f; // Add tiny overlap to prevent gaps
            
            float x = cx + (float)(Math.cos(angle) * avgRad);
            float y = cy + (float)(Math.sin(angle) * avgRad);
            
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation((float)(angle + Math.PI/2)));
            drawSolidFloatRect(context, 0, 0, length, actualThick, color);
            context.getMatrices().pop();
        }
    }

    private int getColor(MinecraftClient mc, boolean isHoveringEntity, boolean isPreview) {
        String mode = colorMode.get();
        if ("Static".equals(mode)) {
            return staticColor.get();
        } else if ("Rainbow".equals(mode)) {
            // Using the new global Chroma Sync from RenderUtils to ensure the crosshair perfectly matches the rest of the HUD
            int scaledWidth = mc.getWindow().getScaledWidth();
            int scaledHeight = mc.getWindow().getScaledHeight();
            return RenderUtils.getChromaColor(scaledWidth / 2.0f, scaledHeight / 2.0f, 1.0f);
        } else if ("Target Hover".equals(mode)) {
            if (isHoveringEntity || (isPreview && System.currentTimeMillis() % 2000 > 1000)) {
                return hoverColor.get();
            }
            return staticColor.get();
        } else if ("Health-Based".equals(mode)) {
            if (mc.player != null) {
                float healthPct = mc.player.getHealth() / mc.player.getMaxHealth();
                if (healthPct > 0.6f) return 0xFF00FF00;
                if (healthPct > 0.3f) return 0xFFFFFF00;
                return 0xFFFF0000;
            }
            return staticColor.get();
        }
        return staticColor.get();
    }
}