package dev.sixik.gprt.api;

import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;

/**
 * Public visibility policy for addon-facing research definitions.
 * <p>
 * This mirrors the current runtime node visibility modes but keeps script/API
 * consumers independent from internal implementation packages.
 * </p>
 */
public enum ResearchVisibilityMode {
    /**
     * The node is always present in the tree, regardless of parent progress.
     */
    ALWAYS_VISIBLE,

    /**
     * The node appears once at least one parent research has been studied.
     */
    REQUIRE_ANY_PARENT_STUDIED,

    /**
     * The node appears only after every parent research has been studied.
     */
    REQUIRE_ALL_PARENTS_STUDIED;

    /**
     * Converts this public mode into the current runtime node visibility mode.
     */
    public ResearchNode.VisibilityMode toInternalMode() {
        return switch (this) {
            case ALWAYS_VISIBLE -> ResearchNode.VisibilityMode.ALWAYS_VISIBLE;
            case REQUIRE_ANY_PARENT_STUDIED -> ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED;
            case REQUIRE_ALL_PARENTS_STUDIED -> ResearchNode.VisibilityMode.REQUIRE_ALL_PARENTS_STUDIED;
        };
    }
}
