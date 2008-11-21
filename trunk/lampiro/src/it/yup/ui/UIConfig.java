/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: UIConfig.java 911 2008-10-16 20:46:27Z luca $
*/

package it.yup.ui;

import javax.microedition.lcdui.Font;

public class UIConfig {

	/** color constants */
	public static int bg_color = 0xCBDBE3;
	public static int fg_color = 0x000000;
	public static int header_bg = 0x5590AD;
	public static int header_fg = 0xDDE7EC;
	public static int scrollbar_bg = 0x0024ff;
	public static int scrollbar_fg = 0x00adff;

	/** the width of the scrollBar */
	public static int scrollbarWidth = 7;

	/** the title font */
	public static Font font_title = Font.getFont(Font.FACE_PROPORTIONAL,
			Font.STYLE_BOLD, Font.SIZE_MEDIUM);

	public static Font gauge_body = Font.getFont(Font.FACE_PROPORTIONAL,
			Font.STYLE_BOLD, Font.SIZE_SMALL);

	public static Font font_body = Font.getFont(Font.FACE_PROPORTIONAL,
			Font.STYLE_PLAIN, Font.SIZE_MEDIUM);

	public static Font small_font = Font.getFont(Font.FACE_PROPORTIONAL,
			Font.STYLE_PLAIN, Font.SIZE_SMALL);

}
