package dev.sixik.gprt.impl.client.research_screen.demo;

import java.util.List;

/**
 * Small preset catalog used by {@link ResearchTreeScreenDebug}.
 * <p>
 * Each preset stores a reproducible set of already studied researches so the debug tree can be
 * reopened in a known state without manually clicking through the whole progression again.
 * </p>
 */
public final class DebugResearchData {
    public static final DebugResearchData EMPTY = preset(
            "empty",
            "Empty",
            "Nothing is studied yet. Useful for first-open and branch-unlock tests."
    );

    public static final DebugResearchData ROOT_ONLY = preset(
            "root_only",
            "Root Only",
            "Only the primitive root is completed, so the three first branches become available.",
            "primitive_tools"
    );

    public static final DebugResearchData EARLY_BRANCHES = preset(
            "early_branches",
            "Early Branches",
            "Early progression state with all three first branches opened.",
            "primitive_tools",
            "metallurgy",
            "farming",
            "logistics"
    );

    public static final DebugResearchData MID_GAME = preset(
            "mid_game",
            "Mid Game",
            "Mid-tree preset with enough progression to test mixed branch visibility and link routing.",
            "primitive_tools",
            "metallurgy",
            "farming",
            "logistics",
            "alloying",
            "steel",
            "irrigation",
            "breeding",
            "carts",
            "storage"
    );

    public static final DebugResearchData WAREHOUSE_READY = preset(
            "warehouse_ready",
            "Warehouse Ready",
            "Warehouse is visible and ready to be tested through the table-study flow.",
            "primitive_tools",
            "metallurgy",
            "farming",
            "logistics",
            "alloying",
            "steel",
            "steam",
            "chemistry",
            "machines",
            "irrigation",
            "breeding",
            "greenhouses",
            "food_processing",
            "carts",
            "storage"
    );

    public static final DebugResearchData ALL_COMPLETED = preset(
            "all_completed",
            "All Completed",
            "Every demo research is already completed. Useful for final color and layout checks.",
            "primitive_tools",
            "metallurgy",
            "farming",
            "logistics",
            "alloying",
            "steel",
            "irrigation",
            "breeding",
            "carts",
            "storage",
            "steam",
            "chemistry",
            "machines",
            "greenhouses",
            "food_processing",
            "rail",
            "warehouse"
    );

    private static final List<DebugResearchData> ALL = List.of(
            EMPTY,
            ROOT_ONLY,
            EARLY_BRANCHES,
            MID_GAME,
            WAREHOUSE_READY,
            ALL_COMPLETED
    );

    private final String id;
    private final String title;
    private final String description;
    private final List<String> studiedResearchKeys;

    private DebugResearchData(String id, String title, String description, List<String> studiedResearchKeys) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.studiedResearchKeys = studiedResearchKeys;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public List<String> studiedResearchKeys() {
        return studiedResearchKeys;
    }

    public static List<DebugResearchData> all() {
        return ALL;
    }

    private static DebugResearchData preset(String id, String title, String description, String... studiedResearchKeys) {
        return new DebugResearchData(id, title, description, List.of(studiedResearchKeys));
    }
}
