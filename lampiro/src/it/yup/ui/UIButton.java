/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: UIButton.java 846 2008-09-11 12:20:05Z luca $
*/

/**
 * 
 */
package it.yup.ui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * @author luca
 * 
 */
public class UIButton extends UILabel {

	private boolean pressed = false;

	/**
	 * @param text
	 * @param screen
	 */
	public UIButton(String text) {
		super(text);
		this.focusable = true;
		this.wrappable = false;
	}

	protected void paint(Graphics g, int w, int h) {
		// what should happen in case the text of the button
		// is too long to fit ?

		Font usedFont = (this.font != null ? this.font : g.getFont());
		this.height = this.getHeight(g);
		this.width = usedFont.stringWidth(text) + 4;

		if (this.wrappable == true && this.getTextLines() == null) {
			computeTextLines(usedFont, w);
			// if h is lower then 0 it means
			// use the minimum height
			if (h < 0)
				paint(g, w, this.getHeight(g));
			else
				paint(g, w, h);
			return;
		}

		g.setColor(getBg_color() >= 0 ? getBg_color() : UIConfig.bg_color);
		g.fillRect(0, 0, w, h);

		int originalX = g.getTranslateX();
		int originalY = g.getTranslateY();
		g.translate(3, 0);
		super.paint(g, w - 5, h);
		g.translate(originalX - g.getTranslateX(), originalY
				- g.getTranslateY());
		this.height = this.getHeight(g);
		g.translate(0, (h - this.height) / 2);

		// int initialY = g.getTranslateY();
		// g.translate(0, (h - this.height) / 2);
		// g.setColor(UIConfig.bg_color);
		// g.fillRect(0, 0, w, height);
		// g.setColor(selected ? UIConfig.header_bg : UIConfig.bg_color);
		// g.fillRect(2, 1, w - 4, height - 1);
		// g.setColor(selected ? UIConfig.header_fg : UIConfig.fg_color);
		// g.drawString(this.text, 4, 1, Graphics.LEFT | Graphics.TOP);
		// System.out.println("Drawn UIButton '" + text + "' at: ("
		// + g.getTranslateX() + ", " + g.getTranslateY() + ")"
		// + (selected ? "S" : ""));

		this.height = this.getHeight(g);
		g.setColor(0x999999);
		g.drawLine(2, 1, w - 2, 1);
		g.drawLine(2, 1, 2, this.height - 2);
		g.setColor(0x444444);
		g.drawLine(w - 2, this.height - 2, w - 2, 1);
		g.drawLine(w - 2, this.height - 2, 2, this.height - 2);
		g.translate(originalX - g.getTranslateX(), originalY
				- g.getTranslateY());
		// g.translate(0, initialY - g.getTranslateY());
	}

	/**
	 * @return the pressed
	 */
	public boolean isPressed() {
		return pressed;
	}

	/**
	 * @param pressed
	 *            the pressed to set
	 */
	public void setPressed(boolean pressed) {
		this.pressed = pressed;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getHeight(Graphics g) {
		return super.getHeight(g) + 2;
	}
}
