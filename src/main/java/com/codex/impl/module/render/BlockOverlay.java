package com.codex.impl.module.render;

import com.codex.api.event.EventTarget;
import com.codex.api.module.Category;
import com.codex.api.module.Module;
import com.codex.api.value.BoolValue;
import com.codex.api.value.ColorValue;
import com.codex.api.value.NumberValue;
import com.codex.client.gui.navigator.IPreviewable;
import com.codex.client.utils.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;

public class BlockOverlay extends Module implements IPreviewable {

    private final BoolValue enableOutline = (BoolValue) new BoolValue("Enable Outline", true).setGroup("Outline");
    private final ColorValue outlineColor = (ColorValue) new ColorValue("Outline Color", 0xFF000000).setGroup("Outline");
    private final NumberValue outlineThickness = (NumberValue) new NumberValue("Thickness", 2.0, 0.5, 10.0, 0.5).setGroup("Outline");
    private final BoolValue rainbowOutline = (BoolValue) new BoolValue("Rainbow Outline", false).setGroup("Outline");
    
    private final BoolValue enableFill = (BoolValue) new BoolValue("Enable Fill", true).setGroup("Fill");
    private final ColorValue fillColor = (ColorValue) new ColorValue("Fill Color", 0x40000000).setGroup("Fill"); // Semi-transparent black
    private final BoolValue rainbowFill = (BoolValue) new BoolValue("Rainbow Fill", false).setGroup("Fill");

    private final BoolValue smoothAnimation = (BoolValue) new BoolValue("Smooth Animation", true).setGroup("Advanced");
    private final NumberValue animationSpeed = (NumberValue) new NumberValue("Animation Speed", 14.0, 1.0, 30.0, 1.0).setGroup("Advanced");
    private final BoolValue ignoreDepth = (BoolValue) new BoolValue("Ignore Depth (See through walls)", false).setGroup("Advanced");

    // Animation state
    private BlockPos lastPos = null;
    private float animX, animY, animZ;
    private float animW, animH, animD;
    private float animProgress = 0f;
    private long lastFrameTime = 0;

    public BlockOverlay() {
        super("Block Overlay", "Custom block selection visuals.", Category.RENDER, false, false); // canBind = false

        addValue(enableOutline);
        addValue(outlineColor);
        addValue(outlineThickness);
        addValue(rainbowOutline);
        
        addValue(enableFill);
        addValue(fillColor);
        addValue(rainbowFill);

        addValue(smoothAnimation);
        addValue(ignoreDepth);
        
        // Register the Fabric API event listener directly inside the module to cancel vanilla outline
        WorldRenderEvents.BLOCK_OUTLINE.register(this::onBlockOutline);
    }

    private boolean onBlockOutline(WorldRenderContext context, WorldRenderContext.BlockOutlineContext outlineContext) {
        if (!isEnabled()) {
            return true; // Let vanilla render
        }
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            lastPos = null;
            return false; // Hide completely
        }
        
        BlockHitResult blockHit = (BlockHitResult) mc.crosshairTarget;
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        
        if (state.isAir()) {
            lastPos = null;
            return false;
        }

        VoxelShape shape = state.getOutlineShape(mc.world, pos);
        if (shape.isEmpty()) {
            lastPos = null;
            return false;
        }
        
        Box box = shape.getBoundingBox();

        long timeNow = System.currentTimeMillis();
        if (lastFrameTime == 0) lastFrameTime = timeNow;
        float frameDelta = (timeNow - lastFrameTime) / 50.0f;
        lastFrameTime = timeNow;

        // Smooth Interpolation Logic with Z-Fighting inflation
        float inflate = 0.002f;
        float targetX = pos.getX() + (float)box.minX - inflate;
        float targetY = pos.getY() + (float)box.minY - inflate;
        float targetZ = pos.getZ() + (float)box.minZ - inflate;
        float targetW = (float)(box.maxX - box.minX) + (inflate * 2);
        float targetH = (float)(box.maxY - box.minY) + (inflate * 2);
        float targetD = (float)(box.maxZ - box.minZ) + (inflate * 2);

