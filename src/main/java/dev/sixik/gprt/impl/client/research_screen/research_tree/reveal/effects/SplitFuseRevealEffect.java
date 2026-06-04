package dev.sixik.gprt.impl.client.research_screen.research_tree.reveal.effects;

import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.gui.GuiGraphics;
import dev.sixik.gprt.impl.client.research_screen.research_tree.reveal.ResearchRevealEffect;
import dev.sixik.gprt.impl.client.research_screen.research_tree.reveal.RevealEffectInstance;
import dev.sixik.gprt.impl.client.research_screen.research_tree.reveal.RevealInitContext;
import dev.sixik.gprt.impl.client.research_screen.research_tree.reveal.RevealRenderBackend;
import dev.sixik.gprt.impl.client.research_screen.research_tree.reveal.RevealRenderContext;
import dev.sixik.gprt.impl.client.research_screen.research_tree.reveal.RevealSnapshot;
import org.jetbrains.annotations.Nullable;

/**
 * First concrete reveal effect implementation backed by the existing split/fuse animation.
 * <p>
 * The currently enabled visual path still uses the proven direct draw version. The composite/FBO
 * pass stays behind a flag so we can evolve it safely without regressing the live animation.
 * </p>
 */
public final class SplitFuseRevealEffect implements ResearchRevealEffect {
    @Override
    public RevealEffectInstance createInstance(RevealInitContext context) {
        return new Instance(context.backend(), context.snapshot());
    }

    private static final class Instance implements RevealEffectInstance {
        private static final float COMPOSITE_PADDING = 12f;
        private static final boolean ENABLE_COMPOSITE_PASS = false;

        private final RevealRenderBackend backend;
        private final RevealSnapshot snapshot;
        private @Nullable TextureTarget compositeTarget;

        private Instance(RevealRenderBackend backend, RevealSnapshot snapshot) {
            this.backend = backend;
            this.snapshot = snapshot;
        }

        @Override
        public void render(RevealRenderContext context) {
            CompositeFrame frame = buildCompositeFrame(context);

            float intro = easeOutCubic(clamp01(context.progress01() / 0.18f));
            float fuseProgress = easeOutCubic(clamp01((context.progress01() - 0.10f) / 0.58f));
            float fadeOut = 1f - clamp01((context.progress01() - 0.90f) / 0.10f);
            float alphaMul = clamp01(intro * fadeOut);

            int backgroundColor = withAlpha(context.backgroundColor(), Math.round(230f * alphaMul));
            int borderColor = withAlpha(context.borderColor(), Math.round(255f * alphaMul));
            int accentColor = withAlpha(context.accentColor(), Math.round(250f * alphaMul));
            int snapshotColor = withAlpha(0xFFFFFFFF, Math.round(255f * alphaMul));

            if (ENABLE_COMPOSITE_PASS && renderCompositeFrame(frame, fuseProgress, backgroundColor, borderColor, accentColor)) {
                backend.drawScreenQuad(
                        context.guiContext(),
                        compositeTarget.getColorTextureId(),
                        frame.screenX(),
                        frame.screenY(),
                        frame.targetWidth(),
                        frame.targetHeight(),
                        0xFFFFFFFF
                );
                return;
            }

            drawCompositePass(
                    context.guiContext().graphics,
                    frame,
                    frame.screenX(),
                    frame.screenY(),
                    fuseProgress,
                    backgroundColor,
                    snapshotColor,
                    borderColor,
                    accentColor
            );
        }

        @Override
        public void dispose() {
            if (compositeTarget != null) {
                backend.releaseTarget(compositeTarget);
                compositeTarget = null;
            }
        }

