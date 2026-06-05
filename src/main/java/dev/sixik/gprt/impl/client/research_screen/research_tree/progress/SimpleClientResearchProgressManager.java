package dev.sixik.gprt.impl.client.research_screen.research_tree.progress;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

/**
 * In-memory client implementation used by the current debug tree and future client sync code.
 */
public final class SimpleClientResearchProgressManager implements ClientResearchProgressManager {
    private final Object2ObjectOpenHashMap<String, ClientResearchProgress> progressByResearchId = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<String, ClientActiveResearchSession> sessionsByResearchId = new Object2ObjectOpenHashMap<>();
    private final ObjectArrayList<String> completedResearchIds = new ObjectArrayList<>();

    @Override
    public void reset() {
        progressByResearchId.clear();
        sessionsByResearchId.clear();
        completedResearchIds.clear();
    }

    @Override
    public void setUnlocked(String researchId, boolean unlocked) {
        getOrCreateProgress(researchId).setUnlocked(unlocked);
    }

    @Override
    public boolean applySnapshot(ResearchProgressSnapshot snapshot) {
        ClientResearchProgress progress = getOrCreateProgress(snapshot.getResearchId());
        boolean changed = progress.isUnlocked() != snapshot.isUnlocked()
                || progress.isStudied() != snapshot.isStudied()
                || progress.isInProgress() != snapshot.isInProgress()
                || progress.getStartedAtMs() != snapshot.getStartedAtMs()
                || progress.getFinishesAtMs() != snapshot.getFinishesAtMs();

        boolean wasStudied = progress.isStudied();

        progress.setUnlocked(snapshot.isUnlocked());
        progress.setStudied(snapshot.isStudied());
        progress.setInProgress(snapshot.isInProgress());
        progress.setStartedAtMs(snapshot.getStartedAtMs());
        progress.setFinishesAtMs(snapshot.getFinishesAtMs());

        if (snapshot.isSessionActive()) {
            ClientActiveResearchSession session = sessionsByResearchId.computeIfAbsent(
                    snapshot.getResearchId(),
                    ClientActiveResearchSession::new
            );
            session.setCurrentStepIndex(snapshot.getSessionStepIndex());
        } else {
            sessionsByResearchId.remove(snapshot.getResearchId());
        }

        if (!wasStudied && snapshot.isStudied() && !completedResearchIds.contains(snapshot.getResearchId())) {
            completedResearchIds.add(snapshot.getResearchId());
        }

        return changed;
    }

    @Override
    public boolean canStartResearch(String researchId) {
        ClientResearchProgress progress = getOrCreateProgress(researchId);
        return progress.getState() == ResearchState.AVAILABLE;
    }

    @Override
    public boolean tryStartResearch(String researchId, ResearchStudyType studyType, long durationMs, long nowMs) {
        ClientResearchProgress progress = getOrCreateProgress(researchId);
        if (progress.getState() != ResearchState.AVAILABLE) {
            return false;
        }

        return switch (studyType) {
            case INSTANT -> {
                progress.setInProgress(false);
                progress.setStudied(true);
                progress.setStartedAtMs(nowMs);
                progress.setFinishesAtMs(nowMs);
                sessionsByResearchId.remove(researchId);
                completedResearchIds.add(researchId);
                yield true;
            }
            case TIMED -> {
                progress.setInProgress(true);
                progress.setStudied(false);
                progress.setStartedAtMs(nowMs);
                progress.setFinishesAtMs(nowMs + Math.max(1L, durationMs));
                sessionsByResearchId.remove(researchId);
                yield true;
            }
            case TABLE -> {
                progress.setInProgress(true);
                progress.setStudied(false);
                progress.setStartedAtMs(nowMs);
                progress.setFinishesAtMs(0L);
                sessionsByResearchId.put(researchId, new ClientActiveResearchSession(researchId));
                yield true;
            }
        };
    }