        if (smoothAnimation.get()) {
            if (lastPos == null || !lastPos.equals(pos)) {
                if (lastPos == null) {
                    animX = targetX; animY = targetY; animZ = targetZ;
                    animW = targetW; animH = targetH; animD = targetD;
                    animProgress = 0f;
                }
                lastPos = pos;
            }
            
            float speed = (float) animationSpeed.asDouble();
            float smoothFactor = 1.0f - (float)Math.pow(1.0 - (speed / 40.0), frameDelta);
            smoothFactor = MathHelper.clamp(smoothFactor, 0.01f, 1.0f);
            
            animX += (targetX - animX) * smoothFactor;
            animY += (targetY - animY) * smoothFactor;
            animZ += (targetZ - animZ) * smoothFactor;
            animW += (targetW - animW) * smoothFactor;
            animH += (targetH - animH) * smoothFactor;
            animD += (targetD - animD) * smoothFactor;
            
            animProgress += (1.0f - animProgress) * smoothFactor;
        } else {
            animX = targetX; animY = targetY; animZ = targetZ;
            animW = targetW; animH = targetH; animD = targetD;
            animProgress = 1.0f;
            lastPos = pos;
        }

        renderCustomOutline(context.matrixStack(), mc.gameRenderer.getCamera(), context.tickCounter().getTickDelta(true));
        
