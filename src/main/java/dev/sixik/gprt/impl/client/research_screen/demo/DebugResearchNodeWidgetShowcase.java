package dev.sixik.gprt.impl.client.research_screen.demo;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchGroup;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoPresentationRules;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeGroupThemeResolver;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeRenderContext;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeTheme;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeThemes;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeVisualDefinition;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeWidgetFactory;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ClientResearchProgress;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/**
 * Debug-only showcase for research node styling.
 * <p>
 * This file intentionally lives outside the core research-tree package so it can act as a
 * sandbox/example for mod authors:
 * different node groups use different presets, different internal compositions and different
 * text placement rules, while the production API stays clean.
 * </p>
 *
 * <p><b>How to read this file:</b></p>
 * <ul>
     *     <li>{@link #configureThemePresets(ResearchNodeGroupThemeResolver.Builder, ResearchGroup, ResearchGroup, ResearchGroup, ResearchGroup)} -
     *     declares per-group visual presets. This is the "high-level styling" entry point.</li>
 *     <li>{@link #createWidgetFactory()} -
 *     quick entry point that returns the default debug factory in {@link StyleMode#BRANCH_SHOWCASE} mode.</li>
 *     <li>{@link #createWidgetFactory(Supplier)} -
 *     returns a custom widget factory used only by the debug screen and lets that screen switch styles at runtime.</li>
 *     <li>{@link ShowcaseNodeWidgetFactory} -
 *     bridges the core tree API with the custom widget implementation.</li>
 *     <li>{@link ShowcaseNodeWidget} -
 *     the actual tutorial widget that demonstrates composition from simple UI pieces.</li>
 * </ul>
 *
 * <p><b>What this file demonstrates:</b></p>
 * <ul>
 *     <li>How to assign different visuals to different research groups.</li>
 *     <li>How to place title/subtitle in completely different positions per node style.</li>
 *     <li>How to render custom layered backgrounds with optional accents, chips and frames.</li>
 *     <li>How to use item icons as research-node icons.</li>
 *     <li>How to render a timed progress bar directly on the node.</li>
 * </ul>
 *
 * <p><b>Important idea:</b></p>
 * <ul>
 *     <li>The core research-tree API does not require one rigid node layout.</li>
 *     <li>You can keep the same data/progression system and completely replace the node widget visuals.</li>
 *     <li>This file is intentionally verbose so future-you can reopen it and quickly remember where each part is customized.</li>
 * </ul>
 */
public final class DebugResearchNodeWidgetShowcase {
    /**
     * Runtime style switch used by the debug screen.
     * <p>
     * The point of this enum is to show that the same research data can be rendered through
     * completely different widget compositions without changing the tree logic itself.
     * </p>
     */
    public enum StyleMode {
        /**
         * Preset that emphasizes big silhouettes and group-specific accent pieces.
         */
        BRANCH_SHOWCASE("Branch Showcase"),
        /**
         * Preset that renders nodes as compact technical cards with a cleaner, stripe-free frame.
         */
        TECH_CARDS("Tech Cards");

        private final String displayName;

        StyleMode(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

        public StyleMode next() {
            return this == BRANCH_SHOWCASE ? TECH_CARDS : BRANCH_SHOWCASE;
        }
    }

    private DebugResearchNodeWidgetShowcase() {
    }

    /**
     * Registers debug-only presets for each branch/group.
     * <p>
     * Think of this method as the "theme layer":
     * it decides colors, badge text and text alignment before the node widget is even built.
     * The widget then reads the already-resolved visual definition and only worries about layout/rendering.
     * </p>
     *
     * <p>
     * This is the easiest extension point when you only want to restyle groups without rewriting
     * the widget composition logic itself.
     * </p>
     */
    public static void configureThemePresets(ResearchNodeGroupThemeResolver.Builder builder,
                                             ResearchGroup rootGroup,
                                             ResearchGroup metallurgyGroup,
                                             ResearchGroup farmingGroup,
                                             ResearchGroup logisticsGroup
    ) {
        builder.group(rootGroup, theme -> ResearchNodeThemes.rootPreset(rootGroup).toBuilder()
                .revealAnimationStyle(ResearchRevealAnimationStyle.FADE_SCALE)
                .build());
        builder.group(metallurgyGroup, theme -> theme
                .primaryColor(metallurgyGroup.getPrimaryColor())
                .secondaryColor(metallurgyGroup.getSecondaryColor())
                .accentColor(0xFFFFC766)
                .badge("FORGE", 0xFFF0C17C)
                .revealAnimationStyle(ResearchRevealAnimationStyle.SPLIT_FUSE)
                .badgeTextShadow(false)
                .titleAlignment(ResearchNodeVisualDefinition.TitleAlignment.LEFT));
        builder.group(farmingGroup, theme -> theme
                .primaryColor(farmingGroup.getPrimaryColor())
                .secondaryColor(farmingGroup.getSecondaryColor())
                .accentColor(0xFF9BE27F)
                .badge("GROW", 0xFFA7E3A4)
                .revealAnimationStyle(ResearchRevealAnimationStyle.FADE_SCALE)
                .badgeTextShadow(false)
                .titleAlignment(ResearchNodeVisualDefinition.TitleAlignment.LEFT));
        builder.group(logisticsGroup, theme -> theme
                .primaryColor(logisticsGroup.getPrimaryColor())
                .secondaryColor(logisticsGroup.getSecondaryColor())
                .accentColor(0xFF8BC0FF)
                .badge("FLOW", 0xFFA9CBFF)
                .revealAnimationStyle(ResearchRevealAnimationStyle.SPLIT_FUSE)
                .badgeTextShadow(false)
                .titleAlignment(ResearchNodeVisualDefinition.TitleAlignment.LEFT));
    }

