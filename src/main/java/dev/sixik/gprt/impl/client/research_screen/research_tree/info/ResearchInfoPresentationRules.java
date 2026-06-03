package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ClientResearchProgress;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import org.jetbrains.annotations.Nullable;

/**
 * Shared formatting and state-to-UI rules for research info panels.
 * <p>
 * This helper is responsible for the "presentation policy" of the panel:
 * how study types are named, how research state text is shown, how durations are formatted,
 * and how timed-progress / action-button states are derived from runtime research state.
 * </p>
 *
 * <p><b>Navigation:</b></p>
 * <ul>
 *     <li>{@link #formatStateText(ResearchState, ResearchNode)} -
 *     text for the current research state.</li>
 *     <li>{@link #formatStudyType(ResearchNode)} -
 *     text for the study mode label.</li>
 *     <li>{@link #formatVisibilityMode(ResearchNode)} -
 *     text for visibility / prerequisite mode.</li>
 *     <li>{@link #formatDuration(long)} -
 *     common duration formatting helper.</li>
 *     <li>{@link #applyTimedProgress(ResearchInfoContentFactory.Builder, ResearchNode, ResearchState, ClientResearchProgress, long)} -
 *     fills the timed progress block according to current runtime state.</li>
 *     <li>{@link #applyResearchButton(ResearchInfoContentFactory.Builder, ResearchNode, ResearchState, ClientResearchProgress, long)} -
 *     picks the correct bottom action button state/text.</li>
 * </ul>
 *
 * <p><b>When to use this class:</b></p>
 * <ul>
 *     <li>Use it when multiple screens should share the same wording and UI-state decisions.</li>
 *     <li>Use it together with {@link ResearchInfoContentFactory} when building a full panel.</li>
 *     <li>Keep custom screen-specific flavor text outside of this class if it is not meant to
 *     be a project-wide default.</li>
 * </ul>
 */
public final class ResearchInfoPresentationRules {
    private static final int IDLE_TIMED_PROGRESS_COLOR = 0x664C90E8;
    private static final int ACTIVE_TIMED_PROGRESS_COLOR = 0xFF67B7FF;

    private ResearchInfoPresentationRules() {
    }

    public static String formatStateText(ResearchState state, ResearchNode node) {
        return switch (state) {
            case STUDIED -> "Studied";
            case AVAILABLE -> "Available";
            case LOCKED -> "Locked";
            case IN_PROGRESS -> switch (node.getStudyType()) {
                case TIMED -> "Timed research in progress";
                case TABLE -> "Table research in progress";
                case INSTANT -> "In progress";
            };
        };
    }

    public static String formatStudyType(ResearchNode node) {
        return switch (node.getStudyType()) {
            case INSTANT -> "Instant";
            case TIMED -> "Timed (" + formatDuration(node.getStudyDurationMs()) + ")";
            case TABLE -> "Table";
        };
    }

    public static String formatVisibilityMode(ResearchNode node) {
        return switch (node.getVisibilityMode()) {
            case ALWAYS_VISIBLE -> "Always visible";
            case REQUIRE_ANY_PARENT_STUDIED -> "Require any parent studied";
            case REQUIRE_ALL_PARENTS_STUDIED -> "Require all parents studied";
        };
    }

    public static void applyTimedProgress(ResearchInfoContentFactory.Builder builder,
                                          ResearchNode node,
                                          ResearchState state,
                                          @Nullable ClientResearchProgress progress,
                                          long nowMs
    ) {
        if (node.getStudyType() != ResearchStudyType.TIMED || state == ResearchState.STUDIED || state == ResearchState.LOCKED) {
            builder.hideTimedProgress();
            return;
        }

        if (progress == null || state != ResearchState.IN_PROGRESS) {
            builder.timedProgress("Duration: " + formatDuration(node.getStudyDurationMs()), 0f, IDLE_TIMED_PROGRESS_COLOR);
            return;
        }

        long remainingMs = progress.getRemainingMs(nowMs);
        float progress01 = progress.getProgress01(nowMs);
        builder.timedProgress(
                "Progress: " + Math.round(progress01 * 100f) + "% | left " + formatDuration(remainingMs),
                progress01,
                ACTIVE_TIMED_PROGRESS_COLOR
        );
    }

    public static void applyResearchButton(ResearchInfoContentFactory.Builder builder,
                                           ResearchNode node,
                                           ResearchState state,
                                           @Nullable ClientResearchProgress progress,
                                           long nowMs
    ) {
        if (state == ResearchState.STUDIED || state == ResearchState.LOCKED) {
            builder.hideResearchButton();
            return;
        }

        if (state == ResearchState.IN_PROGRESS) {
            builder.researchButton(getInProgressButtonText(node, progress, nowMs));
            return;
        }

        builder.researchButton(getAvailableButtonText(node));
    }

    public static String formatDuration(long durationMs) {
        long totalSeconds = Math.max(0L, (durationMs + 999L) / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes > 0L) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    private static String getInProgressButtonText(ResearchNode node,
                                                  @Nullable ClientResearchProgress progress,
                                                  long nowMs
    ) {
        return switch (node.getStudyType()) {
            case TIMED -> {
                long remainingMs = progress == null ? 0L : progress.getRemainingMs(nowMs);
                yield "Timed: " + formatDuration(remainingMs);
            }
            case TABLE -> "Resume Table Research";
            case INSTANT -> "In Progress";
        };
    }

    private static String getAvailableButtonText(ResearchNode node) {
        return switch (node.getStudyType()) {
            case INSTANT -> "Research";
            case TIMED -> "Start Timed Research";
            case TABLE -> "Open Table Research";
        };
    }
}
