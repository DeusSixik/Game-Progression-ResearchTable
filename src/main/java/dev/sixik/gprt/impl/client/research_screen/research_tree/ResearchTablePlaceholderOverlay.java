package dev.sixik.gprt.impl.client.research_screen.research_tree;

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
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoPresentationRules;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoTextResolver;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Temporary client-side table-research overlay.
 * <p>
 * This class replaces the old "one text + two buttons" placeholder with a screen layout that is
 * much closer to the intended table gameplay loop:
 * one investigation slot in the middle, a visible snapshot of the player's inventory, and a
 * scrollable journal area where the table can display prompts / observations / reaction text.
 * </p>
 *
 * <p>
 * Important limitation for now: this is still a pure client scaffold.
 * Clicking inventory items only copies a visual preview into the table slot; it does not move
 * real items yet and does not validate any research recipe. The server-facing logic can later be
 * plugged into the same UI without rewriting the whole layout.
 * </p>
 */
public final class ResearchTablePlaceholderOverlay extends UIElement {
    private static final float PANEL_WIDTH = 860f;
    private static final float PANEL_HEIGHT = 490f;
    private static final float MIN_PANEL_WIDTH = 690f;
    private static final float MIN_PANEL_HEIGHT = 400f;
    private static final float PANEL_MARGIN = 8f;
    private static final float INVENTORY_SECTION_HEIGHT = 126f;
    private static final float SLOT_SIZE = 26f;
    private static final int[] DISPLAYED_PLAYER_SLOTS = createDisplayedPlayerSlots();

    private final UIElement panel;
    private final Button closeButton;
    private final UIElement body;
    private final UIElement leftColumn;
    private final UIElement centerColumn;
    private final UIElement rightColumn;
    private final UIElement inventorySection;
    private final Label titleLabel;
    private final Label stateChipLabel;
    private final Label descriptionLabel;
    private final Label helperLabel;
    private final Label selectedItemLabel;
    private final Label inventoryHintLabel;
    private final Label journalHintLabel;
    private final Button completeButton;
    private final Button cancelButton;
    private final Button observeButton;
    private final ResearchSlotButton researchSlotButton;
    private final ScrollerView journalScroller;
    private final UIElement journalContainer;
    private final InventorySlotButton[] inventoryButtons;

    private final List<String> journalEntries = new ArrayList<>();

    private @Nullable ResearchNode currentNode;
    private @Nullable ResearchState currentState;
    private ItemStack researchInputStack = ItemStack.EMPTY;
    private int selectedInventorySlot = -1;
    private @Nullable String openedResearchKey;