    /**
     * Extended debug preset registration that also demonstrates the extra reveal-animation styles.
     * <p>
     * This overload is useful for compact demo trees where each group is meant to represent one
     * distinct reveal behavior.
     * </p>
     */
    public static void configureThemePresets(ResearchNodeGroupThemeResolver.Builder builder,
                                             ResearchGroup rootGroup,
                                             ResearchGroup metallurgyGroup,
                                             ResearchGroup farmingGroup,
                                             ResearchGroup logisticsGroup,
                                             ResearchGroup energyGroup,
                                             ResearchGroup alchemyGroup
    ) {
        configureThemePresets(builder, rootGroup, metallurgyGroup, farmingGroup, logisticsGroup);
        builder.group(energyGroup, theme -> theme
                .primaryColor(energyGroup.getPrimaryColor())
                .secondaryColor(energyGroup.getSecondaryColor())
                .accentColor(0xFFFFE07A)
                .badge("SPARK", 0xFFFFEDAE)
                .revealAnimationStyle(ResearchRevealAnimationStyle.FADE_SCALE)
                .badgeTextShadow(false)
                .titleAlignment(ResearchNodeVisualDefinition.TitleAlignment.LEFT));
        builder.group(alchemyGroup, theme -> theme
                .primaryColor(alchemyGroup.getPrimaryColor())
                .secondaryColor(alchemyGroup.getSecondaryColor())
                .accentColor(0xFFD8B4FF)
                .badge("MIST", 0xFFE7D4FF)
                .revealAnimationStyle(ResearchRevealAnimationStyle.SPLIT_FUSE)
                .badgeTextShadow(false)
                .titleAlignment(ResearchNodeVisualDefinition.TitleAlignment.LEFT));
    }

    /**
     * Creates the debug widget factory used by {@code ResearchTreeScreenDebug}.
     * <p>
     * In a real project you could swap this for another factory entirely, but for the tutorial
     * screen we keep the custom rendering logic isolated in this one file.
     * This overload is intentionally the simplest one: it always starts from
     * {@link StyleMode#BRANCH_SHOWCASE}.
     * </p>
     */
    public static ResearchNodeWidgetFactory createWidgetFactory() {
        return createWidgetFactory(() -> StyleMode.BRANCH_SHOWCASE);
    }

    /**
     * Creates a widget factory that can switch visual style at runtime.
     * <p>
     * The supplier is queried every refresh, so the debug screen can flip between styles
     * and then call a node refresh without recreating the entire screen.
     * This is useful as a tutorial because it proves the node data/model is independent from
     * the visual composition used to render that data.
     * </p>
     */
    public static ResearchNodeWidgetFactory createWidgetFactory(Supplier<StyleMode> styleModeSupplier) {
        return new ShowcaseNodeWidgetFactory(styleModeSupplier);
    }

    /**
     * Minimal adapter between the graph system and the custom tutorial widget.
     * <p>
     * The graph only knows that it needs a {@link ResearchNodeWidgetFactory}. It does not care
     * whether the widget is a plain button, a complex card or something heavily animated.
     * </p>
     */
    private static final class ShowcaseNodeWidgetFactory implements ResearchNodeWidgetFactory {
        private final Supplier<StyleMode> styleModeSupplier;

        private ShowcaseNodeWidgetFactory(Supplier<StyleMode> styleModeSupplier) {
            this.styleModeSupplier = styleModeSupplier;
        }

        @Override
        public UIElement createNodeWidget(ResearchNode node,
                                          ResearchNodeRenderContext context,
                                          ResearchNodeVisualDefinition visualDefinition,
                                          Runnable onClick
        ) {
            ShowcaseNodeWidget widget = new ShowcaseNodeWidget(onClick, styleModeSupplier);
            widget.apply(node, context, visualDefinition);
            return widget;
        }

        @Override
        public void updateNodeWidget(UIElement widget,
                                     ResearchNode node,
                                     ResearchNodeRenderContext context,
                                     ResearchNodeVisualDefinition visualDefinition
        ) {
            if (widget instanceof ShowcaseNodeWidget showcaseNodeWidget) {
                showcaseNodeWidget.apply(node, context, visualDefinition);
            }
        }
    }

    /**
     * High-level layout presets used inside the custom widget.
     * <p>
     * Each variant intentionally rearranges the same building blocks differently so this file
     * demonstrates that node widgets are not locked to one composition style.
     * </p>
     */
    private enum LayoutVariant {
        ROOT_CENTER,
        METALLURGY_LEFT,
        FARMING_BOTTOM_BANNER,
        LOGISTICS_SPLIT
    }

