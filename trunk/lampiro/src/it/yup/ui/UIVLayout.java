/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: UIVLayout.java 2002 2010-03-06 19:02:12Z luca $
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

	/**
	 * @param rowNumber
	 *            The row number of the layOut.
	 * @param height
	 *            The height of the item
	 */
	public UIVLayout(int rowNumber, int height) {
		super(rowNumber);
		this.dirKey1 = Canvas.UP;
		this.dirKey2 = Canvas.DOWN;
		this.height = height;
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

	protected int getLayoutDimension(int i) {
		return layoutItems[i].getLayoutHeight();
	}

	protected int getMyDimension(Graphics g, int w) {
		return this.getHeight(g);
	}

	protected void paintLayoutItem(int i, Graphics g, int w, int h,
			int forcedDim) {
		layoutItems[i].paint0(g, w, forcedDim);
	}

	protected void paintLastItem(int i, Graphics g, int w, int h, int pixelIndex) {
		paintLayoutItem(i, g, w, h, this.getHeight(g) - pixelIndex);
	}

	protected void translateG(Graphics g, int i, int forcedDim) {
		g.translate(0, forcedDim);
	}

	public int getHeight(Graphics g) {
		if (height == -1) {
			height = g.getClipHeight() + g.getClipY();
		}
		return this.height;
	}

	public void setHeight(int layoutHeight) {
		this.height = layoutHeight;
	}
}
