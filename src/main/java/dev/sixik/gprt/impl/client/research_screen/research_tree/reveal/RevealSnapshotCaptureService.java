package dev.sixik.gprt.impl.client.research_screen.research_tree.reveal;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Captures research node widgets into retained texture snapshots for reveal effects.
 */
public final class RevealSnapshotCaptureService {
    private RevealCaptureMode captureMode = RevealCaptureMode.AUTO_OFFSCREEN_FIRST;
    private @Nullable RevealCaptureDebugData lastDebugData;

    public void setCaptureMode(RevealCaptureMode captureMode) {
        this.captureMode = captureMode == null ? RevealCaptureMode.AUTO_OFFSCREEN_FIRST : captureMode;
    }

    public RevealCaptureMode getCaptureMode() {
        return captureMode;
    }

    public @Nullable RevealCaptureDebugData getLastDebugData() {
        return lastDebugData;
    }

    public @Nullable RevealSnapshot capture(ResearchNode node,
                                            UIElement widget,
                                            float screenX,
                                            float screenY,
                                            float screenWidth,
                                            float screenHeight
    ) {
        if (node == null || widget == null) {
            return null;
        }

        return switch (captureMode) {
            case FORCE_OFFSCREEN -> captureOffscreen(node, widget);
            case FORCE_SCREEN -> captureFromScreen(node, screenX, screenY, screenWidth, screenHeight);
            case AUTO_OFFSCREEN_FIRST -> {
                RevealSnapshot snapshot = captureOffscreen(node, widget);
                yield snapshot != null ? snapshot : captureFromScreen(node, screenX, screenY, screenWidth, screenHeight);
            }
        };
    }

    public void captureDebug(ResearchNode node,
                             UIElement widget,
                             float screenX,
                             float screenY,
                             float screenWidth,
                             float screenHeight
    ) {
        releaseDebugData();

        RevealSnapshot offscreenSnapshot = captureOffscreen(node, widget);
        ScreenCaptureResult screenResult = captureScreenDetailed(node, screenX, screenY, screenWidth, screenHeight);
        RevealSnapshot screenSnapshot = screenResult == null ? null : screenResult.snapshot();

        int framebufferWidth = screenResult == null ? 0 : screenResult.framebufferWidth();
        int framebufferHeight = screenResult == null ? 0 : screenResult.framebufferHeight();
        int guiWidth = screenResult == null ? 0 : screenResult.guiWidth();
        int guiHeight = screenResult == null ? 0 : screenResult.guiHeight();
        float framebufferScaleX = screenResult == null ? 0f : screenResult.framebufferScaleX();
        float framebufferScaleY = screenResult == null ? 0f : screenResult.framebufferScaleY();
        int captureX = screenResult == null ? 0 : screenResult.captureX();
        int captureTop = screenResult == null ? 0 : screenResult.captureTop();
        int captureRight = screenResult == null ? 0 : screenResult.captureRight();
        int captureBottom = screenResult == null ? 0 : screenResult.captureBottom();
        int screenshotCaptureY = screenResult == null ? 0 : screenResult.screenshotCaptureY();
        boolean usedYFlip = screenResult != null;

        lastDebugData = new RevealCaptureDebugData(
                offscreenSnapshot,
                screenSnapshot,
                screenX,
                screenY,
                screenWidth,
                screenHeight,
                framebufferWidth,
                framebufferHeight,
                guiWidth,
                guiHeight,
                framebufferScaleX,
                framebufferScaleY,
                captureX,
                captureTop,
                captureRight,
                captureBottom,
                screenshotCaptureY,
                usedYFlip
        );
    }

    public void release(@Nullable RevealSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (snapshot.target() != null) {
            snapshot.target().destroyBuffers();
        } else if (minecraft != null && snapshot.textureLocation() != null) {
            minecraft.getTextureManager().release(snapshot.textureLocation());
        }
    }

