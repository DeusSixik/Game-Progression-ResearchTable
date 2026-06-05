package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;

/**
 * Mutable runtime wrapper around a logical {@link ResearchNode}.
 * <p>
 * {@link ResearchNode} stays the canonical graph model: dependencies, unlock rules, base layout
 * position and default size all still live there. This wrapper exists for the visual/runtime
 * layer, where a screen or widget factory may want to adjust the effective on-screen bounds
 * without mutating the underlying graph model itself.
 * </p>
 *
 * <p><b>Typical use cases:</b></p>
 * <ul>
 *     <li>make one visual style wider/taller than the base node size;</li>
 *     <li>shift the clickable/rendered card slightly around the logical anchor point;</li>
 *     <li>let reveal animations, focus camera and link attachment use the styled bounds instead of
 *     the raw logical rectangle.</li>
 * </ul>
 *
 * <p>
 * In short: {@code ResearchNode} answers "what exists in the research graph?",
 * while {@code ResearchNodeWrapper} answers "how is this node currently placed/rendered on screen?".
 * </p>
 */
public final class ResearchNodeWrapper {
    private final ResearchNode node;
    private float x;
    private float y;
    private float width;
    private float height;

    private ResearchNodeWrapper(ResearchNode node, float x, float y, float width, float height) {
        this.node = node;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Creates a wrapper that initially mirrors the raw logical node bounds.
     */
    public static ResearchNodeWrapper fromNode(ResearchNode node) {
        return new ResearchNodeWrapper(node, node.getX(), node.getY(), node.getWidth(), node.getHeight());
    }

    public ResearchNode getNode() {
        return node;
    }

    public int getNodeId() {
        return node.getId();
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public ResearchNodeWrapper setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public ResearchNodeWrapper setSize(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public ResearchNodeWrapper setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public float centerX() {
        return x + width * 0.5f;
    }

    public float centerY() {
        return y + height * 0.5f;
    }
}
