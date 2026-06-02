package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reusable details panel for a selected research node.
 * <p>
 * The panel renders rich sections from {@link ResearchInfoContent}: large centered
 * title, dynamic rows, item/icon displays, tooltips and optional jump buttons.
 * </p>
 */
public final class ResearchInfoPanel extends UIElement {
    public static final float DEFAULT_WIDTH = 300f;
    public static final float DEFAULT_HEIGHT = 430f;
    public static final float OPEN_TRANSLATE_X = 0f;

    private static final float MIN_WIDTH = 210f;
    private static final float MIN_HEIGHT = 220f;
    private static final float PANEL_MARGIN = 8f;
    private static final float PREFERRED_TOP = 118f;
    private static final float PROGRESS_BAR_HEIGHT = 8f;
    private static final int SCROLL_BACKGROUND_COLOR = 0x22101824;

    private final Label titleLabel;
    private final Label groupLabel;
    private final Label modeLabel;
    private final Label stateLabel;
    private final Label descriptionLabel;
    private final Label timedProgressLabel;
    private final UIElement timedProgressBar;
    private final UIElement timedProgressFill;
    private final ScrollerView sectionsScroller;
    private final UIElement sectionsContainer;
    private final Button closeButton;
    private final Button researchButton;
    private final Consumer<String> onResearchJump;
    private float currentPanelWidth = DEFAULT_WIDTH;
    private float currentPanelHeight = DEFAULT_HEIGHT;
    private float currentProgressBarWidth = DEFAULT_WIDTH - 16f;

    public ResearchInfoPanel(Runnable onClose, Runnable onResearch, Consumer<String> onResearchJump) {
        this.onResearchJump = onResearchJump;

        layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(PANEL_MARGIN)
                .top(PREFERRED_TOP)
                .width(DEFAULT_WIDTH)
                .height(DEFAULT_HEIGHT)
                .paddingAll(8)
                .gapAll(5)
        );
        style(style -> style
                .backgroundTexture(new ColorRectTexture(0xE6192432))
                .transform2D(new Transform2D().translate(hiddenTranslateX(), 0f)));
        setDisplay(false);

        titleLabel = new Label();
        titleLabel.layout(layout -> layout.widthPercent(100));

        groupLabel = new Label();
        modeLabel = new Label();
        stateLabel = new Label();

        descriptionLabel = createWrappingLabel();