        private CompositeFrame buildCompositeFrame(RevealRenderContext context) {
            float safeScale = Math.max(0.35f, context.scale());
            float renderWidth = context.width() * safeScale;
            float renderHeight = context.height() * safeScale;
            float halfWidth = renderWidth * 0.5f;
            float splitDistance = lerp(renderWidth * 0.46f, 0f, easeOutCubic(clamp01((context.progress01() - 0.10f) / 0.58f)));
            float targetWidth = renderWidth + splitDistance + COMPOSITE_PADDING * 2f;
            float targetHeight = renderHeight + COMPOSITE_PADDING * 2f;
            float localBaseX = (targetWidth - renderWidth) * 0.5f;
            float localBaseY = (targetHeight - renderHeight) * 0.5f;
            float screenX = context.centerX() + context.translateX() - targetWidth * 0.5f;
            float screenY = context.centerY() + context.translateY() - targetHeight * 0.5f;
            return new CompositeFrame(
                    renderWidth,
                    renderHeight,
                    halfWidth,
                    splitDistance,
                    targetWidth,
                    targetHeight,
                    localBaseX,
                    localBaseY,
                    screenX,
                    screenY
            );
        }

        private boolean renderCompositeFrame(CompositeFrame frame,
                                             float fuseProgress,
                                             int backgroundColor,
                                             int borderColor,
                                             int accentColor
        ) {
            int targetWidth = Math.max(1, Math.round(frame.targetWidth()));
            int targetHeight = Math.max(1, Math.round(frame.targetHeight()));
            TextureTarget target = ensureCompositeTarget(targetWidth, targetHeight);
            if (target == null) {
                return false;
            }

            return backend.renderToTarget(target, targetWidth, targetHeight, guiGraphics -> {
                drawCompositePass(guiGraphics, frame, 0f, 0f, fuseProgress, backgroundColor, withAlpha(0xFFFFFFFF, 0xFF), borderColor, accentColor);
                guiGraphics.flush();
            });
        }

        private @Nullable TextureTarget ensureCompositeTarget(int width, int height) {
            if (compositeTarget != null && compositeTarget.width == width && compositeTarget.height == height) {
                return compositeTarget;
            }

            dispose();
            compositeTarget = backend.createTarget(width, height);
            return compositeTarget;
        }

        private void drawCompositePass(GuiGraphics guiGraphics,
                                       CompositeFrame frame,
                                       float originX,
                                       float originY,
                                       float fuseProgress,
                                       int backgroundColor,
                                       int snapshotColor,
                                       int borderColor,
                                       int accentColor
        ) {
            float renderWidth = frame.renderWidth();
            float renderHeight = frame.renderHeight();
            float baseX = originX + frame.localBaseX();
            float baseY = originY + frame.localBaseY();
            float halfWidth = frame.halfWidth();
            float splitDistance = frame.splitDistance();
            float seamWidth = Math.max(2.5f, renderWidth * 0.030f);
            int seamCoreColor = withAlpha(mixColors(accentColor, 0xFFFFFFFF, 0.45f), (accentColor >>> 24) & 0xFF);
            int seamGlowColor = withAlpha(accentColor, Math.round(((accentColor >>> 24) & 0xFF) * 0.52f));

            fillRect(guiGraphics, baseX - splitDistance * 0.5f, baseY, halfWidth, renderHeight, backgroundColor);
            fillRect(guiGraphics, baseX + halfWidth + splitDistance * 0.5f, baseY, halfWidth, renderHeight, backgroundColor);

            drawTexturedHalf(guiGraphics, baseX - splitDistance * 0.5f, baseY, halfWidth, renderHeight, 0f, 0.5f, snapshotColor);
            drawTexturedHalf(guiGraphics, baseX + halfWidth + splitDistance * 0.5f, baseY, halfWidth, renderHeight, 0.5f, 1f, snapshotColor);

            drawHalfFrame(guiGraphics, baseX - splitDistance * 0.5f, baseY, halfWidth, renderHeight, true, borderColor, accentColor);
            drawHalfFrame(guiGraphics, baseX + halfWidth + splitDistance * 0.5f, baseY, halfWidth, renderHeight, false, borderColor, accentColor);

            float seamCenterX = baseX + renderWidth * 0.5f;
            float seamGlowHeight = renderHeight + 10f;
            fillRect(guiGraphics,
                    seamCenterX - seamWidth * 1.6f,
                    baseY - 5f,
                    seamWidth * 3.2f,
                    seamGlowHeight,
                    withAlpha(seamGlowColor, Math.round(((seamGlowColor >>> 24) & 0xFF) * clamp01((fuseProgress - 0.08f) / 0.30f)))
            );
            fillRect(guiGraphics,
                    seamCenterX - seamWidth * 0.55f,
                    baseY - 1f,
                    seamWidth * 1.1f,
                    renderHeight + 2f,
                    seamCoreColor
            );
        }

