package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Small custom texture used by condition-state chips.
 * <p>
 * LDLib does not expose a simple rounded-rect helper here, so this texture fakes a soft rounded
 * silhouette by drawing a border/body without the four corner pixels. The result reads as a
 * compact chip instead of a hard rectangle while staying cheap to render.
 * </p>
 */
public final class ConditionChipTexture implements IGuiTexture {
    private final int fillColor;
    private final int borderColor;

    public ConditionChipTexture(int fillColor, int borderColor) {
        this.fillColor = fillColor;
        this.borderColor = borderColor;
    }

    @Override
    public void draw(GuiGraphics graphics,
                     float mouseX,
                     float mouseY,
                     float x,
                     float y,
                     float width,
                     float height,
                     float partialTicks
    ) {
        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.max(4, Math.round(width));
        int ih = Math.max(4, Math.round(height));

        // Fill: central body plus side strips, leaving the extreme corner pixels empty.
        DrawerHelper.drawSolidRect(graphics, ix + 2, iy + 1, iw - 4, ih - 2, fillColor);
        DrawerHelper.drawSolidRect(graphics, ix + 1, iy + 2, 1, ih - 4, fillColor);
        DrawerHelper.drawSolidRect(graphics, ix + iw - 2, iy + 2, 1, ih - 4, fillColor);

        // Border: top/bottom and left/right strokes, again skipping corner pixels for a clipped look.
        DrawerHelper.drawSolidRect(graphics, ix + 2, iy, iw - 4, 1, borderColor);
        DrawerHelper.drawSolidRect(graphics, ix + 2, iy + ih - 1, iw - 4, 1, borderColor);
        DrawerHelper.drawSolidRect(graphics, ix, iy + 2, 1, ih - 4, borderColor);
        DrawerHelper.drawSolidRect(graphics, ix + iw - 1, iy + 2, 1, ih - 4, borderColor);

        // Small diagonal hints so the clipped corners still feel intentional.
        DrawerHelper.drawSolidRect(graphics, ix + 1, iy + 1, 1, 1, borderColor);
        DrawerHelper.drawSolidRect(graphics, ix + iw - 2, iy + 1, 1, 1, borderColor);
        DrawerHelper.drawSolidRect(graphics, ix + 1, iy + ih - 2, 1, 1, borderColor);
        DrawerHelper.drawSolidRect(graphics, ix + iw - 2, iy + ih - 2, 1, 1, borderColor);
    }
}
