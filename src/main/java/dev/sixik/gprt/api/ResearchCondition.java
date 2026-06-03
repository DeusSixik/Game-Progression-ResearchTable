package dev.sixik.gprt.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.Objects;

/**
 * Public declarative condition entry for one research definition.
 * <p>
 * This class is data-only and intentionally does not describe any rendering.
 * </p>
 */
public final class ResearchCondition {
    public enum Kind {
        CUSTOM,
        STAGE,
        ITEM,
        INGREDIENT
    }

    private final Kind kind;
    private final String key;
    private final String value;
    private final ItemStack stack;
    private final Ingredient ingredient;

    private ResearchCondition(Kind kind, String key, String value, ItemStack stack, Ingredient ingredient) {
        this.kind = kind == null ? Kind.CUSTOM : kind;
        this.key = key == null ? "" : key;
        this.value = value == null ? "" : value;
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.ingredient = ingredient == null ? Ingredient.EMPTY : ingredient;
    }

    public static ResearchCondition custom(String key) {
        return new ResearchCondition(Kind.CUSTOM, key, "", ItemStack.EMPTY, Ingredient.EMPTY);
    }

    public static ResearchCondition custom(String key, String value) {
        return new ResearchCondition(Kind.CUSTOM, key, value, ItemStack.EMPTY, Ingredient.EMPTY);
    }

    public static ResearchCondition item(ItemStack stack) {
        return new ResearchCondition(Kind.ITEM, "", "", stack, Ingredient.EMPTY);
    }

    public static ResearchCondition item(ItemLike itemLike) {
        return item(new ItemStack(Objects.requireNonNull(itemLike, "itemLike")));
    }

    public static ResearchCondition ingredient(Ingredient ingredient) {
        return new ResearchCondition(Kind.INGREDIENT, "", "", ItemStack.EMPTY, ingredient);
    }

    public static ResearchCondition stage(String stage) {
        return new ResearchCondition(Kind.STAGE, "", stage, ItemStack.EMPTY, Ingredient.EMPTY);
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
