/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: UIPanel.java 998 2008-11-18 10:44:25Z luca $
*/

package it.yup.ui;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

/**
 * A panel that stacks vertically items vertically. If the container height is
 * exceeeded, a scrollbar is drawn.
 */
public class UIPanel extends UIItem implements UIIContainer {

	/** the contained items */
	private Vector items;

	/** real height */
	protected int maxHeight;

	/** the first visible item */
	protected int firstVisible;

	/** the last visible item */
	protected int lastVisible;

	/** the selected item */
	protected int selectedIdx;

	public UIPanel() {
		selectedIdx = -1;
		maxHeight = -1;
		items = new Vector();
	}

	public void setScreen(UIScreen _us) {
		screen = _us;
		Enumeration en = this.items.elements();
		while (en.hasMoreElements()) {
			UIItem uit = (UIItem) en.nextElement();
			uit.setScreen(_us);
		}

	}

	protected void paint(Graphics g, int w, int h) {

		int rh = computeRealHeight(g);

		if (rh > h) {
			w -= UIConfig.scrollbarWidth; // + 1;
		}

		int otx = g.getTranslateX();
		int oty = g.getTranslateY();

		// if selectedIndex is after lastVisible or before firstVisible I could
		// need many redraw
		boolean needRedraw = true;
		while (needRedraw) {
			needRedraw = false;
			int th = 0;
			for (int i = firstVisible; i < items.size() && th < h; i++) {
				UIItem ui = ((UIItem) items.elementAt(i));
				int ih = ui.getHeight(g);
				if (ui.isDirty()) {
					ui.paint0(g, w, ih);
				}
				g.translate(0, ih);
				th += ih;
				lastVisible = i;
			}
			g.translate(otx - g.getTranslateX(), oty - g.getTranslateY());
			if (th > h) {
				/* the last item is not fully visible */
				lastVisible--;
			}
			if (th < h) {
				/* fill the gap */
				int oc = g.getColor();
				g.setColor(getBg_color() >= 0 ? getBg_color()
						: UIConfig.bg_color);
				g.fillRect(0, th, w, h - th);// - 1);
				g.setColor(oc);
			}

			/* scroll down -> need to calculate space */
			if (selectedIdx != -1 && selectedIdx > lastVisible
					&& this.selectedIdx > this.firstVisible) {
				int delta = 0;
				for (int i = lastVisible + 1; i <= selectedIdx; i++) {
					delta += ((UIItem) items.elementAt(i)).getHeight(g);
				}
				do {
					firstVisible++;
					delta -= ((UIItem) items.elementAt(firstVisible))
							.getHeight(g);
				} while (delta > 0 && firstVisible < this.items.size() - 1);
				for (int i = firstVisible; i < this.items.size(); i++) {
					((UIItem) items.elementAt(i)).setDirty(true);
				}
				needRedraw = true;
			}

			/* up check is easier ;) */
			if (selectedIdx != -1 && selectedIdx < firstVisible) {
				firstVisible = selectedIdx;
				for (int i = firstVisible; i < this.items.size(); i++) {
					((UIItem) items.elementAt(i)).setDirty(true);
				}
				needRedraw = true;
			}
		}

		if (rh > h) {
			w += UIConfig.scrollbarWidth;// + 1;
			drawScrollBar(g, w, h, rh);
		}

		/* resets origin to old value */
		g.translate(otx - g.getTranslateX(), oty - g.getTranslateY());
	}

