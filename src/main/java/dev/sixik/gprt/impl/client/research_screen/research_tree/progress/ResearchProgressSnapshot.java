package dev.sixik.gprt.impl.client.research_screen.research_tree.progress;

/**
 * One server-style snapshot of a research entry.
 * <p>
 * The intent of this DTO is to give the client one stable structure that can be
 * filled from packets later. The UI and client controller can then consume the
 * same object regardless of whether the data came from a real server sync or a
 * local debug implementation.
 * </p>
 */
public final class ResearchProgressSnapshot {
    private final String researchId;
    private final boolean unlocked;
    private final boolean studied;
    private final boolean inProgress;
    private final long startedAtMs;
    private final long finishesAtMs;
    private final boolean sessionActive;
    private final int sessionStepIndex;

    public ResearchProgressSnapshot(String researchId,
                                    boolean unlocked,
                                    boolean studied,
                                    boolean inProgress,
                                    long startedAtMs,
                                    long finishesAtMs,
                                    boolean sessionActive,
                                    int sessionStepIndex
    ) {
        this.researchId = researchId;
        this.unlocked = unlocked;
        this.studied = studied;
        this.inProgress = inProgress;
        this.startedAtMs = startedAtMs;
        this.finishesAtMs = finishesAtMs;
        this.sessionActive = sessionActive;
        this.sessionStepIndex = sessionStepIndex;
    }

    public String getResearchId() {
        return researchId;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public boolean isStudied() {
        return studied;
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public long getStartedAtMs() {
        return startedAtMs;
    }

    public long getFinishesAtMs() {
        return finishesAtMs;
    }

    public boolean isSessionActive() {
        return sessionActive;
    }

    public int getSessionStepIndex() {
        return sessionStepIndex;
    }

    public static Builder builder(String researchId) {
        return new Builder(researchId);
    }

    public static final class Builder {
        private final String researchId;
        private boolean unlocked;
        private boolean studied;
        private boolean inProgress;
        private long startedAtMs;
        private long finishesAtMs;
        private boolean sessionActive;
        private int sessionStepIndex;

        private Builder(String researchId) {
            this.researchId = researchId;
        }

        public Builder unlocked(boolean unlocked) {
            this.unlocked = unlocked;
            return this;
        }

        public Builder studied(boolean studied) {
            this.studied = studied;
            return this;
        }

        public Builder inProgress(boolean inProgress) {
            this.inProgress = inProgress;
            return this;
        }

        public Builder startedAtMs(long startedAtMs) {
            this.startedAtMs = startedAtMs;
            return this;
        }

        public Builder finishesAtMs(long finishesAtMs) {
            this.finishesAtMs = finishesAtMs;
            return this;
        }

        public Builder sessionActive(boolean sessionActive) {
            this.sessionActive = sessionActive;
            return this;
        }

        public Builder sessionStepIndex(int sessionStepIndex) {
            this.sessionStepIndex = sessionStepIndex;
            return this;
        }

        public ResearchProgressSnapshot build() {
            return new ResearchProgressSnapshot(
                    researchId,
                    unlocked,
                    studied,
                    inProgress,
                    startedAtMs,
                    finishesAtMs,
                    sessionActive,
                    sessionStepIndex
            );
        }
    }
}
