/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: SplashScreen.java 846 2008-09-11 12:20:05Z luca $
*/

package lampiro.screens;

import it.yup.ui.UICanvas;
import it.yup.ui.UIConfig;
import it.yup.ui.UILabel;
import it.yup.ui.UILayout;
import it.yup.ui.UIMenu;
import it.yup.ui.UIScreen;
import it.yup.ui.UIVLayout;
import it.yup.util.Utils;
import it.yup.xmpp.Config;

import java.io.IOException;
import java.util.TimerTask;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class SplashScreen extends UIScreen {

	private static final byte STATE_SPLASH = 0;
	private static final byte STATE_LEFT_WAIT = 1;
	private static final byte STATE_LEFT_OK = 2;
	private static final byte STATE_RIGHT_WAIT = 3;
	private static final byte STATE_RIGHT_OK = 4;
	private static final byte STATE_WAIT_DONE = 5;

	private UILabel txt = null;
	private UILabel err = null;

	private byte status = STATE_SPLASH;
	private int lkey;
	private int rkey;

	private UIMenu confMenu = new UIMenu("Configuring keys.");
	private Image downArrow = null;
	private int oldFg = UIConfig.header_fg;

	public SplashScreen() {
		// to simulate an empty footer!!!
		UIConfig.header_fg = UIConfig.header_bg;
		//
		txt = new UILabel("");
		err = new UILabel("");
		try {
			downArrow = Image.createImage("/icons/downarrow.png");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		txt.setWrappable(true, UICanvas.getInstance().getWidth() - 10);
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

	void checkKeys() {
		int q;
		String keys = Config.getInstance().getProperty(Config.CANVAS_KEYS);
		if (keys != null && (q = keys.indexOf(',')) != -1) {
			int l = Integer.parseInt(keys.substring(0, q));
			int r = Integer.parseInt(keys.substring(q + 1));
			UICanvas.setMenuKeys(l, r);
			UIConfig.header_fg = this.oldFg;
			UICanvas.getInstance().open(RegisterScreen.getInstance(), true);
			UICanvas.getInstance().close(SplashScreen.this);
		} else {
			status = STATE_LEFT_WAIT;
			removeAll();
			setTitle("Configuration");
			txt.setText("Please press the button below the left menu.");
			append(txt);
			append(err);
			askRepaint();

			UILabel confLabel = new UILabel(this.downArrow, "Press left key");
			this.confMenu.append(confLabel);
			this.confMenu.setAbsoluteX(2);
			this.confMenu.setWidth((UICanvas.getInstance().getWidth() * 2) / 3);
			int screenHeight = UICanvas.getInstance().getClipHeight();
			int menuHeight = this.confMenu.getHeight(getGraphics());
			int footerHeight = this.footer.getHeight(getGraphics());
			this.confMenu.setAbsoluteY(screenHeight - menuHeight - footerHeight
					- 2);
			this.addPopup(confMenu);
		}
	}

	public boolean keyPressed(int key) {
		switch (status) {
		case STATE_LEFT_WAIT:
			lkey = key;
			txt.setText("Please confirm the left key.");
			err.setText(" ");
			status = STATE_LEFT_OK;
			this.setDirty(true);
			askRepaint();
			break;
		case STATE_LEFT_OK:
			if (lkey == key) {
				status = STATE_RIGHT_WAIT;
				txt.setText("Please press the button below the right menu.");
				err.setText("");

				UILabel confLabel = new UILabel(this.downArrow,
						"Press right key");
				confLabel.setAnchorPoint(Graphics.RIGHT);
				confLabel.setFlip(true);
				this.confMenu.remove(1);
				this.confMenu.append(confLabel);
				this.confMenu.setAbsoluteX(this.getWidth()
						- confMenu.getWidth());
				this.setDirty(true);
				this.askRepaint();

			} else {
				err.setText("Please press the same key as before!");
				txt.setText("Please press the button below the left menu.");
				askRepaint();

				status = STATE_LEFT_WAIT;
			}
			this.setDirty(true);
			askRepaint();
			break;
		case STATE_RIGHT_WAIT:
			rkey = key;
			txt.setText("Please confirm the right key.");
			err.setText(" ");
			status = STATE_RIGHT_OK;
			this.setDirty(true);
			askRepaint();
			break;
		case STATE_RIGHT_OK:
			if (rkey == key) {
				status = STATE_WAIT_DONE;
				txt.setText("Thank you, press a key to proceed.");
				err.setText(" ");
				this.removePopup(confMenu);
			} else {
				status = STATE_RIGHT_WAIT;
				txt.setText("Please press the button below the right menu.");
				err.setText("Please press the same key as before!");
				askRepaint();
			}
			this.setDirty(true);
			askRepaint();
			break;
		case STATE_WAIT_DONE:
			String keys = lkey + "," + rkey;
			Config.getInstance().setProperty(Config.CANVAS_KEYS, keys);
			Config.getInstance().saveToStorage();
			UICanvas.setMenuKeys(lkey, rkey);
			UIConfig.header_fg = this.oldFg;
			UICanvas.getInstance().open(RegisterScreen.getInstance(), true);
			UICanvas.getInstance().close(SplashScreen.this);
			break;
		}
		return false;
	}
}
