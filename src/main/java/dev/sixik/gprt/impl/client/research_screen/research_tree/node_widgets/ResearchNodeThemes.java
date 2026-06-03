package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchGroup;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;

/**
 * Convenience helpers for creating reusable research-node themes.
 */
public final class ResearchNodeThemes {
    private static final int ROOT_BADGE_COLOR = 0xFFD5DCE6;
    private static final int METALLURGY_BADGE_COLOR = 0xFFF0C17C;
    private static final int FARMING_BADGE_COLOR = 0xFFA7E3A4;
    private static final int LOGISTICS_BADGE_COLOR = 0xFFA9CBFF;

    private ResearchNodeThemes() {
    }

    public static ResearchNodeTheme defaultTheme() {
        return ResearchNodeTheme.builder()
                .colors(0xFF67B7FF, 0xFF67B7FF)
                .shapeStyle(ResearchNodeVisualDefinition.ShapeStyle.ROUNDED_RECTANGLE)
                .titleAlignment(ResearchNodeVisualDefinition.TitleAlignment.CENTER)
                .build();
    }

    public static ResearchNodeTheme fromGroup(ResearchGroup group) {
        ResearchGroup resolvedGroup = group == null ? ResearchGroup.DEFAULT : group;
        return defaultTheme().toBuilder()
                .colors(resolvedGroup.getPrimaryColor(), resolvedGroup.getSecondaryColor())
                .build();
    }

    public static ResearchNodeTheme fromNode(ResearchNode node) {
        ResearchNodeTheme.Builder builder = fromGroup(node != null ? node.getGroup() : ResearchGroup.DEFAULT).toBuilder();
        if (node != null) {
            if (node.getStudyType() == ResearchStudyType.TIMED) {
                builder.badge("TIME", 0xFF67B7FF);
            } else if (node.getStudyType() == ResearchStudyType.TABLE) {
                builder.badge("TABLE", 0xFF8FD4FF);
            }
        }
        return builder.build();
    }

    public static ResearchNodeTheme rootPreset(ResearchGroup group) {
        return fromGroup(group).toBuilder()
                .badge("ROOT", ROOT_BADGE_COLOR)
                .sizePreset(ResearchNodeVisualDefinition.SizePreset.LARGE)
                .titleAlignment(ResearchNodeVisualDefinition.TitleAlignment.CENTER)
                .build();
    }

    public static ResearchNodeTheme metallurgyPreset(ResearchGroup group) {
        return fromGroup(group).toBuilder()
                .badge("FORGE", METALLURGY_BADGE_COLOR)
                .accentColor(group != null ? group.getSecondaryColor() : 0xFFF0C17C)
                .progressBarColor(0xFFFFC766)
                .titleAlignment(ResearchNodeVisualDefinition.TitleAlignment.LEFT)
                .build();
    }

    public static ResearchNodeTheme farmingPreset(ResearchGroup group) {
        return fromGroup(group).toBuilder()
                .badge("GROW", FARMING_BADGE_COLOR)
                .accentColor(group != null ? group.getSecondaryColor() : 0xFF87D99C)
                .progressBarColor(0xFF9BE27F)
                .titleAlignment(ResearchNodeVisualDefinition.TitleAlignment.LEFT)
                .build();
    }

    public static ResearchNodeTheme logisticsPreset(ResearchGroup group) {
        return fromGroup(group).toBuilder()
                .badge("FLOW", LOGISTICS_BADGE_COLOR)
                .accentColor(group != null ? group.getSecondaryColor() : 0xFF81B7FF)
                .progressBarColor(0xFF8BC0FF)
                .titleAlignment(ResearchNodeVisualDefinition.TitleAlignment.LEFT)
                .build();
    }
}
