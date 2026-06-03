package dev.sixik.gprt.api;

/**
 * Static script/addon-facing entry point for research declarations.
 */
public final class Researches {
    private Researches() {
    }

    /**
     * Starts describing one research entry by its unique key/id.
     */
    public static ResearchDefinition.Builder register(String key) {
        return ResearchDefinition.register(key);
    }

    /**
     * Starts describing one logical research group/category by its unique id.
     */
    public static ResearchGroupDefinition.Builder group(String id) {
        return ResearchGroupDefinition.register(id);
    }
}
