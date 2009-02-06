/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: RosterScreen.java 1176 2009-02-06 16:53:35Z luca $
*/

package lampiro.screens;

import it.yup.ui.UIButton;
import it.yup.ui.UICanvas;
import it.yup.ui.UIConfig;
import it.yup.ui.UIGauge;
import it.yup.ui.UIHLayout;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UILayout;
import it.yup.ui.UIMenu;
import it.yup.ui.UIPanel;
import it.yup.ui.UIScreen;
import it.yup.ui.UISeparator;
import it.yup.ui.UITextField;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.util.Utils;
import it.yup.xmlstream.Element;
import it.yup.xmlstream.EventQuery;
import it.yup.xmlstream.PacketListener;
import it.yup.xmpp.Config;
import it.yup.xmpp.Contact;
import it.yup.xmpp.MUC;
import it.yup.xmpp.Task;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.DataForm;
import it.yup.xmpp.packets.IQResultListener;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Message;
import it.yup.xmpp.packets.Presence;
import it.yup.xmpp.packets.Stanza;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextField;

import lampiro.LampiroMidlet;
import lampiro.screens.DataFormScreen.DataFormListener;

//#mdebug
//@import it.yup.util.Logger;
//@
//#enddebug

public class RosterScreen extends UIScreen implements PacketListener {

	static final String IQ_REGISTER = "jabber:iq:register";

	/*
	 * The number of components that are recognized at the jabber server
	 */
	private Element components[] = null;

	/*
	 * The number of components that have answered to the info command
	 * 
	 */
	private int infoedComponents = 0;

	/*
	 * The MUC component jid
	 */
	private String mucJid = null;
	
	/*
	 * The server used to explore gateways 
	 */
	private String serverGateways = Contact.domain(XMPPClient.getInstance().my_jid);

	/*
	 * If true the offline contacts are shown.
	 */
	private boolean show_offlines = false;

	private static ResourceManager rm = ResourceManager.getManager("common",
			"en");

	private Vector hiddenContacts = new Vector();

	private UILabel cmd_send = new UILabel(rm
			.getString(ResourceIDs.STR_SEND_MESSAGE));
	private UILabel cmd_chat = new UILabel(rm.getString(ResourceIDs.STR_CHAT));
	private UILabel cmd_help = new UILabel(rm.getString(ResourceIDs.STR_HELP));
	// XXX info delayed
	// private Command cmd_info = new
	// Command(rm.getString(ResourceIDs.STR_EDIT_CONTACT), Command.SCREEN, 3);
	private UILabel cmd_state = new UILabel(rm
			.getString(ResourceIDs.STR_CHANGE_STATUS));
	private UILabel toggle_offline = new UILabel(rm
			.getString(ResourceIDs.STR_SHOW_OFFLINE));
	private UILabel gateways_discovery = new UILabel(rm
			.getString(ResourceIDs.STR_GATEWAY_DISCOVERY));

	private UILabel cmd_addc = new UILabel(rm
			.getString(ResourceIDs.STR_ADD_CONTACT));
	private UILabel cmd_delc = new UILabel(rm
			.getString(ResourceIDs.STR_DELETE_CONTACT));
	// XXX update delayed
	// private Command cmd_reload = new
	// Command(rm.getString(ResourceIDs.STR_RELOAD_CONTACT), Command.SCREEN, 6);
	private UILabel cmd_exit = new UILabel(rm.getString(ResourceIDs.STR_EXIT));
	// #mdebug
//@		private UILabel cmd_debug = new UILabel(rm.getString(ResourceIDs.STR_DEBUG));
//@		private UILabel cmd_ask_capabilities = new UILabel("Ask capabilities");
	// #enddebug
	private UILabel cmd_about = new UILabel(rm.getString(ResourceIDs.STR_ABOUT));
	private UILabel cmd_querycmd = new UILabel(rm
			.getString(ResourceIDs.STR_QUERYCMD));
	private UILabel cmd_listcmd = new UILabel(rm
			.getString(ResourceIDs.STR_LISTCMD));
	private UILabel cmd_options = new UILabel(rm
			.getString(ResourceIDs.STR_OPTIONS_SETUP));
	private UILabel cmd_tasks = new UILabel(rm
			.getString(ResourceIDs.STR_PENDINGTASK));
	private UILabel cmd_details = new UILabel(rm
			.getString(ResourceIDs.STR_SEE_DETAILS));
	private UILabel cmd_close_muc = new UILabel(rm
			.getString(ResourceIDs.STR_CLOSE_MUC));
	private UILabel cmd_mucs = new UILabel(rm
			.getString(ResourceIDs.STR_GROUP_CHAT));
	private UITextField muc_name_field = new UITextField("Group chat name", "",
			50, TextField.ANY);
	private UIButton muc_button = new UIButton(rm
			.getString(ResourceIDs.STR_SUBMIT));
	private UIButton refresh_gateways = new UIButton(rm
			.getString(ResourceIDs.STR_REFRESH));
	private UIButton acceptButton = null;
	private UIButton denyButton = null;
	private UIMenu groupInviteMenu = null;
	private UIMenu gatewaysMenu = null;
	private Hashtable gateways = new Hashtable();
	private Hashtable transPortHash = new Hashtable();

	/** the shown contacts */
	private Hashtable shown_contacts = new Hashtable(20);

	private Image img_msg;
	private Image img_cmd;
	private Image img_task;
	private static Hashtable chatScreenList = new Hashtable();

	/**
	 * The lookup table used to memorize letters for search pattern
	 */
	private char[][] itu_keys = { { ' ', '0' }, { '1' },
			{ 'a', 'b', 'c', 'à', '2' }, { 'd', 'e', 'f', 'è', 'é', '3' },
			{ 'g', 'h', 'i', 'ì', '4' }, { 'j', 'k', 'l', '5' },
			{ 'm', 'n', 'o', 'ò', '6' }, { 'p', 'q', 'r', 's', '7' },
			{ 't', 'u', 'v', 'ù', '8' }, { 'w', 'x', 'y', 'z', '9' } };
	/*
	 * the key used when filtering contacts
	 */
	private int sel_last_key = -1;

	/*
	 * the pattern used when filtering contacts
	 */

	private String sel_pattern = "";

	/*
	 * the time stamp of the last key press
	 */
	private long sel_last_ts = 0;

	/*
	 * The offset of the slected key in the research pattern
	 */
	private int sel_key_offset = 0;

	// #ifdef TIMING
	// @ private long paint_time = 0;
	// #endif

