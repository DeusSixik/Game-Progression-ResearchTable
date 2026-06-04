package dev.sixik.gprt.impl.client.research_screen.research_tree.reveal;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Shared low-level helpers for reveal effects.
 * <p>
 * The goal is to keep GL/FBO boilerplate outside individual effect classes so they can focus on
 * animation math and composition logic.
 * </p>
 */
public final class RevealRenderBackend {
    public @Nullable TextureTarget createTarget(int width, int height) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        TextureTarget target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        target.setClearColor(0f, 0f, 0f, 0f);
        target.setFilterMode(9729);
        return target;
    }

    public void releaseTarget(@Nullable TextureTarget target) {
        if (target != null) {
            target.destroyBuffers();
        }
    }

    public boolean renderToTarget(TextureTarget target, int width, int height, TargetRenderCallback callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || target == null || width <= 0 || height <= 0) {
            return false;
        }

        var mainTarget = minecraft.getMainRenderTarget();
        boolean projectionBackedUp = false;
        boolean modelViewPushed = false;

        try {
            target.bindWrite(false);
            target.clear(Minecraft.ON_OSX);
            com.mojang.blaze3d.systems.RenderSystem.viewport(0, 0, width, height);

            com.mojang.blaze3d.systems.RenderSystem.backupProjectionMatrix();
            projectionBackedUp = true;
            Matrix4f projection = new Matrix4f()
                    .setOrtho(
                            0.0F,
                            (float) width,
                            (float) height,
                            0.0F,
                            1000.0F,
                            net.neoforged.neoforge.client.ClientHooks.getGuiFarPlane()
                    );
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);

            var modelViewStack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();
            modelViewPushed = true;
            modelViewStack.identity();
            modelViewStack.translation(0.0F, 0.0F, 10000.0F - net.neoforged.neoforge.client.ClientHooks.getGuiFarPlane());
            com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();

            GuiGraphics guiGraphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
            callback.draw(guiGraphics);
            guiGraphics.flush();
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (modelViewPushed) {
                var modelViewStack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
                modelViewStack.popMatrix();
                com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            }
            if (projectionBackedUp) {
                com.mojang.blaze3d.systems.RenderSystem.restoreProjectionMatrix();
            }
            if (mainTarget != null) {
                mainTarget.bindWrite(false);
                com.mojang.blaze3d.systems.RenderSystem.viewport(0, 0, mainTarget.width, mainTarget.height);
            }
        }
    }

    public void drawScreenQuad(GUIContext guiContext,
                               int textureId,
                               float x,
                               float y,
                               float width,
                               float height,
                               int color
    ) {
        guiContext.graphics.flush();
        drawTexturedQuad(guiContext.graphics.pose().last().pose(), textureId, x, y, width, height, 0f, 0f, 1f, 1f, color);
    }

    public void drawTexturedQuad(Matrix4f matrix,
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
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, textureId);

        int alpha = (color >>> 24) & 0xFF;
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;

        var bufferBuilder = com.mojang.blaze3d.vertex.Tesselator.getInstance().begin(
                com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR
        );
        bufferBuilder.addVertex(matrix, x, y + height, 0f).setUv(u0, v1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix, x + width, y + height, 0f).setUv(u1, v1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix, x + width, y, 0f).setUv(u1, v0).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix, x, y, 0f).setUv(u0, v0).setColor(red, green, blue, alpha);
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    @FunctionalInterface
    public interface TargetRenderCallback {
        void draw(GuiGraphics guiGraphics);
    }
}