    public void releaseDebugData() {
        if (lastDebugData == null) {
            return;
        }

        release(lastDebugData.offscreenSnapshot());
        release(lastDebugData.screenSnapshot());
        lastDebugData = null;
    }

    private @Nullable RevealSnapshot captureOffscreen(ResearchNode node, UIElement widget) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || widget.getModularUI() == null) {
            return null;
        }

        int targetWidth = Math.max(1, Math.round(widget.getSizeWidth()));
        int targetHeight = Math.max(1, Math.round(widget.getSizeHeight()));
        TextureTarget offscreenTarget = null;
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        Transform2D previousTransform = widget.getStyle().getInline(PropertyRegistry.TRANSFORM_2D);
        boolean projectionBackedUp = false;
        boolean modelViewPushed = false;

        try {
            offscreenTarget = new TextureTarget(targetWidth, targetHeight, false, Minecraft.ON_OSX);
            offscreenTarget.setClearColor(0f, 0f, 0f, 0f);
            offscreenTarget.setFilterMode(9729);
            offscreenTarget.bindWrite(false);
            offscreenTarget.clear(Minecraft.ON_OSX);
            RenderSystem.viewport(0, 0, targetWidth, targetHeight);

            RenderSystem.backupProjectionMatrix();
            projectionBackedUp = true;
            Matrix4f projection = new Matrix4f()
                    .setOrtho(
                            0.0F,
                            (float) targetWidth,
                            (float) targetHeight,
                            0.0F,
                            1000.0F,
                            net.neoforged.neoforge.client.ClientHooks.getGuiFarPlane()
                    );
            RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);

            var modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();
            modelViewPushed = true;
            modelViewStack.identity();
            modelViewStack.translation(0.0F, 0.0F, 10000.0F - net.neoforged.neoforge.client.ClientHooks.getGuiFarPlane());
            RenderSystem.applyModelViewMatrix();

            widget.getStyle().setInline(PropertyRegistry.TRANSFORM_2D, Transform2D.identity());
            widget.clearPoseCache();

            GuiGraphics guiGraphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
            GUIContext offscreenContext = GUIContext.of(widget.getModularUI(), guiGraphics, 0, 0, 0f);
            offscreenContext.pose.translate(-widget.getPositionX(), -widget.getPositionY(), 0f);
            widget.drawInBackground(offscreenContext);
            guiGraphics.flush();
            offscreenContext.callPostRendering();
            guiGraphics.flush();

            try (NativeImage ignored = Screenshot.takeScreenshot(offscreenTarget)) {
                RevealSnapshot snapshot = new RevealSnapshot(
                        node.getId(),
                        offscreenTarget.getColorTextureId(),
                        targetWidth,
                        targetHeight,
                        0f,
                        0f,
                        targetWidth,
                        targetHeight,
                        RevealSnapshot.CaptureSource.OFFSCREEN,
                        offscreenTarget,
                        null
                );
                offscreenTarget = null;
                return snapshot;
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (previousTransform == null) {
                widget.getStyle().setInline(PropertyRegistry.TRANSFORM_2D, null);
            } else {
                widget.getStyle().setInline(PropertyRegistry.TRANSFORM_2D, previousTransform.copy());
            }
            widget.clearPoseCache();

            if (modelViewPushed) {
                var modelViewStack = RenderSystem.getModelViewStack();
                modelViewStack.popMatrix();
                RenderSystem.applyModelViewMatrix();
            }
            if (projectionBackedUp) {
                RenderSystem.restoreProjectionMatrix();
            }
            if (mainTarget != null) {
                mainTarget.bindWrite(false);
                RenderSystem.viewport(0, 0, mainTarget.width, mainTarget.height);
            }
            if (offscreenTarget != null) {
                offscreenTarget.destroyBuffers();
            }
        }
    }

    private @Nullable RevealSnapshot captureFromScreen(ResearchNode node,
                                                       float screenX,
                                                       float screenY,
                                                       float screenWidth,
                                                       float screenHeight
    ) {
        ScreenCaptureResult result = captureScreenDetailed(node, screenX, screenY, screenWidth, screenHeight);
        return result == null ? null : result.snapshot();
    }

    private @Nullable ScreenCaptureResult captureScreenDetailed(ResearchNode node,
                                                                float screenX,
                                                                float screenY,
                                                                float screenWidth,
                                                                float screenHeight
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return null;
        }

        RenderTarget renderTarget = minecraft.getMainRenderTarget();
        if (renderTarget == null || renderTarget.width <= 0 || renderTarget.height <= 0) {
            return null;
        }

        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();
        if (guiWidth <= 0 || guiHeight <= 0) {
            return null;
        }

        float framebufferScaleX = renderTarget.width / (float) guiWidth;
        float framebufferScaleY = renderTarget.height / (float) guiHeight;

        int captureX = Math.max(0, (int) Math.floor(screenX * framebufferScaleX));
        int captureTop = Math.max(0, (int) Math.floor(screenY * framebufferScaleY));
        int captureRight = Math.min(renderTarget.width, (int) Math.ceil((screenX + screenWidth) * framebufferScaleX));
        int captureBottom = Math.min(renderTarget.height, (int) Math.ceil((screenY + screenHeight) * framebufferScaleY));
        int captureWidth = Math.max(1, captureRight - captureX);
        int captureHeight = Math.max(1, captureBottom - captureTop);

        try (NativeImage screenshot = Screenshot.takeScreenshot(renderTarget)) {
            // Screenshot pixels come from framebuffer space, which is vertically inverted relative
            // to top-left GUI coordinates. Convert the GUI crop rect into screenshot image space.
            int captureY = Math.max(0, screenshot.getHeight() - captureBottom);

            if (captureX >= screenshot.getWidth() || captureY >= screenshot.getHeight()) {
                return null;
            }

            captureWidth = Math.min(captureWidth, screenshot.getWidth() - captureX);
            captureHeight = Math.min(captureHeight, screenshot.getHeight() - captureY);
            if (captureWidth <= 0 || captureHeight <= 0) {
                return null;
            }

            NativeImage cropped = new NativeImage(captureWidth, captureHeight, true);
            for (int y = 0; y < captureHeight; y++) {
                for (int x = 0; x < captureWidth; x++) {
                    cropped.setPixelRGBA(x, y, screenshot.getPixelRGBA(captureX + x, captureY + y));
                }
            }

            net.minecraft.client.renderer.texture.DynamicTexture texture = new net.minecraft.client.renderer.texture.DynamicTexture(cropped);
            texture.upload();
            ResourceLocation textureLocation = minecraft.getTextureManager().register("gprt/research_split_fuse_" + node.getId(), texture);
            RevealSnapshot snapshot = new RevealSnapshot(
                    node.getId(),
                    texture.getId(),
                    captureWidth,
                    captureHeight,
                    screenX,
                    screenY,
                    screenWidth,
                    screenHeight,
                    RevealSnapshot.CaptureSource.SCREEN,
                    null,
                    textureLocation
            );
            return new ScreenCaptureResult(
                    snapshot,
                    renderTarget.width,
                    renderTarget.height,
                    guiWidth,
                    guiHeight,
                    framebufferScaleX,
                    framebufferScaleY,
                    captureX,
                    captureTop,
                    captureRight,
                    captureBottom,
                    captureY
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private record ScreenCaptureResult(RevealSnapshot snapshot,
                                       int framebufferWidth,
                                       int framebufferHeight,
                                       int guiWidth,
                                       int guiHeight,
                                       float framebufferScaleX,
                                       float framebufferScaleY,
                                       int captureX,
                                       int captureTop,
                                       int captureRight,
                                       int captureBottom,
                                       int screenshotCaptureY) {
    }
}