	/** fount used for the conversation */
	public static Font f_u;

	// drawing constants
	// /** true when some conversation has pending messages */
	// private boolean unread_messages = ;

	/** singleton */
	private static RosterScreen _instance;

	/*
	 * the contextual menu associated to a user
	 */
	UIMenu optionsMenu = new UIMenu("");
	
	private XMPPClient xmppClient= XMPPClient.getInstance();
	
	// The input text field used to explore a server for gateways
	private UITextField serverGatewayInput;

	// #ifdef SCREENSAVER
	// @ private long last_key_time;
	// @ private static long SCREENSAVER_DELAY = 10000;
	// @ private TimerTask screensaver_starter = null;
	// #endif

	class UIContact extends UIHLayout {

		protected Contact c;
		private UISeparator sep = new UISeparator(1);
		private UILabel statusLabel = new UILabel("");
		private UILabel contactLabel = new UILabel("");
		private UILabel statusText = new UILabel("");

		public UIContact(Contact c) {
			super(2);
			// the correct width for this img is set below !!!
			super.insert(statusLabel, 0, 0, UILayout.CONSTRAINT_PIXELS);
			super.insert(contactLabel, 1, 100, UILayout.CONSTRAINT_PERCENTUAL);
			this.c = c;
			sep.setBg_color(0x00CCCCCC);
			this.setFocusable(true);
			contactLabel.setFocusable(true);
			statusText.setFont(Font.getFont(Font.FACE_PROPORTIONAL,
					Font.STYLE_PLAIN, Font.SIZE_SMALL));
			statusText.setFg_color(0xAAAAAA);
			this.setGroup(false);
			this.screen = RosterScreen.this;
			this.updateContactData();
		}

		public void updateContactData() {
			String uname = c.getPrintableName();
			Image pimg = null;
			if (c instanceof MUC == false) pimg = xmppClient.getPresenceIcon(c
					.getAvailability());
			else {
				try {
					pimg = Image.createImage("/icons/muc.png");
				} catch (IOException e) {
					pimg = xmppClient.getPresenceIcon(c.getAvailability());
				}
			}
			if (pimg==null)
				pimg = xmppClient.getPresenceIcon(Contact.AV_UNAVAILABLE);
			// setup the status text label
			String status = null;
			Presence[] resources = c.getAllPresences();
			if (resources != null && resources.length > 0) {
				status = resources[0].getStatus();
			}

			this.contactLabel.setText(uname);
			this.statusText.setText(status != null ? status : "");
			this.statusLabel.setImg(pimg);
			statusLabel.setLayoutWidth(pimg.getWidth());

			this.setDirty(true);

			Image cimg = null;
			if (c.cmdlist != null) {
				cimg = img_cmd;
			}
			if (c.pending_tasks) {
				cimg = img_task;
			} else if (c.unread_msg) {
				cimg = img_msg;
			} else if (c.cmdlist != null) {
				cimg = img_cmd;
			}
			contactLabel.setImg(cimg);

		}

		public int getHeight(Graphics g) {
			int superHeight = super.getHeight(g);
			this.height = superHeight + sep.getHeight(g);
			if (this.statusText.getText().length() > 0) this.height += statusText
					.getHeight(g);
			return this.height;
		}

		protected void paint(Graphics g, int w, int h) {
			g.setColor(getBg_color() >= 0 ? getBg_color() : UIConfig.bg_color);
			int statusLabelWidth = statusLabel.getImg().getWidth();
			g.fillRect(0, 0, statusLabelWidth, h);
			super.paint(g, w, super.getHeight(g));
			if (this.statusText.getText().length() > 0) {
				g.translate(statusLabelWidth, super.getHeight(g));
				int statusTextHeight = statusText.getHeight(g);
				statusText.paint0(g, w, statusTextHeight);
				g.translate(-statusLabelWidth, statusTextHeight);
			} else {
				g.translate(0, super.getHeight(g));
			}
			sep.paint0(g, w, sep.getHeight(g));
		}

		public UIItem getSelectedItem() {
			// i want to return myself and not the selected label!
			return this;
		}
	}

	// #ifdef SCREENSAVER
	// @ class ScreenSaverStarter extends TimerTask {
	// @
	// @ public void run() {
	// @ if(isShown() && (System.currentTimeMillis()-
	// @ // last_key_time)>SCREENSAVER_DELAY) {
	// @ LampiroMidlet.disp.setCurrent(new ScreenSaver(RosterScreen.this));
	// @ }
	// @ }
	// @ }
	// #endif

	private RosterScreen() {
		setMenu(new UIMenu(""));
		f_u = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN,
				Font.SIZE_SMALL);

		try {
			img_msg = Image.createImage("/icons/message.png");
		} catch (IOException e) {
			img_msg = Image.createImage(16, 16);
		}

		try {
			img_cmd = Image.createImage("/icons/gear.png");
		} catch (IOException e) {
			img_cmd = Image.createImage(16, 16);
		}

		try {
			img_task = Image.createImage("/icons/task.png");
		} catch (IOException e) {
			img_task = Image.createImage(16, 16);
		}

		/*
		 * XXX: hack, create an item and select it, the item won't relinquish
		 * focus
		 */

		this.setFreezed(true);
		header = new UIHLayout(2);
		header.setGroup(false);
		header.setFocusable(false);
		connData = new UILabel("");
		presenceLabel = new UILabel(null, "");
		presenceLabel.setAnchorPoint(Graphics.RIGHT);
		header.insert(connData, 0, 100, UILayout.CONSTRAINT_PERCENTUAL);
		header.insert(presenceLabel, 1, 20, UILayout.CONSTRAINT_PERCENTUAL);
		this.append(header);
		UISeparator sep = new UISeparator(2);
		sep.setBg_color(0xCCCCCC);
		this.append(sep);
		rosterPanel = new UIPanel();
		rosterPanel.setMaxHeight(-1);
		this.append(rosterPanel);
		this.setSelectedIndex(2);

		this.setFreezed(false);
		this.setDirty(true);
		this.askRepaint();

		// registration to get notified of MUC invite
		EventQuery q = new EventQuery("message", null, null);
		EventQuery x = new EventQuery("x", new String[] { "xmlns" },
				new String[] { XMPPClient.NS_MUC_USER });
		q.child = x;
		EventQuery invite = new EventQuery("invite", null, null);
		x.child = invite;

		xmppClient.registerListener(q, this);

