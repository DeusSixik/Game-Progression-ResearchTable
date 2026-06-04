package dev.sixik.gprt.impl.client.research_screen.research_tree.reveal;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeVisualDefinition;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the active snapshot-based reveal instance for the research tree.
 * <p>
 * This class owns the capture lifecycle, effect instance lifetime and render dispatch. The screen only
 * decides which node/style is currently active and feeds timing/position information into this orchestrator.
 * </p>
 */
public final class ResearchRevealOrchestrator {
    private final RevealSnapshotCaptureService snapshotCaptureService = new RevealSnapshotCaptureService();
    private final RevealRenderBackend renderBackend = new RevealRenderBackend();
    private final ResearchRevealEffectResolver effectResolver;

    private @Nullable RevealSnapshot activeSnapshot;
    private @Nullable RevealEffectInstance activeInstance;
    private @Nullable ResearchRevealEffect activeEffect;
    private boolean captureScheduled;

    public ResearchRevealOrchestrator(ResearchRevealEffectResolver effectResolver) {
        this.effectResolver = effectResolver;
    }

    public boolean isCaptureScheduled() {
        return captureScheduled;
    }

    public void setCaptureMode(RevealCaptureMode captureMode) {
        snapshotCaptureService.setCaptureMode(captureMode);
    }

    public RevealCaptureMode getCaptureMode() {
        return snapshotCaptureService.getCaptureMode();
    }

    public boolean hasActiveSnapshot(int nodeId) {
        return activeSnapshot != null && activeSnapshot.nodeId() == nodeId;
    }

    public @Nullable Integer getActiveSnapshotNodeId() {
        return activeSnapshot == null ? null : activeSnapshot.nodeId();
    }

    public @Nullable RevealSnapshot getActiveSnapshot() {
        return activeSnapshot;
    }

    public @Nullable RevealCaptureDebugData getLastDebugData() {
        return snapshotCaptureService.getLastDebugData();
    }

    public boolean isSupported(ResearchNode node, dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle style) {
        return effectResolver.resolve(node, style) != null;
    }

    public void scheduleCapture(GUIContext guiContext, Runnable captureAction) {
        if (captureScheduled) {
            return;
        }

        captureScheduled = true;
        guiContext.postRendering(ignored -> {
            try {
                // Make sure all pending GUI draw calls are actually written into the main target
                // before we read pixels for a screen-space snapshot.
                if (ignored.graphics != null) {
                    ignored.graphics.flush();
                }
                captureAction.run();
            } finally {
                captureScheduled = false;
            }
        });
    }

    public boolean captureSnapshot(ResearchNode node,
                                   dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle style,
                                   UIElement widget,
                                   float screenX,
                                   float screenY,
                                   float screenWidth,
                                   float screenHeight,
                                   @Nullable ResearchNodeVisualDefinition visualDefinition
    ) {
        if (node == null || widget == null || hasActiveSnapshot(node.getId())) {
            return false;
        }

        ResearchRevealEffect effect = effectResolver.resolve(node, style);
        if (effect == null) {
            return false;
        }

        snapshotCaptureService.captureDebug(node, widget, screenX, screenY, screenWidth, screenHeight);
        RevealSnapshot snapshot = snapshotCaptureService.capture(node, widget, screenX, screenY, screenWidth, screenHeight);
        if (snapshot == null) {
            return false;
        }

        release();
        activeEffect = effect;
        activeSnapshot = snapshot;
        activeInstance = effect.createInstance(new RevealInitContext(node, widget, snapshot, visualDefinition, renderBackend));
        return true;
    }

    public void render(GUIContext guiContext,
                       ResearchNode node,
                       dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle style,
                       float progress01,
                       float width,
                       float height,
                       float scale,
                       float translateX,
                       float translateY,
                       float screenCenterX,
                       float screenCenterY,
                       int backgroundColor,
                       int borderColor,
                       int accentColor,
                       @Nullable ResearchNodeVisualDefinition visualDefinition
    ) {
        if (activeSnapshot == null || activeInstance == null || activeSnapshot.nodeId() != node.getId()) {
            return;
        }
        if (activeEffect == null || effectResolver.resolve(node, style) != activeEffect) {
            return;
        }

        activeInstance.render(new RevealRenderContext(
                guiContext,
                node,
                activeSnapshot,
                visualDefinition,
                renderBackend,
                progress01,
                width,
                height,
                scale,
                translateX,
                translateY,
                screenCenterX,
                screenCenterY,
                backgroundColor,
                borderColor,
                accentColor
        ));
    }

    public void renderDebugPreview(GUIContext guiContext, float x, float y, float width, float height) {
        if (activeSnapshot == null) {
            return;
        }
        renderDebugPreview(guiContext, activeSnapshot, x, y, width, height);
    }

    public void renderDebugPreview(GUIContext guiContext,
                                   @Nullable RevealSnapshot snapshot,
                                   float x,
                                   float y,
                                   float width,
                                   float height
    ) {
        if (snapshot == null) {
            return;
        }
        renderBackend.drawScreenQuad(guiContext, snapshot.textureId(), x, y, width, height, 0xFFFFFFFF);
    }

    public void release() {
        captureScheduled = false;
        if (activeInstance != null) {
            activeInstance.dispose();
            activeInstance = null;
        }
        if (activeSnapshot != null) {
            snapshotCaptureService.release(activeSnapshot);
            activeSnapshot = null;
        }
        snapshotCaptureService.releaseDebugData();
        activeEffect = null;
    }
}
