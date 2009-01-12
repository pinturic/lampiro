/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: RegisterScreen.java 1102 2009-01-12 13:40:17Z luca $
*/

package lampiro.screens;

import it.yup.ui.UIButton;
import it.yup.ui.UICanvas;
import it.yup.ui.UICheckbox;
import it.yup.ui.UICombobox;
import it.yup.ui.UIGauge;
import it.yup.ui.UIHLayout;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UILayout;
import it.yup.ui.UIMenu;
import it.yup.ui.UIScreen;
import it.yup.ui.UITextField;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.util.Utils;
import it.yup.xmlstream.BasicXmlStream;
import it.yup.xmlstream.EventQuery;
import it.yup.xmlstream.EventQueryRegistration;
import it.yup.xmlstream.StreamEventListener;
import it.yup.xmpp.Config;
import it.yup.xmpp.XMPPClient;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.TextField;

import lampiro.LampiroMidlet;

public class RegisterScreen extends UIScreen implements StreamEventListener {

	private static ResourceManager rm = ResourceManager.getManager("common",
			"en");

	private UITextField tf_jid = new UITextField(rm
			.getString(ResourceIDs.STR_JABBER_ID), null, 128,
			TextField.EMAILADDR | TextField.NON_PREDICTIVE);

	private UITextField tf_pwd = new UITextField(rm
			.getString(ResourceIDs.STR_PASSWORD), null, 32, TextField.ANY
			| TextField.PASSWORD);

	private UIButton but_cancel = new UIButton(rm
			.getString(ResourceIDs.STR_STOP_LOGIN));

	private UITextField tf_email = new UITextField(rm
			.getString(ResourceIDs.STR_EMAIL_ADDRESS), null, 128,
			TextField.EMAILADDR);

	private UITextField tf_server = new UITextField(rm
			.getString(ResourceIDs.STR_SERVER_NAME), null, 32, TextField.ANY
			| TextField.NON_PREDICTIVE);

	private UICheckbox grp_new_account = new UICheckbox(rm
			.getString(ResourceIDs.STR_NEW_USER));

	private UICheckbox grp_advanced = new UICheckbox(rm
			.getString(ResourceIDs.STR_ADVANCED_OPTIONS));

	private UITextField resource = null;

	// #ifdef COMPRESSION
	//@					private UICheckbox cb_compression = new UICheckbox(rm
	//@							.getString(ResourceIDs.STR_ENABLE_COMPRESSION));
	// #endif

	// #ifdef TLS
	//@				private UICheckbox cb_TLS = new UICheckbox(rm
	//@						.getString(ResourceIDs.STR_ENABLE_TLS));
	// #endif

	private UICombobox grp_server = new UICombobox("Server type", false);

	/** Progress bar during login */
	private UIGauge progress_gauge = new UIGauge(rm
			.getString(ResourceIDs.STR_WAIT), false, Gauge.INDEFINITE,
			Gauge.CONTINUOUS_RUNNING);

	private UIButton btn_login = new UIButton(rm
			.getString(ResourceIDs.STR_LOGIN));

	private UIHLayout logLayout = new UIHLayout(3);

	private UILabel cmd_exit = new UILabel(rm.getString(ResourceIDs.STR_EXIT));

	private UILabel cmd_state = new UILabel(rm.getString(
			ResourceIDs.STR_CHANGE_STATUS).toUpperCase());

	private UITextField last_status = null;

	/*
	 * The subMenu deputed to open the status screen
	 */
	private UIMenu setStatus = new UIMenu("");

	// #mdebug
	//@	private UIButton cmd_debug = new UIButton("debug");
	// #enddebug

	/** true if we must register a new account */
	private boolean register = false;

	private EventQueryRegistration reg;

	private Config cfg = Config.getInstance();

	/** local copy of the jid server (for detecting changes) */
	private String jid_server = "";

	private UITextField hint = new UITextField(rm
			.getString(ResourceIDs.STR_NOTE), rm
			.getString(ResourceIDs.STR_LOGIN_HINT), 5000, TextField.UNEDITABLE);

	private static RegisterScreen _registerScreen = null;

