package org.cyclops.cyclopscore.client.gui.component.button;

import net.minecraft.client.Minecraft;
import org.cyclops.cyclopscore.client.gui.image.IImage;

/**
 * A button with an image.
 * @author rubensworks
 *
 */
public class GuiButtonImage extends GuiButtonExtended {

    private IImage image;
    private final int offsetX, offsetY;

	/**
	 * Make a new instance.
	 * @param id The ID.
	 * @param x X
	 * @param y Y
	 * @param width Width
	 * @param height Height
	 * @param image The image to render
     * @param offsetX The x coordinate for the image inside the button.
     * @param offsetY The y coordinate for the image inside the button.
     * @param background If the button background should be rendered.
	 */
	public GuiButtonImage(int id, int x, int y,
                          int width, int height,
                          IImage image, int offsetX, int offsetY,
                          boolean background) {
		super(id, x, y, width, height, "", background);
        this.image = image;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
	}

	/**
	 * Make a new instance.
	 * @param id The ID.
	 * @param x X
	 * @param y Y
	 * @param image The image to render
	 */
	public GuiButtonImage(int id, int x, int y, IImage image) {
		this(id, x, y, image.getWidth(), image.getHeight(), image, 0, 0, false);
	}

	protected void drawButtonInner(Minecraft minecraft, int i, int j, boolean mouseOver) {
        image.draw(this, x + offsetX, y + offsetY);
    }

	public void setImage(IImage image) {
		this.image = image;
	}
}
