package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import net.minecraft.client.resources.language.I18n;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ClientResearchProgress;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
    private static final long ONE_MINUTE_MS = 60_000L;
    private static final DateTimeFormatter TIME_ONLY_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private ResearchInfoPresentationRules() {
    }

    public static String formatStateText(ResearchState state, ResearchNode node) {
        return switch (state) {
            case STUDIED -> tr("ui.game_progression_research_table.research_info.state.studied");
            case AVAILABLE -> tr("ui.game_progression_research_table.research_info.state.available");
            case LOCKED -> tr("ui.game_progression_research_table.research_info.state.locked");
            case IN_PROGRESS -> switch (node.getStudyType()) {
                case TIMED -> tr("ui.game_progression_research_table.research_info.state.in_progress.timed");
                case TABLE -> tr("ui.game_progression_research_table.research_info.state.in_progress.table");
                case INSTANT -> tr("ui.game_progression_research_table.research_info.state.in_progress.instant");
            };
        };
    }

    public static String formatStudyType(ResearchNode node) {
        return switch (node.getStudyType()) {
            case INSTANT -> tr("ui.game_progression_research_table.research_info.study_type.instant");
            case TIMED -> tr("ui.game_progression_research_table.research_info.study_type.timed", formatDuration(node.getStudyDurationMs()));
            case TABLE -> tr("ui.game_progression_research_table.research_info.study_type.table");
        };
    }

    public static String formatVisibilityMode(ResearchNode node) {
        return switch (node.getVisibilityMode()) {
            case ALWAYS_VISIBLE -> tr("ui.game_progression_research_table.research_info.visibility.always_visible");
            case REQUIRE_ANY_PARENT_STUDIED -> tr("ui.game_progression_research_table.research_info.visibility.require_any_parent");
            case REQUIRE_ALL_PARENTS_STUDIED -> tr("ui.game_progression_research_table.research_info.visibility.require_all_parents");
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
            builder.timedProgress(tr("ui.game_progression_research_table.research_info.timed.duration", formatDuration(node.getStudyDurationMs())), 0f, IDLE_TIMED_PROGRESS_COLOR);
            return;
        }

        long remainingMs = progress.getRemainingMs(nowMs);
        float progress01 = progress.getProgress01(nowMs);
        int progressPercent = Math.round(progress01 * 100f);
        builder.timedProgress(
                formatTimedProgressText(progressPercent, progress, remainingMs, nowMs),
                progress01,
                ACTIVE_TIMED_PROGRESS_COLOR
        );
    }

    public static void applyResearchButton(ResearchInfoContentFactory.Builder builder,
                                           ResearchNode node,
                                           ResearchState state,
                                           @Nullable ClientResearchProgress progress,
                                           long nowMs,
                                           boolean enabled
    ) {
        if (state == ResearchState.STUDIED || state == ResearchState.LOCKED) {
            builder.hideResearchButton();
            return;
        }

        if (state == ResearchState.IN_PROGRESS) {
            builder.researchButton(getInProgressButtonText(node, progress, nowMs), enabled);
            return;
        }

        builder.researchButton(getAvailableButtonText(node), enabled);
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
                yield tr("ui.game_progression_research_table.research_info.button.in_progress.timed", formatDuration(remainingMs));
            }
            case TABLE -> tr("ui.game_progression_research_table.research_info.button.in_progress.table");
            case INSTANT -> tr("ui.game_progression_research_table.research_info.button.in_progress.instant");
        };
    }

    private static String formatTimedProgressText(int progressPercent,
                                                  ClientResearchProgress progress,
                                                  long remainingMs,
                                                  long nowMs
    ) {
        if (remainingMs <= ONE_MINUTE_MS) {
            return tr("ui.game_progression_research_table.research_info.timed.progress.left",
                    progressPercent,
                    formatDuration(remainingMs));
        }

        long finishesAtMs = nowMs + remainingMs;
        String progressKey = resolveFinishesProgressKey(finishesAtMs);
        return tr(progressKey,
                progressPercent,
                formatFinishArgument(finishesAtMs, progressKey));
    }

    private static String formatFinishArgument(long timestampMs, String progressKey) {
        if (progressKey.endsWith(".today") || progressKey.endsWith(".tomorrow")) {
            return formatFinishTimeOnly(timestampMs);
        }
        return formatFinishTimestamp(timestampMs);
    }

    private static String formatFinishTimestamp(long timestampMs) {
        ZonedDateTime dateTime = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault());
        LocalDate targetDate = dateTime.toLocalDate();
        String day = String.format("%02d", dateTime.getDayOfMonth());
        return tr("ui.game_progression_research_table.research_info.date.day_month_time",
                day,
                localizeMonthShort(dateTime.getMonthValue()),
                TIME_ONLY_FORMAT.format(dateTime));
    }

    private static String formatFinishTimeOnly(long timestampMs) {
        ZonedDateTime dateTime = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault());
        return TIME_ONLY_FORMAT.format(dateTime);
    }

    private static String resolveFinishesProgressKey(long timestampMs) {
        ZonedDateTime dateTime = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault());
        LocalDate targetDate = dateTime.toLocalDate();
        LocalDate today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
        if (targetDate.equals(today)) {
            return "ui.game_progression_research_table.research_info.timed.progress.finishes.today";
        }
        if (targetDate.equals(today.plusDays(1L))) {
            return "ui.game_progression_research_table.research_info.timed.progress.finishes.tomorrow";
        }
        return "ui.game_progression_research_table.research_info.timed.progress.finishes";
    }

    private static String localizeMonthShort(int monthValue) {
        return tr("ui.game_progression_research_table.research_info.month." + monthValue);
    }

    private static String getAvailableButtonText(ResearchNode node) {
        return switch (node.getStudyType()) {
            case INSTANT -> tr("ui.game_progression_research_table.research_info.button.available.instant");
            case TIMED -> tr("ui.game_progression_research_table.research_info.button.available.timed");
            case TABLE -> tr("ui.game_progression_research_table.research_info.button.available.table");
        };
    }

    private static String tr(String key, Object... args) {
        return I18n.get(key, args);
    }
}
