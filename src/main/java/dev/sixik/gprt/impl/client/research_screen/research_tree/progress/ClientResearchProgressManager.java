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

    boolean update(long nowMs);

    ResearchState getState(String researchId);

    boolean isStudied(String researchId);

    boolean isInProgress(String researchId);

    @Nullable ClientResearchProgress getProgress(String researchId);

    @Nullable ResearchProgressSnapshot getSnapshot(String researchId);

    @Nullable ClientActiveResearchSession getActiveSession(String researchId);

    ObjectArrayList<String> drainCompletedResearchIds();
}
