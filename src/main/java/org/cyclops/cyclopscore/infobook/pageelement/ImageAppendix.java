package org.cyclops.cyclopscore.infobook.pageelement;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.cyclops.cyclopscore.infobook.GuiInfoBook;
import org.cyclops.cyclopscore.infobook.IInfoBook;
import org.cyclops.cyclopscore.infobook.InfoSection;

/**
 * Images that can be added to sections.
 * @author rubensworks
 */
public class ImageAppendix extends SectionAppendix {

    private static final int OFFSET_Y = 0;

    private ResourceLocation resource;
    private int width;
    private int height;

    public ImageAppendix(IInfoBook infoBook, ResourceLocation resource, int width, int height) {
        super(infoBook);
        this.resource = resource;
        this.width = width;
        this.height = height;
    }

    @Override
    protected int getOffsetY() {
        return OFFSET_Y;
    }

    @Override
    protected int getWidth() {
        return width;
    }

    @Override
    protected int getHeight() {
        return height;
    }

    @Override
    protected void drawElement(GuiInfoBook gui, int x, int y, int width, int height, int page, int mx, int my) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(resource);
        gui.drawTexturedModalRect(x, y, 0, 0, getWidth(), getHeight());
        gui.drawOuterBorder(x, y, getWidth(), getHeight(), 0.5F, 0.5F, 0.5F, 0.4f);
    }

    @Override
    protected void postDrawElement(GuiInfoBook gui, int x, int y, int width, int height, int page, int mx, int my) {

    }

    @Override
    public void preBakeElement(InfoSection infoSection) {

    }

    @Override
    public void bakeElement(InfoSection infoSection) {

    }
}