    /**
     * Tutorial implementation of one research-node widget.
     * <p>
     * This class is deliberately split into small methods:
     * frame/background, decorations, icon, title, badge and progress.
     * That makes it much easier to understand than one giant render/setup method.
     * </p>
     *
     * <p><b>Navigation inside this widget:</b></p>
     * <ul>
     *     <li>{@link #apply(ResearchNode, ResearchNodeRenderContext, ResearchNodeVisualDefinition)} -
     *     main refresh entry point called on create and on updates.</li>
     *     <li>{@link #applyButtonFrame(ResearchNodeRenderContext, ResearchNodeVisualDefinition, StyleMode)} -
     *     outer card background and border.</li>
     *     <li>{@link #applyDecor(float, float, LayoutVariant, ResearchNodeVisualDefinition, StyleMode)} -
     *     extra visual accents such as stripes and split dividers.</li>
     *     <li>{@link #applyIcon(ResearchNode, float, float, LayoutVariant, ResearchNodeVisualDefinition, StyleMode)} -
     *     item icon frame and placement.</li>
     *     <li>{@link #applyTitle(ResearchNode, ResearchNodeRenderContext, float, float, LayoutVariant, ResearchNodeVisualDefinition, StyleMode)} -
     *     title/subtitle layout and text style.</li>
     *     <li>{@link #applyBadge(float, float, LayoutVariant, ResearchNodeVisualDefinition, StyleMode)} -
     *     small badge chip in the corner.</li>
     *     <li>{@link #applyProgress(ResearchNode, ResearchNodeRenderContext, float, float, ResearchNodeVisualDefinition, StyleMode)} -
     *     timed progress bar.</li>
     * </ul>
     */
    private static final class ShowcaseNodeWidget extends Button {
        private static final float PROGRESS_BAR_HEIGHT = 5f;

        private final Supplier<StyleMode> styleModeSupplier;
        private final UIElement topStripe;
        private final UIElement leftStripe;
        private final UIElement iconFrame;
        private final UIElement iconElement;
        private final UIElement badgeChip;
        private final UIElement progressTrack;
        private final UIElement progressFill;
        private final Label titleLabel;
        private final Label subtitleLabel;
        private final Label badgeLabel;

        /**
         * Builds the widget structure once.
         * <p>
         * Important distinction:
         * the child hierarchy is created in the constructor, while colors/positions/text are updated
         * later in {@link #apply(ResearchNode, ResearchNodeRenderContext, ResearchNodeVisualDefinition)}.
         * This avoids rebuilding widgets every refresh tick.
         * </p>
         */
        private ShowcaseNodeWidget(Runnable onClick, Supplier<StyleMode> styleModeSupplier) {
            this.styleModeSupplier = styleModeSupplier;
            noText();
            setOnClick(event -> onClick.run());
            style(style -> style.overflowVisible(true));

            // Decorative layers. These are optional stripes/dividers that different layout variants
            // can turn on or off to create distinct silhouettes for each branch.
            topStripe = new UIElement();
            leftStripe = new UIElement();

            // Icon area = outer frame + actual icon texture inside.
            iconFrame = new UIElement();
            iconElement = new UIElement();
            iconFrame.addChild(iconElement);

            // Badge area = chip background + small text on top.
            badgeChip = new UIElement();
            badgeLabel = new Label();
            badgeChip.addChild(badgeLabel);

            // Text labels are separate on purpose so title and subtitle can be positioned independently.
            titleLabel = new Label();
            subtitleLabel = new Label();

            // Progress bar is a classic track/fill pair so its width can be updated cheaply.
            progressTrack = new UIElement();
            progressFill = new UIElement();
            progressTrack.addChild(progressFill);

            // This is the full node composition. From here on we only mutate style, text and bounds.
            addChildren(topStripe, leftStripe, iconFrame, titleLabel, subtitleLabel, badgeChip, progressTrack);
        }

        /**
         * Main refresh entry point.
         * <p>
         * Every time the tree needs to update the node, this method reapplies the current visual state:
         * selected, locked, progress, title text, badge, etc.
         * </p>
         */
        private void apply(ResearchNode node,
                           ResearchNodeRenderContext context,
                           ResearchNodeVisualDefinition visualDefinition
        ) {
            // Keep tutorial widgets in layout during reveal playback.
            // Using display=false collapses bounds to 0x0, which breaks snapshot alignment.
            setDisplay(true);
            style(style -> style.opacity(context.isVisible() ? 1f : 0f));
            setActive(context.isVisible() && !context.isInteractionLocked());

            float width = node.getWidth();
            float height = node.getHeight();
            StyleMode styleMode = styleModeSupplier.get();
            LayoutVariant variant = resolveVariant(node, styleMode);

            applyButtonFrame(context, visualDefinition, styleMode);
            applyDecor(width, height, variant, visualDefinition, styleMode);
            applyIcon(node, width, height, variant, visualDefinition, styleMode);
            applyTitle(node, context, width, height, variant, visualDefinition, styleMode);
            applyBadge(width, height, variant, visualDefinition, styleMode);
            applyProgress(node, context, width, height, visualDefinition, styleMode);
            applyRevealAccent(context);
        }

