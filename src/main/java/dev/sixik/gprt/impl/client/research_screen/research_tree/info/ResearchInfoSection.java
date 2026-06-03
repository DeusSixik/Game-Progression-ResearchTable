package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Logical section of the info panel.
 * <p>
 * A section is a titled container of rows. It is intentionally presentation-oriented:
 * the panel does not care whether rows describe conditions, rewards, tips, recipe unlocks or
 * anything else, as long as they are expressed through {@link ResearchInfoEntry}.
 * </p>
 *
 * <p>
 * This keeps the info panel extensible: new content types usually do not require a new panel
 * widget, only a new way to assemble section entries.
 * </p>
 */
public final class ResearchInfoSection {
    private final String title;
    private final List<ResearchInfoEntry> entries;

    private ResearchInfoSection(Builder builder) {
        this.title = builder.title;
        this.entries = List.copyOf(builder.entries);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .title(title)
                .addEntries(entries);
    }

    public String title() {
        return title;
    }

    public List<ResearchInfoEntry> entries() {
        return entries;
    }

    /**
     * Fluent builder for {@link ResearchInfoSection}.
     * <p>
     * Use this builder to assemble one coherent block of rows such as:
     * prerequisites, unlocks, rewards, notes, debug information or table-specific hints.
     * </p>
     *
     * <p>
     * The section builder exposes both low-level row construction APIs and high-level sugar
     * helpers for common cases like text conditions, item rewards and safe research jumps.
     * </p>
     *
     * <p><b>Quick navigation:</b></p>
     * <ul>
     *     <li>{@link #entry(Consumer)}, {@link #condition(Consumer)}, {@link #reward(Consumer)} - low-level row builders;</li>
     *     <li>{@link #infoText(String)}, {@link #infoLine(String, String)} - simple info rows;</li>
     *     <li>{@link #conditionText(String, boolean)}, {@link #conditionResearchVisibleOnly(String, boolean, String)} -
     *     prerequisite helpers;</li>
     *     <li>{@link #rewardItem(ItemStack)}, {@link #rewardIngredient(String, Ingredient)}, {@link #rewardResearchVisibleOnly(String, String)} -
     *     reward/unlock helpers;</li>
     *     <li>{@link #rewardItemId(String)} - convenience resolver from raw item id;</li>
     *     <li>{@link #build()} - final immutable section.</li>
     * </ul>
     */
    public static final class Builder {
        private String title = "";
        private final List<ResearchInfoEntry> entries = new ArrayList<>();

        /**
         * Sets the optional section title shown above the rows.
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Low-level API for adding a fully prepared row.
         * <p>
         * Prefer the sugar helpers like {@code infoText}, {@code conditionText},
         * {@code rewardItem} and the visible-only research helpers for typical usage.
         * Use this only when the row is already built elsewhere or needs custom behavior
         * that the section shortcuts do not cover.
         * </p>
         */
        public Builder addEntry(ResearchInfoEntry entry) {
            if (entry != null) {
                this.entries.add(entry);
            }
            return this;
        }

        public Builder addEntries(Iterable<ResearchInfoEntry> entries) {
            for (ResearchInfoEntry entry : entries) {
                addEntry(entry);
            }
            return this;
        }

        /**
         * Low-level info row builder.
         * <p>
         * This gives full access to {@link ResearchInfoEntry.Builder}. Prefer the smaller
         * sugar helpers when a plain text or simple icon/text line is enough.
         * </p>
         */
        public Builder entry(Consumer<ResearchInfoEntry.Builder> builderConsumer) {
            ResearchInfoEntry.Builder builder = ResearchInfoEntry.builder().kind(ResearchInfoEntry.Kind.INFO);
            builderConsumer.accept(builder);
            return addEntry(builder.build());
        }

        /**
         * Low-level condition row builder.
         * <p>
         * Use this when the condition needs a custom combination of icon, text, tooltip,
         * completion state or jump behavior beyond the predefined condition helpers.
         * </p>
         */
        public Builder condition(Consumer<ResearchInfoEntry.Builder> builderConsumer) {
            ResearchInfoEntry.Builder builder = ResearchInfoEntry.builder().kind(ResearchInfoEntry.Kind.CONDITION);
            builderConsumer.accept(builder);
            return addEntry(builder.build());
        }

        /**
         * Low-level reward/unlock row builder.
         * <p>
         * Prefer the reward sugar helpers for common item/research unlock cases. Use this
         * only when the reward row needs custom layout data or mixed content.
         * </p>
         */
        public Builder reward(Consumer<ResearchInfoEntry.Builder> builderConsumer) {
            ResearchInfoEntry.Builder builder = ResearchInfoEntry.builder().kind(ResearchInfoEntry.Kind.REWARD);
            builderConsumer.accept(builder);
            return addEntry(builder.build());
        }

        /**
         * High-level sugar for a plain informational text row.
         */
        public Builder infoText(String text) {
            return entry(entry -> entry.text(text));
        }

        /**
         * High-level sugar for a simple {@code label: value} informational row.
         */
        public Builder infoLine(String label, String value) {
            return infoText(label + ": " + value);
        }