	private RegisterScreen() {
		resource = new UITextField(rm.getString(ResourceIDs.STR_RESOURCE), cfg
				.getProperty(Config.YUP_RESOURCE, "Lampiro"), 50, TextField.ANY);
		_registerScreen = this;
		setTitle(rm.getString(ResourceIDs.STR_TITLE));

		tf_jid.setFocusable(true);
		tf_pwd.setFocusable(true);
		tf_server.setFocusable(true);
		tf_email.setFocusable(true);

		// Add options to the connecting group
		grp_server.append("automatic");
		grp_server.append("manual");

		// set the values from config
		if (cfg.getProperty(Config.USER) != null) {
			String tempUser = cfg.getProperty(Config.USER, "") + "@"
					+ cfg.getProperty(Config.SERVER, "");
			if (tempUser.compareTo("@") == 0) {
				tempUser = "@jabber.bluendo.com";
			} else
				grp_server.setSelectedIndex(1);
			tf_jid.setText(tempUser);
			jid_server = get_server(tf_jid.getText());
			tf_pwd.setText(cfg.getProperty(Config.PASSWORD, ""));
			tf_server.setText(cfg.getProperty(Config.CONNECTING_SERVER, ""));
			//#ifdef COMPRESSION
			//@															boolean enable_compression = Short.parseShort(cfg.getProperty(
			//@																	Config.COMPRESSION, "0")) == 1;
			//@															cb_compression.setChecked(enable_compression);
			//#endif
			//#ifdef TLS
			//@												boolean enable_TLS = Short.parseShort(cfg.getProperty(Config.TLS,
			//@														"0")) == 1;
			//@												cb_TLS.setChecked(enable_TLS);
			//#endif
			// append(btn_login);
		}

		UILabel dummyLabel = new UILabel("");
		logLayout.setGroup(false);
		logLayout.insert(dummyLabel, 0, 50, UILayout.CONSTRAINT_PERCENTUAL);
		logLayout.insert(dummyLabel, 2, 50, UILayout.CONSTRAINT_PERCENTUAL);
		logLayout.insert(btn_login, 1, 100, UILayout.CONSTRAINT_PIXELS);
		btn_login.setAnchorPoint(Graphics.HCENTER);
		setStatus.append(cmd_state);
		// #debug
		//@		this.append(cmd_debug);
	}

	/** Called to notify that the {@link UIScreen} has become visible */
	public void showNotify() {
		setStatusLabel();
		placeItems();
	}

	public void setStatusLabel() {
		String show = cfg.getProperty(Config.LAST_PRESENCE_SHOW, "");
		String msg = cfg.getProperty(Config.LAST_STATUS_MESSAGE, "");
		String statusText = "";
		statusText += (show.length() > 0 ? "Presence: " + show + "\n" : "");
		statusText += (msg.length() > 0 ? "Message: " + msg : "");
		if (statusText.length() > 0) {
			this.last_status = new UITextField("Last Status", statusText, 1000,
					TextField.UNEDITABLE);
			this.last_status.setWrappable(true);
			this.last_status.setSubmenu(setStatus);
		} else {
			this.last_status = null;
		}
	}

	public static RegisterScreen getInstance() {
		// first delete all the references to the old instance
		if (UICanvas.getInstance().getCurrentScreen() != _registerScreen) {
			UICanvas.getInstance().close(_registerScreen);
			_registerScreen = new RegisterScreen();
		}
		return _registerScreen;
	}

	/*
	 * Chooses which controls should be placed on screen 
	 * depending on user choices, system settings, stream error event
	 * or compile flags.
	 * It should be synch because it is called in many places by different threads.  
	 */
	synchronized private void placeItems() {
		this.setFreezed(true);
		removeAll();

		append(grp_new_account);
		append(tf_jid);
		append(tf_pwd);
		if (this.last_status != null) append(this.last_status);
		if (grp_new_account.isChecked()) {
			append(tf_email);
		}
		append(grp_advanced);
		checkLogin();

		if (grp_advanced.isChecked()) {
			//#ifdef COMPRESSION
			//@															append(this.cb_compression);
			//#endif
			//#ifdef TLS
			//@												append(this.cb_TLS);
			//#endif
			append(grp_server);
			if (grp_server.getSelectedIndex() == 1) {
				append(tf_server);
			}
		}
		// #debug
		//@		this.append(cmd_debug);
		this.setFreezed(false);
		this.askRepaint();
	}

	public void menuAction(UIMenu menu, UIItem c) {
		if (c == cmd_exit) {
			LampiroMidlet.exit();
		} else if (c == cmd_state) {
			StatusScreen ssc = new StatusScreen();
			UICanvas.getInstance().open(ssc, true);
		}
	}

