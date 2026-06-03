package dev.sixik.gprt.impl.client.research_screen.research_tree.presentation;

import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Runtime-only coordinator for delayed unlock presentation.
 * <p>
 * The controller compares currently visible researches against a per-tree seen cache and decides
 * which ones should still play their unlock animation the next time the tree is opened.
 * </p>
 *
 * <p>
 * Current scope is intentionally minimal:
 * </p>
 * <ul>
 *     <li>keeps state only in memory,</li>
 *     <li>tracks seen visible researches and pending unlock animations,</li>
 *     <li>does not depend on server-side progression storage.</li>
 * </ul>
 */
public final class ResearchUnlockPresentationController {
    private static final Object2ObjectOpenHashMap<String, ResearchTreeSeenStateCache> CACHE_BY_TREE_KEY = new Object2ObjectOpenHashMap<>();

    private final String treeKey;
    private final ResearchTreeSeenStateCache cache;

    private ResearchUnlockPresentationController(String treeKey, ResearchTreeSeenStateCache cache) {
        this.treeKey = treeKey;
        this.cache = cache;
    }

    /**
     * Returns the shared runtime controller for one logical research tree key.
     */
    public static ResearchUnlockPresentationController forTree(String treeKey) {
        String normalizedKey = Objects.requireNonNull(treeKey, "treeKey");
        return new ResearchUnlockPresentationController(
                normalizedKey,
                CACHE_BY_TREE_KEY.computeIfAbsent(normalizedKey, ignored -> new ResearchTreeSeenStateCache())
        );
    }

    public String treeKey() {
        return treeKey;
    }

    public ResearchTreeSeenStateCache cache() {
        return cache;
    }

    /**
     * Controls how delayed unlock presentation treats researches that were already completed
     * before the player opened the tree.
     * <p>
     * This enum affects only the cinematic/UI layer that decides whether a node should still
     * replay its "newly appeared in the tree" animation. It does not change actual progression,
     * unlock rules, study completion, or visibility logic.
     * </p>
     */
    public enum StudiedResearchAnimationMode {
        /**
         * Already studied researches are immediately considered "seen".
         * <p>
         * Use this when completed content should quietly appear in its final state and the tree
         * should only animate researches that are merely unlocked/available but not yet studied.
         * </p>
         */
        NEVER_FOR_STUDIED,

        /**
         * Already studied researches are treated as already seen, but the name emphasizes the
         * positive rule: delayed open animation is meant only for not-yet-studied unlocks.
         * <p>
         * This is semantically equivalent to {@link #NEVER_FOR_STUDIED}; it exists as a more
         * descriptive option for call sites where the desired behavior is easier to explain in
         * terms of "animate unlocked-only content" rather than "exclude studied content".
         * </p>
         */
        ONLY_FOR_UNSTUDIED_UNLOCKS,

        /**
         * A researched node may still replay its delayed unlock animation once if it became visible
         * outside the tree and the player has not yet seen it in the research screen.
         * <p>
         * Use this when visual onboarding is more important than strict progression timing, for
         * example when a research can be completed elsewhere but you still want the tree to
         * "introduce" that node the first time the player opens it afterwards.
         * </p>
         */
        ALLOW_FOR_STUDIED_UNSEEN
    }

    /**
     * Collects visible researches that still need their delayed unlock animation.
     * <p>
     * First open behavior is intentionally conservative: all currently visible researches are
     * marked as already seen, so the player does not get a giant animation burst the first time
     * the tree ever appears.
     * </p>
     *
     * <p>
     * {@code studiedMode} controls one narrow presentation edge case: whether an already studied
     * but not-yet-seen research should still get one delayed "introduction" animation.
     * </p>
     */
    public ObjectArrayList<String> collectPendingUnlockAnimationResearchIds(
            Iterable<ResearchNode> visibleNodes,
            StudiedResearchAnimationMode studiedMode
    ) {
        ObjectArrayList<String> visibleResearchIds = new ObjectArrayList<>();
        ObjectArrayList<String> pendingResearchIds = new ObjectArrayList<>();

        for (ResearchNode node : visibleNodes) {
            String researchKey = normalizeResearchKey(node);
            if (researchKey == null) {
                continue;
            }

            visibleResearchIds.add(researchKey);

            if (node.isStudied() && shouldTreatStudiedAsAlreadySeen(studiedMode)) {
                cache.seenVisibleResearchIds().add(researchKey);
                cache.pendingUnlockAnimationResearchIds().remove(researchKey);
            }
        }

        cache.setLastTreeOpenTime(System.currentTimeMillis());
        if (cache.seenVisibleResearchIds().isEmpty() && cache.pendingUnlockAnimationResearchIds().isEmpty()) {
            markVisibleAsSeen(visibleResearchIds);
            return pendingResearchIds;
        }

        for (int i = 0, size = visibleResearchIds.size(); i < size; i++) {
            String researchKey = visibleResearchIds.get(i);
            if (cache.pendingUnlockAnimationResearchIds().contains(researchKey)) {
                pendingResearchIds.add(researchKey);
            }
        }

        for (ResearchNode node : visibleNodes) {
            String researchKey = normalizeResearchKey(node);
            if (researchKey == null) {
                continue;
            }
            if (node.isStudied() && shouldTreatStudiedAsAlreadySeen(studiedMode)) {
                continue;
            }

            if (!cache.seenVisibleResearchIds().contains(researchKey)
                    && cache.pendingUnlockAnimationResearchIds().add(researchKey)) {
                pendingResearchIds.add(researchKey);
            }
        }

        return pendingResearchIds;
    }

    /**
     * Marks a set of currently visible researches as already shown to the player.
     */
    public void markVisibleAsSeen(Iterable<String> researchIds) {
        for (String researchId : researchIds) {
            if (researchId == null || researchId.isBlank()) {
                continue;
            }
            cache.seenVisibleResearchIds().add(researchId);
            cache.pendingUnlockAnimationResearchIds().remove(researchId);
        }
    }

    /**
     * Marks one delayed unlock animation as fully shown.
     */
    public void markUnlockAnimationFinished(String researchId) {
        if (researchId == null || researchId.isBlank()) {
            return;
        }
        cache.pendingUnlockAnimationResearchIds().remove(researchId);
        cache.seenVisibleResearchIds().add(researchId);
    }

    public boolean hasPendingUnlockAnimation() {
        return !cache.pendingUnlockAnimationResearchIds().isEmpty();
    }

    public void resetSeenState() {
        cache.reset();
    }

    private static @Nullable String normalizeResearchKey(@Nullable ResearchNode node) {
        if (node == null) {
            return null;
        }
        String researchKey = node.getResearchKey();
        return researchKey == null || researchKey.isBlank() ? null : researchKey;
    }

    private static boolean shouldTreatStudiedAsAlreadySeen(StudiedResearchAnimationMode studiedMode) {
        return studiedMode != StudiedResearchAnimationMode.ALLOW_FOR_STUDIED_UNSEEN;
    }
}
