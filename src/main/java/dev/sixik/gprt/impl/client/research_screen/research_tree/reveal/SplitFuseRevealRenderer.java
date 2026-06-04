package dev.sixik.gprt.impl.client.research_screen.research_tree.reveal;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Holds the temporary snapshot used by the {@code SPLIT_FUSE} unlock reveal and knows how to draw it.
 * <p>
 * The screen asks this helper to capture a widget once, then the helper keeps the GPU texture alive
 * until the animation finishes. The current implementation still keeps a screenshot-based fallback,
 * but the state object now already stores direct texture ids and source dimensions so a future shader/FBO
 * pass can reuse the same retained snapshot without redesigning the public API again.
 * </p>
 */
public final class SplitFuseRevealRenderer {
    private static final float COMPOSITE_PADDING = 12f;
    // The FBO composite pass is still experimental. Keep the proven immediate path as default
    // until the assembly/shader version fully matches the intended motion.
    private static final boolean ENABLE_COMPOSITE_PASS = false;

    private @Nullable UnlockRevealSnapshot activeSnapshot;
    private @Nullable TextureTarget compositeTarget;
    private boolean captureScheduled;

    public boolean isCaptureScheduled() {
        return captureScheduled;
    }

    public boolean hasSnapshot(int nodeId) {
        return activeSnapshot != null && activeSnapshot.nodeId() == nodeId;
    }

    public @Nullable UnlockRevealSnapshot snapshotFor(int nodeId) {
        return hasSnapshot(nodeId) ? activeSnapshot : null;
    }

    public void scheduleCapture(GUIContext guiContext, Runnable captureAction) {
        if (captureScheduled) {
            return;
        }

        captureScheduled = true;
        guiContext.postRendering(ignored -> {
            try {
                captureAction.run();
            } finally {
                captureScheduled = false;
            }
        });
    }

    public void captureSnapshot(ResearchNode node,
                                UIElement widget,
                                float screenX,
                                float screenY,
                                float screenWidth,
                                float screenHeight
    ) {
        if (node == null || widget == null || hasSnapshot(node.getId())) {
            return;
        }

        if (tryCaptureOffscreenSnapshot(node, widget)) {
            return;
        }

        captureScreenSnapshot(node, screenX, screenY, screenWidth, screenHeight);
    }

    public void draw(GUIContext guiContext,
                     int nodeId,
                     float centerX,
                     float centerY,
                     float width,
                     float height,
                     float scale,
                     float translateX,
                     float translateY,
                     float progress01,
                     int backgroundColor,
                     int borderColor,
                     int accentColor
    ) {
        UnlockRevealSnapshot snapshot = snapshotFor(nodeId);
        if (snapshot == null) {
            return;
        }

        CompositeFrame frame = buildCompositeFrame(centerX, centerY, width, height, scale, translateX, translateY, progress01);

        float intro = easeOutCubic(clamp01(progress01 / 0.18f));
        float fuseProgress = easeOutCubic(clamp01((progress01 - 0.10f) / 0.58f));
        float fadeOut = 1f - clamp01((progress01 - 0.90f) / 0.10f);
        float alphaMul = clamp01(intro * fadeOut);

        int resolvedBackgroundColor = withAlpha(backgroundColor, Math.round(230f * alphaMul));
        int resolvedBorderColor = withAlpha(borderColor, Math.round(255f * alphaMul));
        int resolvedAccentColor = withAlpha(accentColor, Math.round(250f * alphaMul));
        if (ENABLE_COMPOSITE_PASS) {
            if (renderCompositeFrame(snapshot, frame, fuseProgress, resolvedBackgroundColor, resolvedBorderColor, resolvedAccentColor)) {
                guiContext.graphics.flush();
                drawTexturedQuad(
                        guiContext.graphics.pose().last().pose(),
                        compositeTarget.getColorTextureId(),
                        frame.screenX(),
                        frame.screenY(),
                        frame.targetWidth(),
                        frame.targetHeight(),
                        0f,
                        0f,
                        1f,
                        1f,
                        0xFFFFFFFF
                );
                return;
            }
        }

        drawLegacyScreenComposite(guiContext, snapshot, frame, fuseProgress, resolvedBackgroundColor, resolvedBorderColor, resolvedAccentColor);
    }