	/**
	 * Draws the scrollbar for the given rectangle
	 * 
	 * @param g
	 *            the graphics context (origin should be translated)
	 * @param w
	 *            the width of this panel
	 * @param h
	 *            the height of this panel
	 * @param rh
	 *            the real height of this panel
	 */
	protected void drawScrollBar(Graphics g, int w, int h, int rh) {
		int otx = g.getTranslateX();
		int oty = g.getTranslateY();
		int oc = g.getColor();
		g.setColor(UIConfig.scrollbar_bg);
		g.translate(w - UIConfig.scrollbarWidth, 0);
		g.fillRect(0, 0, UIConfig.scrollbarWidth, h);

		/* calculate y and height of scrollbar */
		int sy = h * firstVisible / items.size();
		int sh = (h * h) / rh;
		if (sy + sh > h || lastVisible == items.size() - 1) {
			sy = h - sh;
		}

		g.setColor(UIConfig.scrollbar_fg);
		g.fillRect(0, sy, UIConfig.scrollbarWidth, sh);
		/* resets origin to old value */
		g.translate(otx - g.getTranslateX(), oty - g.getTranslateY());
		g.setColor(oc);
	}

	public int getHeight(Graphics g) {
		/* always all the available space */
		if (maxHeight != -1) { return maxHeight; }
		if (this.height > 0) return this.height;
		// if i have a clip that is my height
		int clipY = g.getClipY();// .getClipY();
		int clippedHeight = g.getClipHeight() + clipY;
		// if (clippedHeight > 0)
		this.height = clippedHeight;
		return this.height;
		// otherwise my last known height

	}

	public void setDirty(boolean _dirty) {
		for (int i = 0; i < items.size(); i++) {
			UIItem ui = (UIItem) items.elementAt(i);
			ui.setDirty(_dirty);
		}
		dirty = _dirty;
		// so that it will be computed again !
		height = -1;
	}

	public boolean isDirty() {
		for (int i = 0; i < this.items.size(); i++)
			if (((UIItem) this.items.elementAt(i)).isDirty()) return true;
		if (this.dirty) return true;
		return false;
	}

	/**
	 * Calculates the real height of the item
	 */
	protected int computeRealHeight(Graphics g) {
		int realHeight = 0;
		/*
		 * saves the old coordinate origin, and calculates the height of each
		 * contained item
		 */
		int otx = g.getTranslateX();
		int oty = g.getTranslateY();
		for (int i = 0; i < items.size(); i++) {
			int ih = ((UIItem) items.elementAt(i)).getHeight(g);
			g.translate(0, ih);
			realHeight += ih;
		}
		/* resets origin to old value */
		g.translate(otx - g.getTranslateX(), oty - g.getTranslateY());
		return realHeight;
	}

	protected void updateChildren() {
		int index = 0;
		for (Enumeration en = this.items.elements(); en.hasMoreElements();) {
			UIItem item = (UIItem) en.nextElement();
			if (this.selectedIdx != index) {
				item.setSelected(false);
			}
			index++;
		}
		((UIItem) this.items.elementAt(selectedIdx)).setSelected(true);
		this.setDirty(true);
		this.askRepaint();
	}

