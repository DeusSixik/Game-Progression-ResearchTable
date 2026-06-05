package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchGroup;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Group-based runtime wrapper resolver.
 * <p>
 * This is the geometry counterpart to {@link ResearchNodeGroupThemeResolver}: instead of saying
 * how a group should be colored, it says how a group's effective widget bounds should be expanded
 * or shifted in graph space.
 * </p>
 *
 * <p>
 * Use this when one branch needs wider cards, another needs a lower title area, or one debug
 * style wants oversized silhouettes. The resolver writes those changes into
 * {@link ResearchNodeWrapper}, and every system that uses wrapper bounds automatically benefits:
 * widget layout, camera focus, glow, links and optional wrapper-aware auto-layout.
 * </p>
 */
public final class ResearchNodeGroupWrapperResolver implements ResearchNodeWrapperResolver {
    private final Object2ObjectOpenHashMap<String, Consumer<ResearchNodeWrapper>> customizersByGroupId;
    private final ResearchNodeWrapperResolver fallbackResolver;

    private ResearchNodeGroupWrapperResolver(Builder builder) {
        this.customizersByGroupId = new Object2ObjectOpenHashMap<>(builder.customizersByGroupId);
        this.fallbackResolver = builder.fallbackResolver;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void resolveWrapper(ResearchNode node, ResearchNodeRenderContext context) {
        if (node == null || context == null) {
            return;
        }

        fallbackResolver.resolveWrapper(node, context);

        ResearchGroup group = node.getGroup() != null ? node.getGroup() : ResearchGroup.DEFAULT;
        Consumer<ResearchNodeWrapper> customizer = customizersByGroupId.get(group.getId());
        if (customizer != null) {
            customizer.accept(context.getNodeWrapper());
        }
    }

    /**
     * Fluent builder for {@link ResearchNodeGroupWrapperResolver}.
     */
    public static final class Builder {
        private final Object2ObjectOpenHashMap<String, Consumer<ResearchNodeWrapper>> customizersByGroupId =
                new Object2ObjectOpenHashMap<>();
        private ResearchNodeWrapperResolver fallbackResolver = ResearchNodeWrapperResolver.identity();

        private Builder() {
        }

        public Builder fallback(ResearchNodeWrapperResolver fallbackResolver) {
            this.fallbackResolver = Objects.requireNonNull(fallbackResolver, "fallbackResolver");
            return this;
        }

        public Builder group(ResearchGroup group, Consumer<ResearchNodeWrapper> customizer) {
            Objects.requireNonNull(group, "group");
            return group(group.getId(), customizer);
        }

        public Builder group(String groupId, Consumer<ResearchNodeWrapper> customizer) {
            if (groupId != null && !groupId.isBlank() && customizer != null) {
                customizersByGroupId.put(groupId, customizer);
            }
            return this;
        }

        public Builder clearGroup(String groupId) {
            if (groupId != null) {
                customizersByGroupId.remove(groupId);
            }
            return this;
        }

        public @Nullable Consumer<ResearchNodeWrapper> getGroupCustomizer(String groupId) {
            return groupId == null ? null : customizersByGroupId.get(groupId);
        }

        public ResearchNodeGroupWrapperResolver build() {
            return new ResearchNodeGroupWrapperResolver(this);
        }
    }
}
