package dev.sixik.gprt.impl.client.research_screen.research_table.widget_factory;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.sixik.gprt.impl.client.research_screen.demo.DebugResearchNodeWidgetShowcase;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeRenderContext;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeVisualDefinition;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeWidgetFactory;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeWrapper;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ClientResearchProgress;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ResearchTableNodeWidgetFactory implements ResearchNodeWidgetFactory {

    @Override
    public UIElement createNodeWidget(ResearchNode node, ResearchNodeRenderContext context, ResearchNodeVisualDefinition visualDefinition, Runnable onClick) {
        NodeWidget nodeWidget = new NodeWidget(onClick);
        nodeWidget.apply(node, context, visualDefinition);
        return nodeWidget;
    }

    @Override
    public void updateNodeWidget(UIElement widget, ResearchNode node, ResearchNodeRenderContext context, ResearchNodeVisualDefinition visualDefinition) {
        if (widget instanceof NodeWidget showcaseNodeWidget) {
            showcaseNodeWidget.apply(node, context, visualDefinition);
        }
    }

    /**
     * <p>
     * Important detail for future changes:
     * this widget does NOT change {@link ResearchNodeWrapper} size by itself.
     * Runtime size must already be resolved before the widget is created/updated, otherwise
     * auto-layout, links and camera will still think the node has the old size.
     * </p>
     */
    public static class NodeWidget extends Button {
        private static final float PROGRESS_BAR_HEIGHT = 5f;

        private final Label titleLabel;
        private final Label progressPercentLabel;

        private final UIElement iconFrame;
        private final UIElement iconElement;
        private final UIElement progressTrack;
        private final UIElement progressFill;

        protected NodeWidget(Runnable onClick) {
            noText();
            setOnClick((event) -> onClick.run());

            style(style -> style.overflowVisible(true));
//            layout(layout -> layout
//                    .widthPercent(100)
//                    .heightPercent(100)
//                    .paddingAll(6)
//                    .gapAll(4)
//                    .flexDirection(FlexDirection.COLUMN)
//            );

            titleLabel = new Label();
            titleLabel.textStyle(style -> style
                    .fontSize(9f)
                    .textWrap(TextWrap.WRAP)
                    .adaptiveHeight(true)
                    .textShadow(false)
                    .textColor(0xFFF5F7FB));

            progressPercentLabel = new Label();
            progressPercentLabel.textStyle(style -> style
                    .fontSize(7f)
                    .adaptiveHeight(true)
                    .textShadow(false)
                    .textAlignHorizontal(Horizontal.CENTER)
                    .textColor(0xFFD8E9FF));

            iconFrame = new UIElement();
            iconElement = new UIElement();
            iconFrame.addChild(iconElement);

            progressTrack = new UIElement();
            progressFill = new UIElement();
            progressTrack.addChild(progressFill);

            addChildren(titleLabel, progressPercentLabel, iconFrame, progressTrack);
        }

        protected void apply(ResearchNode node,
                           ResearchNodeRenderContext context,
                           ResearchNodeVisualDefinition visualDefinition
        ) {
            ResearchNodeWrapper wrapper = context.getNodeWrapper();
            setDisplay(context.isVisible());
            setActive(context.isVisible() && !context.isInteractionLocked());

            float width = wrapper.getWidth();
            float height = wrapper.getHeight();

            applyButtonFrame(context, visualDefinition);
            applyIcon(wrapper, width, height, visualDefinition);

            titleLabel.setText(node.getTitle());
            titleLabel.layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(34)
                    .top(7)
                    .width(width - iconFrame.getSizeWidth())
                    .height(13));

            applyProgress(wrapper, context, width, height, visualDefinition);
        }

        private void applyButtonFrame(ResearchNodeRenderContext context,
                                      ResearchNodeVisualDefinition visualDefinition
        ) {
            int borderSize = context.isSelected() ? 2 : 1;
            int backgroundColor = withAlpha(darkenColor(visualDefinition.getBackgroundColor(), 0.48f), 0xF0);
            int borderColor = brightenColor(visualDefinition.getAccentColor(), 0.10f);

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

        private void applyIcon(ResearchNodeWrapper node,
                               float width,
                               float height,
                               ResearchNodeVisualDefinition visualDefinition
        ) {
            iconFrame.setDisplay(true);
            iconFrame.style(style -> style.backgroundTexture(GuiTextureGroup.of(
                    new ColorRectTexture(withAlpha(0xFFFFFFFF, 0x14)),
                    new ColorBorderTexture(1, withAlpha(visualDefinition.getBorderColor(), 0xCC))
            )));
            iconElement.style(style -> style.backgroundTexture(node.getNode().getIconTexture()));


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
        }

        private void applyProgress(ResearchNodeWrapper node,
                                   ResearchNodeRenderContext context,
                                   float width,
                                   float height,
                                   ResearchNodeVisualDefinition visualDefinition
        ) {
            boolean showProgress = visualDefinition.isProgressVisible()
                    && node.getNode().getStudyType() == ResearchStudyType.TIMED
                    && context.getState() == ResearchState.IN_PROGRESS
                    && context.getProgress() != null;
            progressPercentLabel.setDisplay(showProgress);
            progressTrack.setDisplay(showProgress);
            if (!showProgress) {
                return;
            }

            ClientResearchProgress progress = context.getProgress();
            float progress01 = progress == null ? 0f : progress.getProgress01(context.getNowMs());
            float trackWidth = width - 46f;
            float trackLeft = 34f;

            progressPercentLabel.setText(Math.round(progress01 * 100f) + "%");
            progressPercentLabel.layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(trackLeft)
                    .top(height - 18f)
                    .width(trackWidth)
                    .height(8f));

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
    }

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
}
