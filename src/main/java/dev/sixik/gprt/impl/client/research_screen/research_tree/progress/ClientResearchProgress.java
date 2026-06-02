package dev.sixik.gprt.impl.client.research_screen.research_tree.progress;

/**
 * Client-side runtime state of one research entry.
 */
public final class ClientResearchProgress {
    private final String researchId;
    private boolean unlocked;
    private boolean studied;
    private boolean inProgress;
    private long startedAtMs;
    private long finishesAtMs;

    public ClientResearchProgress(String researchId) {
        this.researchId = researchId;
    }

    public String getResearchId() {
        return researchId;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public boolean isStudied() {
        return studied;
    }

    public void setStudied(boolean studied) {
        this.studied = studied;
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
    }

    public long getStartedAtMs() {
        return startedAtMs;
    }

    public void setStartedAtMs(long startedAtMs) {
        this.startedAtMs = startedAtMs;
    }

    public long getFinishesAtMs() {
        return finishesAtMs;
    }

    public void setFinishesAtMs(long finishesAtMs) {
        this.finishesAtMs = finishesAtMs;
    }

    public long getRemainingMs(long nowMs) {
        if (!inProgress || finishesAtMs <= 0L) {
            return 0L;
        }
        return Math.max(0L, finishesAtMs - nowMs);
    }

    public float getProgress01(long nowMs) {
        if (studied) {
            return 1f;
        }
        if (!inProgress) {
            return 0f;
        }
        long totalMs = Math.max(1L, finishesAtMs - startedAtMs);
        if (finishesAtMs <= 0L) {
            return 0f;
        }
        long elapsedMs = Math.max(0L, nowMs - startedAtMs);
        return Math.min(1f, elapsedMs / (float) totalMs);
    }

    public ResearchState getState() {
        if (studied) {
            return ResearchState.STUDIED;
        }
        if (inProgress) {
            return ResearchState.IN_PROGRESS;
        }
        return unlocked ? ResearchState.AVAILABLE : ResearchState.LOCKED;
    }
}
