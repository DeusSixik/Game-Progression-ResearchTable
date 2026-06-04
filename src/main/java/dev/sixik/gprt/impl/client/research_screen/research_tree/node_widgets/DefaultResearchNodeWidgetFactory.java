package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoPresentationRules;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ClientResearchProgress;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;

/**
 * Default reusable implementation for research node widgets.
 * <p>
 * The widget is intentionally updated in place instead of being recreated every frame:
 * the graph owns node positioning, while this factory only owns visuals and volatile state.
 * That keeps node rendering extensible without coupling the main screen to one specific layout.
 * </p>
 */
public final class DefaultResearchNodeWidgetFactory implements ResearchNodeWidgetFactory {
    @Override
    public UIElement createNodeWidget(ResearchNode node,
                                      ResearchNodeRenderContext context,
                                      ResearchNodeVisualDefinition visualDefinition,
                                      Runnable onClick
    ) {
        NodeWidget widget = new NodeWidget(onClick);
        widget.apply(node, context, visualDefinition);
        return widget;
    }

    @Override
    public void updateNodeWidget(UIElement widget,
                                 ResearchNode node,
                                 ResearchNodeRenderContext context,
                                 ResearchNodeVisualDefinition visualDefinition
    ) {
        if (widget instanceof NodeWidget nodeWidget) {
            nodeWidget.apply(node, context, visualDefinition);
        }
    }

    private static final class NodeWidget extends Button {
        private static final float PROGRESS_BAR_HEIGHT = 5f;

        private final UIElement accentBar;
        private final UIElement headerRow;
        private final UIElement iconElement;
        private final Label titleLabel;
        private final Label badgeLabel;
        private final Label subtitleLabel;
        private final UIElement progressBar;
        private final UIElement progressFill;

        private NodeWidget(Runnable onClick) {
            noText();
            setOnClick(event -> onClick.run());

            layout(layout -> layout
                    .widthPercent(100)
                    .heightPercent(100)
                    .paddingAll(5)
                    .gapAll(3)
                    .flexDirection(FlexDirection.COLUMN)
            );
            style(style -> style.overflowVisible(true));

            accentBar = new UIElement()
                    .layout(layout -> layout.widthPercent(100).height(3));

            headerRow = new UIElement()
                    .layout(layout -> layout
                            .widthPercent(100)
                            .gapAll(4)
                            .flexDirection(FlexDirection.ROW)
                    );

            iconElement = new UIElement()
                    .layout(layout -> layout.width(14).height(14));

            titleLabel = new Label();
            titleLabel.layout(layout -> layout.flex(1));
            titleLabel.textStyle(style -> style
                    .textWrap(TextWrap.WRAP)
                    .adaptiveHeight(true));

            badgeLabel = new Label();
            badgeLabel.layout(layout -> layout.height(14));
            badgeLabel.textStyle(style -> style
                    .fontSize(7f)
                    .textAlignHorizontal(Horizontal.CENTER)
                    .adaptiveHeight(true)
                    .textColor(0xFF091018));

            subtitleLabel = new Label();
            subtitleLabel.layout(layout -> layout.widthPercent(100));
            subtitleLabel.textStyle(style -> style
                    .fontSize(8f)
                    .textWrap(TextWrap.WRAP)
                    .adaptiveHeight(true)
                    .textColor(0xFFD7E6F5));

            progressBar = new UIElement()
                    .layout(layout -> layout.widthPercent(100).height(PROGRESS_BAR_HEIGHT))
                    .style(style -> style.backgroundTexture(new ColorRectTexture(0x55344657)));
            progressFill = new UIElement()
                    .layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(0)
                            .top(0)
                            .widthPercent(0)
                            .height(PROGRESS_BAR_HEIGHT)
                    );
            progressBar.addChild(progressFill);

            headerRow.addChildren(iconElement, titleLabel, badgeLabel);
            addChildren(accentBar, headerRow, subtitleLabel, progressBar);
        }

        private void apply(ResearchNode node,
                           ResearchNodeRenderContext context,
                           ResearchNodeVisualDefinition visualDefinition
        ) {
            // Keep the widget in layout even when the live version should be visually hidden for
            // reveal playback. Collapsing it with display=false resets absolute bounds to 0x0,
            // which breaks snapshot/capture alignment for reveal animations.
            setDisplay(true);
            style(style -> style.opacity(context.isVisible() ? 1f : 0f));
            setActive(context.isVisible() && !context.isInteractionLocked());

            applyFrameStyle(context, visualDefinition);
            applyAccentBar(visualDefinition);
            applyIcon(visualDefinition);
            applyTitle(node, visualDefinition);
            applyBadge(visualDefinition);
            applySubtitle(node, context, visualDefinition);
            applyProgress(node, context, visualDefinition);
        }

