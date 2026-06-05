package dev.sixik.gprt.impl.client.research_screen.demo;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
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
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.AdaptiveResearchInfoPanelWidget;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ConditionChipTexture;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchDisplayValue;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoContent;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoContentFactory;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoEntry;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoPanelContext;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoSection;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoTextResolver;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.TimedProgressStyleHelper;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Standalone demo implementation of a fully custom research info panel widget.
 * <p>
 * This class exists as a concrete example for mod authors who want their own
 * panel layout while still reusing the shared {@link ResearchInfoContent} model,
 * panel context callbacks and adaptive slide-in behavior.
 * </p>
 */
public final class DebugCustomInfoPanel extends AdaptiveResearchInfoPanelWidget {
    private static final float PROGRESS_BAR_HEIGHT = 10f;
    private static final float CONDITION_CHIP_WIDTH = 48f;
    private static final float CONDITION_CHIP_HEIGHT = 14f;
    private static final float CONDITION_TEXT_TOP_OFFSET = 2f;
    private static final com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture ACTIVE_RESEARCH_BUTTON_TEXTURE = GuiTextureGroup.of(
            new ColorRectTexture(0xFF2D5E84),
            new ColorRectTexture(0x222F4D69)
    );
    private static final com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture DISABLED_RESEARCH_BUTTON_TEXTURE = GuiTextureGroup.of(
            new ColorRectTexture(0xFF252A31),
            new ColorRectTexture(0x221A1F25)
    );

    private final Label titleLabel;
    private final Label metaLabel;
    private final Label descriptionLabel;
    private final Label timedProgressLabel;
    private final UIElement timedProgressBar;
    private final UIElement timedProgressFill;
    private final ScrollerView sectionScroller;
    private final UIElement sectionContainer;
    private final Button researchButton;
    private final Button closeButton;
    private float progressBarWidth = 220f;

    public DebugCustomInfoPanel(ResearchInfoPanelContext context) {
        super(context);

        layout(layout -> layout
                .paddingAll(10)
                .gapAll(6));
        style(style -> style.backgroundTexture(new ColorRectTexture(0xE0121823)));

        titleLabel = new Label();
        titleLabel.layout(layout -> layout.widthPercent(100));
        titleLabel.textStyle(style -> style
                .fontSize(15f)
                .textAlignHorizontal(Horizontal.CENTER)
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true));

