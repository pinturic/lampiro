/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: AboutScreen.java 846 2008-09-11 12:20:05Z luca $
*/

package lampiro.screens;

import it.yup.ui.UICanvas;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UIMenu;
import it.yup.ui.UIScreen;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class AboutScreen extends UIScreen {

	Image logo;

	private static String[] lines = { "Mobile Messaging", "", "(c) 2007-2008 Bluendo srl", "http://www.bluendo.com", "", "version 1.0", };

	private static UILabel cmd_ok = new UILabel("OK");

	public AboutScreen() {
		setTitle("ABOUT");
		try {
			logo = Image.createImage("/icons/lampiro_icon.png");
		} catch (Exception ex) {
		}
		UILabel uimg = new UILabel(logo);
		uimg.setAnchorPoint(Graphics.HCENTER | Graphics.VCENTER);
		append(uimg);
		for (int i = 0; i < lines.length; i++) {
			UILabel ul = new UILabel(lines[i]);
			ul.setAnchorPoint(Graphics.HCENTER | Graphics.VCENTER);
			append(ul);
		}
		setMenu(new UIMenu(""));
		UIMenu menu = getMenu();
		menu.append(cmd_ok);
	}

	public void menuAction(UIMenu menu, UIItem cmd) {
		if (cmd == cmd_ok) {
			UICanvas.getInstance().show(RosterScreen.getInstance());
			UICanvas.getInstance().close(this);
		}
	}

}