        /**
         * Configures the outer clickable card.
         * <p>
         * This is the best place to change the main background and border behavior:
         * hover, pressed, selected feel, temporary interaction-lock styling and so on.
         * </p>
         */
        private void applyButtonFrame(ResearchNodeRenderContext context,
                                      ResearchNodeVisualDefinition visualDefinition,
                                      StyleMode styleMode
        ) {
            int borderSize = context.isSelected() ? 2 : 1;
            int backgroundColor = styleMode == StyleMode.TECH_CARDS
                    ? withAlpha(darkenColor(visualDefinition.getBackgroundColor(), 0.48f), 0xF0)
                    : visualDefinition.getBackgroundColor();
            int borderColor = styleMode == StyleMode.TECH_CARDS
                    ? brightenColor(visualDefinition.getAccentColor(), 0.10f)
                    : visualDefinition.getBorderColor();

            buttonStyle(style -> style
                    .baseTexture(GuiTextureGroup.of(
                            new ColorRectTexture(backgroundColor),
                            new ColorBorderTexture(borderSize, borderColor)
                    ))
                    .hoverTexture(GuiTextureGroup.of(
                            new ColorRectTexture(brightenColor(backgroundColor, 0.08f)),
                            new ColorBorderTexture(borderSize, brightenColor(borderColor, 0.10f))
                    ))
                    .pressedTexture(GuiTextureGroup.of(
                            new ColorRectTexture(darkenColor(backgroundColor, 0.08f)),
                            new ColorBorderTexture(borderSize, brightenColor(borderColor, 0.06f))
                    )));
        }