    public void release() {
        captureScheduled = false;
        releaseCompositeTarget();
        if (activeSnapshot == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (activeSnapshot.target() != null) {
            activeSnapshot.target().destroyBuffers();
        } else if (minecraft != null && activeSnapshot.textureLocation() != null) {
            minecraft.getTextureManager().release(activeSnapshot.textureLocation());
        }
        activeSnapshot = null;
    }

    private CompositeFrame buildCompositeFrame(float centerX,
                                               float centerY,
                                               float width,
                                               float height,
                                               float scale,
                                               float translateX,
                                               float translateY,
                                               float progress01
    ) {
        float safeScale = Math.max(0.35f, scale);
        float renderWidth = width * safeScale;
        float renderHeight = height * safeScale;
        float halfWidth = renderWidth * 0.5f;
        float splitDistance = lerp(renderWidth * 0.46f, 0f, easeOutCubic(clamp01((progress01 - 0.10f) / 0.58f)));
        float targetWidth = renderWidth + splitDistance + COMPOSITE_PADDING * 2f;
        float targetHeight = renderHeight + COMPOSITE_PADDING * 2f;
        float localBaseX = (targetWidth - renderWidth) * 0.5f;
        float localBaseY = (targetHeight - renderHeight) * 0.5f;
        float screenX = centerX + translateX - targetWidth * 0.5f;
        float screenY = centerY + translateY - targetHeight * 0.5f;
        return new CompositeFrame(
                renderWidth,
                renderHeight,
                halfWidth,
                splitDistance,
                targetWidth,
                targetHeight,
                localBaseX,
                localBaseY,
                screenX,
                screenY
        );
    }

    private boolean renderCompositeFrame(UnlockRevealSnapshot snapshot,
                                         CompositeFrame frame,
                                         float fuseProgress,
                                         int backgroundColor,
                                         int borderColor,
                                         int accentColor
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return false;
        }

        int targetWidth = Math.max(1, Math.round(frame.targetWidth()));
        int targetHeight = Math.max(1, Math.round(frame.targetHeight()));
        TextureTarget target = ensureCompositeTarget(targetWidth, targetHeight);
        if (target == null) {
            return false;
        }

        return renderToTarget(target, targetWidth, targetHeight, guiGraphics -> {
            drawCompositePass(guiGraphics, snapshot, frame, 0f, 0f, fuseProgress, backgroundColor, borderColor, accentColor);
            guiGraphics.flush();
        });
    }

    private void drawLegacyScreenComposite(GUIContext guiContext,
                                           UnlockRevealSnapshot snapshot,
                                           CompositeFrame frame,
                                           float fuseProgress,
                                           int backgroundColor,
                                           int borderColor,
                                           int accentColor
    ) {
        drawCompositePass(
                guiContext.graphics,
                snapshot,
                frame,
                frame.screenX(),
                frame.screenY(),
                fuseProgress,
                backgroundColor,
                borderColor,
                accentColor
        );
    }

    private void drawCompositePass(GuiGraphics guiGraphics,
                                   UnlockRevealSnapshot snapshot,
                                   CompositeFrame frame,
                                   float originX,
                                   float originY,
                                   float fuseProgress,
                                   int backgroundColor,
                                   int borderColor,
                                   int accentColor
    ) {
        float renderWidth = frame.renderWidth();
        float renderHeight = frame.renderHeight();
        float baseX = originX + frame.localBaseX();
        float baseY = originY + frame.localBaseY();
        float halfWidth = frame.halfWidth();
        float splitDistance = frame.splitDistance();
        float seamWidth = Math.max(2.5f, renderWidth * 0.030f);
        int seamCoreColor = withAlpha(mixColors(accentColor, 0xFFFFFFFF, 0.45f), (accentColor >>> 24) & 0xFF);
        int seamGlowColor = withAlpha(accentColor, Math.round(((accentColor >>> 24) & 0xFF) * 0.52f));

        drawTexturedHalf(
                guiGraphics,
                baseX - splitDistance * 0.5f,
                baseY,
                halfWidth,
                renderHeight,
                0f,
                0.5f,
                snapshot.textureId(),
                backgroundColor
        );
        drawTexturedHalf(
                guiGraphics,
                baseX + halfWidth + splitDistance * 0.5f,
                baseY,
                halfWidth,
                renderHeight,
                0.5f,
                1f,
                snapshot.textureId(),
                backgroundColor
        );

        drawHalfFrame(
                guiGraphics,
                baseX - splitDistance * 0.5f,
                baseY,
                halfWidth,
                renderHeight,
                true,
                borderColor,
                accentColor
        );
        drawHalfFrame(
                guiGraphics,
                baseX + halfWidth + splitDistance * 0.5f,
                baseY,
                halfWidth,
                renderHeight,
                false,
                borderColor,
                accentColor
        );

        float seamCenterX = baseX + renderWidth * 0.5f;
        float seamGlowHeight = renderHeight + 10f;
        fillRect(
                guiGraphics,
                seamCenterX - seamWidth * 1.6f,
                baseY - 5f,
                seamWidth * 3.2f,
                seamGlowHeight,
                withAlpha(seamGlowColor, Math.round(((seamGlowColor >>> 24) & 0xFF) * clamp01((fuseProgress - 0.08f) / 0.30f)))
        );
        fillRect(
                guiGraphics,
                seamCenterX - seamWidth * 0.55f,
                baseY - 1f,
                seamWidth * 1.1f,
                renderHeight + 2f,
                seamCoreColor
        );
    }

