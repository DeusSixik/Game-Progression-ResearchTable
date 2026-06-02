package dev.sixik.gprt.impl.client.research_screen.research_tree;

/**
 * Describes one visual/semantic branch of the research tree.
 * <p>
 * Right now the group mainly provides colors for nodes and links, but the same
 * object can later be extended with icons, localized titles, categories or any
 * other metadata that belongs to a full research branch.
 */
public final class ResearchGroup {

    public static final ResearchGroup DEFAULT = new ResearchGroup("default", "Default", 0xFF87D4FF, 0xFF87D4FF);

    private final String id;
    private final String title;
    private final int primaryColor;
    private final int secondaryColor;

    public ResearchGroup(String id, String title, int primaryColor, int secondaryColor) {
        this.id = id == null || id.isEmpty() ? "default" : id;
        this.title = title == null || title.isEmpty() ? this.id : title;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }

    public static ResearchGroup of(String id, String title, int primaryColor) {
        return new ResearchGroup(id, title, primaryColor, primaryColor);
    }

    public static ResearchGroup of(String id, String title, int primaryColor, int secondaryColor) {
        return new ResearchGroup(id, title, primaryColor, secondaryColor);
    }

    public static ResearchGroup colorOnly(int primaryColor) {
        return new ResearchGroup("custom_" + Integer.toUnsignedString(primaryColor, 16), "Custom", primaryColor, primaryColor);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }
}
