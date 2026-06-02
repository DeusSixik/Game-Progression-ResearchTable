package dev.sixik.gprt.impl.client.research_screen.widgets.nodes;

public class Node {

    protected int id;
    protected float x;
    protected float y;
    protected float width;
    protected float height;

    public Node(int id, float x, float y, float width, float height) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getId() {
        return id;
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

    public float centerX() {
        return x + width / 2f;
    }

    public float centerY() {
        return y + height / 2f;
    }
}
