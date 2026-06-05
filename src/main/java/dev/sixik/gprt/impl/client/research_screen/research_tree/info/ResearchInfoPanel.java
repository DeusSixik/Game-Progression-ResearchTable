package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable details panel for a selected research node.
 * <p>
 * The panel renders rich sections from {@link ResearchInfoContent}: large centered
 * title, dynamic rows, item/icon displays, tooltips and optional jump buttons.
 * </p>
 */
public final class ResearchInfoPanel extends AdaptiveResearchInfoPanelWidget {
    private static final float PROGRESS_BAR_HEIGHT = 8f;
    private static final float CONDITION_CHIP_WIDTH = 48f;
    private static final float CONDITION_CHIP_HEIGHT = 14f;
    private static final float CONDITION_TEXT_TOP_OFFSET = 2f;
    private static final int SCROLL_BACKGROUND_COLOR = 0x22101824;
    private static final IGuiTexture ACTIVE_RESEARCH_BUTTON_TEXTURE = GuiTextureGroup.of(
            new ColorRectTexture(0xFF355E86),
            new ColorRectTexture(0x222F4D69)
    );
    private static final IGuiTexture DISABLED_RESEARCH_BUTTON_TEXTURE = GuiTextureGroup.of(
            new ColorRectTexture(0xFF2B3138),
            new ColorRectTexture(0x221A1F25)
    );

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
    private float currentProgressBarWidth = preferredPanelWidth() - 16f;

    public ResearchInfoPanel(ResearchInfoPanelContext context) {
        super(context);

        layout(layout -> layout
                .paddingAll(8)
                .gapAll(5)
        );
        style(style -> style.backgroundTexture(new ColorRectTexture(0xE6192432)));

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

        closeButton = new Button().setText("X").setOnClick(event -> context().close());
        closeButton.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(preferredPanelWidth() - 36f)
                .top(6)
                .width(24)
                .height(18)
        );

