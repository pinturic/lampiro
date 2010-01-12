/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: UICheckbox.java 1913 2009-12-02 14:21:24Z luca $
*/

package it.yup.ui;

import java.io.IOException;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Image;

/**
 * 
 */

/**
 * @author luca
 * 
 */
public class UICheckbox extends UILabel {
	private Image checkedImg=null;
	private Image uncheckedImg=null;
	
	{
	checkedImg = UICanvas.getUIImage("/icons/checked.png");
	uncheckedImg = UICanvas.getUIImage("/icons/unchecked.png");
	}

	/**
	 * Keeps the checked state of the Checkbox
	 * 
	 */
	private boolean checked = false;

	/**
	 * @throws IOException
	 * 
	 */
	public UICheckbox(String text) {
		super(UICanvas.getUIImage("/icons/unchecked.png"), text);
		this.focusable = true;
		this.wrappable = false;
	}

	public UICheckbox(String text, boolean checked) {
		this(text);
		this.setChecked(checked);
	}

	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean checked) {
		boolean changed = false;
		if (checked != this.checked)
			changed = true;
		this.checked = checked;
		if (changed == true) {
			if (checked == true)
				this.img = this.checkedImg;
			else
				this.img = this.uncheckedImg;
			this.dirty = true;
			this.askRepaint();
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	public boolean keyPressed(int key) {
		if (UICanvas.getInstance().getGameAction(key) == Canvas.FIRE)
			this.setChecked(!this.checked);
		return false;
	}

	/**
	 * @param checkedImg the checkedImg to set
	 */
	public void setCheckedImg(Image checkedImg) {
		this.checkedImg = checkedImg;
		this.dirty = true;
		this.askRepaint();
	}

	/**
	 * @return the checkedImg
	 */
	public Image getCheckedImg() {
		return checkedImg;
	}

	/**
	 * @param uncheckedImg the uncheckedImg to set
	 */
	public void setUncheckedImg(Image uncheckedImg) {
		this.uncheckedImg = uncheckedImg;
		this.dirty = true;
		this.askRepaint();
	}

	/**
	 * @return the uncheckedImg
	 */
	public Image getUncheckedImg() {
		return uncheckedImg;
	}
}
