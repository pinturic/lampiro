/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: UIVLayout.java 846 2008-09-11 12:20:05Z luca $
*/

/**
 * 
 */
package it.yup.ui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

/**
 * @author luca
 * 
 */
public class UIVLayout extends UILayout {

	int layoutHeight;

	/**
	 * @param rowNumber
	 *            The row number of the layOut.
	 * @param height
	 *            The height of the item
	 */
	public UIVLayout(int rowNumber, int height) {
		super(rowNumber);
		this.layoutHeight = height;
	}

	/**
	 * Inserts and {@link UIItem} at the index-th position in the layout.
	 * 
	 * @param item
	 *            The {@link UIItem} to add.
	 * @param index
	 *            The index in the column array.
	 * @param type
	 *            The type of column (can be UIHLayout.pix or UIHLayout.perc)
	 */
	public void insert(UIItem item, int index, int height, int type) {
		this.layoutItems[index] = item;
		item.setLayoutHeight(height);
		item.setType(type);
		item.setScreen(screen);
		item.setContainer(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.yup.ui.UIItem#paint(javax.microedition.lcdui.Graphics, int, int)
	 */
	protected void paint(Graphics g, int w, int h) {
		int originalGx = g.getTranslateX();
		int originalGy = g.getTranslateY();
		if (this.dirty == true) {
			g.setColor(getBg_color() >= 0 ? getBg_color() : UIConfig.bg_color);
			g.fillRect(0, 0, w, h);
		}

		int pixelSum = 0;
		int percentageSum = 0;
		for (int i = 0; i < layoutItems.length; i++) {
			if (layoutItems[i].getType() == UILayout.CONSTRAINT_PIXELS) pixelSum += layoutItems[i]
					.getLayoutHeight();
			if (layoutItems[i].getType() == UILayout.CONSTRAINT_PERCENTUAL) percentageSum += layoutItems[i]
					.getLayoutHeight();
		}
		int remainingPixels = 0;
		if (percentageSum > 0) {
			remainingPixels = ((this.getHeight(g) - pixelSum) * 100)
					/ percentageSum;
		}
		int pixelIndex = 0;
		int i = 0;
		for (i = 0; i < layoutItems.length - 1; i++) {
			if (layoutItems[i].getType() == UILayout.CONSTRAINT_PIXELS) {
				if (layoutItems[i].isDirty()) layoutItems[i].paint0(g, w,
						layoutItems[i].getLayoutHeight());
				pixelIndex += layoutItems[i].getLayoutHeight();
				g.translate(0, layoutItems[i].getLayoutHeight());
			}
			if (layoutItems[i].getType() == UILayout.CONSTRAINT_PERCENTUAL) {
				int ithLayoutHeight = (layoutItems[i].getLayoutHeight() * remainingPixels) / 100;
				if (layoutItems[i].isDirty()) layoutItems[i].paint0(g, w,
						ithLayoutHeight);
				pixelIndex += ithLayoutHeight;
				g.translate(0, ithLayoutHeight);
			}
		}
		// the last row is "painted alone" to fill all the remaining
		// pixels
		if (layoutItems[i].getType() == UILayout.CONSTRAINT_PIXELS) {
			if (layoutItems[i].isDirty()) layoutItems[i].paint0(g, w, this
					.getHeight(g)
					- pixelIndex);
			pixelIndex += layoutItems[i].getLayoutHeight();
			g.translate(0, layoutItems[i].getLayoutHeight());
		}
		if (layoutItems[i].getType() == UILayout.CONSTRAINT_PERCENTUAL) {
			int ithLayoutHeight = this.getHeight(g) - pixelIndex;
			if (layoutItems[i].isDirty()) layoutItems[i].paint0(g, w,
					ithLayoutHeight);
			pixelIndex += ithLayoutHeight;
			g.translate(0, ithLayoutHeight);
		}

		g.translate(originalGx - g.getTranslateX(), +originalGy
				- g.getTranslateY());
		if (this.selected && isGroup()) {
			this.drawSegmentedBorder(g, w, h);
		}
	}

	public int getHeight(Graphics g) {
		if (layoutHeight == -1) {
			layoutHeight = g.getClipHeight() + g.getClipY();
		}
		return this.layoutHeight;
	}

	public void setHeight(int layoutHeight) {
		this.layoutHeight = layoutHeight;
	}

	public boolean keyPressed(int key) {
		/* forward keypress only if this layout is focused */
		if (selectedIndex >= 0 && selectedIndex < this.layoutItems.length
				&& layoutFocused && layoutItems[selectedIndex].keyPressed(key)) {
			// this is needed since we cannot know if anything below has been
			// repainted
			updateChildren();
			return true;
		}

		int ga = UICanvas.getInstance().getGameAction(key);
		if (layoutFocused) {
			switch (ga) {
				case Canvas.UP:
					if (this.selectedIndex > 0) {
						int newSelectedIndex = this.selectedIndex - 1;
						if (this.isFocusable()) newSelectedIndex = traverseFocusable(
								newSelectedIndex, false);
						if (newSelectedIndex >= 0) this.selectedIndex = newSelectedIndex;
						else
							return false;
						updateChildren();
						return true;
					} else
						return false;
				case Canvas.DOWN:
					if (this.selectedIndex < this.layoutItems.length - 1) {
						int newSelectedIndex = this.selectedIndex + 1;
						if (this.isFocusable()) newSelectedIndex = traverseFocusable(
								newSelectedIndex, true);
						if (newSelectedIndex >= 0) this.selectedIndex = newSelectedIndex;
						else
							return false;
						updateChildren();
						return true;
					} else
						return false;
			}
		}

		if ((key == UICanvas.MENU_RIGHT || ga == Canvas.FIRE)) {
			if (this.isFocusable()) {
				this.layoutFocused = true;
				selectedIndex = traverseFocusable(selectedIndex, true);
				this.layoutItems[selectedIndex].setSelected(true);
				updateChildren();
				return true;
			}
		}

		return false;
	}
}
