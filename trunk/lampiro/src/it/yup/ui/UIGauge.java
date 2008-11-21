/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: UIGauge.java 877 2008-09-29 15:22:02Z luca $
*/

/**
 * 
 */
package it.yup.ui;

import java.util.TimerTask;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Graphics;
import javax.microedition.m3g.Background;

/**
 * Re-implementation of the {@link Gauge} class using our library. Differences:
 * maximum Value cannot be changed after construction.
 */
public class UIGauge extends UIItem {

	/** if {@code true} this Gauge may be modified programmatically */
	private boolean modifiable;
	/** the current value */
	private int value;
	/** the maximum value */
	private int maxValue;
	/** the label shown to the user */
	private String label;
	/** behaviour when the maxValue is indefinite */
	private int behaviour;
	/** the task used to animate the gauge */
	private Ticker ticker;

	/**
	 * Constructor, see Gauge constructor for details.
	 * 
	 * @param label
	 *            The label to show.
	 * @param interactive
	 *            tells whether the user can change the value
	 * @param maxVal
	 *            the maximum value, or INDEFINITE
	 * @param initVal
	 *            the initial value in the range [0..maxValue], or one of
	 *            CONTINUOUS_IDLE, INCREMENTAL_IDLE, CONTINUOUS_RUNNING, or
	 *            INCREMENTAL_UPDATING if maxVal is INDEFINITE.
	 */
	public UIGauge(String _label, boolean interactive, int maxVal, int initVal) {
		label = _label;
		if (interactive && maxVal < 0) { throw new IllegalArgumentException(
				"maxValue negative for interactive mode"); }
		if (!interactive && maxVal < 0 && maxVal != Gauge.INDEFINITE) { throw new IllegalArgumentException(
				"invalid maxValue for non interactive mode"); }
		if (maxVal > 0 && initVal < 0) {
			initVal = 0;
		}
		if (maxVal > 0 && initVal > maxVal) {
			initVal = maxVal;
		}
		if (!interactive
				&& maxVal == Gauge.INDEFINITE
				&& !(initVal == Gauge.CONTINUOUS_IDLE
						|| initVal == Gauge.CONTINUOUS_RUNNING
						|| initVal == Gauge.INCREMENTAL_IDLE || initVal == Gauge.INCREMENTAL_UPDATING)) { throw new IllegalArgumentException(
				"invalid initValue for non interactive mode"); }
		modifiable = interactive;
		maxValue = maxVal;
		if (maxValue == Gauge.INDEFINITE) {
			value = 0;
			behaviour = initVal;
		} else {
			value = initVal;
		}

		if (!modifiable && behaviour == Gauge.CONTINUOUS_RUNNING) {
			ticker = new Ticker();
			UICanvas.getTimer().scheduleAtFixedRate(ticker, 1000, 1000);
		}
		setFocusable(modifiable);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.yup.ui.UIItem#paint(javax.microedition.lcdui.Graphics, int, int)
	 */
	protected void paint(Graphics g, int w, int h) {
		/* get gfx state */
		int oc = g.getColor();
		Font of = g.getFont();

		/* clear area */
		int tempBgColor = selected ? UIConfig.header_bg
				: (getBg_color() >= 0 ? getBg_color() : UIConfig.bg_color);
		g.setColor(tempBgColor);
		g.fillRect(0, 0, w, h);

		/* draw title (XXX: should clip or put ...) */
		g.setColor(selected ? UIConfig.header_fg : UIConfig.fg_color);
		g.setFont(UIConfig.gauge_body);
		g.drawString(label, 1, 1, Graphics.LEFT | Graphics.TOP);

		/* border around progress (black) */
		g.setColor(0x00000000);
		g.drawRect(1, 2 + UIConfig.gauge_body.getHeight(), w - 3, 16);

		int gw = 0;

		if (maxValue != Gauge.INDEFINITE) {
			gw = value * (w - 6) / maxValue;
		} else if (behaviour == Gauge.CONTINUOUS_RUNNING
				|| behaviour == Gauge.INCREMENTAL_UPDATING) {
			/* value is percentual to width */
			gw = value * (w - 6) / 100;
		} else {
			gw = 1;
		}
		if (gw == 0) {
			gw = 1;
		}

		/* fill rect */
		int blockHeight = 4 + UIConfig.gauge_body.getHeight();
		int blockColor = (!selected) ? UIConfig.header_bg
				: (getBg_color() >= 0 ? getBg_color() : UIConfig.bg_color);
		g.setColor(blockColor);
		g.fillRect(3, blockHeight, gw, 13);
		g.setColor(tempBgColor);
		for (int i = 3; i < gw; i += ((w - 6) / 10)) {
			g.drawLine(3 + i, blockHeight, 3 + i, blockHeight + 13);
		}

		/* restore state */
		g.setColor(oc);
		g.setFont(of);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.yup.ui.UIItem#keyPressed(int)
	 */
	public boolean keyPressed(int key) {
		boolean keepFocus = false;
		if (!modifiable || maxValue == Gauge.INDEFINITE) { return keepFocus; }

		switch (UICanvas.getInstance().getGameAction(key)) {
			case Canvas.LEFT:
				keepFocus = true;
				if (value > 0) {
					value--;
				}
				dirty = true;
				askRepaint();
				break;
			case Canvas.RIGHT:
				keepFocus = true;
				if (value < maxValue) {
					value++;
				}
				dirty = true;
				askRepaint();
				break;
		}
		return keepFocus;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.yup.ui.UIItem#getHeight(javax.microedition.lcdui.Graphics)
	 */
	public int getHeight(Graphics g) {
		Font fnt = UIConfig.gauge_body;
		/* text row + border(s) + space(s) + gauge */
		int mh = fnt.getHeight();
		mh += 20;
		return mh;
	}

	/**
	 * Changes the label shown
	 * 
	 * @param _label
	 *            the new label
	 */
	public void setLabel(String _label) {
		label = _label;
		dirty = true;
		askRepaint();
	}

	/**
	 * @return the label shown to the user
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Changes the current value
	 * 
	 * @param newVal
	 *            the new value
	 */
	public void setValue(int newVal) {
		if (!modifiable
				&& maxValue == Gauge.INDEFINITE
				&& (newVal != Gauge.CONTINUOUS_IDLE
						|| newVal != Gauge.CONTINUOUS_RUNNING
						|| newVal != Gauge.INCREMENTAL_IDLE || newVal != Gauge.INCREMENTAL_UPDATING)) { throw new IllegalArgumentException(
				"invalid value for non interactive mode"); }
		if (modifiable && newVal < 0) {
			newVal = 0;
		}
		if (modifiable && newVal > maxValue) {
			newVal = maxValue;
		}

		if (maxValue == Gauge.INDEFINITE
				&& behaviour != Gauge.INCREMENTAL_UPDATING) {
			/* don't move */
			return;
		} else if (maxValue == Gauge.INDEFINITE
				&& behaviour == Gauge.INCREMENTAL_UPDATING) {
			value += 5;
			if (value >= 100) {
				value = 0;
			}
		}

		value = newVal;
		dirty = true;
		askRepaint();
	}

	/**
	 * @return the current value
	 */
	public int getValue() {
		return value;
	}

	/**
	 * @return if this Gauge is modificable (
	 */
	public boolean isInteractive() {
		return modifiable;
	}

	public void cancel() {
		this.ticker.cancel();
	}

	/**
	 * @return the current maximum value
	 */
	public int getMaxValue() {
		return maxValue;
	}

	/**
	 * Task used to "tick" the clock on a CONTINUOUS_RUNNING Gauge.
	 */
	private class Ticker extends TimerTask {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.TimerTask#run()
		 */
		public void run() {
			if (behaviour == Gauge.CONTINUOUS_RUNNING) {
				/* ticks in blocks of 10% */
				value += 10;
				if (value > 100) {
					value = 10;
				}
				dirty = true;
				askRepaint();
			}
		}
	}

}
