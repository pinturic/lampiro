/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: UICombobox.java 1017 2008-11-28 21:57:46Z luca $
*/

/**
 * 
 */
package it.yup.ui;

import java.util.Enumeration;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * @author luca
 * 
 */
public class UICombobox extends UILabel {

	/**
	 * @author luca
	 * 
	 */
	private class UIComboMenu extends UIMenu {

		/**
		 * 
		 */
		public UIComboMenu() {
			super("");
		}

		protected UIItem keyPressed(int key, int ga) {
			if ((key == UICanvas.MENU_RIGHT || ga == Canvas.FIRE)
					&& (this.selectedIndex >= 0 || UICombobox.this.multiChoice)) {
				UICombobox.this.selectedIndex = this.selectedIndex;
				UIItem selItem = null;
				if (UICombobox.this.multiChoice == false) {
					UICombobox.this.setDirty(true);
					UICombobox.this.askRepaint();
					this.openedState = false;
					this.screen.removePopup(this);
					selItem = super.keyPressed(key, ga);
					this.screen.itemAction(UICombobox.this);
				} else {
					if (ga == Canvas.FIRE) {
						// UICheckbox selectedItem = (UICheckbox) this.itemList
						// .elementAt(this.selectedIndex);
						// (selectedItem).setChecked(!selectedItem.isChecked());
						super.keyPressed(key, ga);
					} else {
						this.openedState = false;
						if (UICombobox.this.multiChoice == false) {
							Enumeration en = UIComboMenu.this.getItemList()
									.elements();
							while (en.hasMoreElements()) {
								UILabel uil = (UILabel) en.nextElement();
								uil.setWrappable(false, width);
							}
						}
						this.screen.removePopup(this);
						this.screen.itemAction(UICombobox.this);
					}
					UICombobox.this.setDirty(true);
					UICombobox.this.askRepaint();
				}

				return selItem;
			} else {
				UIItem retItem = super.keyPressed(key, ga);
				if (UICombobox.this.multiChoice == false) {
					for (int i = 0; i < itemList.size(); i++) {
						UILabel uil = (UILabel) this.itemList.elementAt(i);
						if (i != UIComboMenu.this.selectedIndex) {
							uil.setWrappable(false, width);
						} else {
							uil.setWrappable(true, this.getWidth() - 10);
						}
					}
				}
				this.askRepaint();
				return retItem;
			}

		}

		public void paint(Graphics g, int w, int h) {
			// is it too wide
			int availableWidth = UICanvas.getInstance().getWidth()
					- (this.screen.getNeedScrollbar() ? UIConfig.scrollbarWidth
							: 0) - 1;
			if (this.absoluteX + this.width > availableWidth) {
				this.absoluteX -= (this.absoluteX - availableWidth + this.width);
				g.translate(this.absoluteX - g.getTranslateX(), 0);// +
			}
			super.paint(g, w, h);
		}
	}

	/**
	 * Used to know the absolute origin of the Menu on the screen. We must
	 * invalidate the background when hiding.
	 */
	private int absoluteY = 0;

	/**
	 * Used to know the absolute origin of the Menu on the screen. We must
	 * invalidate the background when hiding.
	 */
	private int absoluteX = 0;

	/** the selected index in the item Vector */
	private int selectedIndex = 0;

	/** the pop-up menu shown when FIRE is pressed on the combo-box */
	private UIComboMenu comboMenu = null;

	private boolean multiChoice = false;
	protected UILabel title = new UILabel("");

	/** Constructor */
	public UICombobox(String title, boolean multichoice) {
		super(UICanvas.getUIImage("/icons/combo.png"), "");
		this.focusable = true;
		this.flip = true;
		this.multiChoice = multichoice;
		this.title.setText(title);
		this.wrappable = false;
		this.comboMenu = new UIComboMenu();
		this.anchorPoint = Graphics.RIGHT | Graphics.VCENTER;
		Font f = UIConfig.font_body;
		Font titleFont = Font
				.getFont(f.getFace(), Font.STYLE_BOLD, f.getSize());
		this.title.setFont(titleFont);
		this.title.setFocusable(true);

	}

	public void setSelected(boolean _selected) {
		this.dirty = true;
		this.title.setSelected(_selected);
		super.setSelected(_selected);
	}

