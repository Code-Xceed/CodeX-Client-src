package com.codex.client.gui.navigator;

import net.minecraft.client.gui.DrawContext;

public interface IPreviewable {
    void renderPreview(DrawContext context, float tickDelta, int previewX, int previewY, int previewWidth, int previewHeight);
}
