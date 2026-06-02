package dev.sixik.gprt.impl.client.research_screen.research_tree.progress;

/**
 * Client-side placeholder state for a research that owns an active custom study session.
 */
public final class ClientActiveResearchSession {
    private final String researchId;
    private int currentStepIndex;

    public ClientActiveResearchSession(String researchId) {
        this.researchId = researchId;
    }

    public String getResearchId() {
        return researchId;
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public void setCurrentStepIndex(int currentStepIndex) {
        this.currentStepIndex = currentStepIndex;
    }
}
