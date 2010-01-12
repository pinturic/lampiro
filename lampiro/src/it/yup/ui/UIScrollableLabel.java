/**
 * 
 */
package it.yup.ui;

//#mdebug 
//@import it.yup.util.Logger;
// #enddebug

import java.util.TimerTask;
import java.util.Vector;

import javax.microedition.lcdui.Font;

/**
 * @author luca
 *
 */
public class UIScrollableLabel extends UILabel {

	private int lines;
	private Vector hiddenLines = new Vector();
	/** the task used to animate the gauge */
	private Ticker ticker=new Ticker();

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
			UICanvas.lock();
			try {
				if (hiddenLines.size() > 0) {
					/* ticks in blocks of 10% */
					Object o = hiddenLines.elementAt(0);
					hiddenLines.removeElementAt(0);
					textLines.addElement(o);
					o = textLines.elementAt(0);
					textLines.removeElementAt(0);
					hiddenLines.addElement(o);
					dirty = true;
					// #mdebug
//@					Logger.log("Printed: "+o);
					// #enddebug
					askRepaint();
				}
			} catch (Exception e) {
				// #mdebug 
//@				e.printStackTrace();
//@				Logger.log(e.getClass().getName());
				// #enddebug
			}
			UICanvas.unlock();
		}
	}

	public void start() {
		this.ticker.cancel();
		ticker = new Ticker();
		UICanvas.getTimer().scheduleAtFixedRate(ticker, 2000, 2000);
	}

	/**
	 * @param text
	 */
	public UIScrollableLabel(String text, int lines) {
		super(text);
		this.setWrappable(true, UICanvas.getInstance().getWidth() - 10);
		this.lines = lines;
	}

	public void computeTextLines(Font usedFont, int w) {
		super.computeTextLines(usedFont, w);
		this.hiddenLines.removeAllElements();
		while (this.textLines.size() > lines) {
			this.hiddenLines.addElement(this.textLines.elementAt(lines));
			this.textLines.removeElementAt(lines);
		}
	}
	
	public void cancel() {
		this.ticker.cancel();
	}

}