        private void applyFrameStyle(ResearchNodeRenderContext context, ResearchNodeVisualDefinition visualDefinition) {
            int borderSize = context.isSelected() ? 2 : 1;
            int backgroundColor = visualDefinition.getBackgroundColor();
            int borderColor = visualDefinition.getBorderColor();

            IGuiTexture base = buildFrameTexture(backgroundColor, borderColor, borderSize);
            IGuiTexture hover = buildFrameTexture(brightenColor(backgroundColor, 0.08f), brightenColor(borderColor, 0.10f), borderSize);
            IGuiTexture pressed = buildFrameTexture(darkenColor(backgroundColor, 0.08f), brightenColor(borderColor, 0.06f), borderSize);
            buttonStyle(style -> style
                    .baseTexture(base)
                    .hoverTexture(hover)
                    .pressedTexture(pressed));
        }

        private void applyAccentBar(ResearchNodeVisualDefinition visualDefinition) {
            accentBar.style(style -> style.backgroundTexture(new ColorRectTexture(visualDefinition.getAccentColor())));
        }

        private void applyIcon(ResearchNodeVisualDefinition visualDefinition) {
            boolean showIcon = visualDefinition.isIconVisible() && !visualDefinition.getIconPath().isBlank();
            iconElement.setDisplay(showIcon);
            if (showIcon) {
                iconElement.style(style -> style.backgroundTexture(SpriteTexture.of(visualDefinition.getIconPath())));
            }
        }

        private void applyTitle(ResearchNode node, ResearchNodeVisualDefinition visualDefinition) {
            titleLabel.setDisplay(visualDefinition.isTitleVisible());
            titleLabel.setText(node.getTitle());
            titleLabel.textStyle(style -> style
                    .fontSize(resolveTitleFontSize(visualDefinition.getSizePreset()))
                    .textWrap(TextWrap.WRAP)
                    .adaptiveHeight(true)
                    .textShadow(visualDefinition.hasTitleTextShadow())
                    .textAlignHorizontal(visualDefinition.getTitleAlignment() == ResearchNodeVisualDefinition.TitleAlignment.CENTER
                            ? Horizontal.CENTER
                            : Horizontal.LEFT)
                    .textColor(0xFFF3F8FF));
        }

        private void applyBadge(ResearchNodeVisualDefinition visualDefinition) {
            boolean showBadge = visualDefinition.isBadgeVisible() && !visualDefinition.getBadgeText().isBlank();
            badgeLabel.setDisplay(showBadge);
            if (showBadge) {
                badgeLabel.setText(visualDefinition.getBadgeText());
                badgeLabel.textStyle(style -> style
                        .fontSize(7f)
                        .textAlignHorizontal(Horizontal.CENTER)
                        .adaptiveHeight(true)
                        .textShadow(visualDefinition.hasBadgeTextShadow())
                        .textColor(0xFF091018));
                badgeLabel.style(style -> style.backgroundTexture(
                        GuiTextureGroup.of(
                                new ColorRectTexture(visualDefinition.getBadgeColor()),
                                new ColorBorderTexture(1, brightenColor(visualDefinition.getBadgeColor(), 0.18f))
                        )
                ));
            }
        }

        private void applySubtitle(ResearchNode node,
                                   ResearchNodeRenderContext context,
                                   ResearchNodeVisualDefinition visualDefinition
        ) {
            subtitleLabel.setDisplay(visualDefinition.isSubtitleVisible());
            if (!visualDefinition.isSubtitleVisible()) {
                return;
            }
            subtitleLabel.setText(buildSubtitleText(node, context));
            subtitleLabel.textStyle(style -> style
                    .fontSize(resolveSubtitleFontSize(visualDefinition.getSizePreset()))
                    .textWrap(TextWrap.WRAP)
                    .adaptiveHeight(true)
                    .textShadow(visualDefinition.hasSubtitleTextShadow())
                    .textColor(resolveSubtitleColor(context.getState())));
        }