	private void login() {

		removeAll();
		UILabel ul = new UILabel(rm.getString(ResourceIDs.STR_LOGGING_IN));
		ul.setAnchorPoint(Graphics.HCENTER | Graphics.TOP);
		append(ul);
		append(progress_gauge);
		UILabel dummyLabel = new UILabel("");
		// if compression is ebabled even TLS is
		hint.setWrappable(true);
		append(hint);
		UIHLayout uhl = new UIHLayout(3);
		uhl.setGroup(false);
		but_cancel.setAnchorPoint(Graphics.HCENTER);
		uhl.insert(dummyLabel, 0, 50, UILayout.CONSTRAINT_PERCENTUAL);
		uhl.insert(this.but_cancel, 1, 100, UILayout.CONSTRAINT_PIXELS);
		uhl.insert(dummyLabel, 2, 50, UILayout.CONSTRAINT_PERCENTUAL);
		append(uhl);

		new Thread() {
			public void run() {

				String jid = tf_jid.getText();
				int at_idx = jid.indexOf("@");
				String user = jid.substring(0, at_idx);
				String server = jid.substring(at_idx + 1);

				String cfg_user = cfg.getProperty(Config.USER);
				String cfg_server = cfg.getProperty(Config.SERVER);
				boolean clientInitialized = true;
				if ((cfg_user == null || !cfg_user.equals(user))
						|| (cfg_server == null || !cfg_server.equals(server))) {
					cfg.setProperty(Config.CLIENT_INITIALIZED, Config.FALSE);
					clientInitialized = false;
				}
				cfg.setProperty(Config.USER, user);
				cfg.setProperty(Config.SERVER, server);
				cfg.setProperty(Config.PASSWORD, tf_pwd.getText());
				cfg.setProperty(Config.EMAIL, tf_email.getText());
				// #ifdef COMPRESSION
				//@																				String enableCompression = "0";
				//@																				enableCompression = (cb_compression.isChecked() ? 1 : 0) + "";
				//@																				cfg.setProperty(Config.COMPRESSION, enableCompression);
				// #endif
				// #ifdef TLS
				//@																String enableTlS = "0";
				//@																enableTlS = (cb_TLS.isChecked() ? 1 : 0) + "";
				//@																cfg.setProperty(Config.TLS, enableTlS);
				// #endif

				if (grp_server.getSelectedIndex() == 0) {
					if (clientInitialized == false) cfg.setProperty(
							Config.CONNECTING_SERVER, srvQuery());
				} else {
					cfg.setProperty(Config.CONNECTING_SERVER, tf_server
							.getText());
				}

				cfg.saveToStorage();

				// Get the XMPP client
				XMPPClient xmpp = XMPPClient.getInstance();
				//#ifdef COMPRESSION
				//@																				xmpp.addCompression = cb_compression.isChecked();
				//#endif
				//#ifdef TLS
				//@																xmpp.addTLS = cb_TLS.isChecked();
				//#endif
				xmpp.createStream(register);

				EventQuery qAuth = new EventQuery(EventQuery.ANY_EVENT, null,
						null);
				reg = xmpp.registerListener(qAuth, RegisterScreen.this);
				xmpp.openStream();
			}
		}.start();
	}

	public void gotStreamEvent(String event, Object source) {
		XMPPClient client = XMPPClient.getInstance();
		if (BasicXmlStream.STREAM_ERROR.equals(event)
				|| BasicXmlStream.CONNECTION_FAILED.equals(event)
				|| BasicXmlStream.REGISTRATION_FAILED.equals(event)
				|| BasicXmlStream.CONNECTION_LOST.equals(event)) {

			reg.remove();
			try {
				client.closeStream();
			} catch (Exception e) {
				// #mdebug
				//@				System.out.println(e);
				// #enddebug
			}

			String description = null;
			if (BasicXmlStream.CONNECTION_FAILED.equals(event)) {
				description = "Connection failed";
			} else if (BasicXmlStream.CONNECTION_LOST.equals(event)) {
				description = "Connection lost";
			} else if (BasicXmlStream.REGISTRATION_FAILED.equals(event)) {
				description = rm.getString(ResourceIDs.STR_REG_UNALLOWED);
			} else {
				description = (String) source;
			}
			if (source != null && source.equals("conflict")) {
				description = rm.getString(ResourceIDs.STR_ALREADY_EXIST);
			}

			//UITextField error = new UITextField()

			UICanvas.showAlert(AlertType.ERROR, register ? rm
					.getString(ResourceIDs.STR_REGFAIL_TITLE) : rm
					.getString(ResourceIDs.STR_LOGFAIL_TITLE), (register ? (rm
					.getString(ResourceIDs.STR_REGFAIL_DESC)) : (rm
					.getString(ResourceIDs.STR_LOGFAIL_DESC)))
					+ description);
			placeItems();

		} else if (BasicXmlStream.STREAM_INITIALIZED.equals(event)) {
			reg.remove();
			Config cfg = Config.getInstance();
			client.stream_authenticated(Config.FALSE.equals(cfg
					.getProperty(Config.CLIENT_INITIALIZED)));
			this.progress_gauge.cancel();
			/*
			 * non sarebbe pi� logico qui aggiungere un one-time packet listener
			 * sull IQ di roster per fare poi il passaggio di schermo? sarebbe
			 * anche carino fare in modo di mettere un qualche progress che
			 * indichi a che punto si � arrivati (UIProgressBar...)
			 */
		}
	}