	/**
	 * 
	 */
	public boolean keyPressed(int key) {

		if (this.selectedIdx >= 0
				&& selectedIdx < this.items.size()
				&& ((UIItem) this.items.elementAt(selectedIdx)).keyPressed(key) == true) {
			// this is needed since we cannot know if anything below has been
			// repainted
			updateChildren();
			return true;
		}

		int ga = UICanvas.getInstance().getGameAction(key);
		boolean keepFocus = true;
		int nsi;
		UIItem ui = null;

		if (items.size() == 0) { return false; }

		switch (ga) {
			case Canvas.DOWN:
				if (selectedIdx == -1) {
					/* XXX: nothing selected still: select first visible */
					// selectedIdx = firstVisible - 1;
				}
				keepFocus = false;
				// if none of the following items can be focused
				// the UIPanel looses the focus
				for (int i = selectedIdx + 1; i < items.size(); i++) {
					if (((UIItem) this.items.elementAt(i)).isFocusable() == true) {
						keepFocus = true;
						break;
					}
				}
				if (selectedIdx >= items.size() - 1) {
					/*
					 * end of list, won't go further: will lose selection if there's
					 * another item after
					 */
					keepFocus = false;
					break;
				}
				dirty = true;
				/* set selection on next item (if exists) */
				nsi = selectedIdx + 1;
				for (; nsi < items.size(); nsi++) {
					ui = (UIItem) items.elementAt(nsi);
					if (ui.isFocusable()) {
						ui.setSelected(true);
						/* breaks out of loop not from switch */
						break;
					}
				}
				/*
				 * found another selectable item and there was another old selected
				 * item, remove selection from old item
				 */
				if (selectedIdx != -1 && nsi < items.size()) {
					ui = (UIItem) items.elementAt(selectedIdx);
					ui.setSelected(false);
				}
				/* found another selectable item, select it */
				if (nsi < items.size()) {
					selectedIdx = nsi;
				} else if (lastVisible < items.size() - 1) {
					/*
					 * there's still something after but it's not visible, move down
					 * so it can be shown
					 */
					firstVisible++;
				}
				for (int i = firstVisible; i < items.size(); i++) {
					((UIItem) items.elementAt(i)).setDirty(true);
				}
				break;
			case Canvas.UP:
				if (selectedIdx == -1) {
					/* XXX: nothing selected, select last visible */
					// selectedIdx = lastVisible + 1;
				}
				keepFocus = false;
				// if none of the previous items can be focused
				// the UIPanel looses the focus
				for (int i = 0; i < selectedIdx; i++) {
					if (((UIItem) this.items.elementAt(i)).isFocusable() == true) {
						keepFocus = true;
						break;
					}
				}
				if (selectedIdx == 0) {
					/* start of list, won't go further */
					// selectedIdx = -1;
					// ui = (UIItem) items.elementAt(0);
					// ui.setSelected(false);
					keepFocus = false;
					break;
				}

				dirty = true;
				/* set selection on previous item (if exists) */
				nsi = selectedIdx - 1;
				ui = null;
				for (; nsi >= 0; nsi--) {
					ui = (UIItem) items.elementAt(nsi);
					if (ui.isFocusable()) {
						ui.setSelected(true);
						/* breaks out of loop not from switch */
						break;
					}
				}
				if (nsi >= 0) {
					/* found another selectable item, remove selection from old item */
					ui = (UIItem) items.elementAt(selectedIdx);
					ui.setSelected(false);
					selectedIdx = nsi;
				} else if (firstVisible > 0) {
					/* move up anyway */
					firstVisible--;
				}
				for (int i = firstVisible; i < items.size(); i++) {
					((UIItem) items.elementAt(i)).setDirty(true);
				}
				break;
			default:
				break;
		}

		if (key == UICanvas.MENU_LEFT || key == UICanvas.MENU_RIGHT) {
			keepFocus = false;
		}

		if (dirty) {
			askRepaint();
		}

		return keepFocus;
	}

	/**
	 * For now, always focusable
	 */
	public boolean isFocusable() {
		return true;
	}

	/**
	 * Selection status change. Whene select becomes true, select first item if
	 * no item has been selected. De-select last selected item if Panel gets
	 * de-selected.
	 * 
	 * @param _selected
	 *            {@code true} if panel becomes selected, {@code false}
	 *            otherwise.
	 */
	public void setSelected(boolean _selected) {
		super.setSelected(_selected);
		if (_selected && selectedIdx == -1 && items.size() > 0) {
			selectedIdx = 0;
			((UIItem) items.elementAt(0)).setSelected(_selected);
		}
		if (selectedIdx >= 0 && selectedIdx < items.size()) {
			((UIItem) items.elementAt(selectedIdx)).setSelected(_selected);
		}
	}

	/**
	 * Sets the maximum height for this Panel. A max height of -1 (or any
	 * negative value) indicates that the Panel should take up all the available
	 * space.
	 * 
	 * @param mh
	 *            The new max height
	 */
	public void setMaxHeight(int mh) {
		if (mh < -1) {
			mh = -1;
		}
		maxHeight = mh;
	}

	/**
	 * @return The current max height or -1 if this Panel takes up all the
	 *         available space.
	 */
	public int getMaxHeight() {
		return maxHeight;
	}

