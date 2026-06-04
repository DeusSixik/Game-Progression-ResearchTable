package dev.sixik.gprt.impl.client.research_screen.research_tree.reveal;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeVisualDefinition;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import org.jetbrains.annotations.Nullable;

/**
 * Per-frame data passed to a reveal-animation instance.
 */
public final class RevealRenderContext {
    private final GUIContext guiContext;
    private final ResearchNode node;
    private final RevealSnapshot snapshot;
    private final @Nullable ResearchNodeVisualDefinition visualDefinition;
    private final RevealRenderBackend backend;
    private final float progress01;
    private final float width;
    private final float height;
    private final float scale;
    private final float translateX;
    private final float translateY;
    private final float screenCenterX;
    private final float screenCenterY;
    private final int backgroundColor;
    private final int borderColor;
    private final int accentColor;

    public RevealRenderContext(GUIContext guiContext,
                               ResearchNode node,
                               RevealSnapshot snapshot,
                               @Nullable ResearchNodeVisualDefinition visualDefinition,
                               RevealRenderBackend backend,
                               float progress01,
                               float width,
                               float height,
                               float scale,
                               float translateX,
                               float translateY,
                               float screenCenterX,
                               float screenCenterY,
                               int backgroundColor,
                               int borderColor,
                               int accentColor
    ) {
        this.guiContext = guiContext;
        this.node = node;
        this.snapshot = snapshot;
        this.visualDefinition = visualDefinition;
        this.backend = backend;
        this.progress01 = progress01;
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.translateX = translateX;
        this.translateY = translateY;
        this.screenCenterX = screenCenterX;
        this.screenCenterY = screenCenterY;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.accentColor = accentColor;
    }

    public GUIContext guiContext() {
        return guiContext;
    }

    public ResearchNode node() {
        return node;
    }

    public RevealSnapshot snapshot() {
        return snapshot;
    }

    public @Nullable ResearchNodeVisualDefinition visualDefinition() {
        return visualDefinition;
    }

    public RevealRenderBackend backend() {
        return backend;
    }

    public float progress01() {
        return progress01;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public float scale() {
        return scale;
    }

    public float translateX() {
        return translateX;
    }

    public float translateY() {
        return translateY;
    }

    public float screenCenterX() {
        return screenCenterX;
    }

    public float screenCenterY() {
        return screenCenterY;
    }

    public float snapshotSourceScreenX() {
        return snapshot.sourceScreenX();
    }

    public float snapshotSourceScreenY() {
        return snapshot.sourceScreenY();
    }

    public float snapshotSourceScreenWidth() {
        return snapshot.sourceScreenWidth();
    }

    public float snapshotSourceScreenHeight() {
        return snapshot.sourceScreenHeight();
    }

    public int backgroundColor() {
        return backgroundColor;
    }

    public int borderColor() {
        return borderColor;
    }

    public int accentColor() {
        return accentColor;
    }
}