        /**
         * Adds extra shape language on top of the base frame.
         * <p>
         * Here we show that the same node system can feel very different by adding simple accents:
         * top stripe, left stripe, bottom banner or center divider.
         * For {@link StyleMode#TECH_CARDS} we intentionally keep the frame cleaner and skip those
         * extra stripes entirely, so it reads more like a compact technical card without the old
         * decorative bar behind the content.
         * </p>
         */
        private void applyDecor(float width,
                                float height,
                                LayoutVariant variant,
                                ResearchNodeVisualDefinition visualDefinition,
                                StyleMode styleMode
        ) {
            if (styleMode == StyleMode.TECH_CARDS) {
                topStripe.setDisplay(false);
                leftStripe.setDisplay(false);
                return;
            }

            topStripe.style(style -> style.backgroundTexture(new ColorRectTexture(visualDefinition.getAccentColor())));
            leftStripe.style(style -> style.backgroundTexture(new ColorRectTexture(withAlpha(visualDefinition.getAccentColor(), 0xA8))));

            switch (variant) {
                case ROOT_CENTER -> {
                    topStripe.setDisplay(true);
                    topStripe.layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(0)
                            .top(0)
                            .width(width)
                            .height(4));
                    leftStripe.setDisplay(false);
                }
                case METALLURGY_LEFT -> {
                    topStripe.setDisplay(false);
                    leftStripe.setDisplay(true);
                    leftStripe.layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(0)
                            .top(0)
                            .width(5)
                            .height(height));
                }
                case FARMING_BOTTOM_BANNER -> {
                    topStripe.setDisplay(true);
                    topStripe.layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(0)
                            .top(height - 16f)
                            .width(width)
                            .height(16));
                    leftStripe.setDisplay(false);
                }
                case LOGISTICS_SPLIT -> {
                    topStripe.setDisplay(true);
                    topStripe.layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(width * 0.48f)
                            .top(0)
                            .width(3)
                            .height(height));
                    leftStripe.setDisplay(false);
                }
            }
        }

        /**
         * Places the node icon.
         * <p>
         * The icon itself is rendered from an {@link ItemStackTexture}, but the important lesson here
         * is positioning: each layout variant puts the icon in a different place without changing
         * the underlying research data.
         * </p>
         */
        private void applyIcon(ResearchNode node,
                               float width,
                               float height,
                               LayoutVariant variant,
                               ResearchNodeVisualDefinition visualDefinition,
                               StyleMode styleMode
        ) {
            iconFrame.setDisplay(true);
            iconFrame.style(style -> style.backgroundTexture(GuiTextureGroup.of(
                    new ColorRectTexture(withAlpha(0xFFFFFFFF, 0x14)),
                    new ColorBorderTexture(1, withAlpha(visualDefinition.getBorderColor(), 0xCC))
            )));
            iconElement.style(style -> style.backgroundTexture(new ItemStackTexture(resolveIconStack(node))));

            if (styleMode == StyleMode.TECH_CARDS) {
                iconFrame.layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(8)
                        .top(height / 2f - 10f)
                        .width(20)
                        .height(20));
                iconElement.layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(2)
                        .top(2)
                        .width(16)
                        .height(16));
                return;
            }

            switch (variant) {
                case ROOT_CENTER -> iconFrame.layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(width / 2f - 11f)
                        .top(8)
                        .width(22)
                        .height(22));
                case METALLURGY_LEFT -> iconFrame.layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(10)
                        .top(height / 2f - 10f)
                        .width(20)
                        .height(20));
                case FARMING_BOTTOM_BANNER -> iconFrame.layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(width - 28f)
                        .top(8)
                        .width(20)
                        .height(20));
                case LOGISTICS_SPLIT -> iconFrame.layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(8)
                        .top(8)
                        .width(18)
                        .height(18));
            }
            iconElement.layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(2)
                    .top(2)
                    .width(iconFrame == null ? 16 : 16)
                    .height(16));
        }

        /**
         * Places title and subtitle.
         * <p>
         * This method is one of the most useful examples in the file:
         * it shows that text can be centered, left-aligned, pushed into a banner, split into another
         * region, recolored per style, and fully moved around with absolute positioning.
         * </p>
         */
        private void applyTitle(ResearchNode node,
                                ResearchNodeRenderContext context,
                                float width,
                                float height,
                                LayoutVariant variant,
                                ResearchNodeVisualDefinition visualDefinition,
                                StyleMode styleMode
        ) {
            String subtitle = buildSubtitle(node, context);
            float titleFont = switch (visualDefinition.getSizePreset()) {
                case SMALL -> 8f;
                case MEDIUM -> 9f;
                case LARGE -> 11f;
            };

            titleLabel.setText(node.getTitle());
            subtitleLabel.setText(subtitle);
            titleLabel.setDisplay(visualDefinition.isTitleVisible());
            subtitleLabel.setDisplay(visualDefinition.isSubtitleVisible());

            if (styleMode == StyleMode.TECH_CARDS) {
                titleLabel.layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(34)
                        .top(7)
                        .width(width - 76f)
                        .height(13));
                subtitleLabel.layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(34)
                        .top(height - 15f)
                        .width(width - 46f)
                        .height(10));
                titleLabel.textStyle(style -> style
                        .fontSize(titleFont)
                        .textAlignHorizontal(Horizontal.LEFT)
                        .textWrap(TextWrap.WRAP)
                        .adaptiveHeight(true)
                        .textShadow(visualDefinition.hasTitleTextShadow())
                        .textColor(0xFFEFF7FF));
                subtitleLabel.textStyle(style -> style
                        .fontSize(7f)
                        .textAlignHorizontal(Horizontal.LEFT)
                        .textWrap(TextWrap.WRAP)
                        .adaptiveHeight(true)
                        .textShadow(visualDefinition.hasSubtitleTextShadow())
                        .textColor(0xFF8FA6C4));
                return;
            }

            switch (variant) {
                case ROOT_CENTER -> {
                    titleLabel.layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(10)
                            .top(34)
                            .width(width - 20f)
                            .height(14));
                    subtitleLabel.layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(12)
                            .top(height - 15f)
                            .width(width - 24f)
                            .height(10));
                    titleLabel.textStyle(style -> style
                            .fontSize(titleFont)
                            .textAlignHorizontal(Horizontal.CENTER)
                            .textWrap(TextWrap.WRAP)
                            .adaptiveHeight(true)
                            .textShadow(visualDefinition.hasTitleTextShadow())
                            .textColor(0xFFF7FBFF));
                    subtitleLabel.textStyle(style -> style
                            .fontSize(7f)
                            .textAlignHorizontal(Horizontal.CENTER)
                            .textWrap(TextWrap.WRAP)
                            .adaptiveHeight(true)
                            .textShadow(visualDefinition.hasSubtitleTextShadow())
                            .textColor(0xFFDDEBFF));
                }
                case METALLURGY_LEFT -> {
                    titleLabel.layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(36)
                            .top(7)
                            .width(width - 58f)
                            .height(13));
                    subtitleLabel.layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(36)
                            .top(height - 15f)
                            .width(width - 46f)
                            .height(10));
                    titleLabel.textStyle(style -> style
                            .fontSize(titleFont)
                            .textAlignHorizontal(Horizontal.LEFT)
                            .textWrap(TextWrap.WRAP)
                            .adaptiveHeight(true)
                            .textShadow(visualDefinition.hasTitleTextShadow())
                            .textColor(0xFFFFF6E8));
                    subtitleLabel.textStyle(style -> style
                            .fontSize(7f)
                            .textAlignHorizontal(Horizontal.LEFT)
                            .textWrap(TextWrap.WRAP)
                            .adaptiveHeight(true)
                            .textShadow(visualDefinition.hasSubtitleTextShadow())
                            .textColor(0xFFFFD9A7));
                }
                case FARMING_BOTTOM_BANNER -> {
                    titleLabel.layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(8)
                            .top(height - 15f)
                            .width(width - 42f)
                            .height(10));
                    subtitleLabel.layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(8)
                            .top(8)
                            .width(width - 40f)
                            .height(10));
                    titleLabel.textStyle(style -> style
                            .fontSize(titleFont)
                            .textAlignHorizontal(Horizontal.LEFT)
                            .textWrap(TextWrap.WRAP)
                            .adaptiveHeight(true)
                            .textShadow(visualDefinition.hasTitleTextShadow())
                            .textColor(0xFF0E2313));
                    subtitleLabel.textStyle(style -> style
                            .fontSize(7f)
                            .textAlignHorizontal(Horizontal.LEFT)
                            .textWrap(TextWrap.WRAP)
                            .adaptiveHeight(true)
                            .textShadow(visualDefinition.hasSubtitleTextShadow())
                            .textColor(0xFFE5F8DD));
                }
                case LOGISTICS_SPLIT -> {
                    titleLabel.layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(width * 0.52f + 6f)
                            .top(7)
                            .width(width * 0.44f - 10f)
                            .height(13));
                    subtitleLabel.layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(8)
                            .top(height - 15f)
                            .width(width - 16f)
                            .height(10));
                    titleLabel.textStyle(style -> style
                            .fontSize(titleFont)
                            .textAlignHorizontal(Horizontal.LEFT)
                            .textWrap(TextWrap.WRAP)
                            .adaptiveHeight(true)
                            .textShadow(visualDefinition.hasTitleTextShadow())
                            .textColor(0xFFEFF7FF));
                    subtitleLabel.textStyle(style -> style
                            .fontSize(7f)
                            .textAlignHorizontal(Horizontal.LEFT)
                            .textWrap(TextWrap.WRAP)
                            .adaptiveHeight(true)
                            .textShadow(visualDefinition.hasSubtitleTextShadow())
                            .textColor(0xFFB9D8FF));
                }
            }
        }

        /**
         * Positions the small corner badge.
         * <p>
         * Badge text comes from the resolved visual definition, so themes can control its content,
         * while the widget controls only how and where it is drawn.
         * </p>
         */
        private void applyBadge(float width,
                                float height,
                                LayoutVariant variant,
                                ResearchNodeVisualDefinition visualDefinition,
                                StyleMode styleMode
        ) {
            boolean showBadge = visualDefinition.isBadgeVisible() && !visualDefinition.getBadgeText().isBlank();
            badgeChip.setDisplay(showBadge);
            if (!showBadge) {
                return;
            }

            badgeChip.style(style -> style.backgroundTexture(GuiTextureGroup.of(
                    new ColorRectTexture(visualDefinition.getBadgeColor()),
                    new ColorBorderTexture(1, brightenColor(visualDefinition.getBadgeColor(), 0.14f))
            )));
            badgeLabel.setText(visualDefinition.getBadgeText(), false);
            badgeLabel.layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(0)
                    .top(1)
                    .width(34)
                    .height(8));

            badgeLabel.textStyle(style -> style
                    .fontSize(6.5f)
                    .textAlignHorizontal(Horizontal.CENTER)
                    .adaptiveHeight(true)
                    .textShadow(visualDefinition.hasBadgeTextShadow())
                    .textColor(0xFF11161D));

            if (styleMode == StyleMode.TECH_CARDS) {
                badgeChip.layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(width - 40f)
                        .top(height - 20f)
                        .width(34)
                        .height(10));
                return;
            }

            float chipLeft = switch (variant) {
                case ROOT_CENTER -> width - 40f;
                case METALLURGY_LEFT -> width - 40f;
                case FARMING_BOTTOM_BANNER -> width - 40f;
                case LOGISTICS_SPLIT -> width * 0.52f + 2f;
            };
            float chipTop = switch (variant) {
                case ROOT_CENTER -> 8f;
                case METALLURGY_LEFT -> 8f;
                case FARMING_BOTTOM_BANNER -> height - 28f;
                case LOGISTICS_SPLIT -> 8f;
            };

            badgeChip.layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(chipLeft)
                    .top(chipTop)
                    .width(34)
                    .height(10));
        }

        /**
         * Renders timed progress directly on the node.
         * <p>
         * Only {@link ResearchStudyType#TIMED} researches in {@link ResearchState#IN_PROGRESS}
         * show this bar. This is a good reference if you later want to add other overlay bars or meters.
         * </p>
         */
        private void applyProgress(ResearchNode node,
                                   ResearchNodeRenderContext context,
                                   float width,
                                   float height,
                                   ResearchNodeVisualDefinition visualDefinition,
                                   StyleMode styleMode
        ) {
            boolean showProgress = visualDefinition.isProgressVisible()
                    && node.getStudyType() == ResearchStudyType.TIMED
                    && context.getState() == ResearchState.IN_PROGRESS
                    && context.getProgress() != null;
            progressTrack.setDisplay(showProgress);
            if (!showProgress) {
                return;
            }

            ClientResearchProgress progress = context.getProgress();
            float progress01 = progress == null ? 0f : progress.getProgress01(context.getNowMs());
            float trackWidth = styleMode == StyleMode.TECH_CARDS ? width - 46f : width - 16f;
            float trackLeft = styleMode == StyleMode.TECH_CARDS ? 34f : 8f;
            progressTrack.layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(trackLeft)
                    .top(height - 8f)
                    .width(trackWidth)
                    .height(PROGRESS_BAR_HEIGHT));
            progressTrack.style(style -> style.backgroundTexture(new ColorRectTexture(visualDefinition.getProgressBarBackgroundColor())));
            progressFill.layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(0)
                    .top(0)
                    .width(trackWidth * Math.max(0f, Math.min(1f, progress01)))
                    .height(PROGRESS_BAR_HEIGHT));
            progressFill.style(style -> style.backgroundTexture(new ColorRectTexture(visualDefinition.getProgressBarColor())));
        }

        private void applyRevealAccent(ResearchNodeRenderContext context) {
            if (!context.isUnlockAnimating()) {
                resetRevealTransforms();
                return;
            }

            float progress01 = Math.max(0f, Math.min(1f, context.getUnlockNodeProgress01()));
            switch (context.getUnlockRevealAnimationStyle()) {
                case DROP_BOUNCE -> {
                    float iconScale = lerp(0.80f, 1.0f, easeOutBack(progress01));
                    float badgeScale = lerp(0.88f, 1.0f, easeOutBack(progress01));
                    float stripeOffset = lerp(-8f, 0f, easeOutCubic(progress01));
                    applyTransform(iconFrame, 0.5f, 0.5f, 0f, 0f, iconScale);
                    applyTransform(badgeChip, 0.5f, 0.5f, 0f, 0f, badgeScale);
                    applyTransform(topStripe, 0f, 0f, 0f, stripeOffset, 1f);
                    applyTransform(leftStripe, 0f, 0f, stripeOffset, 0f, 1f);
                    applyTransform(titleLabel, 0f, 0f, 0f, 0f, 1f);
                    applyTransform(subtitleLabel, 0f, 0f, 0f, 0f, 1f);
                }
                case SOFT_POP -> {
                    float eased = easeOutCubic(progress01);
                    float textScale = lerp(0.92f, 1f, eased);
                    float iconScale = lerp(0.94f, 1f, eased);
                    applyTransform(iconFrame, 0.5f, 0.5f, 0f, 0f, iconScale);
                    applyTransform(titleLabel, 0f, 0f, 0f, 0f, textScale);
                    applyTransform(subtitleLabel, 0f, 0f, 0f, 0f, textScale);
                    applyTransform(badgeChip, 0.5f, 0.5f, 0f, 0f, lerp(0.95f, 1f, eased));
                    applyTransform(topStripe, 0f, 0f, 0f, 0f, 1f);
                    applyTransform(leftStripe, 0f, 0f, 0f, 0f, 1f);
                }
                case SLIDE_FROM_LEFT -> {
                    float eased = easeOutCubic(progress01);
                    applyTransform(iconFrame, 0.5f, 0.5f, 0f, 0f, lerp(1.08f, 1f, eased));
                    applyTransform(titleLabel, 0f, 0f, 0f, 0f, 1f);
                    applyTransform(subtitleLabel, 0f, 0f, 0f, 0f, 1f);
                    applyTransform(badgeChip, 0.5f, 0.5f, 0f, 0f, lerp(1.04f, 1f, eased));
                    applyTransform(topStripe, 0f, 0f, 0f, 0f, 1f);
                    applyTransform(leftStripe, 0f, 0f, 0f, 0f, 1f);
                }
                case FADE_SCALE -> {
                    float eased = easeOutCubic(progress01);
                    float scale = lerp(0.86f, 1f, eased);
                    applyTransform(iconFrame, 0.5f, 0.5f, 0f, 0f, lerp(0.90f, 1f, eased));
                    applyTransform(titleLabel, 0f, 0f, 0f, 0f, scale);
                    applyTransform(subtitleLabel, 0f, 0f, 0f, 0f, scale);
                    applyTransform(badgeChip, 0.5f, 0.5f, 0f, 0f, lerp(0.92f, 1f, eased));
                    applyTransform(topStripe, 0f, 0f, 0f, 0f, lerp(0.75f, 1f, eased));
                    applyTransform(leftStripe, 0f, 0f, 0f, 0f, lerp(0.75f, 1f, eased));
                }
                case ARC_DROP -> {
                    float eased = easeOutCubic(progress01);
                    float bounce = easeOutBack(progress01);
                    applyTransform(iconFrame, 0.5f, 0.5f, 0f, 0f, lerp(1.16f, 1f, bounce));
                    applyTransform(titleLabel, 0f, 0f, 0f, 0f, 1f);
                    applyTransform(subtitleLabel, 0f, 0f, 0f, 0f, 1f);
                    applyTransform(badgeChip, 0.5f, 0.5f, 0f, 0f, lerp(1.08f, 1f, eased));
                    applyTransform(topStripe, 0f, 0f, 0f, 0f, 1f);
                    applyTransform(leftStripe, 0f, 0f, 0f, 0f, 1f);
                }
                case SPLIT_FUSE -> {
                    float eased = easeOutCubic(progress01);
                    applyTransform(iconFrame, 0.5f, 0.5f, 0f, 0f, lerp(0.96f, 1f, eased));
                    applyTransform(titleLabel, 0f, 0f, 0f, 0f, lerp(0.94f, 1f, eased));
                    applyTransform(subtitleLabel, 0f, 0f, 0f, 0f, lerp(0.94f, 1f, eased));
                    applyTransform(badgeChip, 0.5f, 0.5f, 0f, 0f, lerp(0.96f, 1f, eased));
                    applyTransform(topStripe, 0f, 0f, 0f, 0f, 1f);
                    applyTransform(leftStripe, 0f, 0f, 0f, 0f, 1f);
                }
            }
        }

        /**
         * Chooses which internal composition to use for the current node.
         * <p>
         * In this tutorial the variant is based on the research group, but you could just as easily
         * switch by research key, research state, rarity, tech tier, unlock stage or any other rule.
         * </p>
         */
        private static LayoutVariant resolveVariant(ResearchNode node, StyleMode styleMode) {
            if (styleMode == StyleMode.TECH_CARDS) {
                return LayoutVariant.LOGISTICS_SPLIT;
            }
            String groupId = node.getGroup() != null ? node.getGroup().getId() : "";
            return switch (groupId) {
                case "root" -> LayoutVariant.ROOT_CENTER;
                case "metallurgy" -> LayoutVariant.METALLURGY_LEFT;
                case "farming" -> LayoutVariant.FARMING_BOTTOM_BANNER;
                case "logistics" -> LayoutVariant.LOGISTICS_SPLIT;
                default -> LayoutVariant.ROOT_CENTER;
            };
        }

        /**
         * Demo icon mapping from research key to an item icon.
         * <p>
         * This is intentionally simple and hardcoded because the file is a tutorial.
         * In a real project this could come from the research definition, a registry or a client asset config.
         * </p>
         */
        private static ItemStack resolveIconStack(ResearchNode node) {
            String key = node.getResearchKey();
            if (key == null) {
                return new ItemStack(Items.BOOK);
            }
            return switch (key) {
                case "primitive_tools" -> new ItemStack(Items.WOODEN_PICKAXE);
                case "metallurgy" -> new ItemStack(Items.IRON_INGOT);
                case "alloying" -> new ItemStack(Items.COPPER_INGOT);
                case "steel" -> new ItemStack(Items.IRON_BLOCK);
                case "steam" -> new ItemStack(Items.LAVA_BUCKET);
                case "chemistry" -> new ItemStack(Items.GLASS_BOTTLE);
                case "machines" -> new ItemStack(Items.PISTON);
                case "farming" -> new ItemStack(Items.WHEAT);
                case "irrigation" -> new ItemStack(Items.WATER_BUCKET);
                case "breeding" -> new ItemStack(Items.HAY_BLOCK);
                case "greenhouses" -> new ItemStack(Items.GLASS);
                case "food_processing" -> new ItemStack(Items.BREAD);
                case "logistics" -> new ItemStack(Items.CHEST);
                case "forge_notes" -> new ItemStack(Items.COPPER_INGOT);
                case "seed_sorting" -> new ItemStack(Items.WHEAT_SEEDS);
                case "carts" -> new ItemStack(Items.MINECART);
                case "storage" -> new ItemStack(Items.BARREL);
                case "rail" -> new ItemStack(Items.RAIL);
                case "warehouse" -> new ItemStack(Items.CHEST_MINECART);
                case "rope_making" -> new ItemStack(Items.LEAD);
                case "spark_ignition" -> new ItemStack(Items.REDSTONE_TORCH);
                case "crystal_solvent" -> new ItemStack(Items.AMETHYST_SHARD);
                default -> new ItemStack(Items.BOOK);
            };
        }

        /**
         * Builds the small status line under/near the title.
         * <p>
         * The goal here is to show one simple pattern:
         * let gameplay state decide the subtitle text, while the layout methods decide where it appears.
         * </p>
         */
        private static String buildSubtitle(ResearchNode node, ResearchNodeRenderContext context) {
            ClientResearchProgress progress = context.getProgress();
            long nowMs = context.getNowMs();
            return switch (context.getState()) {
                case STUDIED -> "Studied";
                case LOCKED -> "Locked";
                case AVAILABLE -> switch (node.getStudyType()) {
                    case TIMED -> "Ready - " + ResearchInfoPresentationRules.formatDuration(node.getStudyDurationMs());
                    case TABLE -> "Open table research";
                    case INSTANT -> "Ready to study";
                };
                case IN_PROGRESS -> switch (node.getStudyType()) {
                    case TIMED -> {
                        float progress01 = progress == null ? 0f : progress.getProgress01(nowMs);
                        long remainingMs = progress == null ? node.getStudyDurationMs() : progress.getRemainingMs(nowMs);
                        yield Math.round(progress01 * 100f) + "% - " + ResearchInfoPresentationRules.formatDuration(remainingMs);
                    }
                    case TABLE -> "Table session active";
                    case INSTANT -> "In progress";
                };
            };
        }

        // Small local color helpers keep the demo self-contained. They are duplicated on purpose so the
        // tutorial file can be copied or adapted independently from the main production classes.
        private static int withAlpha(int color, int alpha) {
            return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
        }

        private static int brightenColor(int color, float amount) {
            int alpha = (color >>> 24) & 0xFF;
            int red = (color >>> 16) & 0xFF;
            int green = (color >>> 8) & 0xFF;
            int blue = color & 0xFF;
            float clampedAmount = Math.max(0f, Math.min(1f, amount));
            red += Math.round((255 - red) * clampedAmount);
            green += Math.round((255 - green) * clampedAmount);
            blue += Math.round((255 - blue) * clampedAmount);
            return (alpha << 24) | (Math.min(255, red) << 16) | (Math.min(255, green) << 8) | Math.min(255, blue);
        }

        private static int darkenColor(int color, float amount) {
            int alpha = (color >>> 24) & 0xFF;
            float factor = 1f - Math.max(0f, Math.min(1f, amount));
            int red = Math.max(0, Math.round(((color >>> 16) & 0xFF) * factor));
            int green = Math.max(0, Math.round(((color >>> 8) & 0xFF) * factor));
            int blue = Math.max(0, Math.round((color & 0xFF) * factor));
            return (alpha << 24) | (red << 16) | (green << 8) | blue;
        }

        private void resetRevealTransforms() {
            applyTransform(iconFrame, 0.5f, 0.5f, 0f, 0f, 1f);
            applyTransform(titleLabel, 0f, 0f, 0f, 0f, 1f);
            applyTransform(subtitleLabel, 0f, 0f, 0f, 0f, 1f);
            applyTransform(badgeChip, 0.5f, 0.5f, 0f, 0f, 1f);
            applyTransform(topStripe, 0f, 0f, 0f, 0f, 1f);
            applyTransform(leftStripe, 0f, 0f, 0f, 0f, 1f);
        }

        private static void applyTransform(UIElement element,
                                           float pivotX,
                                           float pivotY,
                                           float translateX,
                                           float translateY,
                                           float scale
        ) {
            element.style(style -> style.transform2D(new Transform2D()
                    .pivot(pivotX, pivotY)
                    .translate(translateX, translateY)
                    .scale(scale)));
        }

        private static float lerp(float start, float end, float delta) {
            return start + (end - start) * delta;
        }

        private static float easeOutCubic(float t) {
            float clamped = Math.max(0f, Math.min(1f, t));
            return 1f - (float) Math.pow(1f - clamped, 3);
        }

        private static float easeOutBack(float t) {
            float clamped = Math.max(0f, Math.min(1f, t));
            float c1 = 1.70158f;
            float c3 = c1 + 1f;
            float shifted = clamped - 1f;
            return 1f + c3 * shifted * shifted * shifted + c1 * shifted * shifted;
        }
    }
}