        private void applyProgress(ResearchNode node,
                                   ResearchNodeRenderContext context,
                                   ResearchNodeVisualDefinition visualDefinition
        ) {
            boolean showProgress = visualDefinition.isProgressVisible()
                    && node.getStudyType() == ResearchStudyType.TIMED
                    && context.getState() == ResearchState.IN_PROGRESS
                    && context.getProgress() != null;

            progressBar.setDisplay(showProgress);
            if (!showProgress) {
                progressFill.layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(0)
                        .top(0)
                        .widthPercent(0)
                        .height(PROGRESS_BAR_HEIGHT));
                return;
            }

            ClientResearchProgress progress = context.getProgress();
            float progress01 = progress == null ? 0f : progress.getProgress01(context.getNowMs());
            progressBar.style(style -> style.backgroundTexture(new ColorRectTexture(visualDefinition.getProgressBarBackgroundColor())));
            progressFill.layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(0)
                    .top(0)
                    .widthPercent(Math.max(0f, Math.min(100f, progress01 * 100f)))
                    .height(PROGRESS_BAR_HEIGHT));
            progressFill.style(style -> style.backgroundTexture(new ColorRectTexture(visualDefinition.getProgressBarColor())));
        }

        private static IGuiTexture buildFrameTexture(int backgroundColor, int borderColor, int borderSize) {
            return GuiTextureGroup.of(
                    new ColorRectTexture(backgroundColor),
                    new ColorBorderTexture(borderSize, borderColor)
            );
        }

        private static String buildSubtitleText(ResearchNode node, ResearchNodeRenderContext context) {
            ResearchState state = context.getState();
            ClientResearchProgress progress = context.getProgress();
            long nowMs = context.getNowMs();
            return switch (state) {
                case STUDIED -> "Studied";
                case LOCKED -> context.isInteractionLocked() ? "Temporarily unavailable" : "Locked";
                case AVAILABLE -> switch (node.getStudyType()) {
                    case TIMED -> "Ready - " + ResearchInfoPresentationRules.formatDuration(node.getStudyDurationMs());
                    case TABLE -> "Ready - table research";
                    case INSTANT -> "Ready to study";
                };
                case IN_PROGRESS -> switch (node.getStudyType()) {
                    case TIMED -> {
                        float progress01 = progress == null ? 0f : progress.getProgress01(nowMs);
                        long remainingMs = progress == null ? node.getStudyDurationMs() : progress.getRemainingMs(nowMs);
                        yield Math.round(progress01 * 100f) + "% - " + ResearchInfoPresentationRules.formatDuration(remainingMs) + " left";
                    }
                    case TABLE -> "Table session active";
                    case INSTANT -> "In progress";
                };
            };
        }

        private static float resolveTitleFontSize(ResearchNodeVisualDefinition.SizePreset preset) {
            return switch (preset) {
                case SMALL -> 8f;
                case MEDIUM -> 9f;
                case LARGE -> 11f;
            };
        }

        private static float resolveSubtitleFontSize(ResearchNodeVisualDefinition.SizePreset preset) {
            return switch (preset) {
                case SMALL -> 6.5f;
                case MEDIUM -> 7.5f;
                case LARGE -> 8.5f;
            };
        }

        private static int resolveSubtitleColor(ResearchState state) {
            return switch (state) {
                case STUDIED -> 0xFFB7ECFF;
                case AVAILABLE -> 0xFFD9F2FF;
                case IN_PROGRESS -> 0xFFE9F7FF;
                case LOCKED -> 0xFFB8C1CC;
            };
        }

        private static int withAlpha(int color, int alpha) {
            return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
        }

        private static int brightenColor(int color, float amount) {
            int alpha = (color >>> 24) & 0xFF;
            int red = (color >>> 16) & 0xFF;
            int green = (color >>> 8) & 0xFF;
            int blue = color & 0xFF;
            float clampedAmount = clamp01(amount);
            red += Math.round((255 - red) * clampedAmount);
            green += Math.round((255 - green) * clampedAmount);
            blue += Math.round((255 - blue) * clampedAmount);
            return (alpha << 24) | (Math.min(255, red) << 16) | (Math.min(255, green) << 8) | Math.min(255, blue);
        }

        private static int darkenColor(int color, float amount) {
            int alpha = (color >>> 24) & 0xFF;
            float factor = 1f - clamp01(amount);
            int red = Math.max(0, Math.round(((color >>> 16) & 0xFF) * factor));
            int green = Math.max(0, Math.round(((color >>> 8) & 0xFF) * factor));
            int blue = Math.max(0, Math.round((color & 0xFF) * factor));
            return (alpha << 24) | (red << 16) | (green << 8) | blue;
        }

        private static float clamp01(float value) {
            return Math.max(0f, Math.min(1f, value));
        }
    }
}
