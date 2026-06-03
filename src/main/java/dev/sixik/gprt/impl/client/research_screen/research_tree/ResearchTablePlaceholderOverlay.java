package dev.sixik.gprt.impl.client.research_screen.research_tree;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.Nullable;

/**
 * Temporary standalone overlay for TABLE research.
 * <p>
 * The real table gameplay screen will replace this later, but moving the placeholder
 * into its own class already keeps the main research screen smaller and gives it a
 * clearer API: show node data, hide itself and forward button callbacks upward.
 * </p>
 */
public final class ResearchTablePlaceholderOverlay extends UIElement {
    private static final float PANEL_WIDTH = 420f;
    private static final float PANEL_HEIGHT = 210f;

    private final Label titleLabel;
    private final Label descriptionLabel;
    private final Button completeButton;
    private final Button cancelButton;

    public ResearchTablePlaceholderOverlay(Runnable onComplete, Runnable onCancel, Runnable onClose) {
        layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0)
                .top(0)
                .widthPercent(100)
                .heightPercent(100)
        );
        style(style -> style.backgroundTexture(new ColorRectTexture(0xAA060A10)));
        setDisplay(false);

        UIElement panel = new UIElement()
                .layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(360)
                        .top(120)
                        .width(PANEL_WIDTH)
                        .height(PANEL_HEIGHT)
                        .paddingAll(10)
                        .gapAll(6)
                )
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xF0182432)));

        titleLabel = new Label();
        titleLabel.setText("Table research placeholder");

        descriptionLabel = new Label();
        descriptionLabel.setText("The real research table flow will be implemented later.");
        descriptionLabel.layout(layout -> layout.widthPercent(100));
        descriptionLabel.textStyle(style -> style
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true));

        completeButton = new Button().setText("Complete Placeholder")
                .setOnClick(event -> onComplete.run());
        completeButton.layout(layout -> layout.flex(1));
        cancelButton = new Button().setText("Cancel Session")
                .setOnClick(event -> onCancel.run());
        cancelButton.layout(layout -> layout.flex(1));
        Button closeButton = new Button().setText("X")
                .setOnClick(event -> onClose.run());
        closeButton.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(PANEL_WIDTH - 34f)
                .top(8)
                .width(24)
                .height(18)
        );

        UIElement footer = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .gapAll(6)
                        .flexDirection(FlexDirection.ROW));

        footer.addChildren(completeButton, cancelButton);
        panel.addChildren(titleLabel, descriptionLabel, footer, closeButton);
        addChildren(panel);
    }

    public void openFor(@Nullable ResearchNode node, @Nullable ResearchState state) {
        setDisplay(true);
        updateFor(node, state);
    }

    public void updateFor(@Nullable ResearchNode node, @Nullable ResearchState state) {
        if (node == null || node.getStudyType() != ResearchStudyType.TABLE) {
            showEmpty();
            return;
        }

        titleLabel.setText("Table research: " + node.getTitle());
        descriptionLabel.setText(
                "Placeholder overlay for '" + node.getTitle() + "'. "
                        + "Current state: " + formatStateText(state) + ". "
                        + "Later this will be replaced with the real table gameplay screen."
        );
        completeButton.setDisplay(state != ResearchState.STUDIED);
        cancelButton.setDisplay(state == ResearchState.IN_PROGRESS);
    }

    public void showEmpty() {
        titleLabel.setText("Table research placeholder");
        descriptionLabel.setText("The real research table flow will be implemented later.");
        completeButton.setDisplay(false);
        cancelButton.setDisplay(false);
    }

    public void hideOverlay() {
        setDisplay(false);
    }

    private String formatStateText(@Nullable ResearchState state) {
        if (state == null) {
            return "Unknown";
        }
        return switch (state) {
            case STUDIED -> "Studied";
            case AVAILABLE -> "Available";
            case LOCKED -> "Locked";
            case IN_PROGRESS -> "Table research in progress";
        };
    }
}