        /**
         * High-level sugar for a text-only condition row.
         */
        public Builder conditionText(String text, boolean completed) {
            return condition(entry -> entry
                    .text(text)
                    .completed(completed));
        }

        /**
         * Condition helper with unconditional navigation.
         * <p>
         * This is the force-navigation variant. Use it only when the design intentionally allows
         * opening the target research regardless of whether that branch is currently visible.
         * </p>
         */
        public Builder conditionResearch(String text, boolean completed, String researchKey) {
            return condition(entry -> entry
                    .text(text)
                    .completed(completed)
                    .jumpToResearch(researchKey)
                    .jumpButtonText("Go to"));
        }

        /**
         * Adds a prerequisite row with a jump button that is shown only while the target
         * research is visible to the player.
         * <p>
         * This is the safe default for dependency-like navigation because it does not expose
         * hidden branches through the info panel.
         * </p>
         */
        public Builder conditionResearchVisibleOnly(String text, boolean completed, String researchKey) {
            return condition(entry -> entry
                    .text(text)
                    .completed(completed)
                    .jumpToResearchVisibleOnly(researchKey)
                    .jumpButtonText("Go to"));
        }

        /**
         * High-level sugar for an item-backed condition row.
         */
        public Builder conditionItem(String text, ItemStack stack, boolean completed) {
            return condition(entry -> entry
                    .item(stack)
                    .text(text)
                    .completed(completed));
        }

        /**
         * High-level sugar for an item-backed condition row using an {@link ItemLike}.
         */
        public Builder conditionItem(String text, ItemLike itemLike, boolean completed) {
            return condition(entry -> entry
                    .item(itemLike)
                    .text(text)
                    .completed(completed));
        }

        /**
         * High-level sugar for a plain reward/unlock text row.
         */
        public Builder rewardText(String text) {
            return reward(entry -> entry.text(text));
        }

        /**
         * High-level sugar for an item reward row that uses the stack hover name as text.
         */
        public Builder rewardItem(ItemStack stack) {
            return reward(entry -> entry
                    .item(stack)
                    .text(defaultItemName(stack)));
        }

        /**
         * High-level sugar for an item reward row with custom text.
         */
        public Builder rewardItem(ItemStack stack, String text) {
            return reward(entry -> entry
                    .item(stack)
                    .text(text));
        }

        /**
         * High-level sugar for an item reward row using an {@link ItemLike}.
         */
        public Builder rewardItem(ItemLike itemLike) {
            ItemStack stack = new ItemStack(itemLike);
            return rewardItem(stack, defaultItemName(stack));
        }

        /**
         * High-level sugar for an item reward row using an {@link ItemLike} with custom text.
         */
        public Builder rewardItem(ItemLike itemLike, String text) {
            return reward(entry -> entry
                    .item(itemLike)
                    .text(text));
        }

        /**
         * High-level sugar for an ingredient-based reward row.
         */
        public Builder rewardIngredient(String text, Ingredient ingredient) {
            return reward(entry -> entry
                    .ingredient(ingredient)
                    .text(text));
        }

        /**
         * Reward helper with unconditional navigation.
         * <p>
         * This is the force-navigation variant. Use it only when the UI should allow opening
         * the referenced research even if it is not currently visible in the tree.
         * </p>
         */
        public Builder rewardResearch(String text, String researchKey) {
            return reward(entry -> entry
                    .text(text)
                    .jumpToResearch(researchKey)
                    .jumpButtonText("View"));
        }

        /**
         * Adds a reward/unlock row with a jump button that is shown only while the target
         * research is visible to the player.
         * <p>
         * Use this when unlock previews should respect the same hidden-branch rules as the tree.
         * </p>
         */
        public Builder rewardResearchVisibleOnly(String text, String researchKey) {
            return reward(entry -> entry
                    .text(text)
                    .jumpToResearchVisibleOnly(researchKey)
                    .jumpButtonText("View"));
        }

        /**
         * High-level sugar that resolves an item id and renders its icon and default name.
         * <p>
         * If the item id cannot be resolved, the raw id string is shown as plain text.
         * </p>
         */
        public Builder rewardItemId(String itemId) {
            ItemStack stack = resolveItemId(itemId);
            if (stack.isEmpty()) {
                return rewardText(itemId);
            }
            return rewardItem(stack, defaultItemName(stack));
        }

        /**
         * Same as {@link #rewardItemId(String)}, but uses a custom fallback text when the item id
         * does not resolve.
         */
        public Builder rewardItemId(String itemId, String fallbackText) {
            ItemStack stack = resolveItemId(itemId);
            if (stack.isEmpty()) {
                return rewardText(fallbackText);
            }
            return rewardItem(stack, defaultItemName(stack));
        }

        /**
         * Builds the immutable logical section.
         */
        public ResearchInfoSection build() {
            return new ResearchInfoSection(this);
        }

        private static String defaultItemName(ItemStack stack) {
            return stack.getHoverName().getString();
        }

        private static ItemStack resolveItemId(String itemId) {
            ResourceLocation location = ResourceLocation.tryParse(itemId);
            if (location == null) {
                return ItemStack.EMPTY;
            }
            var item = BuiltInRegistries.ITEM.getOptional(location).orElse(Items.AIR);
            if (item == Items.AIR) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item);
        }
    }
}
