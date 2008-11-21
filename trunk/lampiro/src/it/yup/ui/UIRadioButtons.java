/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: UIRadioButtons.java 878 2008-09-29 17:00:56Z luca $
*/

package it.yup.ui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class UIRadioButtons extends UIVLayout {

	private int chechedIndex = 0;

	public UIRadioButtons(String[] stringItems) {
		super(stringItems.length, 0);
		int buttonNumber = stringItems.length;
		for (int i = 0; i < buttonNumber; i++) {
			Image img = (i == 0 ? UICanvas
					.getUIImage("/icons/radio_checked.png") : UICanvas
					.getUIImage("/icons/radio_unchecked.png"));
			UILabel ulb = new UILabel(img, stringItems[i]);
			ulb.setFocusable(true);
			this.insert(ulb, i, 100 / buttonNumber,
					UILayout.CONSTRAINT_PERCENTUAL);
		}
		this.focusable = true;
	}

	protected void paint(Graphics g, int w, int h) {
		this.getHeight(g);
		super.paint(g, w, h);
	}

	public int getHeight(Graphics g) {
		int itemHeight = this.layoutItems[0].getHeight(g);
		this.layoutHeight = itemHeight * this.layoutItems.length;
		this.height = layoutHeight;
		return this.height;
	}

	public boolean keyPressed(int key) {
		int ga = UICanvas.getInstance().getGameAction(key);
		if (ga == Canvas.FIRE && this.selectedIndex >= 0
				&& this.chechedIndex != selectedIndex) {
			UILabel ulbOld = (UILabel) this.layoutItems[this.chechedIndex];
			ulbOld.img = UICanvas.getUIImage("/icons/radio_unchecked.png");
			ulbOld.setDirty(true);

			this.chechedIndex = this.selectedIndex;
			UILabel ulb = (UILabel) this.layoutItems[this.chechedIndex];
			ulb.img = UICanvas.getUIImage("/icons/radio_checked.png");
			ulb.setDirty(true);
			this.setDirty(true);
			this.askRepaint();
		}
		boolean keepSelection = super.keyPressed(key);
		// we must save the last selectedIndex
		// when loosing focus
		if (keepSelection == false) {
			if (selectedIndex >= 0) {
				UILabel ulbOld = (UILabel) this.layoutItems[this.selectedIndex];
				ulbOld.setSelected(false);
				ulbOld.setDirty(true);
				this.setDirty(true);
				this.askRepaint();
			}
			this.selectedIndex = this.chechedIndex;
		}
		return keepSelection;
	}

	public void setSelectedIndex(int i) {
		if (i < 0 || i > layoutItems.length) { return; }
		if (selectedIndex != -1) {
			layoutItems[selectedIndex].setSelected(false);
			layoutItems[selectedIndex].setDirty(true);
		}
		if (this.chechedIndex >= 0) {
			((UILabel) layoutItems[chechedIndex]).img = UICanvas
					.getUIImage("/icons/radio_unchecked.png");
			layoutItems[chechedIndex].setSelected(false);
		}
		layoutItems[i].setSelected(true);
		((UILabel) layoutItems[i]).img = UICanvas
				.getUIImage("/icons/radio_checked.png");
		selectedIndex = i;
		this.chechedIndex = this.selectedIndex;
		setDirty(true);
		askRepaint();
	}

	public int getSelectedIndex() {
		return selectedIndex;
	}
}