        researchButton = new Button()
                .setText("Research")
                .setOnClick(event -> context().startResearch());

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
        refreshPanelFrame();
    }

    public void applyContent(ResearchInfoContent content) {
        titleLabel.setText(ResearchInfoTextResolver.resolveText(content.title()));
        titleLabel.textStyle(style -> style
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true)
                .fontSize(content.titleLarge() ? 14f : 9f)
                .textAlignHorizontal(content.titleAlign() == ResearchInfoContent.TitleAlign.CENTER ? Horizontal.CENTER : Horizontal.LEFT)
        );

        groupLabel.setText(ResearchInfoTextResolver.resolveText(content.groupText()));
        modeLabel.setText(ResearchInfoTextResolver.resolveText(content.modeText()));
        stateLabel.setText(ResearchInfoTextResolver.resolveText(content.stateText()));
        descriptionLabel.setText(ResearchInfoTextResolver.resolveText(content.description()));

        style(style -> style.backgroundTexture(new ColorRectTexture(content.panelColor())));

        timedProgressLabel.setDisplay(content.showTimedProgress());
        timedProgressBar.setDisplay(content.showTimedProgress());
        if (content.showTimedProgress()) {
            timedProgressLabel.setText(ResearchInfoTextResolver.resolveText(content.timedProgressText()));
        }
        TimedProgressStyleHelper.apply(
                timedProgressLabel,
                content.timedProgressVisualStyle(),
                content.timedProgress01(),
                content.timedProgressFillColor()
        );
        updateTimedProgressFill(
                content.timedProgress01(),
                TimedProgressStyleHelper.resolveProgressFillColor(
                        content.timedProgressVisualStyle(),
                        content.timedProgress01(),
                        content.timedProgressFillColor()
                )
        );

        rebuildSections(content.sections());

        researchButton.setDisplay(content.showResearchButton());
        researchButton.setText(ResearchInfoTextResolver.resolveText(content.researchButtonText()));
        researchButton.setActive(content.researchButtonEnabled());
        researchButton.style(style -> style.backgroundTexture(
                content.researchButtonEnabled()
                        ? ACTIVE_RESEARCH_BUTTON_TEXTURE
                        : DISABLED_RESEARCH_BUTTON_TEXTURE
        ));
    }

    private void rebuildSections(List<ResearchInfoSection> sections) {
        sectionsContainer.clearAllChildren();
        for (ResearchInfoSection section : sections) {
            if (section.entries().isEmpty()) {
                continue;
            }
            sectionsContainer.addChild(createSectionElement(section));
        }
    }

    private UIElement createSectionElement(ResearchInfoSection section) {
        UIElement container = new UIElement()
                .layout(layout -> layout.widthPercent(100).gapAll(4));

        if (!section.title().isBlank()) {
            Label title = new Label();
            title.setText(ResearchInfoTextResolver.resolveText(section.title()));
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
                .layout(layout -> layout.widthPercent(100).gapAll(entry.kind() == ResearchInfoEntry.Kind.CONDITION ? 2 : 4).flexDirection(FlexDirection.ROW));

        List<Component> tooltips = buildEntryTooltips(entry);

        if (entry.kind() == ResearchInfoEntry.Kind.CONDITION) {
            row.addChild(createConditionPrefixElement(entry, tooltips));
        }

        if (entry.display() != null) {
            row.addChild(createDisplayElement(entry.display(), tooltips));
        }

        if (entry.text() != null && !entry.text().isBlank()) {
            UIElement textWrapper = new UIElement()
                    .layout(layout -> layout.flex(1).gapAll(0));
            if (entry.kind() == ResearchInfoEntry.Kind.CONDITION) {
                textWrapper.addChild(new UIElement()
                        .layout(layout -> layout.widthPercent(100).height(CONDITION_TEXT_TOP_OFFSET)));
            }

            Label text = createWrappingLabel();
            text.layout(layout -> layout.widthPercent(100));
            text.setText(formatEntryText(entry));
            text.textStyle(style -> style.textColor(resolveEntryTextColor(entry)));
            if (!tooltips.isEmpty()) {
                text.style(style -> style.tooltips(Tooltips.of(tooltips)));
            }
            textWrapper.addChild(text);
            row.addChild(textWrapper);
        }

        if (entry.showJumpButton() && entry.jumpToResearchKey() != null && !entry.jumpToResearchKey().isBlank()) {
            Button jumpButton = new Button()
                    .setText((String) (entry.jumpButtonText() != null
                            ? ResearchInfoTextResolver.resolveText(entry.jumpButtonText())
                            : ResearchInfoTextResolver.resolveText("ui.game_progression_research_table.research_info.button.open")))
                    .setOnClick(event -> context().jumpToResearch(entry.jumpToResearchKey()));
            if (!tooltips.isEmpty()) {
                jumpButton.style(style -> style.tooltips(Tooltips.of(tooltips)));
            }
            row.addChild(jumpButton);
        }

        return row;
    }

    private UIElement createConditionPrefixElement(ResearchInfoEntry entry, List<Component> tooltips) {
        Label prefix = new Label();
        prefix.layout(layout -> layout
                .width(CONDITION_CHIP_WIDTH)
                .height(CONDITION_CHIP_HEIGHT)
                .top(2));
        prefix.setText(entry.completed()
                ? ResearchInfoTextResolver.resolveText("ui.game_progression_research_table.research_info.condition.prefix.completed")
                : ResearchInfoTextResolver.resolveText("ui.game_progression_research_table.research_info.condition.prefix.pending"));
        prefix.textStyle(style -> style
                .fontSize(9f)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textShadow(false)
                .textColor(0xFFFFFFFF));
        prefix.style(style -> style.backgroundTexture(new ConditionChipTexture(
                entry.completed() ? 0x5595E59A : 0x55FF8E8E,
                entry.completed() ? 0xFFE0FFE3 : 0xFFFFD6D6
        )));
        if (!tooltips.isEmpty()) {
            prefix.style(style -> style.tooltips(Tooltips.of(tooltips)));
        }
        return prefix;
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
        return entry.text() == null ? "" : entry.text();
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
    protected void onPanelBoundsChanged(float panelWidth, float panelHeight) {
        currentProgressBarWidth = Math.max(72f, panelWidth - 16f);
        timedProgressBar.layout(layout -> layout.width(currentProgressBarWidth).height(PROGRESS_BAR_HEIGHT));
        closeButton.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(panelWidth - 36f)
                .top(6)
                .width(24)
                .height(18)
        );
    }
}