		// setting the
		optionsMenu.append(cmd_chat);
		optionsMenu.append(cmd_send);
		optionsMenu.append(cmd_delc);
		optionsMenu.append(cmd_details);
		optionsMenu.setWidth(this.getWidth() - 20);

		getIMGateways();

		// listen for all incoming messages with subject
		// this is used to set the topic for MUC
		// listen here and dispatch to the correct MUC
		/// XXX: something better ?
		q = new EventQuery("message", new String[] { Iq.ATT_TYPE },
				new String[] { "groupchat" });
		q.child = new EventQuery("subject", null, null);
		xmppClient.registerListener(q, this);
		this.rosterPanel.setSelectedIndex(0);
		// check first login
		Config cfg = Config.getInstance();
		if (Config.FALSE.equals(cfg.getProperty(Config.CLIENT_INITIALIZED))) {
			cfg.setProperty(Config.CLIENT_INITIALIZED, Config.TRUE);
			cfg.saveToStorage();
			String hintText = rm
					.getString(ResourceIDs.STR_GATEWAY_HINT)
					+ "<" + rm.getString(ResourceIDs.STR_SCARY_GMAIL);
			hintText = hintText.replace('<', '\n');
			UITextField gatewayHint = new UITextField("",hintText,hintText.length(),
					TextField.UNEDITABLE);
			gatewayHint.setWrappable(true);
			gatewayHint.setAutoUnexpand(false);
			int canvasWidth = UICanvas.getInstance().getWidth() - 20;
			UIMenu firstLogin = UIMenu.easyMenu(rm
					.getString(ResourceIDs.STR_INSTRUCTIONS), 10, 20,
					canvasWidth, gatewayHint);
			firstLogin.setSelectedIndex(1);
			firstLogin.cancelMenuString = "";
			firstLogin.selectMenuString = rm
					.getString(ResourceIDs.STR_CONTINUE).toUpperCase();
			UIHLayout gatewayLayout = new UIHLayout(5);
			Vector images = new Vector(5);
			images.addElement(UICanvas
					.getUIImage("/transport/msn.png"));
			images.addElement(UICanvas
					.getUIImage("/transport/icq.png"));
			images.addElement(UICanvas
					.getUIImage("/transport/aim.png"));
			images.addElement(UICanvas
					.getUIImage("/transport/yahoo.png"));
			images.addElement(UICanvas
					.getUIImage("/transport/transport.png"));
			Enumeration en = images.elements();
			int i =0;
			while (en.hasMoreElements()){
				UILabel ithLabel = new UILabel((Image)en.nextElement());
				ithLabel.setAnchorPoint(Graphics.HCENTER);
				gatewayLayout.insert(ithLabel, i, 25, UILayout.CONSTRAINT_PERCENTUAL);
				i++;
			}
			
			firstLogin.replace(0,gatewayLayout);
			this.addPopup(firstLogin);
			UICanvas.getInstance().open(this,true);
			this.askRepaint();
			gatewayHint.expand();
		}
	}

	private void updateHeader() {
		int bytes[] = XMPPClient.getTraffic();
		String byteTrans = rm.getString(ResourceIDs.STR_TRAFFIC) + ": "
				+ (bytes[0] + bytes[1]);
		if (byteTrans.compareTo(this.connData.getText()) != 0) {
			this.connData.setText(byteTrans);
		}
		if (sel_pattern.length() > 0) {
			this.connData.setText("sel: " + sel_pattern);
		}
		Image pimg = xmppClient.getPresenceIcon(xmppClient.getMyContact()
				.getAvailability());
		// contacts with unread messages are always at the top
		if (rosterPanel != null
				&& rosterPanel.getItems().size() > 0
				&& ((UIContact) rosterPanel.getItems().elementAt(0)).c.unread_msg) {
			pimg = img_msg;
		}
		this.presenceLabel.setImg(pimg);
	}

	private UIHLayout header = null;
	private UILabel connData = null;
	private UILabel presenceLabel = null;
	protected UIPanel rosterPanel = null;

	/**
	 * The contact that should be deleted at the user request
	 */
	private Contact delContact;

	/**
	 * The question asked whene deleting a contact
	 */
	private UILabel deleteQuestion;

	public static RosterScreen getInstance() {
		if (_instance == null) {
			_instance = new RosterScreen();
			// if a roster is seen no loginScreen is needed
			UICanvas.getInstance().close(RegisterScreen.getInstance());
		}
		return _instance;
	}

	protected void sizeChanged(int w, int h) {
		this.width = w;
		this.height = h;
		// askRepaint();
	}

	// #ifdef SCREENSAVER
	// @ protected void showNotify() {
	// @ last_key_time = System.currentTimeMillis();
	// @ if(screensaver_starter == null) {
	// @ screensaver_starter = new ScreenSaverStarter();
	// @ Utils.tasks.scheduleAtFixedRate(screensaver_starter, SCREENSAVER_DELAY,
	// @ // SCREENSAVER_DELAY);
	// @ }
	// @ }
	// @
	// @ protected void hideNotify() {
	// @ if(screensaver_starter != null) {
	// @ screensaver_starter.cancel();
	// @ screensaver_starter = null;
	// @ }
	// @ }
	// #endif

	public void hideNotify() {
		this.setFreezed(true);
		this.sel_pattern = "";

		try {
			UICanvas.lock();
			filterContacts(false);
		} finally {
			UICanvas.unlock();
		}
		this.setFreezed(false);
	}

	private void toggleContactMenu(UIContact c) {
		c.setSubmenu(this.optionsMenu);
	}

	private void toggleMenus() {
		// add or remove commands only if there is a selected user
		Contact c = getSelectedContact();
		UIMenu menu = getMenu();
		menu.clear();
		// #mdebug
//@				menu.append(cmd_debug);
		// #enddebug
		menu.append(cmd_addc);
		if (this.mucJid != null) menu.append(cmd_mucs);
		menu.append(cmd_state);
		menu.append(gateways_discovery);
		menu.append(toggle_offline);
		menu.append(cmd_options);
		menu.append(cmd_about);
		menu.append(cmd_help);
		menu.append(cmd_exit);

		if (c != null) {
			this.optionsMenu.remove(cmd_listcmd);
			this.optionsMenu.remove(cmd_querycmd);
			this.optionsMenu.remove(cmd_tasks);
			this.optionsMenu.remove(cmd_close_muc);
			
			// #mdebug
//@			this.optionsMenu.remove(cmd_ask_capabilities);
//@			this.optionsMenu.append(cmd_ask_capabilities);
			// #enddebug
			if (c.cmdlist != null) {
				optionsMenu.append(cmd_listcmd);
			} else {
				optionsMenu.append(cmd_querycmd);
			}
			if (c.pending_tasks) {
				optionsMenu.append(cmd_tasks);
			}
			if (c instanceof MUC) {
				optionsMenu.append(cmd_close_muc);
				optionsMenu.remove(cmd_delc);
			}
		}
	}

	/**
	 * Handle key events
	 * 
	 * @param kc
	 *            the pressed key
	 */
	public boolean keyPressed(int kc) {
		// #mdebug
//@				Logger.log("Roster screen keypressed :" + kc);
		// #enddebug
		if (this.popupList.size() == 0
				& this.getMenu().isOpenedState() == false) {
			switch (kc) {
				case Canvas.KEY_NUM0:
				case Canvas.KEY_NUM1:
				case Canvas.KEY_NUM2:
				case Canvas.KEY_NUM3:
				case Canvas.KEY_NUM4:
				case Canvas.KEY_NUM5:
				case Canvas.KEY_NUM6:
				case Canvas.KEY_NUM7:
				case Canvas.KEY_NUM8:
				case Canvas.KEY_NUM9:
					int key_num = kc - Canvas.KEY_NUM0;
					this.setFreezed(true);
					long t = System.currentTimeMillis();
					if ((key_num != sel_last_key) || t - sel_last_ts > 1000) {
						// new key
						sel_key_offset = 0;
						sel_last_key = key_num;
						sel_pattern = sel_pattern
								+ itu_keys[key_num][sel_key_offset];
						try {
							UICanvas.lock();
							filterContacts(true);
						} finally {
							UICanvas.unlock();
						}
					} else {
						// shifted key
						sel_key_offset += 1;
						if (sel_key_offset >= itu_keys[key_num].length) sel_key_offset = 0;
						sel_pattern = sel_pattern.substring(0, sel_pattern
								.length() - 1)
								+ itu_keys[key_num][sel_key_offset];
						try {
							UICanvas.lock();
							filterContacts(false);
						} finally {
							UICanvas.unlock();
						}
						this.rosterPanel.setDirty(true);
					}
					sel_last_ts = t;
					this.setFreezed(false);
					if (this.rosterPanel.getItems().size() > 0) {
						this.rosterPanel.setSelectedIndex(0);
					}
					this.askRepaint();
					return true;

			}

			int ga = UICanvas.getInstance().getGameAction(kc);
			switch (ga) {
				case Canvas.RIGHT: {
					/*
					 * Contact c = getSelectedContact(); if (c != null) {
					 * chatWithSelected(true); }
					 */
					showNextScreen(this);
					return true;
				}
				case Canvas.LEFT: {
					if (sel_pattern.length() > 0) {
						cutPattern();
						return true;
					}
					// // go to the top
					// if (this.rosterPanel.getItems().size() > 0) {
					// this.rosterPanel.setSelectedIndex(0);
					// }

					showPreviousScreen(this);
					return true;

				}

				default: {
					break;
				}
			}
			if (kc == UICanvas.MENU_CANCEL){
				if (sel_pattern.length() > 0) {
					cutPattern();
					return true;
				}
			}
		}
		return super.keyPressed(kc);
	}

	private void cutPattern() {
		this.setFreezed(true);
		sel_pattern = sel_pattern.substring(0, sel_pattern
				.length() - 1);
		try {
			UICanvas.lock();
			filterContacts(false);
		} finally {
			UICanvas.unlock();
		}
		this.setFreezed(false);
		askRepaint();
	}

	public void itemAction(UIItem item) {
		if (item instanceof UIContact) {
			UIContact uic = (UIContact) item;
			Contact c = uic.c;
			if (this.getSelectedContact() != c) {
				this.rosterPanel.setSelectedIndex(this.rosterPanel.getItems()
						.indexOf(uic));
			}
			if (c != null) {
				chatWithSelected(true);
			}
		}
	}

	/**
	 * Handle a command
	 * 
	 * @param c
	 *            the selected command
	 * @param d
	 *            the object on which the command has been ivoked
	 * 
	 */
	public void menuAction(UIMenu menu, UIItem c) {
		if (c == cmd_exit) {
			LampiroMidlet.exit();
		} else if (c == this.deleteQuestion) {
			xmppClient.getRoster().unsubscribeContact(
					this.delContact);
		} else if (c == cmd_delc) {
			Contact cont = getSelectedContact();
			deleteQuestion = new UILabel(rm
					.getString(ResourceIDs.STR_DELETE_CONTACT)
					+ ": " + cont.jid + "?");
			UIMenu deleteMenu = UIMenu.easyMenu(rm
					.getString(ResourceIDs.STR_DELETE_CONTACT), 10, -1,
					UICanvas.getInstance().getWidth() - 20, deleteQuestion);
			deleteQuestion.setFocusable(true);
			deleteMenu.setSelectedIndex(1);
			deleteQuestion.setWrappable(true, deleteMenu.getWidth() - 5);
			deleteMenu.cancelMenuString = rm.getString(ResourceIDs.STR_NO);
			deleteMenu.selectMenuString = rm.getString(ResourceIDs.STR_YES);
			Graphics cg = this.getGraphics();
			int offset = (cg.getClipHeight() - deleteMenu.getHeight(cg)) / 2;
			deleteMenu.setAbsoluteY(offset);
			this.delContact = cont;
			this.addPopup(deleteMenu);
		} else if (c == cmd_help) {
			boolean oldFreezed = this.isFreezed();
			this.setFreezed(true);
			String help = rm.getString(ResourceIDs.STR_HELP_TEXT);
			help = help.replace('<', '\n');
			UITextField helpField = new UITextField("", help, help.length(),
					TextField.UNEDITABLE);
			helpField.setWrappable(true);
			UIMenu helpMenu = UIMenu.easyMenu(rm
					.getString(ResourceIDs.STR_HELP), 1, 20, UICanvas
					.getInstance().getWidth() - 2, helpField);
			helpMenu.setSelectedIndex(1);
			helpMenu.cancelMenuString = "";
			helpMenu.selectMenuString = rm.getString(ResourceIDs.STR_CLOSE)
					.toUpperCase();
			this.addPopup(helpMenu);
			this.setFreezed(oldFreezed);
			this.askRepaint();
			helpField.expand();

		} else if (c == cmd_addc) {
			AddContactScreen acs = new AddContactScreen();
			UICanvas.getInstance().open(acs, true);
			// } else if(c == cmd_info) {
			// Contact user = getSelectedContact();
			// AddContactScreen acs = new AddContactScreen(user);
			// disp.setCurrent(acs);
		} else if (c == cmd_send) {
			Contact user = getSelectedContact();
			MessageComposerScreen ms = new MessageComposerScreen(user,
					MessageComposerScreen.MESSAGE);
			UICanvas.getInstance().open(ms, true);
		} else if (c == refresh_gateways) {
			this.gateways.clear();
			this.serverGateways = this.serverGatewayInput.getText();
			this.getIMGateways();
			this.setFreezed(true);
			gatewaysMenu.remove(refresh_gateways);
			UIGauge progressGauge = new UIGauge(rm
					.getString(ResourceIDs.STR_WAIT), false, Gauge.INDEFINITE,
					Gauge.CONTINUOUS_RUNNING);
			gatewaysMenu.append(progressGauge);
			this.setFreezed(false);
			int count = 10;
			// At most 10 seconds
			while (count-- > 0
					&& (components == null || components.length != infoedComponents)) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			this.setFreezed(true);
			progressGauge.cancel();
			refresh_gateways.setSelected(false);
			gatewaysMenu.remove(progressGauge);
			this.removePopup(gatewaysMenu);
			this.menuAction(this.getMenu(), gateways_discovery);
			this.setFreezed(false);
			this.askRepaint();
		} else if (c == gateways_discovery) {
			this.setFreezed(true);
			gatewaysMenu = UIMenu.easyMenu(rm.getString(ResourceIDs.STR_GATEWAYS), 
					10, 20, this.getWidth() - 20, null);
			// why ?
			//((UIItem) gatewaysMenu.getItemList().elementAt(0))
			//		.setFocusable(true);
			serverGatewayInput = new UITextField(
					rm.getString(ResourceIDs.STR_SERVER_EXPLORE),
					this.serverGateways,
					255,TextField.ANY);
			gatewaysMenu.selectMenuString = rm.getString(
					ResourceIDs.STR_REGISTER).toUpperCase();
			this.addPopup(gatewaysMenu);
			gatewaysMenu.append(serverGatewayInput);
			Enumeration en = this.gateways.keys();
			while (en.hasMoreElements()) {
				String from = (String) en.nextElement();
				Object[] nameImg = (Object[]) this.gateways.get(from);
				String name = (String) nameImg[0];
				Image img = (Image) nameImg[1];
				UILabel ithTransport = new UILabel(img, name);
				ithTransport.setFocusable(true);
				this.transPortHash.put(ithTransport, from);
				UIMenu gatewaysMenu = RosterScreen.this.gatewaysMenu;
				gatewaysMenu.append(ithTransport);
				gatewaysMenu.setDirty(true);
			}
			gatewaysMenu.append(refresh_gateways);
			this.setFreezed(false);
			RosterScreen.this.askRepaint();

		} else if (c == toggle_offline) {
			try {
				UICanvas.lock();
				this.show_offlines = !this.show_offlines;
				this.setDirty(true);
				this.setFreezed(true);
				Enumeration en = xmppClient.roster.contacts.elements();
				while (en.hasMoreElements()) {
					Contact ithContact = (Contact) en.nextElement();
					this.reorganizeContact(ithContact, Contact.CH_STATUS);
				}
				if (show_offlines) toggle_offline.setText(rm
						.getString(ResourceIDs.STR_HIDE_OFFLINE));
				else
					toggle_offline.setText(rm
							.getString(ResourceIDs.STR_SHOW_OFFLINE));

			} finally {
				UICanvas.unlock();
			}
			this.setFreezed(false);
			this.askRepaint();
		} else if (c == cmd_chat) {
			chatWithSelected(true);
			// } else if (c == cmd_reload) {
			// Roster.getInstance().updateRoster();
		} else if (c == cmd_state) {
			StatusScreen ssc = new StatusScreen();
			UICanvas.getInstance().open(ssc, true);
			// #mdebug
//@		} else if (c == cmd_debug) {
//@			DebugScreen debugScreen = new DebugScreen();
//@			UICanvas.getInstance().open(debugScreen, true);
//@		} else if (c == cmd_ask_capabilities) {
//@			Contact usr = getSelectedContact();
//@			usr.getCapabilities();
//@			usr.askCapabilities();
			// #enddebug
		} else if (c == cmd_querycmd) {
			Contact usr = getSelectedContact();
			usr.cmdlist = null;
			Iq iq = new Iq(usr.getFullJid(), Iq.T_GET);
			Element query = iq.addElement(XMPPClient.NS_IQ_DISCO_ITEMS,
					Iq.QUERY);
			query.setAttribute("node", "http://jabber.org/protocol/commands");
			AdHocCommandsHandler handler = new AdHocCommandsHandler();
			xmppClient.sendIQ(iq, handler);
		} else if (c == cmd_listcmd) {
			CommandListScreen cmdscr = new CommandListScreen(
					getSelectedContact());
			UICanvas.getInstance().open(cmdscr, true);
		} else if (c == cmd_mucs) {
			UIMenu mucNameMenu = UIMenu.easyMenu(rm
					.getString(ResourceIDs.STR_CHOOSE_NAME), 10, 20, this
					.getWidth() - 20, muc_name_field);
			mucNameMenu.append(muc_button);
			mucNameMenu.setDirty(true);
			mucNameMenu.setSelectedIndex(mucNameMenu.indexOf(muc_name_field));
			this.addPopup(mucNameMenu);
		} else if (c == cmd_about) {
			AboutScreen as = new AboutScreen();
			UICanvas.getInstance().open(as, true);
		} else if (c == cmd_options) {
			OptionsScreen os = new OptionsScreen();
			UICanvas.getInstance().open(os, true);
		} else if (c == cmd_tasks) {
			Contact usr = getSelectedContact();
			Task tasks[] = usr.getTasks();
			if (tasks.length == 1) {
				// #ifdef UI
				tasks[0].display();
				// #endif
			} else if (tasks.length > 1) {
				TaskListScreen taskListScreen = new TaskListScreen(tasks);
				UICanvas.getInstance().open(taskListScreen, true);
			}
		} else if (c == cmd_details) {
			Contact cont = this.getSelectedContact();
			if (cont != null) {
				ContactInfoScreen ci = new ContactInfoScreen(cont);
				UICanvas.getInstance().open(ci, true);
			}
		} else if (c == muc_button) {
			String mucName = this.muc_name_field.getText().replace(' ', '_');
			Contact myContact = xmppClient.getMyContact();
			Presence pres = myContact.getPresence();
			pres.setAttribute(Stanza.ATT_TO, mucName + "@" + this.mucJid + "/"
					+ Contact.user(myContact.getPrintableName()));
			Element el = new Element(XMPPClient.NS_MUC, DataForm.X);
			pres.children.addElement(el);
			xmppClient.sendPacket(pres);

			Iq iq = new Iq(mucName + "@" + this.mucJid + "/", "set");
			Element query = new Element(XMPPClient.NS_MUC_OWNER, Iq.QUERY);
			iq.children.addElement(query);
			Element x = new Element(DataForm.NAMESPACE, DataForm.X);
			x.setAttribute("type", "submit");
			query.children.addElement(x);
			xmppClient.sendPacket(iq);
			this.muc_name_field.setText("");
		} else if (c == this.acceptButton) {
			UIHLayout buttons = (UIHLayout) this.groupInviteMenu.getItemList()
					.lastElement();
			UILabel groupChatLabel = (UILabel) buttons.getItem(2);
			String invitedChatJid = groupChatLabel.getText();
			Contact myContact = xmppClient.getMyContact();
			Presence pres = myContact.getPresence();
			pres.setAttribute(Stanza.ATT_TO, invitedChatJid + "/"
					+ Contact.user(myContact.getPrintableName()));
			Element el = new Element(XMPPClient.NS_MUC, DataForm.X);
			pres.children.addElement(el);
			xmppClient.sendPacket(pres);
			this.removePopup(this.groupInviteMenu);
		} else if (c == this.denyButton) {
			this.removePopup(this.groupInviteMenu);
		} else if (c == this.cmd_close_muc) {
			Presence pres = new Presence();
			MUC muc = (MUC) this.getSelectedContact();
			pres.setAttribute(Stanza.ATT_TO, muc.jid);
			pres.setAttribute(Stanza.ATT_TYPE, Presence.T_UNAVAILABLE);
			xmppClient.sendPacket(pres);
			rosterPanel.removeItem(rosterPanel.getSelectedItem());
			this.askRepaint();
		} else if (menu == this.gatewaysMenu) {
			String from = "";
			Enumeration en = gatewaysMenu.getItemList().elements();
			// search the containing object
			while (en.hasMoreElements()) {
				UIItem ithLabel = (UIItem) en.nextElement();
				if (ithLabel instanceof UILabel && ithLabel == c) {
					from = (String) this.transPortHash.get(ithLabel);
					break;
				}
			}
			RegisterHandler rh = new RegisterHandler();
			Iq iq = new Iq(from, Iq.T_GET);
			iq.addElement(IQ_REGISTER, Iq.QUERY);
			// from this point on all the subscription 
			// "from" and "username@from"
			// will be autoaccepted from this 
			xmppClient.autoAcceptGateways.addElement(from);
			xmppClient.sendIQ(iq, rh);
		}
	}

	private void getIMGateways() {
		components = null;
		infoedComponents = 0;
		transPortHash.clear();
		IQResultListener dih = new IQResultListener() {
			public void handleResult(Element e) {
				Element q = e.getChildByName(XMPPClient.NS_IQ_DISCO_ITEMS,
						Iq.QUERY);
				if (q != null) {
					Element items[] = q.getChildrenByName(
							XMPPClient.NS_IQ_DISCO_ITEMS, "item");
					components = items;
					for (int i = 0; i < items.length; i++) {
						String ithJid = items[i].getAttribute("jid");
						IQResultListener dih = new IQResultListener() {
							public void handleError(Element e) {
							}

							public void handleResult(Element e) {
								Element q = e.getChildByName(
										XMPPClient.NS_IQ_DISCO_INFO, Iq.QUERY);
								if (q != null) {
									String type = null;
									String name = "";
									String from = e.getAttribute("from");
									Element identity = q.getChildByName(
											XMPPClient.NS_IQ_DISCO_INFO,
											"identity");
									if (identity != null) {
										type = identity.getAttribute("type");
										String category = identity
												.getAttribute("category");
										if (category.compareTo("conference") == 0
												&& type.compareTo("text") == 0) mucJid = from;
										name = identity.getAttribute("name");
									} else {
										name = from;
									}

									Element features[] = q.getChildrenByName(
											XMPPClient.NS_IQ_DISCO_INFO,
											"feature");
									for (int i = 0; i < features.length; i++) {
										String var = features[i]
												.getAttribute("var");
										if (var.compareTo(IQ_REGISTER) == 0) {
											Image img = null;
											if (type != null) {
												try {
													img = Image
															.createImage("/transport/"
																	+ type
																	+ ".png");
												} catch (IOException ex) {
													try {
														img = Image
																.createImage("/transport/transport.png");
													} catch (IOException e1) {
														// TODO Auto-generated
														// catch block
														e1.printStackTrace();
													}
												}
											} else {
												try {
													img = Image
															.createImage("/transport/transport.png");
												} catch (IOException e1) {
													// TODO Auto-generated catch
													// block
													e1.printStackTrace();
												}
											}

											String category = identity
													.getAttribute("category");
											if (category.compareTo("gateway") == 0) {
												RosterScreen.this.addGateway(
														name, from, img,type);
											}
										}
									}
								}
								infoedComponents++;
							}
						};
						Iq iq = new Iq(ithJid, Iq.T_GET);
						iq.addElement(XMPPClient.NS_IQ_DISCO_INFO, Iq.QUERY);
						xmppClient.sendIQ(iq, dih);
					}
				}
			}

			public void handleError(Element e) {
			}
		};
		Iq iq = new Iq(this.serverGateways, Iq.T_GET);
		iq.addElement(XMPPClient.NS_IQ_DISCO_ITEMS, Iq.QUERY);
		xmppClient.sendIQ(iq, dih);
	}
	
	private void addGateway(String name, String from, Image img, String type) {
		Enumeration en = gateways.keys();
		while (en.hasMoreElements()) {
			String ithFrom = (String) en.nextElement();
			Object[] ithData = (Object[]) gateways.get(ithFrom);
			String ithType = (String) ithData[2];
			if (ithType.compareTo(type) == 0) {
				if (from.indexOf(Config.BLUENDO_SERVER) >= 0) return;
				else {
					gateways.remove(from);
					break;
				}
			}
		}
		gateways.put(from, new Object[] { name, img, type });
	}

	private Contact getSelectedContact() {
		if (rosterPanel == null || rosterPanel.getSelectedItem() == null) return null;
		UIItem selContact = rosterPanel.getSelectedItem();
		if (selContact instanceof UIContact) return ((UIContact) selContact).c;
		else
			return null;
	}

	private void chatWithSelected(boolean force_chat) {
		Contact user = getSelectedContact();
		if (user.unread_msg || force_chat) {
			ChatScreen ms = (ChatScreen) chatScreenList.get(user);
			if (ms == null) {
				if (user instanceof MUC == true) {
					ms = new MUCScreen(user);
				} else {
					ms = new ChatScreen(user);
				}
				chatScreenList.put(user, ms);
			}
			UICanvas.getInstance().open(ms, true);
		} else {
			SimpleComposerScreen cs = new SimpleComposerScreen(user);
			UICanvas.display(cs);
		}
		//user.unread_msg = false;
	}

	public static void showNextScreen(UIScreen currentScreen) {
		Vector screenList = UICanvas.getInstance().getScreenList();
		int currentIndex = screenList.indexOf(currentScreen);
		currentIndex++;
		if (currentIndex >= screenList.size()) currentIndex = 0;
		if (screenList.size() > 1) UICanvas.getInstance().show(currentIndex);
	}

	public static void showPreviousScreen(UIScreen currentScreen) {
		Vector screenList = UICanvas.getInstance().getScreenList();
		int currentIndex = screenList.indexOf(currentScreen);
		currentIndex--;
		if (currentIndex < 0) currentIndex = screenList.size() - 1;
		if (screenList.size() > 1) UICanvas.getInstance().show(currentIndex);
	}

	private class AdHocCommandsHandler extends IQResultListener {

		public void handleError(Element e) {
			// simply ignore -> XXX we could add an alert
		}

		public void handleResult(Element e) {
			RosterScreen.this.xmppClient.handleClientCommands(e, true);
		}
	}

	private class RegisterHandler extends IQResultListener {

		/*
		 * 
		 * the received registration packet with form
		 */
		private Element e;

		/*
		 * the received dataform
		 */
		private DataForm df;

		/*
		 * the dataformscreen opened with registration data
		 */
		private UIScreen dfs;

		public void handleError(Element e) {
			// TODO Auto-generated method stub

		}

		public void handleResult(Element e) {
			Element q = e.getChildByName(RosterScreen.IQ_REGISTER, Iq.QUERY);
			if (q != null) {
				/* Parse the dataform if present */
				this.e = e;
				UIScreen screen = null;
				Element form = q.getChildByName(DataForm.NAMESPACE, DataForm.X);
				if (form != null) {
					DataForm df = new DataForm(form);
					this.df = df;
					RegisterDataFormExecutor rdf = new RegisterDataFormExecutor(
							this);
					screen = new DataFormScreen(df, rdf);
					this.dfs = screen;
				} else if (q.getChildByName(null, "username") != null) {
					screen = new GatewayRegisterScreen(e);
				}
				if (screen != null) {
					UICanvas.getInstance().open(screen, true);
				}
			}
		}
	}

	public class RegisterDataFormExecutor implements DataFormListener {

		private RegisterHandler registerHandler;

		public RegisterDataFormExecutor(RegisterHandler registerHandler) {
			this.registerHandler = registerHandler;
		}

		public void execute(int cmd) {
			if (cmd == DataFormListener.CMD_SUBMIT) {
				String from = registerHandler.e.getAttribute(Stanza.ATT_FROM);
				Stanza reply = new Iq(from, Iq.T_SET);
				reply.setAttribute(Stanza.ATT_FROM, registerHandler.e
						.getAttribute(Stanza.ATT_TO));
				Element query = new Element(IQ_REGISTER, Iq.QUERY);
				reply.children.addElement(query);
				DataForm df = registerHandler.df;
				df.type = DataForm.TYPE_SUBMIT;
				query.children.addElement(df.getResultElement());
				xmppClient.sendPacket(reply);
				UICanvas.getInstance().close(registerHandler.dfs);
				Object[] nameImg = (Object[]) gateways.get(from);
				String name = (String) nameImg[0];
				String selectedText = name + ": ";
				UILabel regLabel = new UILabel(selectedText + " "
						+ rm.getString(ResourceIDs.STR_REG_GATEWAYS));
				UIMenu regMenu = UIMenu.easyMenu(rm
						.getString(ResourceIDs.STR_GATEWAYS), 10, 20,
						RosterScreen.this.getWidth() - 20, regLabel);
				regLabel.setWrappable(true, regMenu.getWidth());
				RosterScreen.this.addPopup(regMenu);
				UICanvas.getInstance().close(registerHandler.dfs);
			} else if (cmd == DataFormListener.CMD_CANCEL) {
				UICanvas.getInstance().close(registerHandler.dfs);
			}
		}
	}

	/**
	 * Update the (global) status of a contact and repaint the roster
	 * accordingly to the new situation
	 * 
	 * @param c
	 */
	public void updateContact(Contact c, int reason) {

		// #ifdef TIMING
		// @ long t1 = System.currentTimeMillis();
		// #endif
		boolean needRepaint = false;
		try {
			UICanvas.lock();
			needRepaint = reorganizeContact(c, reason);
		} finally {
			UICanvas.unlock();
			if (needRepaint) askRepaint();
		}

		// #ifdef TIMING
		// @ System.out.println("New sort time: " + (System.currentTimeMillis()
		// @ // - t1));
		// #endif
	}

	boolean reorganizeContact(Contact c, int reason) {
		boolean needRepaint = false;
		UIContact uic = (UIContact) shown_contacts.get(c);
		if (uic != null) {
			this.rosterPanel.removeItem(uic);
		}
		Object objectRemoved = shown_contacts.remove(c);
		if (objectRemoved != null || c.isVisible() || this.show_offlines) {
			// reinsert if its visible
			int i = 0;
			if (c.isVisible() || this.show_offlines
					|| chatScreenList.contains(c)) {
				//				Enumeration en = null;
				//				for (en = rosterPanel.getItems().elements(); en
				//						.hasMoreElements();) {
				//					UIContact ithContact = (UIContact) en.nextElement();
				//					if (c.compareTo(ithContact.c) > 0) break;
				//					i++;
				//				}
				// better the dicotomic
				Vector items = rosterPanel.getItems();
				int min = 0;
				int max = items.size();
				int med = 0;
				while (min != max) {
					med = (min + max) / 2;
					UIContact ithContact = (UIContact) items.elementAt(med);
					if (c.compareTo(ithContact.c) < 0) min = med + 1;
					else
						max = med;
				}
				i = min;
				if (uic == null) {
					uic = new UIContact(c);
					this.toggleContactMenu(uic);
				}
				this.rosterPanel.insertItemAt(uic, i);
				shown_contacts.put(c, uic);
				uic.updateContactData();
				String newTitle = rm.getString(ResourceIDs.STR_ROSTER_TITLE)
						+ "(" + shown_contacts.size() + ")";
				if (newTitle.compareTo(this.getTitle()) != 0) this
						.setTitle(newTitle);
			}

			if (reason == Contact.CH_MESSAGE_NEW
					|| reason == Contact.CH_TASK_NEW) {
				// set the correct selection to the just updated task
				this.rosterPanel.setSelectedIndex(i);
			}
			if (filtering == false) filterContacts(true);
			needRepaint = true;
		}
		return needRepaint;
	}

	protected boolean askRepaint() {
		this.updateHeader();
		this.toggleMenus();

		return super.askRepaint();
	}

	private boolean filtering = false;

	/*
	 * Filter the contacts by means of the letters
	 * selected by user in the rosterscreen.
	 * 
	 * @param reuse If false restart filtering from scratch
	 * 
	 */
	private void filterContacts(boolean reuse) {
		// sometimes i must repaint here and sometimes not
		// i hence must save the freeze status
		boolean oldFreezed = this.isFreezed();
		filtering = true;
		this.setFreezed(true);
		if (!reuse && this.hiddenContacts.size() > 0) {
			Enumeration en = hiddenContacts.elements();
			while (en.hasMoreElements()) {
				Contact c = (Contact) en.nextElement();
				this.reorganizeContact(c, Contact.CH_STATUS);
			}
			this.hiddenContacts.removeAllElements();
		}
		if (sel_pattern.length() > 0) {
			// cannot use enumeration !!!

			for (int i = this.rosterPanel.getItems().size() - 1; i >= 0; i--) {
				UIContact uic = (UIContact) this.rosterPanel.getItems()
						.elementAt(i);
				String contactName = uic.c.getPrintableName().toLowerCase();
				if (contactName.indexOf(sel_pattern) != 0) {
					hiddenContacts.addElement(uic.c);
					this.rosterPanel.removeItem(uic);
				}
			}
		}
		this.setFreezed(oldFreezed);
		filtering = false;
	}

	public void removeContact(Contact c) {
		try {
			UICanvas.lock();
			UIContact uic = (UIContact) shown_contacts.get(c);
			if (uic != null) {
				this.rosterPanel.removeItem(uic);
				chatScreenList.remove(c);
				this.removePaintedItem(uic);
			}
			shown_contacts.remove(c);
		} finally {
			UICanvas.unlock();
		}

		// XXX we could repaint only if this contact is really displayed
		askRepaint();
	}

	public void removeAllContacts() {
		shown_contacts.clear();
		this.rosterPanel.removeAllItems();
		askRepaint();
	}

	public void packetReceived(Element e) {
		// Dispatch the topic to the correct MUC
		String userHost = Contact.userhost(e.getAttribute(Iq.ATT_FROM));
		if (e.name.compareTo("message") == 0) {
			Element subject = e.getChildByName(null, "subject");
			Contact muc = xmppClient.roster
					.getContactByJid(userHost);
			if (muc != null) {
				MUCScreen mucScreen = (MUCScreen) chatScreenList.get(muc);
				if (subject != null) {
					if (mucScreen == null) {
						mucScreen = new MUCScreen(muc);
						chatScreenList.put(muc, mucScreen);
						UICanvas.getInstance().open(mucScreen, false);
					}
					((MUC) mucScreen.user).topic = subject.content;
					UILabel mucName = (UILabel) mucScreen.header.getItem(0);
					mucName.setText(rm.getString(ResourceIDs.STR_TOPIC) + ": "
							+ subject.content);
					UICanvas.getInstance().askRepaint(mucScreen);

					return;
				}
			}
		}

		String invitedMuc = e.getAttribute(Message.ATT_FROM);
		String mucName = Contact.user(invitedMuc);
		String inviterName = e.getChildByName(XMPPClient.NS_MUC_USER, "x")
				.getChildByName(null, "invite").getAttribute(Message.ATT_FROM);
		Contact c = xmppClient.roster.getContactByJid(inviterName);
		String printableName = "";
		if (c != null) printableName = c.getPrintableName();
		else
			printableName = inviterName;
		UILabel info = new UILabel(rm
				.getString(ResourceIDs.STR_GROUP_CHAT_INVITATION)
				+ " " + printableName + "?");
		info.setWrappable(true, groupInviteMenu.getWidth() - 5);
		groupInviteMenu = UIMenu.easyMenu(rm.getString(ResourceIDs.STR_GROUP_CHAT), 10, 20, this.getWidth() - 20, info);
		info.setFocusable(false);
		UILabel groupName = new UILabel(rm
				.getString(ResourceIDs.STR_GROUP_CHAT)
				+ ": " + mucName);
		groupInviteMenu.append(groupName);
		groupName.setFocusable(false);
		UIHLayout buttons = new UIHLayout(3);
		acceptButton = new UIButton(rm.getString(ResourceIDs.STR_YES));
		denyButton = new UIButton(rm.getString(ResourceIDs.STR_NO));
		buttons.insert(acceptButton, 0, 50, UILayout.CONSTRAINT_PERCENTUAL);
		buttons.insert(denyButton, 1, 50, UILayout.CONSTRAINT_PERCENTUAL);
		// hide conference name
		buttons.insert(new UILabel(invitedMuc), 2, 0,
				UILayout.CONSTRAINT_PIXELS);
		buttons.setGroup(false);
		groupInviteMenu.append(buttons);
		this.addPopup(groupInviteMenu);
	}

	/**
	 * @return the chatScreenList
	 */
	public static Hashtable getChatScreenList() {
		return chatScreenList;
	}

	public void longPressed(UIItem item) {
		if (item instanceof UIContact) {
			UIContact uic = (UIContact) item;
			Contact c = uic.c;
			// #mdebug
//@						Logger.log("longPressed on :" + c.getFullJid());
			// #enddebug
			if (this.getSelectedContact() != c) {
				this.rosterPanel.setSelectedIndex(this.rosterPanel.getItems()
						.indexOf(uic));
			}
			this.keyPressed(UICanvas.MENU_LEFT);
		}
	}
}