    public ResearchTablePlaceholderOverlay(Runnable onComplete, Runnable onCancel, Runnable onClose) {
        layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0)
                .top(0)
                .widthPercent(100)
                .heightPercent(100)
        );
        style(style -> style.backgroundTexture(new ColorRectTexture(0xCC060A10)));
        setDisplay(false);

        panel = new UIElement()
                .layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .width(PANEL_WIDTH)
                        .height(PANEL_HEIGHT)
                        .paddingAll(10)
                        .gapAll(8)
                )
                .style(style -> style.backgroundTexture(
                        GuiTextureGroup.of(
                                new ColorRectTexture(0xF015202D),
                                new ColorBorderTexture(1, 0xFF3A5469)
                        )
                ));

        closeButton = new Button()
                .setText(tr("ui.game_progression_research_table.table_overlay.button.close_short"))
                .setOnClick(event -> onClose.run());
        closeButton.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).width(24).height(18));

        UIElement header = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .gapAll(6)
                        .flexDirection(FlexDirection.ROW)
                );

        titleLabel = new Label();
        titleLabel.layout(layout -> layout.flex(1));
        titleLabel.textStyle(style -> style
                .fontSize(13f)
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true));

        stateChipLabel = new Label();
        stateChipLabel.layout(layout -> layout.width(124).height(18));
        stateChipLabel.textStyle(style -> style
                .fontSize(8f)
                .textAlignHorizontal(Horizontal.CENTER)
                .adaptiveHeight(true));

        header.addChildren(titleLabel, stateChipLabel);

        body = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .gapAll(10)
                        .flex(1)
                        .flexDirection(FlexDirection.ROW)
                );

        leftColumn = new UIElement()
                .layout(layout -> layout.heightPercent(100).gapAll(6));

        descriptionLabel = createWrappingLabel();
        helperLabel = createWrappingLabel();
        helperLabel.textStyle(style -> style
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true)
                .textColor(0xFFB5D4EB));

        selectedItemLabel = createWrappingLabel();
        selectedItemLabel.textStyle(style -> style
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true)
                .textColor(0xFFE8D38C));

        inventoryHintLabel = createWrappingLabel();
        inventoryHintLabel.textStyle(style -> style
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true)
                .textColor(0xFF8AB4D4));

        leftColumn.addChildren(descriptionLabel, helperLabel, selectedItemLabel, inventoryHintLabel);

        centerColumn = new UIElement()
                .layout(layout -> layout.heightPercent(100).gapAll(8));

        Label slotTitle = new Label();
        slotTitle.setText(tr("ui.game_progression_research_table.table_overlay.slot.title"));
        slotTitle.textStyle(style -> style.fontSize(10f));

        researchSlotButton = new ResearchSlotButton(this::clearResearchInput);
        researchSlotButton.layout(layout -> layout
                .width(158)
                .height(158)
        );

        observeButton = new Button()
                .setText(tr("ui.game_progression_research_table.table_overlay.button.observe"))
                .setOnClick(event -> handleObserve());
        observeButton.layout(layout -> layout.widthPercent(100));

        completeButton = new Button()
                .setText(tr("ui.game_progression_research_table.table_overlay.button.complete"))
                .setOnClick(event -> onComplete.run());
        completeButton.layout(layout -> layout.widthPercent(100));

        cancelButton = new Button()
                .setText(tr("ui.game_progression_research_table.table_overlay.button.cancel"))
                .setOnClick(event -> onCancel.run());
        cancelButton.layout(layout -> layout.widthPercent(100));

        centerColumn.addChildren(slotTitle, researchSlotButton, observeButton, completeButton, cancelButton);

        rightColumn = new UIElement()
                .layout(layout -> layout.heightPercent(100).gapAll(6));

        Label journalTitle = new Label();
        journalTitle.setText(tr("ui.game_progression_research_table.table_overlay.journal.title"));
        journalTitle.textStyle(style -> style.fontSize(10f));

        journalHintLabel = createWrappingLabel();
        journalHintLabel.textStyle(style -> style
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true)
                .textColor(0xFF8AB4D4));

        journalContainer = new UIElement()
                .layout(layout -> layout.widthPercent(100).gapAll(5));

        journalScroller = new ScrollerView();
        journalScroller.layout(layout -> layout.widthPercent(100).flex(1));
        journalScroller.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .minScrollPixel(12f)
                .maxScrollPixel(32f));
        journalScroller.viewPort(view -> view
                .layout(layout -> layout.paddingAll(6))
                .style(style -> style.backgroundTexture(new ColorRectTexture(0x33111A24))));
        journalScroller.verticalScroller(scroller -> {
            scroller.headButton.setDisplay(false);
            scroller.tailButton.setDisplay(false);
        });
        journalScroller.addScrollViewChild(journalContainer);

        rightColumn.addChildren(journalTitle, journalHintLabel, journalScroller);

        body.addChildren(leftColumn, centerColumn, rightColumn);

        inventorySection = new UIElement()
                .layout(layout -> layout.widthPercent(100).height(INVENTORY_SECTION_HEIGHT).gapAll(6));

        Label inventoryTitle = new Label();
        inventoryTitle.setText(tr("ui.game_progression_research_table.table_overlay.inventory.title"));
        inventoryTitle.textStyle(style -> style.fontSize(10f));

        UIElement inventoryGrid = new UIElement()
                .layout(layout -> layout.widthPercent(100).gapAll(2));
        inventoryButtons = new InventorySlotButton[DISPLAYED_PLAYER_SLOTS.length];
        for (int row = 0; row < 4; row++) {
            UIElement rowElement = new UIElement()
                    .layout(layout -> layout.widthPercent(100).gapAll(2).flexDirection(FlexDirection.ROW));
            for (int column = 0; column < 9; column++) {
                int displayIndex = row * 9 + column;
                int playerSlot = DISPLAYED_PLAYER_SLOTS[displayIndex];
                InventorySlotButton button = new InventorySlotButton(playerSlot, () -> handleInventorySlotClick(playerSlot));
                inventoryButtons[displayIndex] = button;
                rowElement.addChild(button);
            }
            inventoryGrid.addChild(rowElement);
        }

        inventorySection.addChildren(inventoryTitle, inventoryGrid);

        panel.addChildren(header, body, inventorySection, closeButton);
        addChildren(panel);

        updateResponsiveLayout();
        showEmpty();
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        updateResponsiveLayout();
    }

    public void openFor(@Nullable ResearchNode node, @Nullable ResearchState state) {
        setDisplay(true);
        setActive(true);
        if (node == null || node.getStudyType() != ResearchStudyType.TABLE) {
            showEmpty();
            return;
        }

        boolean researchChanged = openedResearchKey == null || !openedResearchKey.equals(node.getResearchKey());
        if (researchChanged) {
            openedResearchKey = node.getResearchKey();
            researchInputStack = ItemStack.EMPTY;
            selectedInventorySlot = -1;
            journalEntries.clear();
            appendJournalLine(tr("ui.game_progression_research_table.table_overlay.journal.begin",
                    ResearchInfoTextResolver.resolveText(node.getTitle())));
            appendJournalLine(tr("ui.game_progression_research_table.table_overlay.journal.begin_hint"));
        }

        updateFor(node, state);
    }

    public void updateFor(@Nullable ResearchNode node, @Nullable ResearchState state) {
        currentNode = node;
        currentState = state;

        if (node == null || node.getStudyType() != ResearchStudyType.TABLE) {
            showEmpty();
            return;
        }

        refreshHeader();
        refreshDescription();
        refreshButtons();
        refreshResearchSlotVisual();
        refreshInventoryPreview();
        refreshJournal();
    }

    public void showEmpty() {
        currentNode = null;
        currentState = null;
        openedResearchKey = null;
        researchInputStack = ItemStack.EMPTY;
        selectedInventorySlot = -1;
        journalEntries.clear();
        appendJournalLine(tr("ui.game_progression_research_table.table_overlay.journal.empty"));
        refreshHeader();
        refreshDescription();
        refreshButtons();
        refreshResearchSlotVisual();
        refreshInventoryPreview();
        refreshJournal();
    }

    public void hideOverlay() {
        setDisplay(false);
        setActive(false);
    }

    private void updateResponsiveLayout() {
        float availableWidth = getContentWidth();
        float availableHeight = getContentHeight();
        if (availableWidth <= 0f || availableHeight <= 0f) {
            return;
        }

        float maxPanelWidth = Math.max(0f, availableWidth - PANEL_MARGIN * 2f);
        float maxPanelHeight = Math.max(0f, availableHeight - PANEL_MARGIN * 2f);
        float panelWidth = maxPanelWidth <= MIN_PANEL_WIDTH
                ? maxPanelWidth
                : Math.max(MIN_PANEL_WIDTH, Math.min(PANEL_WIDTH, maxPanelWidth));
        float panelHeight = maxPanelHeight <= MIN_PANEL_HEIGHT
                ? maxPanelHeight
                : Math.max(MIN_PANEL_HEIGHT, Math.min(PANEL_HEIGHT, maxPanelHeight));

        float left = Math.max(PANEL_MARGIN, (availableWidth - panelWidth) * 0.5f);
        float top = Math.max(PANEL_MARGIN, (availableHeight - panelHeight) * 0.5f);
        panel.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(left)
                .top(top)
                .width(panelWidth)
                .height(panelHeight)
                .paddingAll(10)
                .gapAll(8));

        closeButton.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(panelWidth - 34f)
                .top(8)
                .width(24)
                .height(18));

        float innerWidth = Math.max(0f, panelWidth - 20f);
        float bodyWidth = innerWidth;
        float columnGapTotal = 20f;
        float sideSpace = Math.max(0f, bodyWidth - columnGapTotal);
        float centerWidth = Math.max(158f, Math.min(210f, sideSpace * 0.24f));
        float leftWidth = Math.max(200f, sideSpace * 0.38f);
        float rightWidth = Math.max(200f, sideSpace - centerWidth - leftWidth);

        if (leftWidth + centerWidth + rightWidth > sideSpace) {
            float overflow = leftWidth + centerWidth + rightWidth - sideSpace;
            rightWidth = Math.max(180f, rightWidth - overflow);
        }

        final float finalLeftWidth = leftWidth;
        final float finalCenterWidth = centerWidth;
        final float finalRightWidth = rightWidth;
        leftColumn.layout(layout -> layout.width(finalLeftWidth).heightPercent(100).gapAll(6));
        centerColumn.layout(layout -> layout.width(finalCenterWidth).heightPercent(100).gapAll(8));
        rightColumn.layout(layout -> layout.width(finalRightWidth).heightPercent(100).gapAll(6));
    }

    private void refreshHeader() {
        if (currentNode == null) {
            titleLabel.setText(tr("ui.game_progression_research_table.table_overlay.title.idle"));
            stateChipLabel.setText(tr("ui.game_progression_research_table.table_overlay.state.idle"));
            stateChipLabel.style(style -> style.backgroundTexture(buildStateChipTexture(0xFF425364)));
            return;
        }

        titleLabel.setText(tr("ui.game_progression_research_table.table_overlay.title.active",
                ResearchInfoTextResolver.resolveText(currentNode.getTitle())));
        stateChipLabel.setText(formatStateText(currentState));
        stateChipLabel.style(style -> style.backgroundTexture(buildStateChipTexture(resolveStateChipColor(currentState))));
    }

    private void refreshDescription() {
        if (currentNode == null) {
            descriptionLabel.setText(tr("ui.game_progression_research_table.table_overlay.description.idle"));
            helperLabel.setText(tr("ui.game_progression_research_table.table_overlay.helper.idle"));
            selectedItemLabel.setText(tr("ui.game_progression_research_table.table_overlay.selected.none"));
            inventoryHintLabel.setText(tr("ui.game_progression_research_table.table_overlay.inventory.hint.idle"));
            journalHintLabel.setText(tr("ui.game_progression_research_table.table_overlay.journal.hint.idle"));
            return;
        }

        String description = currentNode.getDescription();
        if (description == null || description.isBlank()) {
            description = "ui.game_progression_research_table.research_info.description.missing";
        }

        descriptionLabel.setText(ResearchInfoTextResolver.resolveText(description));
        helperLabel.setText(
                tr("ui.game_progression_research_table.table_overlay.helper.active")
        );
        selectedItemLabel.setText(researchInputStack.isEmpty()
                ? tr("ui.game_progression_research_table.table_overlay.selected.none")
                : tr("ui.game_progression_research_table.table_overlay.selected.item",
                        ResearchInfoTextResolver.resolveItemName(researchInputStack)));
        inventoryHintLabel.setText(
                tr("ui.game_progression_research_table.table_overlay.inventory.hint.active")
        );
        journalHintLabel.setText(
                tr("ui.game_progression_research_table.table_overlay.journal.hint.active")
        );
    }

    private void refreshButtons() {
        boolean studied = currentState == ResearchState.STUDIED;
        boolean inProgress = currentState == ResearchState.IN_PROGRESS || currentState == ResearchState.AVAILABLE;
        observeButton.setDisplay(currentNode != null && !studied);
        observeButton.setActive(currentNode != null && !studied);
        completeButton.setDisplay(currentNode != null && !studied);
        completeButton.setActive(currentNode != null && !studied);
        cancelButton.setDisplay(currentNode != null && inProgress);
        cancelButton.setActive(currentNode != null && inProgress);
    }

    private void refreshResearchSlotVisual() {
        researchSlotButton.apply(researchInputStack);
    }

    private void refreshInventoryPreview() {
        LocalPlayer player = Minecraft.getInstance().player;
        for (InventorySlotButton button : inventoryButtons) {
            ItemStack stack = ItemStack.EMPTY;
            if (player != null && button.playerSlot >= 0 && button.playerSlot < player.getInventory().getContainerSize()) {
                stack = player.getInventory().getItem(button.playerSlot);
            }
            button.apply(stack, button.playerSlot == selectedInventorySlot);
        }
    }

    private void refreshJournal() {
        journalContainer.clearAllChildren();
        for (String entry : journalEntries) {
            Label line = createWrappingLabel();
            line.setText("- " + entry);
            line.textStyle(style -> style
                    .textWrap(TextWrap.WRAP)
                    .adaptiveHeight(true)
                    .textColor(0xFFD8E5F1));
            journalContainer.addChild(line);
        }
    }

    private void handleInventorySlotClick(int playerSlot) {
        if (currentNode == null || currentState == ResearchState.STUDIED) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || playerSlot < 0 || playerSlot >= player.getInventory().getContainerSize()) {
            return;
        }

        ItemStack stack = player.getInventory().getItem(playerSlot);
        if (stack.isEmpty()) {
            selectedInventorySlot = playerSlot;
            researchInputStack = ItemStack.EMPTY;
            appendJournalLine(tr("ui.game_progression_research_table.table_overlay.journal.inventory_empty"));
        } else {
            selectedInventorySlot = playerSlot;
            researchInputStack = stack.copy();
            researchInputStack.setCount(1);
            appendJournalLine(tr("ui.game_progression_research_table.table_overlay.journal.place_item",
                    ResearchInfoTextResolver.resolveItemName(stack)));
        }

        refreshDescription();
        refreshResearchSlotVisual();
        refreshInventoryPreview();
        refreshJournal();
    }

    private void clearResearchInput() {
        if (researchInputStack.isEmpty()) {
            return;
        }

        appendJournalLine(tr("ui.game_progression_research_table.table_overlay.journal.remove_item",
                ResearchInfoTextResolver.resolveItemName(researchInputStack)));
        researchInputStack = ItemStack.EMPTY;
        selectedInventorySlot = -1;
        refreshDescription();
        refreshResearchSlotVisual();
        refreshInventoryPreview();
        refreshJournal();
    }

    private void handleObserve() {
        if (currentNode == null) {
            appendJournalLine(tr("ui.game_progression_research_table.table_overlay.journal.no_research"));
        } else if (researchInputStack.isEmpty()) {
            appendJournalLine(tr("ui.game_progression_research_table.table_overlay.journal.no_item"));
        } else {
            String itemName = ResearchInfoTextResolver.resolveItemName(researchInputStack);
            appendJournalLine(tr("ui.game_progression_research_table.table_overlay.journal.observe",
                    itemName,
                    ResearchInfoTextResolver.resolveText(currentNode.getTitle())));
            appendJournalLine(buildObservationLine(currentNode, itemName));
        }

        refreshJournal();
    }

    private void appendJournalLine(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        journalEntries.add(line);
        if (journalEntries.size() > 18) {
            journalEntries.remove(0);
        }
    }

    private String buildObservationLine(ResearchNode node, String itemName) {
        int variant = Math.abs((node.getId() * 31) ^ itemName.hashCode()) % 4;
        return switch (variant) {
            case 0 -> tr("ui.game_progression_research_table.table_overlay.observation.0");
            case 1 -> tr("ui.game_progression_research_table.table_overlay.observation.1", itemName);
            case 2 -> tr("ui.game_progression_research_table.table_overlay.observation.2");
            default -> tr("ui.game_progression_research_table.table_overlay.observation.3",
                    ResearchInfoTextResolver.resolveText(node.getTitle()));
        };
    }

    private static Label createWrappingLabel() {
        Label label = new Label();
        label.layout(layout -> layout.widthPercent(100));
        label.textStyle(style -> style
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true));
        return label;
    }

    private static IGuiTexture buildStateChipTexture(int color) {
        return GuiTextureGroup.of(
                new ColorRectTexture(color),
                new ColorBorderTexture(1, brightenColor(color, 0.18f))
        );
    }

    private static int resolveStateChipColor(@Nullable ResearchState state) {
        if (state == null) {
            return 0xFF425364;
        }
        return switch (state) {
            case LOCKED -> 0xFF6A3A3A;
            case AVAILABLE -> 0xFF3A5E76;
            case IN_PROGRESS -> 0xFF4F5D9B;
            case STUDIED -> 0xFF3F6E4B;
        };
    }

    private static String formatStateText(@Nullable ResearchState state) {
        if (state == null) {
            return tr("ui.game_progression_research_table.table_overlay.state.unknown");
        }
        return ResearchInfoPresentationRules.formatStateText(state, new ResearchNode(-1, 0, 0, 0, 0).setStudyType(ResearchStudyType.TABLE));
    }

    private static String tr(String key, Object... args) {
        return net.minecraft.client.resources.language.I18n.get(key, args);
    }

    private static int brightenColor(int color, float amount) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        r = clampColor(r + Math.round((255 - r) * amount));
        g = clampColor(g + Math.round((255 - g) * amount));
        b = clampColor(b + Math.round((255 - b) * amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static int[] createDisplayedPlayerSlots() {
        int[] slots = new int[36];
        int index = 0;
        for (int slot = 9; slot < 36; slot++) {
            slots[index++] = slot;
        }
        for (int slot = 0; slot < 9; slot++) {
            slots[index++] = slot;
        }
        return slots;
    }

    private static final class ResearchSlotButton extends Button {
        private final UIElement iconElement;
        private final Label titleLabel;
        private final Label hintLabel;

        private ResearchSlotButton(Runnable onClick) {
            noText();
            setOnClick(event -> onClick.run());

            layout(layout -> layout
                    .width(158)
                    .height(158)
                    .paddingAll(8)
                    .gapAll(6)
            );
            buttonStyle(style -> style
                    .baseTexture(GuiTextureGroup.of(
                            new ColorRectTexture(0xFF1A2531),
                            new ColorBorderTexture(1, 0xFF4B687F)
                    ))
                    .hoverTexture(GuiTextureGroup.of(
                            new ColorRectTexture(0xFF213040),
                            new ColorBorderTexture(1, 0xFF6591B3)
                    ))
            );

            iconElement = new UIElement()
                    .layout(layout -> layout
                            .width(54)
                            .height(54));

            titleLabel = new Label();
            titleLabel.layout(layout -> layout.widthPercent(100));
            titleLabel.textStyle(style -> style
                    .fontSize(9f)
                    .textAlignHorizontal(Horizontal.CENTER)
                    .adaptiveHeight(true));

            hintLabel = new Label();
            hintLabel.layout(layout -> layout.widthPercent(100));
            hintLabel.textStyle(style -> style
                    .fontSize(8f)
                    .textWrap(TextWrap.WRAP)
                    .textAlignHorizontal(Horizontal.CENTER)
                    .adaptiveHeight(true)
                    .textColor(0xFFB8C9D8));

            addChildren(iconElement, titleLabel, hintLabel);
        }

        private void apply(ItemStack stack) {
            boolean hasItem = !stack.isEmpty();
            iconElement.style(style -> style.backgroundTexture(hasItem ? new ItemStackTexture(stack) : new ColorRectTexture(0x55293A49)));
            titleLabel.setText(hasItem
                    ? ResearchInfoTextResolver.resolveItemName(stack)
                    : tr("ui.game_progression_research_table.table_overlay.slot.empty_title"));
            hintLabel.setText(hasItem
                    ? tr("ui.game_progression_research_table.table_overlay.slot.hint.remove")
                    : tr("ui.game_progression_research_table.table_overlay.slot.hint.pick"));
            setActive(hasItem);
            if (hasItem) {
                style(style -> style.tooltips(Tooltips.of(List.of(stack.getHoverName()))));
            } else {
                style(style -> style.tooltips(Tooltips.of(List.of())));
            }
        }
    }

    private static final class InventorySlotButton extends Button {
        private final int playerSlot;
        private final UIElement iconElement;
        private final Label countLabel;

        private InventorySlotButton(int playerSlot, Runnable onClick) {
            this.playerSlot = playerSlot;

            noText();
            setOnClick(event -> onClick.run());
            layout(layout -> layout.width(SLOT_SIZE).height(SLOT_SIZE));

            iconElement = new UIElement()
                    .layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(4)
                            .top(4)
                            .width(18)
                            .height(18));

            countLabel = new Label();
            countLabel.layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(13)
                    .top(14)
                    .width(10)
                    .height(10));
            countLabel.textStyle(style -> style
                    .fontSize(7f)
                    .textAlignHorizontal(Horizontal.RIGHT)
                    .adaptiveHeight(true));

            addChildren(iconElement, countLabel);
        }

        private void apply(ItemStack stack, boolean selected) {
            int borderColor = selected ? 0xFFE8D38C : 0xFF4A5A68;
            int baseColor = stack.isEmpty() ? 0x88141D26 : 0xAA1B2733;
            buttonStyle(style -> style
                    .baseTexture(GuiTextureGroup.of(
                            new ColorRectTexture(baseColor),
                            new ColorBorderTexture(1, borderColor)
                    ))
                    .hoverTexture(GuiTextureGroup.of(
                            new ColorRectTexture(brightenColor(baseColor, 0.08f)),
                            new ColorBorderTexture(1, brightenColor(borderColor, 0.14f))
                    ))
            );

            if (stack.isEmpty()) {
                iconElement.style(style -> style.backgroundTexture(new ColorRectTexture(0x4425323F)));
                countLabel.setDisplay(false);
                style(style -> style.tooltips(Tooltips.of(List.of())));
                return;
            }

            iconElement.style(style -> style.backgroundTexture(new ItemStackTexture(stack)));
            countLabel.setDisplay(stack.getCount() > 1);
            countLabel.setText(Integer.toString(stack.getCount()));
            style(style -> style.tooltips(Tooltips.of(List.of(
                    stack.getHoverName(),
                    Component.literal(tr("ui.game_progression_research_table.table_overlay.inventory.slot", playerSlot))
            ))));
        }
    }
}
