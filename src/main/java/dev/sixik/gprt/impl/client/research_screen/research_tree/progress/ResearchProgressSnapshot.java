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

    /**
     * Fluent builder for immutable {@link ResearchProgressSnapshot} payloads.
     * <p>
     * This builder is the client-side transport shape for research progression data. The same API
     * can be used by debug code, packet decoders and future server-sync adapters without forcing
     * the UI to care where the state came from.
     * </p>
     *
     * <p><b>Quick navigation:</b></p>
     * <ul>
     *     <li>{@link #unlocked(boolean)} / {@link #studied(boolean)} - coarse progression flags;</li>
     *     <li>{@link #inProgress(boolean)} - active timed/session flag;</li>
     *     <li>{@link #startedAtMs(long)} / {@link #finishesAtMs(long)} - timed study timestamps;</li>
     *     <li>{@link #sessionActive(boolean)} / {@link #sessionStepIndex(int)} - table/session state;</li>
     *     <li>{@link #build()} - create the immutable snapshot.</li>
     * </ul>
     */
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

        /**
         * Marks whether the research is currently unlocked/available to start.
         */
        public Builder unlocked(boolean unlocked) {
            this.unlocked = unlocked;
            return this;
        }

        /**
         * Marks whether the research has already been fully completed.
         */
        public Builder studied(boolean studied) {
            this.studied = studied;
            return this;
        }

        /**
         * Marks whether the research is currently being processed.
         * <p>
         * For timed research this usually means an active timer is running. For table/session-based
         * research this can mean the investigation flow is currently open or active.
         * </p>
         */
        public Builder inProgress(boolean inProgress) {
            this.inProgress = inProgress;
            return this;
        }

        /**
         * Stores the server-authoritative start timestamp for timed progression.
         */
        public Builder startedAtMs(long startedAtMs) {
            this.startedAtMs = startedAtMs;
            return this;
        }

        /**
         * Stores the server-authoritative finish timestamp for timed progression.
         */
        public Builder finishesAtMs(long finishesAtMs) {
            this.finishesAtMs = finishesAtMs;
            return this;
        }

        /**
         * Marks whether a table/session-style investigation is currently active.
         */
        public Builder sessionActive(boolean sessionActive) {
            this.sessionActive = sessionActive;
            return this;
        }

        /**
         * Stores the current step index for multi-step/session-based investigations.
         */
        public Builder sessionStepIndex(int sessionStepIndex) {
            this.sessionStepIndex = sessionStepIndex;
            return this;
        }

        /**
         * Builds the immutable snapshot.
         */
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
