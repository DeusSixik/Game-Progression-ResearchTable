package dev.sixik.gprt.impl.client.research_screen.research_tree.reveal.effects;

import dev.sixik.gprt.impl.client.research_screen.research_tree.reveal.ResearchRevealEffect;
import dev.sixik.gprt.impl.client.research_screen.research_tree.reveal.RevealEffectInstance;
import dev.sixik.gprt.impl.client.research_screen.research_tree.reveal.RevealInitContext;
import dev.sixik.gprt.impl.client.research_screen.research_tree.reveal.RevealRenderContext;

/**
 * Lightweight snapshot-based reveal that fades and settles the node into place.
 * <p>
 * This effect is intentionally simple: it is the first non-split example proving that the new reveal
 * API can host additional styles without touching the tree screen internals again.
 * </p>
 */
public final class FadeScaleRevealEffect implements ResearchRevealEffect {
    @Override
    public RevealEffectInstance createInstance(RevealInitContext context) {
        return new Instance();
    }

    private static final class Instance implements RevealEffectInstance {
        @Override
        public void render(RevealRenderContext context) {
            float safeScale = Math.max(0.1f, context.scale());
            float renderWidth = context.width() * safeScale;
            float renderHeight = context.height() * safeScale;
            float baseX = context.centerX() + context.translateX() - renderWidth * 0.5f;
            float baseY = context.centerY() + context.translateY() - renderHeight * 0.5f;

            float intro = easeOutCubic(clamp01(context.progress01() / 0.22f));
            float settle = easeOutCubic(clamp01((context.progress01() - 0.08f) / 0.72f));
            float alphaMul = clamp01(0.08f + 0.92f * intro);
            int alpha = Math.round(255f * alphaMul);
            int color = withAlpha(0xFFFFFFFF, alpha);
            int backingColor = withAlpha(context.backgroundColor(), Math.round(210f * alphaMul));

            fillRect(context, baseX, baseY, renderWidth, renderHeight, backingColor);

            context.backend().drawTexturedQuad(
                    context.guiContext().graphics.pose().last().pose(),
                    context.snapshot().textureId(),
                    baseX,
                    baseY,
                    renderWidth,
                    renderHeight,
                    0f,
                    0f,
                    1f,
                    1f,
                    color
            );

            int borderColor = withAlpha(context.borderColor(), Math.round(210f * intro));
            int accentColor = withAlpha(context.accentColor(), Math.round(150f * settle));
            float border = Math.max(1f, Math.min(2f, renderHeight * 0.025f));
            fillRect(context, baseX, baseY, renderWidth, border, accentColor);
            fillRect(context, baseX, baseY, border, renderHeight, borderColor);
            fillRect(context, baseX + renderWidth - border, baseY, border, renderHeight, borderColor);
            fillRect(context, baseX, baseY + renderHeight - border, renderWidth, border, borderColor);
        }

        @Override
        public void dispose() {
            // No extra resources for this lightweight effect.
        }

        private void fillRect(RevealRenderContext context, float x, float y, float width, float height, int color) {
            if (width <= 0.5f || height <= 0.5f || ((color >>> 24) & 0xFF) <= 0) {
                return;
            }
            context.guiContext().graphics.fill(
                    Math.round(x),
                    Math.round(y),
                    Math.round(x + width),
                    Math.round(y + height),
                    color
            );
        }

        private static float clamp01(float value) {
            return Math.max(0f, Math.min(1f, value));
        }

        private static float easeOutCubic(float progress) {
            float inverse = 1f - clamp01(progress);
            return 1f - inverse * inverse * inverse;
        }

        private static int withAlpha(int color, int alpha) {
            return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
        }
    }
}
