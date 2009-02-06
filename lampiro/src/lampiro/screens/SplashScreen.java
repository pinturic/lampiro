/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: SplashScreen.java 1176 2009-02-06 16:53:35Z luca $
*/

package lampiro.screens;

import it.yup.ui.UICanvas;
import it.yup.ui.UIConfig;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UILayout;
import it.yup.ui.UIMenu;
import it.yup.ui.UIScreen;
import it.yup.ui.UITextField;
import it.yup.ui.UIVLayout;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.util.Utils;
import it.yup.xmpp.Config;

import java.util.TimerTask;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextField;

public class SplashScreen extends UIScreen {

	private static ResourceManager rm = ResourceManager.getManager("common",
	"en");
	
	private UIMenu helpMenu; 
	//private UIButton close;
	
	public SplashScreen() {
		try {
			setTitle("Lampiro");
			UIVLayout uvl = new UIVLayout(4, -1);

			UILabel dummyLabel = new UILabel("");
			uvl.insert(dummyLabel, 0, 50, UILayout.CONSTRAINT_PERCENTUAL);

			Image logo = Image.createImage("/icons/lampiro_icon.png");
			UILabel up = new UILabel(logo);
			up.setAnchorPoint(Graphics.HCENTER | Graphics.VCENTER);
			uvl.insert(up, 1, logo.getHeight(), UILayout.CONSTRAINT_PIXELS);

			UILabel ul = new UILabel("Loading Lampiro...");
			ul.setAnchorPoint(Graphics.HCENTER | Graphics.VCENTER);
			uvl.insert(ul, 2, UIConfig.font_body.getHeight(),
						UILayout.CONSTRAINT_PIXELS);

			uvl.insert(dummyLabel, 3, 50, UILayout.CONSTRAINT_PERCENTUAL);

			append(uvl);

		} catch (Exception ex) {
		}

		Utils.tasks.schedule(new TimerTask() {
			public void run() {
				checkKeys();
			}
		}, 3000);
	}
	
	public boolean keyPressed(int kc) {
		if (helpMenu == null)
			return super.keyPressed(kc);
		
		int ga = UICanvas.getInstance().getGameAction(kc);
		switch (ga) {
			case Canvas.UP:
			case Canvas.DOWN:
			case Canvas.LEFT:
			case Canvas.RIGHT:
				return super.keyPressed(kc);
			case Canvas.FIRE:
				this.removePopup(this.helpMenu);
				this.helpMenu = null;
				UICanvas.getInstance().open(RegisterScreen.getInstance(), true);
				UICanvas.getInstance().close(SplashScreen.this);
				return true;
		}
		return super.keyPressed(kc);
	}
	

	private void checkKeys() {
		int q;
		String keys = Config.getInstance().getProperty(Config.CANVAS_KEYS);
		if (keys != null && (q = keys.indexOf(',')) != -1) {
			int l = Integer.parseInt(keys.substring(0, q));
			int r = Integer.parseInt(keys.substring(q + 1));
			UICanvas.setMenuKeys(l, r);
			UICanvas.getInstance().open(RegisterScreen.getInstance(), true);
			UICanvas.getInstance().close(SplashScreen.this);
		}
		else
		{
			// save actual configuration
			//SplashScreen.this.close = new UIButton(rm.getString(ResourceIDs.STR_CLOSE));
			keys = UICanvas.MENU_LEFT + "," + UICanvas.MENU_RIGHT;
			Config.getInstance().setProperty(Config.CANVAS_KEYS, keys);
			Config.getInstance().saveToStorage();
			
			String help = rm.getString(ResourceIDs.STR_KEY_HELP);
			help = help.replace('<', '\n');

			UITextField helpField = new UITextField("", help, help.length(),
					TextField.UNEDITABLE);
			helpField.setWrappable(true);
			helpField.setAutoUnexpand(false);
			//helpField.unExpand();
			helpMenu = UIMenu.easyMenu(rm.getString(ResourceIDs.STR_HELP), 1,
					20, UICanvas.getInstance().getWidth() - 2, helpField);
			helpMenu.selectMenuString = "";
			((UIItem)helpMenu.getItemList().elementAt(0)).setFocusable(true);
			helpMenu.setSelectedIndex(1);
			helpMenu.cancelMenuString = "";
			//UIHLayout uhl = UIHLayout.easyCenterLayout(close, 80);
			//helpMenu.append(uhl);
			this.addPopup(helpMenu);
			this.askRepaint();
			helpField.expand();
		}
	}
	
	public void menuAction(UIMenu menu, UIItem c) {
		if (menu == this.helpMenu) {
			UICanvas.getInstance().open(RegisterScreen.getInstance(), true);
			UICanvas.getInstance().close(SplashScreen.this);
		}
	}
}
