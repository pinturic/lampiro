/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: UIHLayout.java 2002 2010-03-06 19:02:12Z luca $
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
		this.dirKey1 = Canvas.LEFT;
		this.dirKey2 = Canvas.RIGHT;
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
		item.setDirty(true);
	}

	protected int getLayoutDimension(int i) {
		return layoutItems[i].getLayoutWidth();
	}

	protected int getMyDimension(Graphics g, int w) {
		return w;
	}

	protected void paintLayoutItem(int i, Graphics g, int w, int h,
			int forcedDim) {
		layoutItems[i].paint0(g, forcedDim, h);
	}

	protected void paintLastItem(int i, Graphics g, int w, int h, int pixelIndex) {
		paintLayoutItem(i, g, w, h, w - pixelIndex);
	}

	protected void translateG(Graphics g, int i, int forcedDim) {
		g.translate(forcedDim, 0);
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
}
