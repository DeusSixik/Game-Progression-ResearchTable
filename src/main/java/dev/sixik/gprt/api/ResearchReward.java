package dev.sixik.gprt.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.Objects;

/**
 * Public declarative reward/unlock entry for one research definition.
 */
public final class ResearchReward {
    public enum Kind {
        CUSTOM,
        RESEARCH,
        ITEM,
        INGREDIENT
    }

    private final Kind kind;
    private final String key;
    private final String value;
    private final ItemStack stack;
    private final Ingredient ingredient;

    private ResearchReward(Kind kind, String key, String value, ItemStack stack, Ingredient ingredient) {
        this.kind = kind == null ? Kind.CUSTOM : kind;
        this.key = key == null ? "" : key;
        this.value = value == null ? "" : value;
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.ingredient = ingredient == null ? Ingredient.EMPTY : ingredient;
    }

    public static ResearchReward custom(String key) {
        return new ResearchReward(Kind.CUSTOM, key, "", ItemStack.EMPTY, Ingredient.EMPTY);
    }

    public static ResearchReward custom(String key, String value) {
        return new ResearchReward(Kind.CUSTOM, key, value, ItemStack.EMPTY, Ingredient.EMPTY);
    }

    public static ResearchReward research(String researchKey) {
        return new ResearchReward(Kind.RESEARCH, researchKey, "", ItemStack.EMPTY, Ingredient.EMPTY);
    }

    public static ResearchReward item(ItemStack stack) {
        return new ResearchReward(Kind.ITEM, "", "", stack, Ingredient.EMPTY);
    }

    public static ResearchReward item(ItemLike itemLike) {
        return item(new ItemStack(Objects.requireNonNull(itemLike, "itemLike")));
    }

    public static ResearchReward ingredient(Ingredient ingredient) {
        return new ResearchReward(Kind.INGREDIENT, "", "", ItemStack.EMPTY, ingredient);
    }

    public Kind getKind() {
        return kind;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public ItemStack getStack() {
        return stack.copy();
    }

    public Ingredient getIngredient() {
        return ingredient;
    }
}