	public void itemAction(UIItem item) {

		if (item == grp_new_account) {
			register = grp_new_account.isChecked();
			if (register) {
				insert(indexOf(tf_pwd) + 1, tf_email);
			} else {
				remove(tf_email);
			}
			// #mdebug
			//@		} else if (item == cmd_debug) {
			//@			DebugScreen debugScreen = new DebugScreen();
			//@			UICanvas.getInstance().open(debugScreen, true);
			// #enddebug

		} else if (item == but_cancel) {
			try {
				XMPPClient.getInstance().closeStream();
			} catch (Exception e) {
				// during closing connection
				// exceptions from the transport can be generated
				//#mdebug
				//@				System.out.println(e);
				// #enddebug     
			}
			placeItems();
		} else if (item == grp_server) {
			if (grp_server.getSelectedIndex() == 1) {
				String jid = tf_jid.getText();
				tf_server.setText(get_server(jid) + ":5222");
				if (this.getItemList().contains(tf_server) == false) {
					append(tf_server);
				}
			} else {
				remove(tf_server);
			}
			return;
		} else if (item == grp_advanced) {
			if (grp_advanced.isChecked()) {
				this.setFreezed(true);
				//#ifdef COMPRESSION
				//@																				append(this.cb_compression);
				//#endif
				//#ifdef TLS
				//@																append(this.cb_TLS);
				//#endif
				append(resource);

				append(grp_server);
				if (grp_server.getSelectedIndex() == 1) {
					append(tf_server);
				}
				this.setFreezed(false);
				this.askRepaint();
			} else {
				this.setFreezed(true);
				remove(grp_server);
				remove(tf_server);
				//#ifdef COMPRESSION
				//@																				remove(this.cb_compression);
				//#endif
				//#ifdef TLS
				//@																remove(this.cb_TLS);
				//#endif
				remove(resource);
				this.setFreezed(false);
				this.askRepaint();
			}
			return;
		} else if (item == tf_jid || item == tf_pwd) {
			if (grp_server.getSelectedIndex() == 1) {
				String jsvr = get_server(tf_jid.getText());
				if (!jid_server.equals(jsvr)) {
					jid_server = jsvr;
					grp_server.setSelectedIndex(0);
					remove(tf_server);
				}
			}
			checkLogin();
		} else if (item == btn_login) {
			String resourceString = this.resource.getText();
			cfg.setProperty(Config.YUP_RESOURCE, resourceString);
			cfg.saveToStorage();
			login();
			return;
		} else if (item == this.last_status) {
			menuAction(setStatus, cmd_state);
		} else if (item == this.hint) { return; }

		// check if we must enable / disable
		checkLogin();
	}

	/**
	 * check if we must enable the login
	 */
	private void checkLogin() {

		String items[] = new String[4];
		boolean checkmail[] = new boolean[] { true, false, true, false };

		items[0] = tf_jid.getText();
		items[1] = tf_pwd.getText();

		if (register) {
			items[2] = tf_email.getText();
		}
		if (grp_server.getSelectedIndex() == 1) {
			items[3] = tf_server.getText();
		}

		boolean complete = true;
		for (int i = 0; i < items.length; i++) {
			if (items[i] == null) {
				continue;
			}
			String s = items[i];
			if (s.length() == 0) {
				complete = false;
				break;
			} else if (checkmail[i] && !Utils.is_email(s)) {
				complete = false;
				break;
			}
		}

		int idx = indexOf(this.logLayout);
		if (complete && idx == -1) {
			insert(indexOf(grp_advanced), logLayout);
		} else if (!complete && idx != -1) {
			remove(idx);
		}
	}

	private String srvQuery() {

		String jid = RegisterScreen.this.tf_jid.getText();
		String host = jid.substring(jid.indexOf("@") + 1);
		try {
			String domain = host;
			HttpConnection conn = (HttpConnection) Connector
					.open(Config.SRV_QUERY_PATH + domain);
			InputStream is = conn.openInputStream();
			int b = -1;
			StringBuffer buffer = new StringBuffer();
			while ((b = is.read()) != -1) {
				buffer.append((char) b);
			}
			String result = buffer.toString();
			if ("_:-1".equals(result)) {
				result = host + ":5222";
			}
			return result;
		} catch (IOException e) {
			return host + ":5222";
		}

	}

	private String get_server(String jid) {
		int server_idx = jid.indexOf("@");
		if (server_idx >= 0) {
			return jid.substring(server_idx + 1);
		} else {
			return "";
		}
	}
}