	public int getHeight(Graphics g) {
		this.height = super.getHeight(g) + this.title.getHeight(g);
		return this.height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.yup.ui.UIItem#paint(javax.microedition.lcdui.Graphics, int, int)
	 */
	protected void paint(Graphics g, int w, int h) {
		if (this.multiChoice == false) {
			if (this.comboMenu.getItemList().size() > 0) this.text = ((UILabel) this.comboMenu
					.getItemList().elementAt(this.selectedIndex)).getText();
		} else {
			this.text = "";
			if (this.comboMenu.getItemList().size() > 0) {
				for (int i = 0; i < this.comboMenu.getItemList().size(); i++) {
					UICheckbox uic = (UICheckbox) this.comboMenu.getItemList()
							.elementAt(i);
					if (uic.isChecked()) this.text += (uic.getText() + " ");
				}
			}
		}
		this.height = h;
		this.absoluteY = g.getTranslateY();
		this.absoluteX = g.getTranslateX();
		comboMenu.setAbsoluteX(this.absoluteX);
		int superHeight = super.getHeight(g);
		int comboHeight = comboMenu.getHeight(g);
		// the computation above is used to compute the correct offset in
		// drawing
		if (this.absoluteY + this.height + comboHeight < UICanvas.getInstance()
				.getClipHeight()) {
			comboMenu.setAbsoluteY(this.absoluteY + superHeight
					+ (h - superHeight) / 2);
		} else {
			comboMenu.setAbsoluteY(this.absoluteY - comboHeight
					+ (h - superHeight) / 2);
		}
		this.title.paint(g, w, this.title.getHeight(g));
		int titleHeight = title.getHeight(g);
		g.translate(0, titleHeight);
		super.paint(g, w, super.getHeight(g));
	}

	public void append(String comboItem) {

		UIItem uimi = null;
		if (this.multiChoice == false) {
			uimi = new UILabel(comboItem);
		} else {
			uimi = new UICheckbox(comboItem);
		}
		comboMenu.append(uimi);
	}

	public void removeAt(int index) {
		this.comboMenu.remove(index);
	}

	public boolean keyPressed(int key) {
		int ga = UICanvas.getInstance().getGameAction(key);
		if (ga == Canvas.FIRE && !this.screen.popupIsPresent(comboMenu)) {
			return openMenu();
		} else if (ga == Canvas.FIRE && this.comboMenu != null) {
			comboMenu.setOpenedState(false);
			return false;
		}
		return super.keyPressed(key);
	}

	/**
	 * @return
	 */
	public boolean openMenu() {
		comboMenu.setWidth(this.width);
		screen.addPopup(comboMenu);
		this.setDirty(true);
		this.askRepaint();
		return false;
	}

	/**
	 * @return The selected index in the item list.
	 */
	public int getSelectedIndex() {
		return selectedIndex;
	}

	public int[] getSelectedIndeces() {

		int selNumber = 0;
		for (Enumeration en = comboMenu.getItemList().elements(); en
				.hasMoreElements();) {
			UICheckbox uic = (UICheckbox) en.nextElement();
			if (uic.isChecked()) selNumber++;
		}
		int[] result = new int[selNumber];
		selNumber = 0;
		int i = 0;
		for (Enumeration en = comboMenu.getItemList().elements(); en
				.hasMoreElements();) {
			UICheckbox uic = (UICheckbox) en.nextElement();
			if (uic.isChecked()) {
				result[selNumber] = i;
				selNumber++;
			}
			i++;
		}
		return result;
	}

	/**
	 * Sets the slected index in the list
	 * 
	 * @param si
	 *            the new selected index
	 */
	public void setSelectedIndex(int si) {
		selectedIndex = si;
		dirty = true;
	}

	/**
	 * Used only for a multichoice combo box: sets selected/deselected flags on
	 * all items
	 * 
	 * @param flags
	 *            the selected flags
	 */
	public void setSelectedFlags(boolean[] flags) {
		if (!isMultiChoice() || flags == null
				|| flags.length != comboMenu.getItemList().size()) { return; }
		for (int i = 0; i < flags.length; i++) {
			UICheckbox uic = (UICheckbox) comboMenu.getItemList().elementAt(i);
			uic.setChecked(flags[i]);
		}
		dirty = true;
	}

	/**
	 * @return The array of selection flags.
	 */
	public boolean[] getSelectedFlags() {
		boolean[] flags = new boolean[comboMenu.getItemList().size()];
		if (this.multiChoice) {
			for (int i = 0; i < flags.length; i++) {
				UICheckbox uic = (UICheckbox) comboMenu.getItemList()
						.elementAt(i);
				flags[i] = uic.isChecked();
			}
		} else if (this.selectedIndex >= 0
				&& comboMenu.getItemList().size() > 0) flags[this.selectedIndex] = true;
		return flags;
	}

	public void setDirty(boolean dirty) {
		super.setDirty(dirty);
		if (comboMenu != null) this.comboMenu.setDirty(dirty);
	}

	/**
	 * @return the multiChoice
	 */
	public boolean isMultiChoice() {
		return multiChoice;
	}

	/**
	 * Returns true if the item at the selected index is selected.
	 * 
	 * @param idx
	 *            The item to check for selection
	 * @return {@code true} it the item is selected. {@code false} if the item
	 *         is not selected or idx is not a valid index.
	 */
	public boolean isSelected(int idx) {
		if (idx < 0 || idx > comboMenu.getItemList().size()) { return false; }
		UICheckbox uic = (UICheckbox) comboMenu.getItemList().elementAt(idx);
		return uic.isChecked();
	}

	/**
	 * Return the selected UIItem within the UIItem itself; usually it is the
	 * UIItem itself but in the subclasses (like UIVLayout) it could one of the
	 * contained object.
	 * 
	 * @return
	 */
	public UIItem getSelectedItem() {
		// UICombobox SHOULD not raise any itemAction when it is "clicked"
		// and it is closed
		return null;
	}

}
