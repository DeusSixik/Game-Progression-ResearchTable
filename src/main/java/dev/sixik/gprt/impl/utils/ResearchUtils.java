package dev.sixik.gprt.impl.utils;

import dev.sixik.gpf.impl.client.ClientStageData;
import dev.sixik.gprt.GameProgressionResearchTable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public final class ResearchUtils {
    private ResearchUtils() {
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasStage(String stage) {
       try {
           return ClientStageData.INSTANCE.hasStageSlow(stage);
       } catch (IllegalArgumentException e) {
           GameProgressionResearchTable.LOGGER.error(e.getMessage(), e);
           return false;
       }
    }

    /**
     * Checks whether the local client player currently has the exact required stack
     * in inventory, including item components and minimum count.
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean hasPlayerItem(ItemStack stack) {
        return hasPlayerItem(Minecraft.getInstance().player, stack);
    }

    /**
     * Checks whether the provided player inventory contains the exact required stack,
     * including item components and minimum count.
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean hasPlayerItem(@Nullable LocalPlayer player, ItemStack requiredStack) {
        if (requiredStack == null || requiredStack.isEmpty()) {
            return true;
        }
        if (player == null) {
            return false;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (ItemStack.isSameItemSameComponents(stack, requiredStack) && stack.getCount() >= requiredStack.getCount()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the local client player currently has any inventory stack that matches
     * the given ingredient.
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean hasPlayerIngredient(Ingredient ingredient) {
        return hasPlayerIngredient(Minecraft.getInstance().player, ingredient);
    }

    /**
     * Checks whether the provided player inventory contains any stack that matches
     * the given ingredient.
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean hasPlayerIngredient(@Nullable LocalPlayer player, Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return true;
        }
        if (player == null) {
            return false;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                return true;
            }
        }
        return false;
    }
}
