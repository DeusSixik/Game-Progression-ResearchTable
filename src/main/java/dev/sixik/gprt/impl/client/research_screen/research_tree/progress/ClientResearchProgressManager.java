package dev.sixik.gprt.impl.client.research_screen.research_tree.progress;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only entry points for reading and mutating research progression state.
 */
public interface ClientResearchProgressManager {
    void reset();

    void setUnlocked(String researchId, boolean unlocked);

    boolean applySnapshot(ResearchProgressSnapshot snapshot);

    default boolean applySnapshots(Iterable<ResearchProgressSnapshot> snapshots) {
        boolean changed = false;
        if (snapshots == null) {
            return false;
        }
        for (ResearchProgressSnapshot snapshot : snapshots) {
            if (snapshot != null) {
                changed |= applySnapshot(snapshot);
            }
        }
        return changed;
    }

    boolean canStartResearch(String researchId);

    boolean tryStartResearch(String researchId, ResearchStudyType studyType, long durationMs, long nowMs);

    boolean tryCompleteResearch(String researchId);

    boolean tryCancelResearch(String researchId);

    /**
     * Recalculates the end timestamp of an active timed research while preserving current
     * completion percentage.
     * <p>
     * Intended for future mechanics such as accelerators, debuffs or server-side duration
     * corrections. Implementations should treat {@code totalDurationMs} as the new full duration
     * and rebuild the time window around {@code nowMs}.
     * </p>
     */
    default boolean recalculateTimedResearchDuration(String researchId, long totalDurationMs) {
        return recalculateTimedResearchDuration(researchId, totalDurationMs, System.currentTimeMillis());
    }

    boolean recalculateTimedResearchDuration(String researchId, long totalDurationMs, long nowMs);

    /**
     * Recalculates the end timestamp of an active timed research from a speed multiplier while
     * preserving current completion percentage.
     * <p>
     * Multiplier semantics are gameplay-oriented: {@code 2.0} makes the research twice as fast,
     * {@code 0.5} makes it twice as slow.
     * </p>
     */
    default boolean recalculateTimedResearchSpeed(String researchId, float speedMultiplier) {
        return recalculateTimedResearchSpeed(researchId, speedMultiplier, System.currentTimeMillis());
    }

    boolean recalculateTimedResearchSpeed(String researchId, float speedMultiplier, long nowMs);

    boolean update(long nowMs);

    ResearchState getState(String researchId);

    boolean isStudied(String researchId);

    boolean isInProgress(String researchId);

    @Nullable ClientResearchProgress getProgress(String researchId);

    @Nullable ResearchProgressSnapshot getSnapshot(String researchId);

    @Nullable ClientActiveResearchSession getActiveSession(String researchId);

    ObjectArrayList<String> drainCompletedResearchIds();
}
