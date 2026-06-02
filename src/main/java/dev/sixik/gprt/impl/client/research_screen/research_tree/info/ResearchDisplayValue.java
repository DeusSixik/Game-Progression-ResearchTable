package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Visual payload for rich info-panel entries.
 * <p>
 * It can represent a single item, a block/item-like, an ingredient that cycles
 * through multiple matching stacks, or a plain texture icon.
 * </p>
 */
public final class ResearchDisplayValue {
    public enum Kind {
        ITEM_STACKS,
        TEXTURE
    }

    private final Kind kind;
    private final ItemStack[] itemStacks;
    private final @Nullable IGuiTexture texture;

    private ResearchDisplayValue(Kind kind, ItemStack[] itemStacks, @Nullable IGuiTexture texture) {
        this.kind = kind;
        this.itemStacks = itemStacks;
        this.texture = texture;
    }

    public static ResearchDisplayValue of(ItemStack stack) {
        return new ResearchDisplayValue(Kind.ITEM_STACKS, new ItemStack[]{stack.copy()}, null);
    }

    public static ResearchDisplayValue of(ItemLike itemLike) {
        return of(new ItemStack(itemLike));
    }

    public static ResearchDisplayValue of(Ingredient ingredient) {
        return new ResearchDisplayValue(Kind.ITEM_STACKS, extractIngredientStacks(ingredient), null);
    }

    public static ResearchDisplayValue ofTexture(IGuiTexture texture) {
        return new ResearchDisplayValue(Kind.TEXTURE, new ItemStack[0], texture);
    }

    public Kind kind() {
        return kind;
    }

    public ItemStack[] itemStacks() {
        return itemStacks;
    }

    public @Nullable IGuiTexture texture() {
        return texture;
    }

    public boolean isEmpty() {
        return kind == Kind.ITEM_STACKS && itemStacks.length == 0;
    }

    private static ItemStack[] extractIngredientStacks(Ingredient ingredient) {
        List<ItemStack> result = new ArrayList<>();

        tryAddStacks(result, invokeMethod(ingredient, "getItems"));
        if (!result.isEmpty()) {
            return result.toArray(ItemStack[]::new);
        }

        tryAddStacks(result, invokeMethod(ingredient, "items"));
        if (!result.isEmpty()) {
            return result.toArray(ItemStack[]::new);
        }

        return new ItemStack[0];
    }

    private static @Nullable Object invokeMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void tryAddStacks(List<ItemStack> result, @Nullable Object source) {
        if (source == null) {
            return;
        }

        if (source instanceof ItemStack stack) {
            if (!stack.isEmpty()) {
                result.add(stack.copy());
            }
            return;
        }

        if (source instanceof ItemStack[] stackArray) {
            for (ItemStack stack : stackArray) {
                if (stack != null && !stack.isEmpty()) {
                    result.add(stack.copy());
                }
            }
            return;
        }

        if (source instanceof Iterable<?> iterable) {
            for (Object entry : iterable) {
                tryAddStacks(result, entry);
            }
            return;
        }

        if (source instanceof Stream<?> stream) {
            stream.forEach(entry -> tryAddStacks(result, entry));
            return;
        }

        Class<?> sourceClass = source.getClass();
        if (sourceClass.isArray()) {
            int length = Array.getLength(source);
            for (int i = 0; i < length; i++) {
                tryAddStacks(result, Array.get(source, i));
            }
        }
    }
}
