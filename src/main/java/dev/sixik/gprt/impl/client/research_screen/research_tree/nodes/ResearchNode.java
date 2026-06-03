package dev.sixik.gprt.impl.client.research_screen.research_tree.nodes;

import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchGroup;
import dev.sixik.gprt.impl.client.research_screen.research_tree.definition.ResearchDefinition;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.Node;

import java.util.function.UnaryOperator;

public class ResearchNode extends Node {

    public enum VisibilityMode {
        ALWAYS_VISIBLE,
        REQUIRE_ANY_PARENT_STUDIED,
        REQUIRE_ALL_PARENTS_STUDIED
    }

    private boolean studied;
    private ResearchDefinition definition;
    private VisibilityMode visibilityMode = VisibilityMode.ALWAYS_VISIBLE;
    private int groupColor = 0xFF87D4FF;
    private ResearchGroup group = ResearchGroup.DEFAULT;

    public ResearchNode(int id, float x, float y, float width, float height) {
        super(id, x, y, width, height);
        this.definition = ResearchDefinition.builder("node_" + id).build();
    }

    public boolean isStudied() {
        return studied;
    }

    public ResearchDefinition getDefinition() {
        return definition;
    }

    public ResearchNode setDefinition(ResearchDefinition definition) {
        if (definition != null) {
            this.definition = definition;
        }
        return this;
    }

    public String getResearchKey() {
        return definition.getKey();
    }

    public String getTitle() {
        return definition.getTitle();
    }

    public ResearchNode setStudied(boolean studied) {
        this.studied = studied;
        return this;
    }

    public ResearchNode setResearchKey(String researchKey) {
        if (researchKey != null && !researchKey.isBlank()) {
            definition = ResearchDefinition.builder(researchKey)
                    .title(definition.getTitle())
                    .description(definition.getDescription())
                    .studyType(definition.getStudyType())
                    .revealAnimationStyle(definition.getRevealAnimationStyle())
                    .studyDurationMs(definition.getStudyDurationMs())
                    .build();
        }
        return this;
    }

    public ResearchNode setTitle(String title) {
        updateDefinition(builder -> builder.title(title));
        return this;
    }

    public String getDescription() {
        return definition.getDescription();
    }

    public ResearchNode setDescription(String description) {
        updateDefinition(builder -> builder.description(description));
        return this;
    }

    public ResearchStudyType getStudyType() {
        return definition.getStudyType();
    }

    public ResearchRevealAnimationStyle getRevealAnimationStyle() {
        return definition.getRevealAnimationStyle();
    }

    public ResearchNode setRevealAnimationStyle(ResearchRevealAnimationStyle revealAnimationStyle) {
        updateDefinition(builder -> builder.revealAnimationStyle(revealAnimationStyle));
        return this;
    }

    public ResearchNode setStudyType(ResearchStudyType studyType) {
        updateDefinition(builder -> builder.studyType(studyType));
        return this;
    }

    public long getStudyDurationMs() {
        return definition.getStudyDurationMs();
    }

    public ResearchNode setStudyDurationMs(long studyDurationMs) {
        updateDefinition(builder -> builder.studyDurationMs(studyDurationMs));
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

    private void updateDefinition(UnaryOperator<ResearchDefinition.Builder> updater) {
        definition = updater.apply(definition.toBuilder()).build();
    }
}
