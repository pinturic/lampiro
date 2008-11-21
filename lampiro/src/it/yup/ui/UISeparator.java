/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: UISeparator.java 846 2008-09-11 12:20:05Z luca $
*/

/**
 * 
 */
package it.yup.ui;

import javax.microedition.lcdui.Graphics;

/**
 * @author luca
 * 
 */
public class UISeparator extends UIItem {

	/**
	 * 
	 */
	public UISeparator(int height) {
		this.height = height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.yup.ui.UIItem#paint(javax.microedition.lcdui.Graphics, int, int)
	 */
	protected void paint(Graphics g, int w, int h) {
		g.setColor(getBg_color() >= 0 ? getBg_color() : UIConfig.bg_color);
		g.fillRect(0, 0, w, h);
	}
}
