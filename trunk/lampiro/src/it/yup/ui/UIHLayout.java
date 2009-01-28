/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: UIHLayout.java 1136 2009-01-28 11:25:30Z luca $
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
public class UIHLayout extends UILayout {

	/**
	 * @param colNumber
	 *            The number of columns in the layOut.
	 */
	public UIHLayout(int colNumber) {
		super(colNumber);
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
	public void insert(UIItem item, int index, int width, int type) {
		this.layoutItems[index] = item;
		item.setLayoutWidth(width);
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
		this.height = h;
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
					.getLayoutWidth();
			if (layoutItems[i].getType() == UILayout.CONSTRAINT_PERCENTUAL) percentageSum += layoutItems[i]
					.getLayoutWidth();
		}
		int remainingPixels = 0;
		if (percentageSum > 0) {
			remainingPixels = ((w - pixelSum) * 100) / percentageSum;
		}
		int pixelIndex = 0;
		int i = 0;
		for (i = 0; i < layoutItems.length - 1; i++) {
			if (layoutItems[i].getType() == UILayout.CONSTRAINT_PIXELS) {
				if (layoutItems[i].isDirty()) layoutItems[i].paint0(g,
						layoutItems[i].getLayoutWidth(), h);
				pixelIndex += layoutItems[i].getLayoutWidth();
				g.translate(layoutItems[i].getLayoutWidth(), 0);
			}
			if (layoutItems[i].getType() == UILayout.CONSTRAINT_PERCENTUAL) {
				int ithLayoutWidth = (layoutItems[i].getLayoutWidth() * remainingPixels) / 100;
				if (layoutItems[i].isDirty()) layoutItems[i].paint0(g,
						ithLayoutWidth, h);
				pixelIndex += ithLayoutWidth;
				g.translate(ithLayoutWidth, 0);
			}
		}

		// the last column is "painted alone" to fill all the remaining
		// pixels
		if (layoutItems[i].getType() == UILayout.CONSTRAINT_PIXELS) {
			if (layoutItems[i].isDirty()) layoutItems[i].paint0(g, w
					- pixelIndex, h);
			pixelIndex += layoutItems[i].getLayoutWidth();
			g.translate(layoutItems[i].getLayoutWidth(), 0);
		}
		if (layoutItems[i].getType() == UILayout.CONSTRAINT_PERCENTUAL) {
			int ithLayoutWidth = w - pixelIndex;
			if (layoutItems[i].isDirty()) layoutItems[i].paint0(g,
					ithLayoutWidth, h);
			pixelIndex += ithLayoutWidth;
			g.translate(ithLayoutWidth, 0);
		}

		g.translate(originalGx - g.getTranslateX(), +originalGy
				- g.getTranslateY());
		if (this.selected && isGroup()) {
			this.drawSegmentedBorder(g, w, h);
		}
	}

	public int getHeight(Graphics g) {
		this.height = 0;
		// the height in this case is the greatest among
		// all the items
		for (int i = 0; i < layoutItems.length; i++) {
			int tempHeight = layoutItems[i].getHeight(g);
			if (tempHeight > this.height) this.height = tempHeight;
		}
		return height;
	}

	public boolean keyPressed(int key) {
		// first forward the keyPressed!!!!
		if (selectedIndex >= 0 && selectedIndex < this.layoutItems.length
				&& this.layoutFocused == true
				&& this.layoutItems[selectedIndex].keyPressed(key) == true) {
			// this is needed since we cannot know if anything below has been
			// repainted
			updateChildren();
			return true;
		}

		int ga = UICanvas.getInstance().getGameAction(key);
		if (this.layoutFocused == true) {
			switch (ga) {
				case Canvas.RIGHT:
					if (this.selectedIndex < this.layoutItems.length - 1) {
						int newSelectedIndex = this.selectedIndex + 1;
						if (this.isFocusable()) newSelectedIndex = traverseFocusable(
								newSelectedIndex, true);
						if (newSelectedIndex >= 0) this.selectedIndex = newSelectedIndex;
						else
							return false;
						updateChildren();
						return true;
					}
					return false;
				case Canvas.LEFT:
					if (this.selectedIndex > 0) {
						int newSelectedIndex = this.selectedIndex - 1;
						if (this.isFocusable()) newSelectedIndex = traverseFocusable(
								newSelectedIndex, false);
						if (newSelectedIndex >= 0) this.selectedIndex = newSelectedIndex;
						else
							return false;
						updateChildren();
						return true;
					}
					return false;
			}
		}

		if ((key == UICanvas.MENU_RIGHT || ga == Canvas.FIRE)) {
			if (this.isFocusable()) {
				this.layoutFocused = true;
				selectedIndex = traverseFocusable(selectedIndex, true);
				this.layoutItems[selectedIndex].setSelected(true);
				updateChildren();
			}
		}

		return false;
	}

	/*
	 * An helper function that builds an horizontal layout of three items.
	 * The first and third item are dummy and the second is the passed item.
	 * Size is the size in pixel od the seconde layout element
	 * 
	 * @param item
	 * 			the item to insert in the middle of the layout
	 * @param size
	 * 			the size of the middle element of the layout  
	 */
	public static UIHLayout easyCenterLayout (UILabel item, int size){
		UIHLayout buttonLayout = new UIHLayout(3);
		UILabel dummyLabel = new UILabel("");
		item.setAnchorPoint(Graphics.HCENTER);
		buttonLayout.setGroup(false);
		buttonLayout.insert(dummyLabel, 0, 50, UILayout.CONSTRAINT_PERCENTUAL);
		buttonLayout.insert(item, 1, size, UILayout.CONSTRAINT_PIXELS);
		buttonLayout.insert(dummyLabel, 2, 50, UILayout.CONSTRAINT_PERCENTUAL);
		return buttonLayout;
	}

}
