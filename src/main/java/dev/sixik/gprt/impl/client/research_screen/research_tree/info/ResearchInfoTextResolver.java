package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Shared text resolver for info-panel content.
 * <p>
 * Builder-driven research content may contain either a plain human-readable text or a translation
 * key supplied by scripts/data builders. This helper keeps that policy in one place so UI widgets
 * do not need to guess individually how to interpret every string.
 * </p>
 */
public final class ResearchInfoTextResolver {
    private ResearchInfoTextResolver() {
    }

    /**
     * Resolves a plain string that may actually be a translation key.
     * <p>
     * Rules:
     * <ul>
     *     <li>If the string is blank, return it unchanged.</li>
     *     <li>If a translation exists for the whole string, use that localized value.</li>
     *     <li>Otherwise treat the input as literal text.</li>
     * </ul>
     */
    public static String resolveText(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        return I18n.exists(text) ? I18n.get(text) : text;
    }

    /**
     * Resolves a translatable label/value pair with a localized fallback label.
     */
    public static String resolveLabeledValue(String labelKey, String value) {
        return resolveText(I18n.get(labelKey)) + ": " + resolveText(value);
    }

    /**
     * Returns the best visible item name for condition/reward rows.
     * <p>
     * For items created from plain {@code ItemLike}, {@link ItemStack#getDisplayName()} may be
     * affected by stack-level custom names later. For UI conditions we want the stable item hover
     * name of the current stack instance.
     * </p>
     */
    public static String resolveItemName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return stack.getHoverName().getString();
    }

    /**
     * Creates a compact readable label for an ingredient.
     * <p>
     * If the ingredient resolves to at least one stack we use the first localized stack name as a
     * human-readable representative. Otherwise the generic localized fallback text is used.
     * </p>
     */
    public static String resolveIngredientName(Ingredient ingredient) {
        if (ingredient == null) {
            return resolveText("ui.game_progression_research_table.research_info.condition.ingredient");
        }
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length > 0) {
            return resolveItemName(stacks[0]);
        }
        return resolveText("ui.game_progression_research_table.research_info.condition.ingredient");
    }

    /**
     * Resolves a stage label. If a dedicated localized stage name exists under
     * {@code ui.info.condition.stage.<stageKey>} it is used, otherwise the raw key is preserved.
     */
    public static String resolveStageText(String stageKey) {
        String safeStageKey = stageKey == null ? "" : stageKey;
        String translationKey = "ui.info.condition.stage." + safeStageKey;
        if (I18n.exists(translationKey)) {
            return I18n.get("ui.game_progression_research_table.research_info.condition.stage.localized",
                    I18n.get(translationKey));
        }
        return I18n.get("ui.game_progression_research_table.research_info.condition.stage.raw", safeStageKey);
    }

    public static Component resolveComponent(String text) {
        return Component.literal(resolveText(text));
    }
}
