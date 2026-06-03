package dev.sixik.gprt.impl.client.research_screen.research_table.session;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Objects;

/**
 * Immutable authoritative-or-client-draft snapshot of one table research session.
 * <p>
 * This object is the main state payload the screen should render. A future packet handler can
 * construct it from server data, while debug/local code can construct the same object manually.
 * </p>
 */
public final class ResearchTableSessionSnapshot {
    private final String researchId;
    private final String title;
    private final String description;
    private final ResearchTableSessionPhase phase;
    private final String primaryPrompt;
    private final String secondaryPrompt;
    private final boolean canSubmit;
    private final boolean canRequestHint;
    private final boolean canCancel;
    private final int selectedInventorySlot;
    private final int revision;
    private final ObjectArrayList<ResearchTableInputSlotState> inputSlots;
    private final ObjectArrayList<ResearchTableJournalEntry> journalEntries;

    public ResearchTableSessionSnapshot(String researchId,
                                        String title,
                                        String description,
                                        ResearchTableSessionPhase phase,
                                        String primaryPrompt,
                                        String secondaryPrompt,
                                        boolean canSubmit,
                                        boolean canRequestHint,
                                        boolean canCancel,
                                        int selectedInventorySlot,
                                        int revision,
                                        List<ResearchTableInputSlotState> inputSlots,
                                        List<ResearchTableJournalEntry> journalEntries
    ) {
        this.researchId = Objects.requireNonNull(researchId, "researchId");
        this.title = title == null || title.isBlank() ? researchId : title;
        this.description = description == null ? "" : description;
        this.phase = phase == null ? ResearchTableSessionPhase.OPENING : phase;
        this.primaryPrompt = primaryPrompt == null ? "" : primaryPrompt;
        this.secondaryPrompt = secondaryPrompt == null ? "" : secondaryPrompt;
        this.canSubmit = canSubmit;
        this.canRequestHint = canRequestHint;
        this.canCancel = canCancel;
        this.selectedInventorySlot = selectedInventorySlot;
        this.revision = revision;
        this.inputSlots = new ObjectArrayList<>(inputSlots == null ? List.of() : inputSlots);
        this.journalEntries = new ObjectArrayList<>(journalEntries == null ? List.of() : journalEntries);
    }

    public static Builder builder(String researchId) {
        return new Builder(researchId);
    }

    public String getResearchId() {
        return researchId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ResearchTableSessionPhase getPhase() {
        return phase;
    }

    public String getPrimaryPrompt() {
        return primaryPrompt;
    }

    public String getSecondaryPrompt() {
        return secondaryPrompt;
    }

    public boolean canSubmit() {
        return canSubmit;
    }

    public boolean canRequestHint() {
        return canRequestHint;
    }

    public boolean canCancel() {
        return canCancel;
    }

    public int getSelectedInventorySlot() {
        return selectedInventorySlot;
    }

    public int getRevision() {
        return revision;
    }

    public ObjectArrayList<ResearchTableInputSlotState> getInputSlots() {
        return new ObjectArrayList<>(inputSlots);
    }

    public ObjectArrayList<ResearchTableJournalEntry> getJournalEntries() {
        return new ObjectArrayList<>(journalEntries);
    }

    public Builder toBuilder() {
        return new Builder(researchId)
                .title(title)
                .description(description)
                .phase(phase)
                .primaryPrompt(primaryPrompt)
                .secondaryPrompt(secondaryPrompt)
                .canSubmit(canSubmit)
                .canRequestHint(canRequestHint)
                .canCancel(canCancel)
                .selectedInventorySlot(selectedInventorySlot)
                .revision(revision)
                .inputSlots(inputSlots)
                .journalEntries(journalEntries);
    }

    /**
     * Fluent builder for immutable table-session snapshots.
     */
    public static final class Builder {
        private final String researchId;
        private String title;
        private String description = "";
        private ResearchTableSessionPhase phase = ResearchTableSessionPhase.OPENING;
        private String primaryPrompt = "";
        private String secondaryPrompt = "";
        private boolean canSubmit;
        private boolean canRequestHint;
        private boolean canCancel = true;
        private int selectedInventorySlot = -1;
        private int revision;
        private final ObjectArrayList<ResearchTableInputSlotState> inputSlots = new ObjectArrayList<>();
        private final ObjectArrayList<ResearchTableJournalEntry> journalEntries = new ObjectArrayList<>();

        private Builder(String researchId) {
            this.researchId = Objects.requireNonNull(researchId, "researchId");
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder phase(ResearchTableSessionPhase phase) {
            if (phase != null) {
                this.phase = phase;
            }
            return this;
        }

        public Builder primaryPrompt(String primaryPrompt) {
            this.primaryPrompt = primaryPrompt;
            return this;
        }

        public Builder secondaryPrompt(String secondaryPrompt) {
            this.secondaryPrompt = secondaryPrompt;
            return this;
        }

        public Builder canSubmit(boolean canSubmit) {
            this.canSubmit = canSubmit;
            return this;
        }

        public Builder canRequestHint(boolean canRequestHint) {
            this.canRequestHint = canRequestHint;
            return this;
        }

        public Builder canCancel(boolean canCancel) {
            this.canCancel = canCancel;
            return this;
        }

        public Builder selectedInventorySlot(int selectedInventorySlot) {
            this.selectedInventorySlot = selectedInventorySlot;
            return this;
        }

        public Builder revision(int revision) {
            this.revision = revision;
            return this;
        }

        public Builder inputSlots(Iterable<ResearchTableInputSlotState> inputSlots) {
            this.inputSlots.clear();
            if (inputSlots != null) {
                for (ResearchTableInputSlotState inputSlot : inputSlots) {
                    if (inputSlot != null) {
                        this.inputSlots.add(inputSlot);
                    }
                }
            }
            return this;
        }

        public Builder addInputSlot(ResearchTableInputSlotState inputSlot) {
            if (inputSlot != null) {
                this.inputSlots.add(inputSlot);
            }
            return this;
        }

        public Builder journalEntries(Iterable<ResearchTableJournalEntry> journalEntries) {
            this.journalEntries.clear();
            if (journalEntries != null) {
                for (ResearchTableJournalEntry journalEntry : journalEntries) {
                    if (journalEntry != null) {
                        this.journalEntries.add(journalEntry);
                    }
                }
            }
            return this;
        }

        public Builder addJournalEntry(ResearchTableJournalEntry journalEntry) {
            if (journalEntry != null) {
                this.journalEntries.add(journalEntry);
            }
            return this;
        }

        public ResearchTableSessionSnapshot build() {
            return new ResearchTableSessionSnapshot(
                    researchId,
                    title,
                    description,
                    phase,
                    primaryPrompt,
                    secondaryPrompt,
                    canSubmit,
                    canRequestHint,
                    canCancel,
                    selectedInventorySlot,
                    revision,
                    inputSlots,
                    journalEntries
            );
        }
    }
}
