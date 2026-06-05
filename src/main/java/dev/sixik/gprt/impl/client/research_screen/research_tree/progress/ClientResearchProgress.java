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

    public long getTotalDurationMs() {
        if (finishesAtMs <= 0L || startedAtMs <= 0L) {
            return 0L;
        }
        return Math.max(0L, finishesAtMs - startedAtMs);
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

    public boolean recalculateTimedDuration(long totalDurationMs) {
        return recalculateTimedDuration(System.currentTimeMillis(), totalDurationMs);
    }

    /**
     * Rebuilds the linear timed-progress window around the current moment while preserving already
     * completed progress.
     * <p>
     * This is the helper you want for future research accelerators/decelerators:
     * first read the current progress at {@code nowMs}, then remap the timed research to a new
     * total duration. The method updates both {@link #startedAtMs} and {@link #finishesAtMs} so
     * the visible percentage does not jump backwards/forwards when the duration changes.
     * </p>
     *
     * <p><b>Example:</b></p>
     * <ul>
     *     <li>Original duration: 100s</li>
     *     <li>Current progress: 50%</li>
     *     <li>New duration: 50s (accelerated)</li>
     *     <li>Result: progress stays at 50%, finish time moves closer, remaining time becomes 25s</li>
     * </ul>
     *
     * @param nowMs current wall-clock timestamp used as the recalculation anchor
     * @param totalDurationMs new full duration for this timed research
     * @return {@code true} when the timed window was rebuilt; {@code false} when this progress is
     * not currently a timed in-progress entry
     */
    public boolean recalculateTimedDuration(long nowMs, long totalDurationMs) {
        if (!inProgress || finishesAtMs <= 0L) {
            return false;
        }

        long clampedDurationMs = Math.max(1L, totalDurationMs);
        float progress01 = Math.max(0f, Math.min(1f, getProgress01(nowMs)));
        long elapsedMs = Math.round(progress01 * clampedDurationMs);
        startedAtMs = nowMs - elapsedMs;
        finishesAtMs = startedAtMs + clampedDurationMs;
        return true;
    }

    public boolean recalculateTimedDurationByMultiplier(float speedMultiplier) {
        return recalculateTimedDurationByMultiplier(System.currentTimeMillis(), speedMultiplier);
    }

    /**
     * Rebuilds the timed window from a speed multiplier instead of an absolute duration.
     * <p>
     * Multiplier semantics follow the usual gameplay expectation:
     * {@code 2.0} means "twice as fast" and therefore halves the total duration,
     * while {@code 0.5} means "two times slower" and therefore doubles it.
     * The current completion percentage is preserved.
     * </p>
     *
     * @param nowMs current wall-clock timestamp used as the recalculation anchor
     * @param speedMultiplier speed factor where values greater than {@code 1} accelerate research
     * @return {@code true} when recalculation succeeded
     */
    public boolean recalculateTimedDurationByMultiplier(long nowMs, float speedMultiplier) {
        if (!inProgress || finishesAtMs <= 0L || speedMultiplier <= 0f) {
            return false;
        }

        long currentDurationMs = Math.max(1L, getTotalDurationMs());
        long scaledDurationMs = Math.max(1L, Math.round(currentDurationMs / speedMultiplier));
        return recalculateTimedDuration(nowMs, scaledDurationMs);
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
