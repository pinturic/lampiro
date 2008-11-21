/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: LampiroMidlet.java 934 2008-10-29 14:23:54Z luca $
*/

package lampiro;

// #mdebug
//@import it.yup.util.Logger;
//@import it.yup.util.MemoryLogConsumer;
//@import it.yup.util.StderrConsumer;
//@import it.yup.util.XMPPConsumer; 
// #enddebug

// #ifdef UI
//@import it.yup.ui.UICanvas;
//@import it.yup.ui.UIConfig;
//@import lampiro.screens.SplashScreen;
//@
// #endif
// #ifndef UI

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import it.yup.screens.SplashScreen;

// #endif

import it.yup.xmpp.Config;
import it.yup.xmpp.Contact;
import it.yup.xmpp.XMPPClient;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Font;
import javax.microedition.midlet.MIDlet;

/**
 * Lampiro Midlet.
 * 
 * XXX: Use ResourceMgr for the phone hold on message or move the hold-on logic
 * in XMPPClient (maybe better)
 */
public class LampiroMidlet extends MIDlet {

	/** The main display */
	public static Display disp;

	/** The midlet instance */
	public static LampiroMidlet _lampiro;

	private XMPPClient xmpp = null;

	/**
	 * information saved when the app is paused (i.e. a phone call or an SMS is
	 * received or the user switches to another application).
	 */
	private int last_availability = -1;
	private String last_status;

	/**
	 * Constructor
	 */
	public LampiroMidlet() {
		xmpp = XMPPClient.getInstance();
		// #mdebug
		//@		Logger.addConsumer(new StderrConsumer());
		//@		Logger.addConsumer(MemoryLogConsumer.getConsumer());
		//@		//XMPPConsumer xmppConsumer = XMPPConsumer.getConsumer();
		//@		//xmppConsumer.debugJid = "blutest@jabber.bluendo.com";
		//@		//Logger.addConsumer(xmppConsumer);
		// #enddebug
		_lampiro = this;
		// XXX: remove!
		LampiroMidlet.disp = Display.getDisplay(this);
		// #ifdef UI
//@		UICanvas.setDisplay(Display.getDisplay(this));
//@		UICanvas canvas = UICanvas.getInstance();
//@		UICanvas.display(null);
//@		String colorString = Config.getInstance()
//@				.getProperty(Config.COLOR, "0");
//@		int colorInt = colorString.toCharArray()[0] - '0';
//@		LampiroMidlet.changeColor(colorInt);
//@		String fontString = Config.getInstance().getProperty(Config.FONT_SIZE,
//@				"1");
//@		int fontInt = fontString.toCharArray()[0] - '0';
//@		LampiroMidlet.changeFont(fontInt);
//@		canvas.open(new SplashScreen(), true);
		// #endif
// #ifndef UI
				disp.setCurrent(new SplashScreen());
		// #endif

	}

	/**
	 * Starts the application or re-starts it after being placed in background.
	 */
	public void startApp() {

		if (last_availability >= 0) {
			xmpp.setPresence(last_availability, last_status);
			last_availability = -1;
		}
	}

	/**
	 * Closes the application.
	 * 
	 * @param unconditional
	 *            stop is forced
	 */
	protected void destroyApp(boolean unconditional) {
		xmpp.stopClient();
		Config.getInstance().saveToStorage();
		_lampiro = null;
	}

	/**
	 * Pauses the application placing it in background (i.e. due to a phone call
	 * or an SMS or the user switches to another application). The app saves the
	 * current Presence and sets it to a status indicating the user is not
	 * available.
	 */
	protected void pauseApp() {

		last_availability = xmpp.getMyContact().getAvailability();
		last_status = xmpp.getMyContact().getPresence().getStatus();

		xmpp.setPresence(Contact.AV_DND,
				"Phone hold on, please don't send messages");

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void exit() {
		if (_lampiro == null) { return; }
		LampiroMidlet m = _lampiro;
		m.destroyApp(false);
		m.notifyDestroyed();
	}

	// #ifdef UI
//@
//@	static public void changeFont(int fontIndex) {
//@		switch (fontIndex) {
//@			case 0:
//@				UIConfig.font_body = Font.getFont(Font.FACE_PROPORTIONAL,
//@						Font.STYLE_PLAIN, Font.SIZE_SMALL);
//@				break;
//@			case 1:
//@				UIConfig.font_body = Font.getFont(Font.FACE_PROPORTIONAL,
//@						Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
//@				break;
//@			case 2:
//@				UIConfig.font_body = Font.getFont(Font.FACE_PROPORTIONAL,
//@						Font.STYLE_PLAIN, Font.SIZE_LARGE);
//@				break;
//@
//@			default:
//@				break;
//@		}
//@	}
//@
//@	static public void changeColor(int colorIndex) {
//@		switch (colorIndex) {
//@			case 0:
//@				UIConfig.scrollbar_bg = 0x444444;
//@				UIConfig.scrollbar_fg = 0x13a0f7;
//@				UIConfig.header_bg = 0x2407db;
//@				UIConfig.bg_color = 0xddddff;
//@				break;
//@			case 1:
//@				UIConfig.scrollbar_bg = 0x444444;
//@				UIConfig.scrollbar_fg = 0x10d288;
//@				UIConfig.header_bg = 0x24982f;
//@				UIConfig.bg_color = 0xddffdd;
//@				break;
//@			case 2:
//@				UIConfig.scrollbar_bg = 0x444444;
//@				UIConfig.scrollbar_fg = 0xf7654e;
//@				UIConfig.header_bg = 0xdb0724;
//@				UIConfig.bg_color = 0xffdddd;
//@				break;
//@			default:
//@				break;
//@		}
//@	}
//@
	// #endif 
}