        metaLabel = new Label();
        metaLabel.layout(layout -> layout.widthPercent(100));
        metaLabel.textStyle(style -> style
                .fontSize(9f)
                .textAlignHorizontal(Horizontal.CENTER)
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true)
                .textColor(0xFF9BC2FF));

        descriptionLabel = new Label();
        descriptionLabel.layout(layout -> layout.widthPercent(100));
        descriptionLabel.textStyle(style -> style
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true)
                .textColor(0xFFE6EEF8));

        timedProgressLabel = new Label();
        timedProgressBar = new UIElement()
                .layout(layout -> layout.width(progressBarWidth).height(PROGRESS_BAR_HEIGHT))
                .style(style -> style.backgroundTexture(new ColorRectTexture(0x55344657)));
        timedProgressFill = new UIElement()
                .layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(0)
                        .top(0)
                        .width(0)
                        .height(PROGRESS_BAR_HEIGHT))
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF6BC5FF)));
        timedProgressBar.addChild(timedProgressFill);

        sectionContainer = new UIElement()
                .layout(layout -> layout.widthPercent(100).gapAll(6));
        sectionScroller = new ScrollerView();
        sectionScroller.layout(layout -> layout.widthPercent(100).flex(1));
        sectionScroller.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .minScrollPixel(12f)
                .maxScrollPixel(36f));
        sectionScroller.viewPort(view -> view
                .style(style -> style.backgroundTexture(new ColorRectTexture(0x1AFFFFFF))));
        sectionScroller.verticalScroller(scroller -> {
            scroller.headButton.setDisplay(false);
            scroller.tailButton.setDisplay(false);
        });
        sectionScroller.addScrollViewChild(sectionContainer);

        UIElement footer = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .gapAll(6)
                        .flexDirection(FlexDirection.ROW));

        researchButton = new Button().setText("Research").setOnClick(event -> context().startResearch());
        researchButton.layout(layout -> layout.flex(1));

        closeButton = new Button()
                .setText(ResearchInfoTextResolver.resolveText("ui.game_progression_research_table.research_info.button.close"))
                .setOnClick(event -> context().close());
        closeButton.layout(layout -> layout.width(68));

        footer.addChildren(researchButton, closeButton);

        addChildren(
                titleLabel,
                metaLabel,
                descriptionLabel,
                timedProgressLabel,
                timedProgressBar,
                sectionScroller,
                footer
        );

        applyContent(ResearchInfoContentFactory.emptySelection());
        refreshPanelFrame();
    }

    @Override
    protected void onPanelBoundsChanged(float panelWidth, float panelHeight) {
        progressBarWidth = Math.max(96f, panelWidth - 20f);
        timedProgressBar.layout(layout -> layout.width(progressBarWidth).height(PROGRESS_BAR_HEIGHT));
    }

    @Override
    public void applyContent(ResearchInfoContent content) {
        titleLabel.setText(ResearchInfoTextResolver.resolveText(content.title()));
        titleLabel.textStyle(style -> style
                .fontSize(content.titleLarge() ? 15f : 10f)
                .textAlignHorizontal(content.titleAlign() == ResearchInfoContent.TitleAlign.CENTER ? Horizontal.CENTER : Horizontal.LEFT)
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true));

        metaLabel.setText(
                ResearchInfoTextResolver.resolveText(content.groupText()) + " | "
                        + ResearchInfoTextResolver.resolveText(content.modeText()) + " | "
                        + ResearchInfoTextResolver.resolveText(content.stateText()));
        descriptionLabel.setText(ResearchInfoTextResolver.resolveText(content.description()));
        style(style -> style.backgroundTexture(new ColorRectTexture(content.panelColor())));

        timedProgressLabel.setDisplay(content.showTimedProgress());
        timedProgressBar.setDisplay(content.showTimedProgress());
        timedProgressLabel.setText(ResearchInfoTextResolver.resolveText(content.timedProgressText()));
        TimedProgressStyleHelper.apply(
                timedProgressLabel,
                content.timedProgressVisualStyle(),
                content.timedProgress01(),
                content.timedProgressFillColor()
        );
        updateProgress(
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

    private void updateProgress(float progress01, int fillColor) {
        float clamped = Math.max(0f, Math.min(1f, progress01));
        timedProgressFill.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0)
                .top(0)
                .width(progressBarWidth * clamped)
                .height(PROGRESS_BAR_HEIGHT));
        timedProgressFill.style(style -> style.backgroundTexture(new ColorRectTexture(fillColor)));
    }

    private void rebuildSections(List<ResearchInfoSection> sections) {
        sectionContainer.clearAllChildren();

        for (ResearchInfoSection section : sections) {
            if (section.entries().isEmpty()) {
                continue;
            }
            sectionContainer.addChild(createSection(section));
        }
    }

    private UIElement createSection(ResearchInfoSection section) {
        UIElement wrapper = new UIElement()
                .layout(layout -> layout.widthPercent(100).gapAll(4).paddingAll(6))
                .style(style -> style.backgroundTexture(new ColorRectTexture(0x221E2C3A)));

        if (!section.title().isBlank()) {
            Label sectionTitle = new Label();
            sectionTitle.setText(section.title());
            sectionTitle.textStyle(style -> style
                    .fontSize(10f)
                    .textColor(0xFFFFD27A));
            wrapper.addChild(sectionTitle);
        }

        for (ResearchInfoEntry entry : section.entries()) {
            wrapper.addChild(createEntry(entry));
        }

        return wrapper;
    }

    private UIElement createEntry(ResearchInfoEntry entry) {
        UIElement row = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .gapAll(entry.kind() == ResearchInfoEntry.Kind.CONDITION ? 2 : 4)
                        .flexDirection(FlexDirection.ROW));

        List<Component> tooltips = buildEntryTooltips(entry);

        if (entry.kind() == ResearchInfoEntry.Kind.CONDITION) {
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
            row.addChild(prefix);
        }

        UIElement textWrapper = new UIElement()
                .layout(layout -> layout.flex(1).gapAll(0));
        if (entry.kind() == ResearchInfoEntry.Kind.CONDITION) {
            textWrapper.addChild(new UIElement()
                    .layout(layout -> layout.widthPercent(100).height(CONDITION_TEXT_TOP_OFFSET)));
        }

        Label text = new Label();
        text.layout(layout -> layout.widthPercent(100));
        text.setText(formatEntryText(entry));
        text.textStyle(style -> style
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true)
                .textColor(resolveEntryColor(entry)));

        if (!tooltips.isEmpty()) {
            text.style(style -> style.tooltips(Tooltips.of(tooltips)));
        }
        textWrapper.addChild(text);
        row.addChild(textWrapper);

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

    private String formatEntryText(ResearchInfoEntry entry) {
        String prefix = switch (entry.kind()) {
            case INFO -> "- ";
            case CONDITION -> "";
            case REWARD -> "+ ";
        };
        return prefix + (entry.text() == null ? "" : entry.text());
    }

    private int resolveEntryColor(ResearchInfoEntry entry) {
        return switch (entry.kind()) {
            case INFO -> 0xFFE8EEF7;
            case CONDITION -> entry.completed() ? 0xFF9EEA98 : 0xFFFF9797;
            case REWARD -> 0xFF8FD4FF;
        };
    }

    private List<Component> buildEntryTooltips(ResearchInfoEntry entry) {
        List<Component> tooltips = new ArrayList<>(entry.tooltips());
        if (entry.display() != null && entry.display().kind() == ResearchDisplayValue.Kind.ITEM_STACKS) {
            for (ItemStack stack : entry.display().itemStacks()) {
                if (stack != null && !stack.isEmpty()) {
                    tooltips.add(0, stack.getHoverName());
                }
            }
        }
        return tooltips;
    }
}
