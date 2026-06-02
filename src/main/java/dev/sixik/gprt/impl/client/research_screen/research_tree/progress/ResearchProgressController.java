package dev.sixik.gprt.impl.client.research_screen.research_tree.progress;

import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Thin convenience wrapper around {@link ClientResearchProgressManager}.
 * <p>
 * The main goal of this class is to give UI code and future packet handlers one
 * compact entry point:
 * <ul>
 *     <li>UI can work directly with {@link ResearchNode} without repeating key checks;</li>
 *     <li>server sync code can push one or many {@link ResearchProgressSnapshot} objects;</li>
 *     <li>call sites do not need to know which concrete manager implementation is behind it.</li>
 * </ul>
 */
public final class ResearchProgressController {
    private final ClientResearchProgressManager manager;

    public ResearchProgressController(ClientResearchProgressManager manager) {
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    public ClientResearchProgressManager manager() {
        return manager;
    }

    public void reset() {
        manager.reset();
    }

    public boolean update(long nowMs) {
        return manager.update(nowMs);
    }

    public boolean syncFromServer(ResearchProgressSnapshot snapshot) {
        return manager.applySnapshot(snapshot);
    }

    public boolean syncFromServer(Iterable<ResearchProgressSnapshot> snapshots) {
        return manager.applySnapshots(snapshots);
    }

    public void setUnlocked(ResearchNode node, boolean unlocked) {
        String researchKey = keyOf(node);
        if (researchKey != null) {
            manager.setUnlocked(researchKey, unlocked);
        }
    }

    public ResearchState getState(ResearchNode node) {
        String researchKey = keyOf(node);
        return researchKey == null ? ResearchState.LOCKED : manager.getState(researchKey);
    }

    public boolean isStudied(ResearchNode node) {
        String researchKey = keyOf(node);
        return researchKey != null && manager.isStudied(researchKey);
    }

    public boolean isInProgress(ResearchNode node) {
        String researchKey = keyOf(node);
        return researchKey != null && manager.isInProgress(researchKey);
    }

    public boolean tryStartResearch(ResearchNode node, long nowMs) {
        String researchKey = keyOf(node);
        return researchKey != null && manager.tryStartResearch(
                researchKey,
                node.getStudyType(),
                node.getStudyDurationMs(),
                nowMs
        );
    }

    public boolean tryCompleteResearch(ResearchNode node) {
        String researchKey = keyOf(node);
        return researchKey != null && manager.tryCompleteResearch(researchKey);
    }

    public boolean tryCancelResearch(ResearchNode node) {
        String researchKey = keyOf(node);
        return researchKey != null && manager.tryCancelResearch(researchKey);
    }

    public @Nullable ClientResearchProgress getProgress(ResearchNode node) {
        String researchKey = keyOf(node);
        return researchKey == null ? null : manager.getProgress(researchKey);
    }

    public @Nullable ResearchProgressSnapshot snapshot(ResearchNode node) {
        String researchKey = keyOf(node);
        return researchKey == null ? null : manager.getSnapshot(researchKey);
    }

    public boolean canResumeTableSession(ResearchNode node) {
        return node.getStudyType() == ResearchStudyType.TABLE && getState(node) == ResearchState.IN_PROGRESS;
    }

    public ObjectArrayList<String> drainCompletedResearchIds() {
        return manager.drainCompletedResearchIds();
    }

    private static @Nullable String keyOf(@Nullable ResearchNode node) {
        if (node == null) {
            return null;
        }
        String researchKey = node.getResearchKey();
        return researchKey == null || researchKey.isBlank() ? null : researchKey;
    }
}