    private @Nullable TextureTarget ensureCompositeTarget(int width, int height) {
        if (compositeTarget != null && compositeTarget.width == width && compositeTarget.height == height) {
            return compositeTarget;
        }

        releaseCompositeTarget();
        TextureTarget target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        target.setClearColor(0f, 0f, 0f, 0f);
        target.setFilterMode(9729);
        compositeTarget = target;
        return compositeTarget;
    }

    private void releaseCompositeTarget() {
        if (compositeTarget != null) {
            compositeTarget.destroyBuffers();
            compositeTarget = null;
        }
    }

    private boolean renderToTarget(TextureTarget target,
                                   int targetWidth,
                                   int targetHeight,
                                   TargetRenderCallback callback
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return false;
        }

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        boolean projectionBackedUp = false;
        boolean modelViewPushed = false;

        try {
            target.bindWrite(false);
            target.clear(Minecraft.ON_OSX);
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

            GuiGraphics guiGraphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
            callback.draw(guiGraphics);
            guiGraphics.flush();
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
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
        }
    }

    private boolean tryCaptureOffscreenSnapshot(ResearchNode node, UIElement widget) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || widget.getModularUI() == null) {
            return false;
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
                activeSnapshot = new UnlockRevealSnapshot(
                        node.getId(),
                        offscreenTarget.getColorTextureId(),
                        targetWidth,
                        targetHeight,
                        offscreenTarget,
                        null
                );
                offscreenTarget = null;
                return true;
            }
        } catch (Exception ignored) {
            return false;
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

    private void captureScreenSnapshot(ResearchNode node,
                                       float screenX,
                                       float screenY,
                                       float screenWidth,
                                       float screenHeight
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        RenderTarget renderTarget = minecraft.getMainRenderTarget();
        if (renderTarget == null || renderTarget.width <= 0 || renderTarget.height <= 0) {
            return;
        }

        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();
        if (guiWidth <= 0 || guiHeight <= 0) {
            return;
        }

        float framebufferScaleX = renderTarget.width / (float) guiWidth;
        float framebufferScaleY = renderTarget.height / (float) guiHeight;

        int captureX = Math.max(0, (int) Math.floor(screenX * framebufferScaleX));
        int captureY = Math.max(0, (int) Math.floor(screenY * framebufferScaleY));
        int captureRight = Math.min(renderTarget.width, (int) Math.ceil((screenX + screenWidth) * framebufferScaleX));
        int captureBottom = Math.min(renderTarget.height, (int) Math.ceil((screenY + screenHeight) * framebufferScaleY));
        int captureWidth = Math.max(1, captureRight - captureX);
        int captureHeight = Math.max(1, captureBottom - captureY);

        try (NativeImage screenshot = Screenshot.takeScreenshot(renderTarget)) {
            if (captureX >= screenshot.getWidth() || captureY >= screenshot.getHeight()) {
                return;
            }

            captureWidth = Math.min(captureWidth, screenshot.getWidth() - captureX);
            captureHeight = Math.min(captureHeight, screenshot.getHeight() - captureY);
            if (captureWidth <= 0 || captureHeight <= 0) {
                return;
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
            activeSnapshot = new UnlockRevealSnapshot(
                    node.getId(),
                    texture.getId(),
                    captureWidth,
                    captureHeight,
                    null,
                    textureLocation
            );
        } catch (Exception ignored) {
            // If capture fails we simply keep drawing the live widget; the reveal remains functional.
        }
    }

    private void drawTexturedHalf(GuiGraphics guiGraphics,
                                  float x,
                                  float y,
                                  float width,
                                  float height,
                                  float u0,
                                  float u1,
                                  int textureId,
                                  int color
    ) {
        if (width <= 0.5f || height <= 0.5f) {
            return;
        }

        drawTexturedQuad(guiGraphics.pose().last().pose(), textureId, x, y, width, height, u0, 0f, u1, 1f, color);
    }

    private void drawHalfFrame(GuiGraphics guiGraphics,
                               float x,
                               float y,
                               float width,
                               float height,
                               boolean leftHalf,
                               int borderColor,
                               int accentColor
    ) {
        if (width <= 0.5f || height <= 0.5f) {
            return;
        }

        fillRect(guiGraphics, x, y, width, Math.max(2f, height * 0.08f), accentColor);
        fillRect(guiGraphics,
                x + 5f,
                y + Math.max(7f, height * 0.22f),
                Math.max(10f, width * 0.46f),
                Math.max(3f, height * 0.10f),
                withAlpha(accentColor, Math.min(255, ((accentColor >>> 24) & 0xFF) / 2 + 40))
        );

        float iconSize = Math.min(height * 0.28f, width * 0.18f);
        if (iconSize >= 4f) {
            float iconX = leftHalf ? x + 6f : x + width - iconSize - 6f;
            float iconY = y + height - iconSize - 6f;
            fillRect(guiGraphics,
                    iconX,
                    iconY,
                    iconSize,
                    iconSize,
                    withAlpha(accentColor, Math.min(255, ((accentColor >>> 24) & 0xFF) / 2 + 64))
            );
        }

        float borderThickness = 1f;
        fillRect(guiGraphics, x, y, width, borderThickness, borderColor);
        fillRect(guiGraphics, x, y + height - borderThickness, width, borderThickness, borderColor);
        fillRect(guiGraphics, x, y, borderThickness, height, borderColor);
        fillRect(guiGraphics, x + width - borderThickness, y, borderThickness, height, borderColor);
    }

    private void drawTexturedQuad(Matrix4f matrix,
                                  int textureId,
                                  float x,
                                  float y,
                                  float width,
                                  float height,
                                  float u0,
                                  float v0,
                                  float u1,
                                  float v1,
                                  int color
    ) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, textureId);

        int alpha = (color >>> 24) & 0xFF;
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.addVertex(matrix, x, y + height, 0f).setUv(u0, v1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix, x + width, y + height, 0f).setUv(u1, v1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix, x + width, y, 0f).setUv(u1, v0).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix, x, y, 0f).setUv(u0, v0).setColor(red, green, blue, alpha);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    private void fillRect(GuiGraphics guiGraphics, float x, float y, float width, float height, int color) {
        if (width <= 0.5f || height <= 0.5f || ((color >>> 24) & 0xFF) <= 0) {
            return;
        }
        guiGraphics.fill(
                Math.round(x),
                Math.round(y),
                Math.round(x + width),
                Math.round(y + height),
                color
        );
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    private static float easeOutCubic(float progress) {
        float inverse = 1f - clamp01(progress);
        return 1f - inverse * inverse * inverse;
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static int mixColors(int firstColor, int secondColor, float amount) {
        float clampedAmount = clamp01(amount);
        int firstA = (firstColor >>> 24) & 0xFF;
        int firstR = (firstColor >>> 16) & 0xFF;
        int firstG = (firstColor >>> 8) & 0xFF;
        int firstB = firstColor & 0xFF;

        int secondA = (secondColor >>> 24) & 0xFF;
        int secondR = (secondColor >>> 16) & 0xFF;
        int secondG = (secondColor >>> 8) & 0xFF;
        int secondB = secondColor & 0xFF;

        int alpha = Math.round(firstA + (secondA - firstA) * clampedAmount);
        int red = Math.round(firstR + (secondR - firstR) * clampedAmount);
        int green = Math.round(firstG + (secondG - firstG) * clampedAmount);
        int blue = Math.round(firstB + (secondB - firstB) * clampedAmount);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Snapshot descriptor kept alive for the duration of one reveal animation.
     * <p>
     * Width and height are stored already so a future shader step can sample or reconstruct UVs
     * without asking the original widget again.
     * </p>
     */
    public record UnlockRevealSnapshot(int nodeId,
                                       int textureId,
                                       int width,
                                       int height,
                                       @Nullable TextureTarget target,
                                       @Nullable ResourceLocation textureLocation
    ) {
    }

    private record CompositeFrame(float renderWidth,
                                  float renderHeight,
                                  float halfWidth,
                                  float splitDistance,
                                  float targetWidth,
                                  float targetHeight,
                                  float localBaseX,
                                  float localBaseY,
                                  float screenX,
                                  float screenY
    ) {
    }

    @FunctionalInterface
    private interface TargetRenderCallback {
        void draw(GuiGraphics guiGraphics);
    }
}
