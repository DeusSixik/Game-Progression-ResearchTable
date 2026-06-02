package dev.sixik.gprt.impl.client.research_screen.research_tree.nodes;

import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchGroup;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.Node;

public class ResearchNode extends Node {

    public enum VisibilityMode {
        ALWAYS_VISIBLE,
        REQUIRE_ANY_PARENT_STUDIED,
        REQUIRE_ALL_PARENTS_STUDIED
    }

    private boolean studied;
    private String title;
    private VisibilityMode visibilityMode = VisibilityMode.ALWAYS_VISIBLE;
    private int groupColor = 0xFF87D4FF;
    private ResearchGroup group = ResearchGroup.DEFAULT;

    public ResearchNode(int id, float x, float y, float width, float height) {
        super(id, x, y, width, height);
    }

    public boolean isStudied() {
        return studied;
    }

    public String getTitle() {
        return title;
    }

    public ResearchNode setStudied(boolean studied) {
        this.studied = studied;
        return this;
    }

    public ResearchNode setTitle(String title) {
        this.title = title;
        return this;
    }

    public VisibilityMode getVisibilityMode() {
        return visibilityMode;
    }

    public ResearchNode setVisibilityMode(VisibilityMode visibilityMode) {
        if (visibilityMode != null) {
            this.visibilityMode = visibilityMode;
        }
        return this;
    }

    public int getGroupColor() {
        return group != null ? group.getPrimaryColor() : groupColor;
    }

    public ResearchNode setGroupColor(int groupColor) {
        this.groupColor = groupColor;
        this.group = ResearchGroup.colorOnly(groupColor);
        return this;
    }

    public ResearchGroup getGroup() {
        return group;
    }

    public ResearchNode setGroup(ResearchGroup group) {
        if (group != null) {
            this.group = group;
            this.groupColor = group.getPrimaryColor();
        }
        return this;
    }
}