        timedProgressLabel = new Label();
        timedProgressBar = new UIElement()
                .layout(layout -> layout.width(currentProgressBarWidth).height(PROGRESS_BAR_HEIGHT))
                .style(style -> style.backgroundTexture(new ColorRectTexture(0x55232D39)));
        timedProgressFill = new UIElement()
                .layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(0)
                        .top(0)
                        .width(0)
                        .height(PROGRESS_BAR_HEIGHT)
                )
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF67B7FF)));
        timedProgressBar.addChildren(timedProgressFill);

        sectionsContainer = new UIElement()
                .layout(layout -> layout.widthPercent(100).gapAll(6));

        sectionsScroller = new ScrollerView();
        sectionsScroller.layout(layout -> layout
                .widthPercent(100)
                .flex(1));
        sectionsScroller.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .minScrollPixel(12f)
                .maxScrollPixel(36f));
        sectionsScroller.viewPort(view -> view
                .layout(layout -> layout.paddingAll(0))
                .style(style -> style.backgroundTexture(new ColorRectTexture(SCROLL_BACKGROUND_COLOR))));
        sectionsScroller.verticalScroller(scroller -> {
            scroller.headButton.setDisplay(false);
            scroller.tailButton.setDisplay(false);
        });
        sectionsScroller.addScrollViewChild(sectionsContainer);

        closeButton = new Button().setText("X").setOnClick(event -> onClose.run());
        closeButton.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(DEFAULT_WIDTH - 36f)
                .top(6)
                .width(24)
                .height(18)
        );

        researchButton = new Button()
                .setText("Research")
                .setOnClick(event -> onResearch.run());

        addChildren(
                titleLabel,
                groupLabel,
                modeLabel,
                stateLabel,
                descriptionLabel,
                timedProgressLabel,
                timedProgressBar,
                sectionsScroller,
                researchButton,
                closeButton
        );

        applyContent(ResearchInfoContent.builder().build());
        applyResponsiveLayout();
    }

    public void applyContent(ResearchInfoContent content) {
        titleLabel.setText(content.title());
        titleLabel.textStyle(style -> style
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true)
                .fontSize(content.titleLarge() ? 14f : 9f)
                .textAlignHorizontal(content.titleAlign() == ResearchInfoContent.TitleAlign.CENTER ? Horizontal.CENTER : Horizontal.LEFT)
        );

        groupLabel.setText(content.groupText());
        modeLabel.setText(content.modeText());
        stateLabel.setText(content.stateText());
        descriptionLabel.setText(content.description());

        style(style -> style.backgroundTexture(new ColorRectTexture(content.panelColor())));

        timedProgressLabel.setDisplay(content.showTimedProgress());
        timedProgressBar.setDisplay(content.showTimedProgress());
        if (content.showTimedProgress()) {
            timedProgressLabel.setText(content.timedProgressText());
        }
        updateTimedProgressFill(content.timedProgress01(), content.timedProgressFillColor());

        rebuildSections(content.sections());

        researchButton.setDisplay(content.showResearchButton());
        researchButton.setText(content.researchButtonText());
    }

    public void setSlideProgress(float progress01) {
        float clamped = Math.max(0f, Math.min(1f, progress01));
        float hiddenTranslateX = hiddenTranslateX();
        float translateX = hiddenTranslateX + (OPEN_TRANSLATE_X - hiddenTranslateX) * clamped;
        style(style -> style.transform2D(new Transform2D().translate(translateX, 0f)));
        if (clamped <= 0f) {
            setDisplay(false);
        }
    }

    private void rebuildSections(List<ResearchInfoSection> sections) {
        sectionsScroller.clearAllScrollViewChildren();
        sectionsContainer.clearAllChildren();
        for (ResearchInfoSection section : sections) {
            if (section.entries().isEmpty()) {
                continue;
            }
            sectionsContainer.addChild(createSectionElement(section));
        }
        sectionsScroller.addScrollViewChild(sectionsContainer);
    }

    private UIElement createSectionElement(ResearchInfoSection section) {
        UIElement container = new UIElement()
                .layout(layout -> layout.widthPercent(100).gapAll(4));

        if (!section.title().isBlank()) {
            Label title = new Label();
            title.setText(section.title());
            title.textStyle(style -> style.fontSize(10f));
            container.addChild(title);
        }

        for (ResearchInfoEntry entry : section.entries()) {
            container.addChild(createEntryElement(entry));
        }

        return container;
    }

    private UIElement createEntryElement(ResearchInfoEntry entry) {
        UIElement row = new UIElement()
                .layout(layout -> layout.widthPercent(100).gapAll(4).flexDirection(FlexDirection.ROW));

        List<Component> tooltips = buildEntryTooltips(entry);

        if (entry.display() != null) {
            row.addChild(createDisplayElement(entry.display(), tooltips));
        }

        if (entry.text() != null && !entry.text().isBlank()) {
            Label text = createWrappingLabel();
            text.layout(layout -> layout.flex(1));
            text.setText(formatEntryText(entry));
            text.textStyle(style -> style.textColor(resolveEntryTextColor(entry)));
            if (!tooltips.isEmpty()) {
                text.style(style -> style.tooltips(Tooltips.of(tooltips)));
            }
            row.addChild(text);
        }

        if (entry.jumpToResearchKey() != null && !entry.jumpToResearchKey().isBlank()) {
            Button jumpButton = new Button()
                    .setText(entry.jumpButtonText() != null ? entry.jumpButtonText() : "Open")
                    .setOnClick(event -> onResearchJump.accept(entry.jumpToResearchKey()));
            if (!tooltips.isEmpty()) {
                jumpButton.style(style -> style.tooltips(Tooltips.of(tooltips)));
            }
            row.addChild(jumpButton);
        }

        return row;
    }

    private UIElement createDisplayElement(ResearchDisplayValue displayValue, List<Component> tooltips) {
        UIElement display = new UIElement()
                .layout(layout -> layout.width(18).height(18))
                .style(style -> style.backgroundTexture(resolveDisplayTexture(displayValue)));

        if (!tooltips.isEmpty()) {
            display.style(style -> style.tooltips(Tooltips.of(tooltips)));
        }
        return display;
    }

    private IGuiTexture resolveDisplayTexture(ResearchDisplayValue displayValue) {
        if (displayValue.kind() == ResearchDisplayValue.Kind.ITEM_STACKS) {
            ItemStack[] stacks = displayValue.itemStacks();
            if (stacks.length == 0) {
                return new ItemStackTexture();
            }
            return new ItemStackTexture(stacks);
        }

        return displayValue.texture() != null ? displayValue.texture() : new ColorRectTexture(0xFF555555);
    }

    private List<Component> buildEntryTooltips(ResearchInfoEntry entry) {
        List<Component> tooltips = new ArrayList<>();

        if (entry.display() != null && entry.display().kind() == ResearchDisplayValue.Kind.ITEM_STACKS) {
            for (ItemStack stack : entry.display().itemStacks()) {
                if (stack != null && !stack.isEmpty()) {
                    tooltips.add(stack.getHoverName());
                }
            }
        }

        tooltips.addAll(entry.tooltips());
        return tooltips;
    }

    private String formatEntryText(ResearchInfoEntry entry) {
        if (entry.kind() != ResearchInfoEntry.Kind.CONDITION) {
            return entry.text() == null ? "" : entry.text();
        }
        return (entry.completed() ? "[OK] " : "[WAIT] ") + (entry.text() == null ? "" : entry.text());
    }

    private int resolveEntryTextColor(ResearchInfoEntry entry) {
        return switch (entry.kind()) {
            case CONDITION -> entry.completed() ? 0xFF95E59A : 0xFFFF8E8E;
            case REWARD -> 0xFFB7E6FF;
            case INFO -> -1;
        };
    }

    private Label createWrappingLabel() {
        Label label = new Label();
        label.layout(layout -> layout.widthPercent(100));
        label.textStyle(style -> style
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true));
        return label;
    }

    private void updateTimedProgressFill(float progress01, int fillColor) {
        float clamped = Math.max(0f, Math.min(1f, progress01));
        timedProgressFill.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0)
                .top(0)
                .width(currentProgressBarWidth * clamped)
                .height(PROGRESS_BAR_HEIGHT)
        );
        timedProgressFill.style(style -> style.backgroundTexture(new ColorRectTexture(fillColor)));
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        applyResponsiveLayout();
    }

    private void applyResponsiveLayout() {
        UIElement parent = getParent();
        if (parent == null) {
            return;
        }

        float parentWidth = parent.getContentWidth();
        float parentHeight = parent.getContentHeight();
        if (parentWidth <= 0f || parentHeight <= 0f) {
            return;
        }

        float availableWidth = Math.max(0f, parentWidth - PANEL_MARGIN * 2f);
        float availableHeight = Math.max(0f, parentHeight - PANEL_MARGIN * 2f);

        float responsiveWidth = Math.min(DEFAULT_WIDTH, availableWidth * 0.46f);
        float newWidth = availableWidth <= MIN_WIDTH
                ? availableWidth
                : Math.max(MIN_WIDTH, responsiveWidth);
        newWidth = Math.min(newWidth, DEFAULT_WIDTH);

        float top = Math.min(PREFERRED_TOP, Math.max(18f, parentHeight * 0.10f));
        float maxHeight = Math.max(0f, parentHeight - top - PANEL_MARGIN);
        float newHeight = maxHeight <= MIN_HEIGHT
                ? maxHeight
                : Math.max(MIN_HEIGHT, Math.min(DEFAULT_HEIGHT, maxHeight));

        if (Math.abs(newWidth - currentPanelWidth) > 0.5f || Math.abs(newHeight - currentPanelHeight) > 0.5f) {
            currentPanelWidth = newWidth;
            currentPanelHeight = newHeight;
            currentProgressBarWidth = Math.max(72f, currentPanelWidth - 16f);

            layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(PANEL_MARGIN)
                    .top(top)
                    .width(currentPanelWidth)
                    .height(currentPanelHeight)
                    .paddingAll(8)
                    .gapAll(5)
            );
            timedProgressBar.layout(layout -> layout.width(currentProgressBarWidth).height(PROGRESS_BAR_HEIGHT));
            closeButton.layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(currentPanelWidth - 36f)
                    .top(6)
                    .width(24)
                    .height(18)
            );
        }
    }

    private float hiddenTranslateX() {
        return -(currentPanelWidth + 18f);
    }
}