        return false; // Cancel vanilla outline
    }

    private void renderCustomOutline(MatrixStack matrices, Camera camera, float tickDelta) {
        if (animProgress < 0.01f) return;

        double camX = camera.getPos().x;
        double camY = camera.getPos().y;
        double camZ = camera.getPos().z;

        float drawX = (float) (animX - camX);
        float drawY = (float) (animY - camY);
        float drawZ = (float) (animZ - camZ);

        int outColor = rainbowOutline.get() ? getRainbowColor(1.0f) : outlineColor.get();
        int fColor = rainbowFill.get() ? getRainbowColor(0.25f) : fillColor.get(); // 0.25 alpha for rainbow fill
        
        // Ensure fill has some transparency if user set it to solid
        if (rainbowFill.get()) {
             fColor = (0x40 << 24) | (fColor & 0x00FFFFFF);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (ignoreDepth.get()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        net.minecraft.client.render.Tessellator tessellator = net.minecraft.client.render.Tessellator.getInstance();
        
        matrices.push();
        matrices.translate(drawX, drawY, drawZ);
        
        // Draw Fill
        if (enableFill.get()) {
            RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);
            net.minecraft.client.render.BufferBuilder buffer = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
            drawBoxFill(matrices, buffer, animW, animH, animD, fColor);
            net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        // Draw Outline
        if (enableOutline.get()) {
            RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.RENDERTYPE_LINES);
            RenderSystem.lineWidth((float) outlineThickness.asDouble());
            net.minecraft.client.render.BufferBuilder buffer = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.DEBUG_LINES, net.minecraft.client.render.VertexFormats.LINES);
            drawBoxOutline(matrices, buffer, animW, animH, animD, outColor);
            net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.lineWidth(1.0f);
        }

        matrices.pop();

        if (ignoreDepth.get()) {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.disableBlend();
    }

    private int getRainbowColor(float saturation) {
        float hue = (System.currentTimeMillis() % 4000L) / 4000.0f;
        return java.awt.Color.HSBtoRGB(hue, saturation, 1.0f);
    }

    private void drawBoxFill(MatrixStack matrices, VertexConsumer consumer, float w, float h, float d, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        
        MatrixStack.Entry entry = matrices.peek();
        
        // Down
        consumer.vertex(entry, 0, 0, 0).color(r, g, b, a);
        consumer.vertex(entry, w, 0, 0).color(r, g, b, a);
        consumer.vertex(entry, w, 0, d).color(r, g, b, a);
        consumer.vertex(entry, 0, 0, d).color(r, g, b, a);
        // Up
        consumer.vertex(entry, 0, h, 0).color(r, g, b, a);
        consumer.vertex(entry, 0, h, d).color(r, g, b, a);
        consumer.vertex(entry, w, h, d).color(r, g, b, a);
        consumer.vertex(entry, w, h, 0).color(r, g, b, a);
        // North
        consumer.vertex(entry, 0, 0, 0).color(r, g, b, a);
        consumer.vertex(entry, 0, h, 0).color(r, g, b, a);
        consumer.vertex(entry, w, h, 0).color(r, g, b, a);
        consumer.vertex(entry, w, 0, 0).color(r, g, b, a);
        // South
        consumer.vertex(entry, 0, 0, d).color(r, g, b, a);
        consumer.vertex(entry, w, 0, d).color(r, g, b, a);
        consumer.vertex(entry, w, h, d).color(r, g, b, a);
        consumer.vertex(entry, 0, h, d).color(r, g, b, a);
        // West
        consumer.vertex(entry, 0, 0, 0).color(r, g, b, a);
        consumer.vertex(entry, 0, 0, d).color(r, g, b, a);
        consumer.vertex(entry, 0, h, d).color(r, g, b, a);
        consumer.vertex(entry, 0, h, 0).color(r, g, b, a);
        // East
        consumer.vertex(entry, w, 0, 0).color(r, g, b, a);
        consumer.vertex(entry, w, h, 0).color(r, g, b, a);
        consumer.vertex(entry, w, h, d).color(r, g, b, a);
        consumer.vertex(entry, w, 0, d).color(r, g, b, a);
    }

    private void drawBoxOutline(MatrixStack matrices, VertexConsumer consumer, float w, float h, float d, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        if (a == 0) a = 255; // Default to opaque outline if 0 alpha passed accidentally
        
        MatrixStack.Entry entry = matrices.peek();
        
        // Bottom square
        addLine(consumer, entry, 0, 0, 0, w, 0, 0, r, g, b, a);
        addLine(consumer, entry, w, 0, 0, w, 0, d, r, g, b, a);
        addLine(consumer, entry, w, 0, d, 0, 0, d, r, g, b, a);
        addLine(consumer, entry, 0, 0, d, 0, 0, 0, r, g, b, a);
        
        // Top square
        addLine(consumer, entry, 0, h, 0, w, h, 0, r, g, b, a);
        addLine(consumer, entry, w, h, 0, w, h, d, r, g, b, a);
        addLine(consumer, entry, w, h, d, 0, h, d, r, g, b, a);
        addLine(consumer, entry, 0, h, d, 0, h, 0, r, g, b, a);
        
        // Pillars
        addLine(consumer, entry, 0, 0, 0, 0, h, 0, r, g, b, a);
        addLine(consumer, entry, w, 0, 0, w, h, 0, r, g, b, a);
        addLine(consumer, entry, w, 0, d, w, h, d, r, g, b, a);
        addLine(consumer, entry, 0, 0, d, 0, h, d, r, g, b, a);
    }

    private void addLine(VertexConsumer consumer, MatrixStack.Entry entry, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        consumer.vertex(entry, x1, y1, z1).color(r, g, b, a).normal(entry, x2 - x1, y2 - y1, z2 - z1);
        consumer.vertex(entry, x2, y2, z2).color(r, g, b, a).normal(entry, x2 - x1, y2 - y1, z2 - z1);
    }

    @Override
    public void renderPreview(DrawContext context, float tickDelta, int previewX, int previewY, int previewWidth, int previewHeight) {
        float cx = previewX + (previewWidth / 2.0f);
        float cy = previewY + (previewHeight / 2.0f);
        
        // Draw a simulated isometric 3D block in the 2D GUI context
        context.getMatrices().push();
        context.getMatrices().translate(cx, cy + 20, 0);
        
        // Isometric projection rotation (pitch then yaw)
        context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-30f));
        context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(45f));
        
        float scale = 40f;
        context.getMatrices().scale(scale, scale, scale);
        
        // Center the 1x1x1 block around the origin
        context.getMatrices().translate(-0.5f, -0.5f, -0.5f);

        int outColor = rainbowOutline.get() ? getRainbowColor(1.0f) : outlineColor.get();
        int fColor = rainbowFill.get() ? getRainbowColor(0.25f) : fillColor.get(); 
        if (rainbowFill.get()) fColor = (0x40 << 24) | (fColor & 0x00FFFFFF);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);

        net.minecraft.client.render.Tessellator tessellator = net.minecraft.client.render.Tessellator.getInstance();

        if (enableFill.get()) {
            net.minecraft.client.render.BufferBuilder buffer = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_COLOR);
            drawBoxFill(context.getMatrices(), buffer, 1f, 1f, 1f, fColor);
            net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        if (enableOutline.get()) {
            RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.RENDERTYPE_LINES);
            RenderSystem.lineWidth((float) outlineThickness.asDouble());
            net.minecraft.client.render.BufferBuilder buffer = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.DEBUG_LINES, net.minecraft.client.render.VertexFormats.LINES);
            drawBoxOutline(context.getMatrices(), buffer, 1f, 1f, 1f, outColor);
            net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.lineWidth(1.0f); // restore
        }
        
        RenderSystem.disableBlend();

        context.getMatrices().pop();
    }
}