	/**
	 * Adds an item to the panel
	 * 
	 * @param it
	 *            The item to add
	 */
	public void addItem(UIItem it) {
		items.addElement(it);
		it.setScreen(this.screen);
		it.setContainer(this);
		this.dirty = true;
		this.height = -1;
	}

	/**
	 * 
	 * The number of items in the Panel.
	 * 
	 * @return The number of items.
	 */
	public Vector getItems() {
		return this.items;
	}

	/**
	 * Removes an item from the panel
	 * 
	 * @param it
	 *            The item to remove
	 */
	public void removeItem(UIItem it) {
		int iIndex = items.indexOf(it);
		if (this.screen != null) {
			this.screen.removePaintedItem(it);
		}
		this.removeItemAt(iIndex);
	}

	/**
	 * Removes the item at the given index from the list.
	 * 
	 * @param idx
	 *            The item index to remove
	 */
	public void removeItemAt(int idx) {
		if (idx < 0 || idx > items.size()) { return; }
		if (selectedIdx >= idx) {
			/* clear selection */
			selectedIdx -= 1;
		}
		UIItem ithItem = ((UIItem) items.elementAt(idx));
		ithItem.setSelected(false);
		items.removeElementAt(idx);
		if (this.screen != null) {
			this.screen.removePaintedItem(ithItem);
		}
		if (selectedIdx > 0 && selectedIdx < this.items.size()) ((UIItem) items
				.elementAt(selectedIdx)).setDirty(true);
		for (int i = idx; i < items.size(); i++) {
			((UIItem) items.elementAt(i)).setDirty(true);
		}
		this.dirty = true;
	}

	/**
	 * Removes the item at the given index from the list.
	 * 
	 * @param idx
	 *            The item index to remove
	 */
	public void insertItemAt(UIItem it, int idx) {
		if (idx < 0 || idx > items.size()) { return; }
		if (selectedIdx >= idx) {
			/* move selection after */
			selectedIdx++;
		}
		items.insertElementAt(it, idx);
		for (int i = idx + 1; i < items.size(); i++) {
			((UIItem) items.elementAt(i)).setDirty(true);
		}
		it.setContainer(this);
		this.dirty = true;
	}

	/**
	 * Remove all elements
	 */
	public void removeAllItems() {
		if (selectedIdx > 0 && selectedIdx < this.items.size()) ((UIItem) items
				.elementAt(this.selectedIdx)).setSelected(false);
		items.removeAllElements();
		setDirty(true);
		selectedIdx = -1;
		firstVisible = 0;
	}

	/**
	 * Sets the currently selected item at the position given.
	 * 
	 * @param idx
	 *            The index to select. -1 to clear selection
	 */
	public void setSelectedIndex(int idx) {
		if (idx < -1 || idx >= items.size()) {
			/* wrong index, ignore */
			return;
		}
		if (selectedIdx != -1) {
			((UIItem) items.elementAt(selectedIdx)).setSelected(false);
			((UIItem) items.elementAt(selectedIdx)).setDirty(true);
		}
		selectedIdx = idx;
		if (selectedIdx != -1) {
			((UIItem) items.elementAt(idx)).setSelected(true);
			((UIItem) items.elementAt(idx)).setDirty(true);
		}
		if (selectedIdx > lastVisible) {
			// forcing it to be the last visible to avoid useless
			// redraw
			lastVisible = selectedIdx;
		}
	}

	/**
	 * Return the selected UIItem within the UIItem itself; usually it is the
	 * UIItem itself but in the subclasses (like UIVLayout) it could be one of
	 * the contained object.
	 * 
	 * @return
	 */
	public UIItem getSelectedItem() {
		if (this.selectedIdx >= 0
				&& this.items.size() >= (this.selectedIdx + 1)) {
			return ((UIItem) this.items.elementAt(this.selectedIdx))
					.getSelectedItem();
		} else {
			return this;
		}
	}

	public void setSelectedItem(UIItem item) {
		int index = this.items.indexOf(item);
		this.setSelectedIndex(index);
		if (this.getContainer() != null) {
			this.getContainer().setSelectedItem(this);
		}
	}
}