    @Override
    public boolean tryCompleteResearch(String researchId) {
        ClientResearchProgress progress = getOrCreateProgress(researchId);
        if (progress.isStudied()) {
            return false;
        }

        progress.setStudied(true);
        progress.setInProgress(false);
        progress.setFinishesAtMs(progress.getFinishesAtMs() <= 0L ? System.currentTimeMillis() : progress.getFinishesAtMs());
        sessionsByResearchId.remove(researchId);
        completedResearchIds.add(researchId);
        return true;
    }

    @Override
    public boolean tryCancelResearch(String researchId) {
        ClientResearchProgress progress = getOrCreateProgress(researchId);
        if (!progress.isInProgress()) {
            return false;
        }

        progress.setInProgress(false);
        progress.setStartedAtMs(0L);
        progress.setFinishesAtMs(0L);
        sessionsByResearchId.remove(researchId);
        return true;
    }

    @Override
    public boolean recalculateTimedResearchDuration(String researchId, long totalDurationMs) {
        return ClientResearchProgressManager.super.recalculateTimedResearchDuration(researchId, totalDurationMs);
    }

    @Override
    public boolean recalculateTimedResearchDuration(String researchId, long totalDurationMs, long nowMs) {
        ClientResearchProgress progress = progressByResearchId.get(researchId);
        if (progress == null) {
            return false;
        }
        return progress.recalculateTimedDuration(nowMs, totalDurationMs);
    }

    @Override
    public boolean recalculateTimedResearchSpeed(String researchId, float speedMultiplier) {
        return ClientResearchProgressManager.super.recalculateTimedResearchSpeed(researchId, speedMultiplier);
    }

    @Override
    public boolean recalculateTimedResearchSpeed(String researchId, float speedMultiplier, long nowMs) {
        ClientResearchProgress progress = progressByResearchId.get(researchId);
        if (progress == null) {
            return false;
        }
        return progress.recalculateTimedDurationByMultiplier(nowMs, speedMultiplier);
    }

    @Override
    public boolean update(long nowMs) {
        boolean changed = false;
        for (ClientResearchProgress progress : progressByResearchId.values()) {
            if (progress.isInProgress() && progress.getFinishesAtMs() > 0L && nowMs >= progress.getFinishesAtMs()) {
                progress.setInProgress(false);
                progress.setStudied(true);
                sessionsByResearchId.remove(progress.getResearchId());
                completedResearchIds.add(progress.getResearchId());
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public ResearchState getState(String researchId) {
        return getOrCreateProgress(researchId).getState();
    }

    @Override
    public boolean isStudied(String researchId) {
        return getOrCreateProgress(researchId).isStudied();
    }

    @Override
    public boolean isInProgress(String researchId) {
        return getOrCreateProgress(researchId).isInProgress();
    }

    @Override
    public @Nullable ClientResearchProgress getProgress(String researchId) {
        return progressByResearchId.get(researchId);
    }

    @Override
    public @Nullable ResearchProgressSnapshot getSnapshot(String researchId) {
        ClientResearchProgress progress = progressByResearchId.get(researchId);
        if (progress == null) {
            return null;
        }

        ClientActiveResearchSession session = sessionsByResearchId.get(researchId);
        return ResearchProgressSnapshot.builder(researchId)
                .unlocked(progress.isUnlocked())
                .studied(progress.isStudied())
                .inProgress(progress.isInProgress())
                .startedAtMs(progress.getStartedAtMs())
                .finishesAtMs(progress.getFinishesAtMs())
                .sessionActive(session != null)
                .sessionStepIndex(session == null ? 0 : session.getCurrentStepIndex())
                .build();
    }

    @Override
    public @Nullable ClientActiveResearchSession getActiveSession(String researchId) {
        return sessionsByResearchId.get(researchId);
    }

    @Override
    public ObjectArrayList<String> drainCompletedResearchIds() {
        ObjectArrayList<String> drained = new ObjectArrayList<>(completedResearchIds);
        completedResearchIds.clear();
        return drained;
    }

    private ClientResearchProgress getOrCreateProgress(String researchId) {
        return progressByResearchId.computeIfAbsent(researchId, ClientResearchProgress::new);
    }
}
