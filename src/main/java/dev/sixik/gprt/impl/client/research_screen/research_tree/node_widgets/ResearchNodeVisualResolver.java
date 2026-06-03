package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;

/**
 * Converts a reusable {@link ResearchNodeTheme} plus runtime state into a final visual definition.
 */
public final class ResearchNodeVisualResolver {
    private ResearchNodeVisualResolver() {
    }

    public static ResearchNodeVisualDefinition resolve(ResearchNodeTheme theme,
                                                       ResearchNode node,
                                                       ResearchNodeRenderContext context,
                                                       ResearchNodeVisualDefinition.SizePreset fallbackSizePreset
    ) {
        int primaryColor = theme.getPrimaryColor();
        int secondaryColor = theme.getSecondaryColor();
        int resolvedAccentColor = theme.getAccentColor() != null ? theme.getAccentColor() : primaryColor;

        int backgroundColor = switch (context.getState()) {
            case STUDIED -> brightenColor(secondaryColor, 0.04f);
            case AVAILABLE -> primaryColor;
            case IN_PROGRESS -> brightenColor(primaryColor, 0.16f);
            case LOCKED -> darkenColor(primaryColor, 0.52f);
        };
        int accentColor = switch (context.getState()) {
            case STUDIED -> brightenColor(secondaryColor, 0.22f);
            case AVAILABLE, IN_PROGRESS -> brightenColor(resolvedAccentColor, 0.22f);
            case LOCKED -> darkenColor(resolvedAccentColor, 0.18f);
        };
        int borderColor = context.isSelected()
                ? brightenColor(accentColor, 0.30f)
                : (context.isHighlighted() ? brightenColor(primaryColor, 0.18f) : accentColor);

        ResearchNodeVisualDefinition.Builder builder = ResearchNodeVisualDefinition.builder()
                .titleVisible(theme.isTitleVisible())
                .subtitleVisible(theme.isSubtitleVisible())
                .iconVisible(theme.isIconVisible())
                .progressVisible(theme.isProgressVisible() && node.getStudyType() == ResearchStudyType.TIMED)
                .backgroundColor(backgroundColor)
                .accentColor(accentColor)
                .borderColor(borderColor)
                .badgeColor(resolveBadgeColor(theme, context, node))
                .progressBarColor(theme.getProgressBarColor() != null ? theme.getProgressBarColor() : accentColor)
                .progressBarBackgroundColor(theme.getProgressBarBackgroundColor() != null
                        ? theme.getProgressBarBackgroundColor()
                        : withAlpha(darkenColor(backgroundColor, 0.22f), 0xA8))
                .iconPath(theme.getIconPath())
                .badgeText(resolveBadgeText(theme, context, node))
                .shapeStyle(theme.getShapeStyle())
                .sizePreset(theme.getSizePreset() != null ? theme.getSizePreset() : fallbackSizePreset)
                .titleAlignment(theme.getTitleAlignment() != null
                        ? theme.getTitleAlignment()
                        : ResearchNodeVisualDefinition.TitleAlignment.CENTER);

        if (builder.build().getBadgeText().isBlank()) {
            builder.badgeVisible(false);
        }

        return builder.build();
    }

    private static String resolveBadgeText(ResearchNodeTheme theme, ResearchNodeRenderContext context, ResearchNode node) {
        if (!theme.getBadgeText().isBlank()) {
            return theme.getBadgeText();
        }
        if (context.hasNewUnlockMarker()) {
            return "NEW";
        }
        return switch (node.getStudyType()) {
            case TIMED -> "TIME";
            case TABLE -> "TABLE";
            case INSTANT -> "";
        };
    }

    private static int resolveBadgeColor(ResearchNodeTheme theme, ResearchNodeRenderContext context, ResearchNode node) {
        if (theme.getBadgeColor() != null) {
            return theme.getBadgeColor();
        }
        if (context.hasNewUnlockMarker()) {
            return 0xFFE8B34B;
        }
        return switch (node.getStudyType()) {
            case TIMED -> 0xFF67B7FF;
            case TABLE -> 0xFF8FD4FF;
            case INSTANT -> 0xFF67B7FF;
        };
    }

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    private static int brightenColor(int color, float amount) {
        int alpha = (color >>> 24) & 0xFF;
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        float clampedAmount = clamp01(amount);
        red += Math.round((255 - red) * clampedAmount);
        green += Math.round((255 - green) * clampedAmount);
        blue += Math.round((255 - blue) * clampedAmount);
        return (alpha << 24) | (Math.min(255, red) << 16) | (Math.min(255, green) << 8) | Math.min(255, blue);
    }

    private static int darkenColor(int color, float amount) {
        int alpha = (color >>> 24) & 0xFF;
        float factor = 1f - clamp01(amount);
        int red = Math.max(0, Math.round(((color >>> 16) & 0xFF) * factor));
        int green = Math.max(0, Math.round(((color >>> 8) & 0xFF) * factor));
        int blue = Math.max(0, Math.round((color & 0xFF) * factor));
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
