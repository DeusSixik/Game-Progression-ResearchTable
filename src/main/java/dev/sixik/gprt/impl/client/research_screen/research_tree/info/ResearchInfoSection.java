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

    public String title() {
        return title;
    }

    public List<ResearchInfoEntry> entries() {
        return entries;
    }

    public static final class Builder {
        private String title = "";
        private final List<ResearchInfoEntry> entries = new ArrayList<>();

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder addEntry(ResearchInfoEntry entry) {
            if (entry != null) {
                this.entries.add(entry);
            }
            return this;
        }

        public Builder entry(Consumer<ResearchInfoEntry.Builder> builderConsumer) {
            ResearchInfoEntry.Builder builder = ResearchInfoEntry.builder().kind(ResearchInfoEntry.Kind.INFO);
            builderConsumer.accept(builder);
            return addEntry(builder.build());
        }

        public Builder condition(Consumer<ResearchInfoEntry.Builder> builderConsumer) {
            ResearchInfoEntry.Builder builder = ResearchInfoEntry.builder().kind(ResearchInfoEntry.Kind.CONDITION);
            builderConsumer.accept(builder);
            return addEntry(builder.build());
        }

        public Builder reward(Consumer<ResearchInfoEntry.Builder> builderConsumer) {
            ResearchInfoEntry.Builder builder = ResearchInfoEntry.builder().kind(ResearchInfoEntry.Kind.REWARD);
            builderConsumer.accept(builder);
            return addEntry(builder.build());
        }

        public Builder infoText(String text) {
            return entry(entry -> entry.text(text));
        }

        public Builder infoLine(String label, String value) {
            return infoText(label + ": " + value);
        }

        public Builder conditionText(String text, boolean completed) {
            return condition(entry -> entry
                    .text(text)
                    .completed(completed));
        }

        public Builder conditionResearch(String text, boolean completed, String researchKey) {
            return condition(entry -> entry
                    .text(text)
                    .completed(completed)
                    .jumpToResearch(researchKey)
                    .jumpButtonText("Go to"));
        }

        public Builder conditionItem(String text, ItemStack stack, boolean completed) {
            return condition(entry -> entry
                    .item(stack)
                    .text(text)
                    .completed(completed));
        }

        public Builder conditionItem(String text, ItemLike itemLike, boolean completed) {
            return condition(entry -> entry
                    .item(itemLike)
                    .text(text)
                    .completed(completed));
        }

        public Builder rewardText(String text) {
            return reward(entry -> entry.text(text));
        }

        public Builder rewardItem(ItemStack stack) {
            return reward(entry -> entry
                    .item(stack)
                    .text(defaultItemName(stack)));
        }

        public Builder rewardItem(ItemStack stack, String text) {
            return reward(entry -> entry
                    .item(stack)
                    .text(text));
        }

        public Builder rewardItem(ItemLike itemLike) {
            ItemStack stack = new ItemStack(itemLike);
            return rewardItem(stack, defaultItemName(stack));
        }

        public Builder rewardItem(ItemLike itemLike, String text) {
            return reward(entry -> entry
                    .item(itemLike)
                    .text(text));
        }

        public Builder rewardIngredient(String text, Ingredient ingredient) {
            return reward(entry -> entry
                    .ingredient(ingredient)
                    .text(text));
        }

        public Builder rewardResearch(String text, String researchKey) {
            return reward(entry -> entry
                    .text(text)
                    .jumpToResearch(researchKey)
                    .jumpButtonText("View"));
        }

        public Builder rewardItemId(String itemId) {
            ItemStack stack = resolveItemId(itemId);
            if (stack.isEmpty()) {
                return rewardText(itemId);
            }
            return rewardItem(stack, defaultItemName(stack));
        }

        public Builder rewardItemId(String itemId, String fallbackText) {
            ItemStack stack = resolveItemId(itemId);
            if (stack.isEmpty()) {
                return rewardText(fallbackText);
            }
            return rewardItem(stack, defaultItemName(stack));
        }

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
