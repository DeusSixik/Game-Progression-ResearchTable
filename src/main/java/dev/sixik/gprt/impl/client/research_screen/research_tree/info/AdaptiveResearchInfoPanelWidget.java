package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;

/**
 * Base class for custom research info-panel widgets that want shared responsive sizing
 * and the standard slide-in / slide-out behavior.
 * <p>
 * This class handles:
 * </p>
 * <ul>
 *     <li>absolute panel anchoring on the left side of the screen,</li>
 *     <li>responsive width/height clamping against the current viewport,</li>
 *     <li>the shared open/close slide animation,</li>
 *     <li>a resize hook so subclasses can reposition their own child widgets.</li>
 * </ul>
 *
 * <p><b>Typical subclass workflow:</b></p>
 * <ol>
 *     <li>Extend this class instead of {@link ResearchInfoPanelWidget} directly.</li>
 *     <li>Create your own child widgets in the constructor.</li>
 *     <li>Override {@link #onPanelBoundsChanged(float, float)} to reposition internal widgets
 *     whenever the panel size changes.</li>
 *     <li>Call {@link #refreshPanelFrame()} once after child creation to apply the initial layout.</li>
 * </ol>
 */
public abstract class AdaptiveResearchInfoPanelWidget extends ResearchInfoPanelWidget {
    private static final float DEFAULT_WIDTH = 300f;
    private static final float DEFAULT_HEIGHT = 430f;
    private static final float DEFAULT_MIN_WIDTH = 210f;
    private static final float DEFAULT_MIN_HEIGHT = 220f;
    private static final float DEFAULT_PANEL_MARGIN = 8f;
    private static final float DEFAULT_PREFERRED_TOP = 118f;
    private static final float DEFAULT_MIN_TOP = 18f;
    private static final float DEFAULT_WIDTH_FACTOR = 0.46f;
    private static final float DEFAULT_HIDDEN_TRANSLATE_EXTRA = 18f;

    private float currentPanelWidth = preferredPanelWidth();
    private float currentPanelHeight = preferredPanelHeight();

    protected AdaptiveResearchInfoPanelWidget(ResearchInfoPanelContext context) {
        super(context);
        layout(layout -> layout
                .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .left(panelMargin())
                .top(preferredTop())
                .width(currentPanelWidth)
                .height(currentPanelHeight)
        );
        style(style -> style.transform2D(new Transform2D().translate(hiddenTranslateX(), 0f)));
        setDisplay(false);
    }

    protected float preferredPanelWidth() {
        return DEFAULT_WIDTH;
    }

    protected float preferredPanelHeight() {
        return DEFAULT_HEIGHT;
    }

    protected float minPanelWidth() {
        return DEFAULT_MIN_WIDTH;
    }

    protected float minPanelHeight() {
        return DEFAULT_MIN_HEIGHT;
    }

    protected float panelMargin() {
        return DEFAULT_PANEL_MARGIN;
    }

    protected float preferredTop() {
        return DEFAULT_PREFERRED_TOP;
    }

    protected float minTop() {
        return DEFAULT_MIN_TOP;
    }

    protected float widthViewportFactor() {
        return DEFAULT_WIDTH_FACTOR;
    }

    protected float hiddenTranslateExtra() {
        return DEFAULT_HIDDEN_TRANSLATE_EXTRA;
    }

    protected float openTranslateX() {
        return 0f;
    }

    protected final float panelWidth() {
        return currentPanelWidth;
    }

    protected final float panelHeight() {
        return currentPanelHeight;
    }

    /**
     * Resize hook for subclasses.
     * <p>
     * Override this to reposition internal widgets after the responsive panel size changes.
     * </p>
     */
    protected void onPanelBoundsChanged(float panelWidth, float panelHeight) {
    }

    /**
     * Forces the responsive outer frame to refresh immediately.
     * <p>
     * Subclasses should call this once after their child widgets are created so they can receive
     * the initial {@link #onPanelBoundsChanged(float, float)} callback.
     * </p>
     */
    protected final void refreshPanelFrame() {
        applyResponsiveLayout(true);
    }

    @Override
    public void setSlideProgress(float progress01) {
        float clamped = Math.max(0f, Math.min(1f, progress01));
        float hiddenTranslateX = hiddenTranslateX();
        float translateX = hiddenTranslateX + (openTranslateX() - hiddenTranslateX) * clamped;
        style(style -> style.transform2D(new Transform2D().translate(translateX, 0f)));
        if (clamped <= 0f) {
            setDisplay(false);
        }
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        applyResponsiveLayout(false);
    }

    private void applyResponsiveLayout(boolean force) {
        var parent = getParent();
        if (parent == null) {
            return;
        }

        float parentWidth = parent.getContentWidth();
        float parentHeight = parent.getContentHeight();
        if (parentWidth <= 0f || parentHeight <= 0f) {
            return;
        }

        float availableWidth = Math.max(0f, parentWidth - panelMargin() * 2f);
        float responsiveWidth = Math.min(preferredPanelWidth(), availableWidth * widthViewportFactor());
        float newWidth = availableWidth <= minPanelWidth()
                ? availableWidth
                : Math.max(minPanelWidth(), responsiveWidth);
        newWidth = Math.min(newWidth, preferredPanelWidth());

        float top = Math.min(preferredTop(), Math.max(minTop(), parentHeight * 0.10f));
        float maxHeight = Math.max(0f, parentHeight - top - panelMargin());
        float newHeight = maxHeight <= minPanelHeight()
                ? maxHeight
                : Math.max(minPanelHeight(), Math.min(preferredPanelHeight(), maxHeight));

        if (force || Math.abs(newWidth - currentPanelWidth) > 0.5f || Math.abs(newHeight - currentPanelHeight) > 0.5f) {
            currentPanelWidth = newWidth;
            currentPanelHeight = newHeight;
            layout(layout -> layout
                    .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                    .left(panelMargin())
                    .top(top)
                    .width(currentPanelWidth)
                    .height(currentPanelHeight)
            );
            onPanelBoundsChanged(currentPanelWidth, currentPanelHeight);
        }
    }

    private float hiddenTranslateX() {
        return -(currentPanelWidth + hiddenTranslateExtra());
    }
}
