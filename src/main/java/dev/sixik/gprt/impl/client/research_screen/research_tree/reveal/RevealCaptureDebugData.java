package dev.sixik.gprt.impl.client.research_screen.research_tree.reveal;

import org.jetbrains.annotations.Nullable;

/**
 * Diagnostic snapshot of both reveal capture paths plus the screen-space crop geometry.
 * <p>
 * This exists purely for debugging capture bugs:
 * it lets the screen preview both OFFSCREEN and SCREEN textures side-by-side and inspect the
 * exact crop rectangle that was used for the screen-based capture.
 * </p>
 */
public record RevealCaptureDebugData(
        @Nullable RevealSnapshot offscreenSnapshot,
        @Nullable RevealSnapshot screenSnapshot,
        float screenX,
        float screenY,
        float screenWidth,
        float screenHeight,
        int framebufferWidth,
        int framebufferHeight,
        int guiWidth,
        int guiHeight,
        float framebufferScaleX,
        float framebufferScaleY,
        int captureX,
        int captureTop,
        int captureRight,
        int captureBottom,
        int screenshotCaptureY,
        boolean usedYFlip
) {
}
