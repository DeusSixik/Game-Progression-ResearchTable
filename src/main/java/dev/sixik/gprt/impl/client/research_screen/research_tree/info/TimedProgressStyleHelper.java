package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

/**
 * Shared styling helper for timed-progress label presentation.
 */
public final class TimedProgressStyleHelper {
    private TimedProgressStyleHelper() {
    }

    public static void apply(Label label,
                             TimedProgressVisualStyle style,
                             float progress01,
                             int fillColor
    ) {
        TimedProgressVisualStyle resolvedStyle = style == null ? TimedProgressVisualStyle.DEFAULT : style;
        float clamped = Math.max(0f, Math.min(1f, progress01));

        int textColor = resolveTextColor(resolvedStyle, clamped, fillColor);
        IGuiTexture backgroundTexture = resolveBackgroundTexture(resolvedStyle, clamped, fillColor);
        float paddingLeft = backgroundTexture == null ? 0f : 6f;
        float paddingRight = backgroundTexture == null ? 0f : 6f;
        float paddingTop = switch (resolvedStyle) {
            case SUBTLE_BACKGROUND, DYNAMIC_TEXT_WITH_BACKGROUND -> 3f;
            default -> backgroundTexture == null ? 0f : 2f;
        };
        float paddingBottom = switch (resolvedStyle) {
            case SUBTLE_BACKGROUND -> 2f;
            case DYNAMIC_TEXT_WITH_BACKGROUND -> 3f;
            default -> backgroundTexture == null ? 0f : 2f;
        };
        float minHeight = switch (resolvedStyle) {
            case SUBTLE_BACKGROUND -> 12f;
            case DYNAMIC_TEXT_WITH_BACKGROUND -> 13f;
            default -> 0f;
        };

        label.textStyle(styleBuilder -> styleBuilder
                .textColor(textColor)
                .textAlignVertical(Vertical.CENTER)
                .textShadow(false));
        label.style(styleBuilder -> styleBuilder.backgroundTexture(backgroundTexture));
        if (minHeight > 0f) {
            label.layout(layout -> layout
                    .widthPercent(100)
                    .minHeight(minHeight)
                    .paddingLeft(paddingLeft)
                    .paddingRight(paddingRight)
                    .paddingTop(paddingTop)
                    .paddingBottom(paddingBottom));
        } else {
            label.layout(layout -> layout
                    .widthPercent(100)
                    .paddingLeft(paddingLeft)
                    .paddingRight(paddingRight)
                    .paddingTop(paddingTop)
                    .paddingBottom(paddingBottom));
        }
    }

    public static int resolveProgressFillColor(TimedProgressVisualStyle style, float progress01, int fallbackFillColor) {
        TimedProgressVisualStyle resolvedStyle = style == null ? TimedProgressVisualStyle.DEFAULT : style;
        float clamped = Math.max(0f, Math.min(1f, progress01));
        return switch (resolvedStyle) {
            case DYNAMIC_TEXT, DYNAMIC_TEXT_WITH_BACKGROUND -> brighten(interpolateDurationColor(clamped, fallbackFillColor), 0.08f);
            case DEFAULT, SUBTLE_BACKGROUND -> fallbackFillColor;
        };
    }

    private static int resolveTextColor(TimedProgressVisualStyle style, float progress01, int fillColor) {
        return switch (style) {
            case DEFAULT, SUBTLE_BACKGROUND -> 0xFFDDE9F8;
            case DYNAMIC_TEXT, DYNAMIC_TEXT_WITH_BACKGROUND -> interpolateDurationColor(progress01, fillColor);
        };
    }

    private static IGuiTexture resolveBackgroundTexture(TimedProgressVisualStyle style, float progress01, int fillColor) {
        return switch (style) {
            case DEFAULT, DYNAMIC_TEXT -> null;
            case SUBTLE_BACKGROUND, DYNAMIC_TEXT_WITH_BACKGROUND -> GuiTextureGroup.of(
                    new ColorRectTexture(resolveBackgroundFill(progress01, fillColor)),
                    new ColorBorderTexture(-1, resolveBackgroundBorder(progress01, fillColor))
            );
        };
    }

    private static int interpolateDurationColor(float progress01, int fillColor) {
        if (progress01 <= 0.50f) {
            float local = progress01 / 0.50f;
            return lerpColor(0xFFFF7F7F, 0xFFFFD774, local);
        }
        float local = (progress01 - 0.50f) / 0.50f;
        return lerpColor(0xFFFFD774, brighten(fillColor, 0.28f), local);
    }

    private static int resolveBackgroundFill(float progress01, int fillColor) {
        int base = lerpColor(0x3A15202C, tintToward(fillColor, 0xFF0F1823, 0.70f), progress01 * 0.65f);
        return ensureAlpha(base, 0x72);
    }

    private static int resolveBackgroundBorder(float progress01, int fillColor) {
        int accent = lerpColor(0xFF6B89A8, brighten(fillColor, 0.38f), progress01);
        return ensureAlpha(accent, 0xD8);
    }

    private static int brighten(int color, float amount) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        r = clamp255((int) (r + (255 - r) * amount));
        g = clamp255((int) (g + (255 - g) * amount));
        b = clamp255((int) (b + (255 - b) * amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int tintToward(int source, int target, float amount) {
        return lerpColor(source, target, amount);
    }

    private static int ensureAlpha(int color, int alpha) {
        return (clamp255(alpha) << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpColor(int start, int end, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        int sa = (start >>> 24) & 0xFF;
        int sr = (start >>> 16) & 0xFF;
        int sg = (start >>> 8) & 0xFF;
        int sb = start & 0xFF;
        int ea = (end >>> 24) & 0xFF;
        int er = (end >>> 16) & 0xFF;
        int eg = (end >>> 8) & 0xFF;
        int eb = end & 0xFF;
        int a = Math.round(sa + (ea - sa) * clamped);
        int r = Math.round(sr + (er - sr) * clamped);
        int g = Math.round(sg + (eg - sg) * clamped);
        int b = Math.round(sb + (eb - sb) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp255(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
