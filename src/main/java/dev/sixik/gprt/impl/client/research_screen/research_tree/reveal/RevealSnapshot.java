package dev.sixik.gprt.impl.client.research_screen.research_tree.reveal;

import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Texture-backed immutable snapshot captured from one research node widget.
 */
public record RevealSnapshot(int nodeId,
                             int textureId,
                             int width,
                             int height,
                             float sourceScreenX,
                             float sourceScreenY,
                             float sourceScreenWidth,
                             float sourceScreenHeight,
                             CaptureSource source,
                             @Nullable TextureTarget target,
                             @Nullable ResourceLocation textureLocation
) {
    public enum CaptureSource {
        SCREEN,
        OFFSCREEN
    }
}
