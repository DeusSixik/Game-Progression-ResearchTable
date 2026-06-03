package dev.sixik.gprt.api;

/**
 * Public study-mode enum for GPTR research definitions.
 * <p>
 * This enum intentionally lives outside the internal implementation packages so addon code can
 * describe research behavior without importing classes from {@code impl}.
 * </p>
 */
public enum ResearchStudyType {
    /**
     * Research completes immediately when the player starts it successfully.
     */
    INSTANT,
    /**
     * Research runs for a fixed duration and finishes automatically later.
     */
    TIMED,
    /**
     * Research opens a dedicated table/session workflow instead of resolving instantly.
     */
    TABLE;

    /**
     * Converts the public API enum into the current internal runtime enum.
     */
    public dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType toInternalType() {
        return switch (this) {
            case INSTANT -> dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType.INSTANT;
            case TIMED -> dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType.TIMED;
            case TABLE -> dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType.TABLE;
        };
    }
}
