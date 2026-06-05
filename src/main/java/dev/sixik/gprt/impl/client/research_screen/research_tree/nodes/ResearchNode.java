package dev.sixik.gprt.impl.client.research_screen.research_tree.nodes;

import com.lowdragmc.lowdraglib2.editor.resource.BuiltinPath;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TransformTexture;
import com.lowdragmc.lowdraglib2.gui.texture.UIResourceTexture;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchGroup;
import dev.sixik.gprt.impl.client.research_screen.research_tree.definition.ResearchDefinition;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.Node;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.function.UnaryOperator;

public class ResearchNode extends Node {

    public enum VisibilityMode {
        ALWAYS_VISIBLE,
        REQUIRE_ANY_PARENT_STUDIED,
        REQUIRE_ALL_PARENTS_STUDIED
    }

    @Getter
    private boolean studied;
    @Getter
    private ResearchDefinition definition;
    @Getter
    private VisibilityMode visibilityMode = VisibilityMode.ALWAYS_VISIBLE;
    private int groupColor = 0xFF87D4FF;
    @Getter
    private ResearchGroup group = ResearchGroup.DEFAULT;

    @Getter
    private TransformTexture iconTexture = new ItemStackTexture(ItemStack.EMPTY);

    public ResearchNode(int id, float x, float y, float width, float height) {
        super(id, x, y, width, height);
        this.definition = ResearchDefinition.builder("node_" + id).build();
    }

    public void setIconTexture(Object obj) {
        switch (obj) {
            case ItemStack item -> this.iconTexture = new ItemStackTexture(item);
            case Item item -> this.iconTexture = new ItemStackTexture(item);
            case ItemLike item -> this.iconTexture = new ItemStackTexture(item.asItem());
            case Ingredient item -> this.iconTexture = new ItemStackTexture(item.getItems());
            case String item -> this.iconTexture = new UIResourceTexture(new BuiltinPath(ResourceLocation.tryParse(item).toString()));
            case ResourceLocation item -> this.iconTexture = new UIResourceTexture(new BuiltinPath(item.toString()));
            default -> throw new UnsupportedOperationException("Unsupported icon type: " + obj.getClass().getName());
        }
    }


    public ResearchNode setDefinition(ResearchDefinition definition) {
        if (definition != null) {
            this.definition = definition;
            if (definition.getIconTexture() != null) {
                setIconTexture(definition.getIconTexture());
            }
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
                    .iconTexture(definition.getIconTexture())
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
