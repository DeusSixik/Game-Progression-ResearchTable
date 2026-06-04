package dev.sixik.gprt.impl.client.research_screen.research_tree.reveal;

/**
 * One active reveal-animation instance bound to a single unlock run.
 * <p>
 * Implementations may cache effect-specific state during construction and reuse it for the whole
 * animation lifetime. The screen calls {@link #render(RevealRenderContext)} every frame until the
 * reveal ends, then always calls {@link #dispose()}.
 * </p>
 */
public interface RevealEffectInstance {
    void render(RevealRenderContext context);

    void dispose();
}
