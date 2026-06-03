package dev.sixik.gprt.impl.client.research_screen.research_table.session;

/**
 * High-level client-visible phase of a research-table session.
 * <p>
 * The goal of this enum is to keep screen code independent from packet details: the UI can react
 * to one coarse phase value instead of guessing from multiple low-level flags.
 * </p>
 */
public enum ResearchTableSessionPhase {
    /**
     * The screen selected a research, but the authoritative session payload has not arrived yet.
     */
    OPENING,
    /**
     * The table is waiting for the player to place or submit the next clue.
     */
    AWAITING_INPUT,
    /**
     * The table accepted an action and is resolving the next response.
     */
    PROCESSING,
    /**
     * The table investigation finished successfully.
     */
    COMPLETED,
    /**
     * The table investigation ended early or was cancelled.
     */
    CANCELLED
}
