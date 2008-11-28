/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: UIEmoLabel.java 846 2008-09-11 12:20:05Z luca $
*/

/**
 * 
 */
package it.yup.ui;

import java.util.Hashtable;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * @author luca
 * 
 */
public class UIEmoLabel extends UILabel {

	private static Hashtable emos;
	private static String[] codes;

	/**
	 * @param text
	 */
	public UIEmoLabel(String text) {
		super(text);
	}

	static {
		initialize();
	}

	private static void initialize() {
		emos = new Hashtable();
		// the number of emoticons
		int emoNumber = 16;
		codes = new String[] { /*
		 * these are the possible emos the next lines
		 * are only other codes for these
		 */
		":-S", "B-)", ":D", ":O", "O:)", ">:)", "|-)", ";-)", ":-/", ":-@", ":-&", ":'(", ":(", ":)", ":*", ":P",
		/* additional codes for the same emos (starting at emonuber index) */
		";)" };
		for (int i = 0; i < emoNumber; i++) {
			emos.put(codes[i], UICanvas.getUIImage("/emo/" + i + ".png"));
		}
		// additional code (in case of multiple coding for the same emo)
		emos.put(codes[emoNumber], UICanvas.getUIImage("/emo/7.png"));
	}

	void paintTextLine(Graphics g, String textLine, int horizontalSpace,
			int verticalSpace) {
		Object[] emoTuple = this.findEmoTuple(textLine);
		if (emoTuple == null) {
			g.drawString(textLine, horizontalSpace, verticalSpace,
							Graphics.LEFT | Graphics.TOP);
		} else {
			int index = ((int[]) emoTuple[0])[0];
			String code = (String) emoTuple[1];
			Image img = (Image) UIEmoLabel.emos.get(code);
			String firstHalf = textLine.substring(0, index);
			String secondHalf = textLine.substring(index + code.length());
			g.drawString(firstHalf, horizontalSpace, verticalSpace,
							Graphics.LEFT | Graphics.TOP);
			int additionalSpace = 0;
			if (g.getFont().getHeight() > img.getHeight()) {
				additionalSpace = (g.getFont().getHeight() - img.getHeight()) / 2;
			}
			g.drawImage(img, horizontalSpace
					+ g.getFont().stringWidth(firstHalf), verticalSpace
					+ additionalSpace, Graphics.LEFT | Graphics.TOP);
			int originalX = g.getTranslateX();
			g.translate(horizontalSpace + g.getFont().stringWidth(firstHalf)
					+ img.getWidth() + 1, 0);
			paintTextLine(g, secondHalf, 0, verticalSpace);
			g.translate(originalX - g.getTranslateX(), 0);
		}
	}

	private Object[] findEmoTuple(String textLine) {
		int index = textLine.length() + 1;
		String code = null;
		for (int i = 0; i < UIEmoLabel.codes.length; i++) {
			int tempIndex = textLine.indexOf(UIEmoLabel.codes[i]);
			if (tempIndex >= 0 && tempIndex < index) {
				index = tempIndex;
				code = UIEmoLabel.codes[i];
			}
		}
		if (code != null) {
			Object[] retVal = new Object[2];
			retVal[0] = new int[] { index };
			retVal[1] = code;
			return retVal;
		}

		return null;
	}

	int getTextWidth(String textLine, Font font) {
		Object[] emoTuple = this.findEmoTuple(textLine);
		if (emoTuple == null) {
			return font.stringWidth(textLine);
		} else {
			int index = ((int[]) emoTuple[0])[0];
			String code = (String) emoTuple[1];
			Image img = (Image) UIEmoLabel.emos.get(code);
			String firstHalf = textLine.substring(0, index);
			String secondHalf = textLine.substring(index + code.length());
			return font.stringWidth(firstHalf) + img.getWidth() + 1
					+ getTextWidth(secondHalf, font);
		}
	}

}