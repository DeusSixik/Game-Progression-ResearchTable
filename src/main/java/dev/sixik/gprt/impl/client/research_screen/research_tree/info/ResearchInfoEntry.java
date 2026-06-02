package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Single rich row inside a panel section.
 * <p>
 * Each row can render text, an item/icon display, tooltips and an optional
 * jump button that focuses another research by key.
 * </p>
 */
public final class ResearchInfoEntry {
    public enum Kind {
        INFO,
        CONDITION,
        REWARD
    }

    private final Kind kind;
    private final @Nullable String text;
    private final @Nullable ResearchDisplayValue display;
    private final List<Component> tooltips;
    private final boolean completed;
    private final @Nullable String jumpToResearchKey;
    private final @Nullable String jumpButtonText;

    private ResearchInfoEntry(Builder builder) {
        this.kind = builder.kind;
        this.text = builder.text;
        this.display = builder.display;
        this.tooltips = List.copyOf(builder.tooltips);
        this.completed = builder.completed;
        this.jumpToResearchKey = builder.jumpToResearchKey;
        this.jumpButtonText = builder.jumpButtonText;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Kind kind() {
        return kind;
    }

    public @Nullable String text() {
        return text;
    }

    public @Nullable ResearchDisplayValue display() {
        return display;
    }

    public List<Component> tooltips() {
        return tooltips;
    }

    public boolean completed() {
        return completed;
    }

    public @Nullable String jumpToResearchKey() {
        return jumpToResearchKey;
    }

    public @Nullable String jumpButtonText() {
        return jumpButtonText;
    }

    public static final class Builder {
        private Kind kind = Kind.INFO;
        private @Nullable String text;
        private @Nullable ResearchDisplayValue display;
        private final List<Component> tooltips = new ArrayList<>();
        private boolean completed;
        private @Nullable String jumpToResearchKey;
        private @Nullable String jumpButtonText = "Open";

        public Builder kind(Kind kind) {
            this.kind = kind;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder display(ResearchDisplayValue display) {
            this.display = display;
            return this;
        }

        public Builder item(ItemStack stack) {
            this.display = ResearchDisplayValue.of(stack);
            return this;
        }

        public Builder item(ItemLike itemLike) {
            this.display = ResearchDisplayValue.of(itemLike);
            return this;
        }

        public Builder ingredient(Ingredient ingredient) {
            this.display = ResearchDisplayValue.of(ingredient);
            return this;
        }

        public Builder icon(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture texture) {
            this.display = ResearchDisplayValue.ofTexture(texture);
            return this;
        }

        public Builder completed(boolean completed) {
            this.completed = completed;
            return this;
        }

        public Builder tooltip(Component tooltip) {
            this.tooltips.add(tooltip);
            return this;
        }

        public Builder tooltip(String tooltip) {
            this.tooltips.add(Component.literal(tooltip));
            return this;
        }

        public Builder tooltips(Iterable<Component> tooltips) {
            for (Component tooltip : tooltips) {
                if (tooltip != null) {
                    this.tooltips.add(tooltip);
                }
            }
            return this;
        }

        public Builder jumpToResearch(String jumpToResearchKey) {
            this.jumpToResearchKey = jumpToResearchKey;
            return this;
        }

        public Builder jumpButtonText(String jumpButtonText) {
            this.jumpButtonText = jumpButtonText;
            return this;
        }

        public ResearchInfoEntry build() {
            return new ResearchInfoEntry(this);
        }
    }
}
