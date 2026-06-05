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
 * This is the smallest reusable content unit of the info-panel system.
 * </p>
 *
 * <p><b>Common examples:</b></p>
 * <ul>
 *     <li>a prerequisite line like "Open Metallurgy"</li>
 *     <li>a reward line with an item icon and custom text</li>
 *     <li>a plain note/help row</li>
 *     <li>a row with a jump button that focuses another visible research</li>
 * </ul>
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
    private final boolean showJumpButton;
    private final boolean visibleJumpTargetOnly;

    private ResearchInfoEntry(Builder builder) {
        this.kind = builder.kind;
        this.text = builder.text;
        this.display = builder.display;
        this.tooltips = List.copyOf(builder.tooltips);
        this.completed = builder.completed;
        this.jumpToResearchKey = builder.jumpToResearchKey;
        this.jumpButtonText = builder.jumpButtonText;
        this.showJumpButton = builder.showJumpButton;
        this.visibleJumpTargetOnly = builder.visibleJumpTargetOnly;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .kind(kind)
                .text(text)
                .display(display)
                .tooltips(tooltips)
                .completed(completed)
                .showJumpButton(showJumpButton)
                .visibleJumpTargetOnly(visibleJumpTargetOnly)
                .jumpButtonText(jumpButtonText)
                .jumpToResearch(jumpToResearchKey);
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

    public boolean showJumpButton() {
        return showJumpButton;
    }

    public boolean visibleJumpTargetOnly() {
        return visibleJumpTargetOnly;
    }

    /**
     * Fluent builder for {@link ResearchInfoEntry}.
     * <p>
     * Use this builder when section-level sugar helpers are not enough and a row needs a custom
     * combination of text, display payload, tooltips, completion state and navigation behavior.
     * </p>
     *
     * <p>
     * Most callers will not instantiate this builder directly very often; instead it is usually
     * accessed through {@link ResearchInfoSection.Builder#entry(java.util.function.Consumer)},
     * {@link ResearchInfoSection.Builder#condition(java.util.function.Consumer)} or
     * {@link ResearchInfoSection.Builder#reward(java.util.function.Consumer)}.
     * </p>
     *
     * <p><b>Quick navigation:</b></p>
     * <ul>
     *     <li>{@link #text(String)} - primary label text;</li>
     *     <li>{@link #item(ItemStack)}, {@link #ingredient(Ingredient)}, {@link #icon(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture)} -
     *     visual payload;</li>
     *     <li>{@link #tooltip(Component)}, {@link #tooltips(Iterable)} - hover hints;</li>
     *     <li>{@link #completed(boolean)} - condition/result state;</li>
     *     <li>{@link #jumpToResearch(String)}, {@link #jumpToResearchVisibleOnly(String)} - navigation behavior;</li>
     *     <li>{@link #showJumpButton(boolean)}, {@link #jumpButtonText(String)} - jump button presentation;</li>
     *     <li>{@link #build()} - final immutable row snapshot.</li>
     * </ul>
     */
    public static final class Builder {
        private Kind kind = Kind.INFO;
        private @Nullable String text;
        private @Nullable ResearchDisplayValue display;
        private final List<Component> tooltips = new ArrayList<>();
        private boolean completed;
        private @Nullable String jumpToResearchKey;
        private @Nullable String jumpButtonText = "Open";
        private boolean showJumpButton = true;
        private boolean visibleJumpTargetOnly;

        /**
         * Low-level row type selector.
         * <p>
         * In normal usage this is usually assigned by {@link ResearchInfoSection.Builder}
         * through {@code entry(...)}, {@code condition(...)} or {@code reward(...)}.
         * </p>
         */
        public Builder kind(Kind kind) {
            this.kind = kind;
            return this;
        }

        /**
         * Sets the primary text of the row.
         */
        public Builder text(String text) {
            this.text = ResearchInfoTextResolver.resolveText(text);
            return this;
        }

        /**
         * Low-level display setter for fully prepared visual payloads.
         * <p>
         * Prefer {@link #item(ItemStack)}, {@link #item(ItemLike)}, {@link #ingredient(Ingredient)}
         * or {@link #icon(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture)} for common cases.
         * </p>
         */
        public Builder display(ResearchDisplayValue display) {
            this.display = display;
            return this;
        }

        /**
         * Uses an {@link ItemStack} as the row icon/display.
         */
        public Builder item(ItemStack stack) {
            this.display = ResearchDisplayValue.of(stack);
            return this;
        }

        /**
         * Uses an {@link ItemLike} as the row icon/display.
         */
        public Builder item(ItemLike itemLike) {
            this.display = ResearchDisplayValue.of(itemLike);
            return this;
        }

        /**
         * Uses an {@link Ingredient} as the row display, allowing multiple matching stacks.
         */
        public Builder ingredient(Ingredient ingredient) {
            this.display = ResearchDisplayValue.of(ingredient);
            return this;
        }

        /**
         * Uses a raw GUI texture as the row display.
         */
        public Builder icon(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture texture) {
            this.display = ResearchDisplayValue.ofTexture(texture);
            return this;
        }

        /**
         * Marks the row as completed/incomplete.
         * <p>
         * This is especially meaningful for condition rows, where the panel changes
         * the prefix and color based on completion state.
         * </p>
         */
        public Builder completed(boolean completed) {
            this.completed = completed;
            return this;
        }

        /**
         * Adds one tooltip line.
         */
        public Builder tooltip(Component tooltip) {
            this.tooltips.add(tooltip);
            return this;
        }

        /**
         * Adds one tooltip line from plain text.
         */
        public Builder tooltip(String tooltip) {
            this.tooltips.add(ResearchInfoTextResolver.resolveComponent(tooltip));
            return this;
        }

        /**
         * Adds multiple tooltip lines.
         */
        public Builder tooltips(Iterable<Component> tooltips) {
            for (Component tooltip : tooltips) {
                if (tooltip != null) {
                    this.tooltips.add(tooltip);
                }
            }
            return this;
        }

        /**
         * Configures a jump target that can always be opened from the panel.
         * <p>
         * Use this only when the design intentionally allows navigation regardless of current
         * visibility rules.
         * </p>
         */
        public Builder jumpToResearch(String jumpToResearchKey) {
            this.jumpToResearchKey = jumpToResearchKey;
            return this;
        }

        /**
         * Configures a jump target that should only render its button while the target research
         * is currently visible to the player.
         * <p>
         * Use this for prerequisites, contextual hints and any other entries that must not expose
         * hidden branches through the info panel.
         * </p>
         *
         * <p>
         * Use {@link #jumpToResearch(String)} only when the design intentionally allows navigation
         * regardless of current visibility rules.
         * </p>
         */
        public Builder jumpToResearchVisibleOnly(String jumpToResearchKey) {
            this.jumpToResearchKey = jumpToResearchKey;
            this.visibleJumpTargetOnly = true;
            return this;
        }

        /**
         * Controls whether the jump button is allowed to render when a target is present.
         */
        public Builder showJumpButton(boolean showJumpButton) {
            this.showJumpButton = showJumpButton;
            return this;
        }

        /**
         * Explicitly disables jump-button rendering for this row even if a target key is set.
         */
        public Builder hideJumpButton() {
            this.showJumpButton = false;
            return this;
        }

        /**
         * Sets the label of the jump button.
         */
        public Builder jumpButtonText(String jumpButtonText) {
            this.jumpButtonText = ResearchInfoTextResolver.resolveText(jumpButtonText);
            return this;
        }

        /**
         * Low-level visibility constraint for jump rendering.
         * <p>
         * Prefer {@link #jumpToResearchVisibleOnly(String)} when you want the common
         * "render jump only for visible targets" behavior in one call.
         * </p>
         */
        public Builder visibleJumpTargetOnly(boolean visibleJumpTargetOnly) {
            this.visibleJumpTargetOnly = visibleJumpTargetOnly;
            return this;
        }

        /**
         * Builds the immutable row description.
         */
        public ResearchInfoEntry build() {
            return new ResearchInfoEntry(this);
        }
    }
}
