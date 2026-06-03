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
                .titleTextShadow(explicitTheme.hasTitleTextShadow())
                .subtitleTextShadow(explicitTheme.hasSubtitleTextShadow())
                .badgeTextShadow(explicitTheme.hasBadgeTextShadow())
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

    /**
     * Fluent builder for {@link ResearchNodeGroupThemeResolver}.
     * <p>
     * This is the ergonomic API for group-based styling. Typical usage:
     * </p>
     * <ol>
     *     <li>configure one fallback theme/resolver,</li>
     *     <li>register one explicit preset per research group,</li>
     *     <li>build the resolver and hand it to the main screen.</li>
     * </ol>
     *
     * <p><b>Quick navigation:</b></p>
     * <ul>
     *     <li>{@link #fallback(ResearchNodeThemeResolver)} / {@link #fallback(ResearchNodeTheme)} - default styling;</li>
     *     <li>{@link #group(ResearchGroup)}, {@link #group(ResearchGroup, ResearchNodeTheme)},
     *     {@link #group(ResearchGroup, Consumer)} - group registration by logical group object;</li>
     *     <li>{@link #group(String, ResearchNodeTheme)}, {@link #group(String, Consumer)} - raw id registration;</li>
     *     <li>{@link #clearGroup(String)}, {@link #getGroupTheme(String)} - maintenance helpers;</li>
     *     <li>{@link #build()} - final immutable resolver.</li>
     * </ul>
     */
    public static final class Builder {
        private final Object2ObjectOpenHashMap<String, ResearchNodeTheme> themesByGroupId = new Object2ObjectOpenHashMap<>();
        private ResearchNodeThemeResolver fallbackResolver = new DefaultResearchNodeThemeResolver();

        private Builder() {
        }

        /**
         * Sets the fallback resolver used when a group has no explicit preset.
         */
        public Builder fallback(ResearchNodeThemeResolver fallbackResolver) {
            this.fallbackResolver = Objects.requireNonNull(fallbackResolver, "fallbackResolver");
            return this;
        }

        /**
         * Sets a constant fallback theme for groups without explicit presets.
         */
        public Builder fallback(ResearchNodeTheme fallbackTheme) {
            Objects.requireNonNull(fallbackTheme, "fallbackTheme");
            this.fallbackResolver = (node, context) -> fallbackTheme;
            return this;
        }

        /**
         * Registers the default theme derived from the provided group's metadata/colors.
         */
        public Builder group(ResearchGroup group) {
            Objects.requireNonNull(group, "group");
            return group(group.getId(), ResearchNodeThemes.fromGroup(group));
        }

        /**
         * Registers a fully prepared theme for the provided group.
         */
        public Builder group(ResearchGroup group, ResearchNodeTheme theme) {
            Objects.requireNonNull(group, "group");
            return group(group.getId(), theme);
        }

        /**
         * Registers a group by starting from its default preset and then applying a customizer.
         */
        public Builder group(ResearchGroup group, Consumer<ResearchNodeTheme.Builder> customizer) {
            Objects.requireNonNull(group, "group");
            ResearchNodeTheme.Builder builder = ResearchNodeThemes.fromGroup(group).toBuilder();
            customizer.accept(builder);
            return group(group.getId(), builder.build());
        }

        /**
         * Registers a fully prepared theme under a raw group id.
         */
        public Builder group(String groupId, ResearchNodeTheme theme) {
            if (groupId != null && !groupId.isBlank() && theme != null) {
                themesByGroupId.put(groupId, theme);
            }
            return this;
        }

        /**
         * Registers a raw group id by starting from the default theme and applying a customizer.
         */
        public Builder group(String groupId, Consumer<ResearchNodeTheme.Builder> customizer) {
            Objects.requireNonNull(customizer, "customizer");
            ResearchNodeTheme.Builder builder = ResearchNodeThemes.defaultTheme().toBuilder();
            customizer.accept(builder);
            return group(groupId, builder.build());
        }

        /**
         * Removes any explicit preset for the group id.
         */
        public Builder clearGroup(String groupId) {
            if (groupId != null) {
                themesByGroupId.remove(groupId);
            }
            return this;
        }

        /**
         * Returns the currently registered explicit theme for the group, if one exists.
         */
        public @Nullable ResearchNodeTheme getGroupTheme(String groupId) {
            return groupId == null ? null : themesByGroupId.get(groupId);
        }

        /**
         * Builds the immutable resolver.
         */
        public ResearchNodeGroupThemeResolver build() {
            return new ResearchNodeGroupThemeResolver(this);
        }
    }
}
