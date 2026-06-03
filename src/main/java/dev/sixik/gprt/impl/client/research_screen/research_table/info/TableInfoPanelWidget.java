package dev.sixik.gprt.impl.client.research_screen.research_table.info;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.*;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TableInfoPanelWidget extends AdaptiveResearchInfoPanelWidget {

    private static final float PROGRESS_BAR_HEIGHT = 10f;
    private static final float ENTRY_TEXT_INFO_TOP_OFFSET = 2f;
    private static final float ENTRY_TEXT_CONDITION_TOP_OFFSET = 3f;
    private static final float ENTRY_TEXT_REWARD_TOP_OFFSET = 4f;
    private static final IGuiTexture ACTIVE_RESEARCH_BUTTON_TEXTURE = GuiTextureGroup.of(
            new ColorRectTexture(0xFF2D5E84),
            new ColorBorderTexture(-1, 0xFF73C2FF)
    );
    private static final IGuiTexture DISABLED_RESEARCH_BUTTON_TEXTURE = GuiTextureGroup.of(
            new ColorRectTexture(0xFF252A31),
            new ColorBorderTexture(-1, 0xFF4A5560)
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

    public TableInfoPanelWidget(ResearchInfoPanelContext context) {
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

        closeButton = new Button().setText("Close").setOnClick(event -> context().close());
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
        titleLabel.setText(content.title());
        titleLabel.textStyle(style -> style
                .fontSize(content.titleLarge() ? 15f : 10f)
                .textAlignHorizontal(content.titleAlign() == ResearchInfoContent.TitleAlign.CENTER ? Horizontal.CENTER : Horizontal.LEFT)
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true));

        metaLabel.setText(content.groupText() + " | " + content.modeText() + " | " + content.stateText());
        descriptionLabel.setText(content.description());
        style(style -> style.backgroundTexture(IGuiTexture.group(
                new ColorRectTexture(0xD0221820),
                new ColorBorderTexture(-2, content.panelColor())
        )));

        timedProgressLabel.setDisplay(content.showTimedProgress());
        timedProgressBar.setDisplay(content.showTimedProgress());
        timedProgressLabel.setText(content.timedProgressText());
        updateProgress(content.timedProgress01(), content.timedProgressFillColor());

        rebuildSections(content.sections());

        researchButton.setDisplay(content.showResearchButton());
        researchButton.setText(content.researchButtonText());
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
                        .gapAll(4)
                        .flexDirection(FlexDirection.ROW));

        List<Component> tooltips = buildEntryTooltips(entry);

        if (entry.display() != null) {
            row.addChild(createDisplayElement(entry.display(), tooltips));
        }

        String formattedText = formatEntryText(entry);
        if (!formattedText.isBlank()) {
            UIElement textWrapper = new UIElement()
                    .layout(layout -> layout.flex(1).gapAll(0));

            if (entry.display() != null) {
                textWrapper.addChild(new UIElement()
                        .layout(layout -> layout.widthPercent(100).height(resolveEntryTextTopOffset(entry))));
            }

            Label text = new Label();
            text.layout(layout -> layout.widthPercent(100));
            text.setText(formattedText);
            text.textStyle(style -> style
                    .textWrap(TextWrap.WRAP)
                    .adaptiveHeight(true)
                    .textColor(resolveEntryColor(entry)));

            if (!tooltips.isEmpty()) {
                text.style(style -> style.tooltips(Tooltips.of(tooltips)));
            }
            textWrapper.addChild(text);
            row.addChild(textWrapper);
        }

        if (entry.showJumpButton() && entry.jumpToResearchKey() != null && !entry.jumpToResearchKey().isBlank()) {
            Button jumpButton = new Button()
                    .setText(entry.jumpButtonText() != null ? entry.jumpButtonText() : "Open")
                    .setOnClick(event -> context().jumpToResearch(entry.jumpToResearchKey()));
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
        return displayValue.texture() != null ? displayValue.texture() : new ColorRectTexture(0x66FFFFFF);
    }

    private String formatEntryText(ResearchInfoEntry entry) {
        String prefix = switch (entry.kind()) {
            case INFO -> "- ";
            case CONDITION -> entry.completed() ? "[OK] " : "[WAIT] ";
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

    private float resolveEntryTextTopOffset(ResearchInfoEntry entry) {
        return switch (entry.kind()) {
            case INFO -> ENTRY_TEXT_INFO_TOP_OFFSET;
            case CONDITION -> ENTRY_TEXT_CONDITION_TOP_OFFSET;
            case REWARD -> ENTRY_TEXT_REWARD_TOP_OFFSET;
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
