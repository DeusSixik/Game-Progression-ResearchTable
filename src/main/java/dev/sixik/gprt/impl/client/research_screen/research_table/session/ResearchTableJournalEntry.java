package dev.sixik.gprt.impl.client.research_screen.research_table.session;

import java.util.Objects;

/**
 * Immutable log line shown by the research-table screen.
 * <p>
 * The UI can render these entries as a journal, chat-like feed or animated subtitle queue without
 * depending on the internal step system used by the server.
 * </p>
 */
public final class ResearchTableJournalEntry {
    private final ResearchTableJournalEntryType type;
    private final String text;

    public ResearchTableJournalEntry(ResearchTableJournalEntryType type, String text) {
        this.type = type == null ? ResearchTableJournalEntryType.SYSTEM : type;
        this.text = text == null ? "" : text;
    }

    public ResearchTableJournalEntryType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public static ResearchTableJournalEntry system(String text) {
        return new ResearchTableJournalEntry(ResearchTableJournalEntryType.SYSTEM, text);
    }

    public static ResearchTableJournalEntry prompt(String text) {
        return new ResearchTableJournalEntry(ResearchTableJournalEntryType.PROMPT, text);
    }

    public static ResearchTableJournalEntry observation(String text) {
        return new ResearchTableJournalEntry(ResearchTableJournalEntryType.OBSERVATION, text);
    }

    public static ResearchTableJournalEntry success(String text) {
        return new ResearchTableJournalEntry(ResearchTableJournalEntryType.SUCCESS, text);
    }

    public static ResearchTableJournalEntry error(String text) {
        return new ResearchTableJournalEntry(ResearchTableJournalEntryType.ERROR, text);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ResearchTableJournalEntry other)) {
            return false;
        }
        return type == other.type && Objects.equals(text, other.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, text);
    }
}