        private void drawTexturedHalf(GuiGraphics guiGraphics,
                                      float x,
                                      float y,
                                      float width,
                                      float height,
                                      float u0,
                                      float u1,
                                      int tintColor
        ) {
            if (width <= 0.5f || height <= 0.5f) {
                return;
            }

            backend.drawTexturedQuad(guiGraphics.pose().last().pose(), snapshot.textureId(), x, y, width, height, u0, 0f, u1, 1f, tintColor);
        }

        private void drawHalfFrame(GuiGraphics guiGraphics,
                                   float x,
                                   float y,
                                   float width,
                                   float height,
                                   boolean leftHalf,
                                   int borderColor,
                                   int accentColor
        ) {
            if (width <= 0.5f || height <= 0.5f) {
                return;
            }

            fillRect(guiGraphics, x, y, width, Math.max(2f, height * 0.08f), accentColor);
            fillRect(guiGraphics,
                    x + 5f,
                    y + Math.max(7f, height * 0.22f),
                    Math.max(10f, width * 0.46f),
                    Math.max(3f, height * 0.10f),
                    withAlpha(accentColor, Math.min(255, ((accentColor >>> 24) & 0xFF) / 2 + 40))
            );

            float iconSize = Math.min(height * 0.28f, width * 0.18f);
            if (iconSize >= 4f) {
                float iconX = leftHalf ? x + 6f : x + width - iconSize - 6f;
                float iconY = y + height - iconSize - 6f;
                fillRect(guiGraphics,
                        iconX,
                        iconY,
                        iconSize,
                        iconSize,
                        withAlpha(accentColor, Math.min(255, ((accentColor >>> 24) & 0xFF) / 2 + 64))
                );
            }

            float borderThickness = 1f;
            fillRect(guiGraphics, x, y, width, borderThickness, borderColor);
            fillRect(guiGraphics, x, y + height - borderThickness, width, borderThickness, borderColor);
            fillRect(guiGraphics, x, y, borderThickness, height, borderColor);
            fillRect(guiGraphics, x + width - borderThickness, y, borderThickness, height, borderColor);
        }

        private void fillRect(GuiGraphics guiGraphics, float x, float y, float width, float height, int color) {
            if (width <= 0.5f || height <= 0.5f || ((color >>> 24) & 0xFF) <= 0) {
                return;
            }
            guiGraphics.fill(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height), color);
        }

        private static float clamp01(float value) {
            return Math.max(0f, Math.min(1f, value));
        }

        private static float lerp(float start, float end, float progress) {
            return start + (end - start) * progress;
        }

        private static float easeOutCubic(float progress) {
            float inverse = 1f - clamp01(progress);
            return 1f - inverse * inverse * inverse;
        }

        private static int withAlpha(int color, int alpha) {
            return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
        }

        private static int mixColors(int firstColor, int secondColor, float amount) {
            float clampedAmount = clamp01(amount);
            int firstA = (firstColor >>> 24) & 0xFF;
            int firstR = (firstColor >>> 16) & 0xFF;
            int firstG = (firstColor >>> 8) & 0xFF;
            int firstB = firstColor & 0xFF;

            int secondA = (secondColor >>> 24) & 0xFF;
            int secondR = (secondColor >>> 16) & 0xFF;
            int secondG = (secondColor >>> 8) & 0xFF;
            int secondB = secondColor & 0xFF;

            int alpha = Math.round(firstA + (secondA - firstA) * clampedAmount);
            int red = Math.round(firstR + (secondR - firstR) * clampedAmount);
            int green = Math.round(firstG + (secondG - firstG) * clampedAmount);
            int blue = Math.round(firstB + (secondB - firstB) * clampedAmount);
            return (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
    }

    private record CompositeFrame(float renderWidth,
                                  float renderHeight,
                                  float halfWidth,
                                  float splitDistance,
                                  float targetWidth,
                                  float targetHeight,
                                  float localBaseX,
                                  float localBaseY,
                                  float screenX,
                                  float screenY
    ) {
    }
}
