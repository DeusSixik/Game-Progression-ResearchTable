package dev.sixik.gprt.impl.client.research_screen.research_tree.presentation;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Runtime presentation cache for one research tree instance/key.
 * <p>
 * This cache intentionally stores only client-side "what has the player already been shown"
 * information. It does not replace real progression state from the server. Its job is only to
 * support UX features such as delayed unlock-animation playback on the next tree open.
 * </p>
 */
public final class ResearchTreeSeenStateCache {
    private final Set<String> seenVisibleResearchIds = new LinkedHashSet<>();
    private final Set<String> pendingUnlockAnimationResearchIds = new LinkedHashSet<>();
    private long lastTreeOpenTime;
    private boolean tutorialWasShown;

    public Set<String> seenVisibleResearchIds() {
        return seenVisibleResearchIds;
    }

    public Set<String> pendingUnlockAnimationResearchIds() {
        return pendingUnlockAnimationResearchIds;
    }

    public long lastTreeOpenTime() {
        return lastTreeOpenTime;
    }

    public void setLastTreeOpenTime(long lastTreeOpenTime) {
        this.lastTreeOpenTime = lastTreeOpenTime;
    }

    public boolean tutorialWasShown() {
        return tutorialWasShown;
    }

    public void setTutorialWasShown(boolean tutorialWasShown) {
        this.tutorialWasShown = tutorialWasShown;
    }

    public void reset() {
        seenVisibleResearchIds.clear();
        pendingUnlockAnimationResearchIds.clear();
        lastTreeOpenTime = 0L;
        tutorialWasShown = false;
    }
}
