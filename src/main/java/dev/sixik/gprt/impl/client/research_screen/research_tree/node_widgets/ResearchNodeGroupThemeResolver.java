package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchGroup;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Map-based {@link ResearchNodeThemeResolver} that assigns theme presets per research group.
 * <p>
 * This is the intended "ergonomic" API for real screens: declare one preset per branch/group,
 * keep one fallback for everything else, and let the main screen reuse that resolver for all nodes.
 * </p>
 */
public final class ResearchNodeGroupThemeResolver implements ResearchNodeThemeResolver {
    private final Object2ObjectOpenHashMap<String, ResearchNodeTheme> themesByGroupId;
    private final ResearchNodeThemeResolver fallbackResolver;

    private ResearchNodeGroupThemeResolver(Builder builder) {
        this.themesByGroupId = new Object2ObjectOpenHashMap<>(builder.themesByGroupId);
        this.fallbackResolver = builder.fallbackResolver;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ResearchNodeTheme resolveTheme(ResearchNode node, ResearchNodeRenderContext context) {
        ResearchGroup group = node != null ? node.getGroup() : ResearchGroup.DEFAULT;
        String groupId = group != null ? group.getId() : ResearchGroup.DEFAULT.getId();
        ResearchNodeTheme theme = themesByGroupId.get(groupId);
        if (theme != null) {
            return mergeWithFallbackDefaults(theme, node, context);
        }
        return fallbackResolver.resolveTheme(node, context);
    }

    private ResearchNodeTheme mergeWithFallbackDefaults(ResearchNodeTheme explicitTheme,
                                                        ResearchNode node,
                                                        ResearchNodeRenderContext context
    ) {
        ResearchNodeTheme fallbackTheme = fallbackResolver.resolveTheme(node, context);
        ResearchNodeTheme.Builder builder = fallbackTheme.toBuilder()
                .titleVisible(explicitTheme.isTitleVisible())
                .subtitleVisible(explicitTheme.isSubtitleVisible())
                .iconVisible(explicitTheme.isIconVisible())
                .progressVisible(explicitTheme.isProgressVisible())
                .primaryColor(explicitTheme.getPrimaryColor())
                .secondaryColor(explicitTheme.getSecondaryColor())
                .shapeStyle(explicitTheme.getShapeStyle())
                .iconPath(explicitTheme.getIconPath())
                .badgeText(explicitTheme.getBadgeText());

        if (explicitTheme.getAccentColor() != null) {
            builder.accentColor(explicitTheme.getAccentColor());
        }
        if (explicitTheme.getBadgeColor() != null) {
            builder.badgeColor(explicitTheme.getBadgeColor());
        }
        if (explicitTheme.getProgressBarColor() != null) {
            builder.progressBarColor(explicitTheme.getProgressBarColor());
        }
        if (explicitTheme.getProgressBarBackgroundColor() != null) {
            builder.progressBarBackgroundColor(explicitTheme.getProgressBarBackgroundColor());
        }
        if (explicitTheme.getSizePreset() != null) {
            builder.sizePreset(explicitTheme.getSizePreset());
        }
        if (explicitTheme.getTitleAlignment() != null) {
            builder.titleAlignment(explicitTheme.getTitleAlignment());
        }

        return builder.build();
    }

    public static final class Builder {
        private final Object2ObjectOpenHashMap<String, ResearchNodeTheme> themesByGroupId = new Object2ObjectOpenHashMap<>();
        private ResearchNodeThemeResolver fallbackResolver = new DefaultResearchNodeThemeResolver();

        private Builder() {
        }

        public Builder fallback(ResearchNodeThemeResolver fallbackResolver) {
            this.fallbackResolver = Objects.requireNonNull(fallbackResolver, "fallbackResolver");
            return this;
        }

        public Builder fallback(ResearchNodeTheme fallbackTheme) {
            Objects.requireNonNull(fallbackTheme, "fallbackTheme");
            this.fallbackResolver = (node, context) -> fallbackTheme;
            return this;
        }

        public Builder group(ResearchGroup group) {
            Objects.requireNonNull(group, "group");
            return group(group.getId(), ResearchNodeThemes.fromGroup(group));
        }

        public Builder group(ResearchGroup group, ResearchNodeTheme theme) {
            Objects.requireNonNull(group, "group");
            return group(group.getId(), theme);
        }

        public Builder group(ResearchGroup group, Consumer<ResearchNodeTheme.Builder> customizer) {
            Objects.requireNonNull(group, "group");
            ResearchNodeTheme.Builder builder = ResearchNodeThemes.fromGroup(group).toBuilder();
            customizer.accept(builder);
            return group(group.getId(), builder.build());
        }

        public Builder group(String groupId, ResearchNodeTheme theme) {
            if (groupId != null && !groupId.isBlank() && theme != null) {
                themesByGroupId.put(groupId, theme);
            }
            return this;
        }

        public Builder group(String groupId, Consumer<ResearchNodeTheme.Builder> customizer) {
            Objects.requireNonNull(customizer, "customizer");
            ResearchNodeTheme.Builder builder = ResearchNodeThemes.defaultTheme().toBuilder();
            customizer.accept(builder);
            return group(groupId, builder.build());
        }

        public Builder clearGroup(String groupId) {
            if (groupId != null) {
                themesByGroupId.remove(groupId);
            }
            return this;
        }

        public @Nullable ResearchNodeTheme getGroupTheme(String groupId) {
            return groupId == null ? null : themesByGroupId.get(groupId);
        }

        public ResearchNodeGroupThemeResolver build() {
            return new ResearchNodeGroupThemeResolver(this);
        }
    }
}
