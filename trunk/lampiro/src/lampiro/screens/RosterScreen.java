/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: RosterScreen.java 2059 2010-04-15 11:04:51Z luca $
*/

package lampiro.screens;

import it.yup.ui.UIAccordion;
import it.yup.ui.UIButton;
import it.yup.ui.UICanvas;
import it.yup.ui.UIConfig;
import it.yup.ui.UIHLayout;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UILayout;
import it.yup.ui.UIMenu;
import it.yup.ui.UIScreen;
import it.yup.ui.UISeparator;
import it.yup.ui.UITextField;
import it.yup.ui.UIUtils;
import it.yup.ui.UIVLayout;
import it.yup.util.EventDispatcher;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.util.Utils;
import it.yup.xml.BProcessor;
import it.yup.xml.Element;
import it.yup.xmlstream.BasicXmlStream;
import it.yup.xmlstream.EventQuery;
import it.yup.xmlstream.EventQueryRegistration;
import it.yup.xmlstream.PacketListener;
import it.yup.xmpp.Config;
import it.yup.xmpp.Contact;
import it.yup.xmpp.DataFormListener;
import it.yup.xmpp.FTReceiver;
import it.yup.xmpp.FTSender;
import it.yup.xmpp.IQResultListener;
import it.yup.xmpp.MUC;
import it.yup.xmpp.Roster;
import it.yup.xmpp.Task;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.CommandExecutor.CommandExecutorListener;
import it.yup.xmpp.FTReceiver.FTREventHandler;
import it.yup.xmpp.FTReceiver.OpenListener;
import it.yup.xmpp.FTSender.FTSEventHandler;
import it.yup.xmpp.packets.DataForm;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Message;
import it.yup.xmpp.packets.Presence;
import it.yup.xmpp.packets.Stanza;


import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimerTask;
import java.util.Vector;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextField;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;

import lampiro.LampiroMidlet;
import lampiro.screens.HandleMucScreen.HMC_CONSTANTS;



import lampiro.screens.rosterItems.UIContact;
import lampiro.screens.rosterItems.UIContactGroup;
import lampiro.screens.rosterItems.UIGateway;
import lampiro.screens.rosterItems.UIGatewayGroup;
import lampiro.screens.rosterItems.UIGroup;
import lampiro.screens.rosterItems.UIRosterItem;
import lampiro.screens.rosterItems.UIServices;


//#mdebug
//@
//@import it.yup.util.Logger;
//@
//#enddebug

// #ifndef UI
//@import javax.microedition.lcdui.Displayable;
// #endif

public class RosterScreen extends UIScreen implements PacketListener,
		FTREventHandler, FTSEventHandler, XMPPClient.XmppListener {

	// #ifdef TEST_COMPONENTS 
//@	private String testServer = "moneiro.biz";
	// #endif

	/*
	 * A flag to know if the client is online
	 */
	private static boolean online = false;

	/*
	 * the volume for playing tones
	 */
	private int volume;

	/*
	 * a mask used to playing tones
	 */
	private boolean play_flags[];

	/*
	 * The MUC component jid
	 */
	String mucJid = null;

	/*
	 * The upload file contact
	 */
	String uploadJid = null;

	/*
	 * The upload file suffix
	 */
	String uploadSuffix = "";

	/*
	 * The base path used for construct the file upload url
	 */
	String basePath = "";

	/*
	 * The server used to explore gateways 
	 */
	String gatewaysServer = null;

	/*
	 * If true the offline contacts are shown.
	 */
	private boolean show_offlines = false;

	private static ResourceManager rm = ResourceManager.getManager();

	private UILabel cmd_fts = new UILabel(rm
			.getString(ResourceIDs.STR_FT_STATUS));

	UILabel cmd_logout = new UILabel(rm.getString(ResourceIDs.STR_LOGOUT));
	private UILabel cmd_login = new UILabel(rm.getString(ResourceIDs.STR_LOGIN));
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
	private UILabel cmd_album = new UILabel(rm
			.getString(ResourceIDs.STR_MM_ALBUM));
	// XXX update delayed
	// private Command cmd_reload = new
	// Command(rm.getString(ResourceIDs.STR_RELOAD_CONTACT), Command.SCREEN, 6);
	private UILabel cmd_exit = new UILabel(rm
			.getString(ResourceIDs.STR_CLOSE_APPLICATION));
	// #mdebug
//@	private UILabel cmd_debug = new UILabel(rm.getString(ResourceIDs.STR_DEBUG));
	// #enddebug

	private UILabel cmd_refresh_roster = new UILabel(rm
			.getString(ResourceIDs.STR_REFRESH_ROSTER));

	private UILabel cmd_about = new UILabel(rm.getString(ResourceIDs.STR_ABOUT));

	UILabel cmd_options = new UILabel(rm
			.getString(ResourceIDs.STR_OPTIONS_SETUP));

	UILabel cmd_capture_img = new UILabel(rm
			.getString(ResourceIDs.STR_CAPTURE_IMAGE));

	UILabel cmd_capture_aud = new UILabel(rm
			.getString(ResourceIDs.STR_CAPTURE_AUDIO));

	private UILabel cmd_mucs = new UILabel(rm
			.getString(ResourceIDs.STR_CREATE_GROUP_CHAT));

	private UILabel confirmDeleteLabel = new UILabel(rm
			.getString(ResourceIDs.STR_SCARY_MUC));

	private UIMenu deleteMucMenu = UIUtils.easyMenu(rm
			.getString(ResourceIDs.STR_GROUP_CHAT), 10, 30, UICanvas
			.getInstance().getWidth() - 20, confirmDeleteLabel, "", "");

	/**
	 * The question asked whene deleting a contact
	 */
	private UILabel deleteQuestion;

	private UIButton cmdMucYes = new UIButton(rm.getString(ResourceIDs.STR_YES));

	private UIButton cmdMucNo = new UIButton(rm.getString(ResourceIDs.STR_NO));

	Hashtable gateways = new Hashtable();

	public static Hashtable chatScreenList = new Hashtable();

	private String searchString = rm.getString(ResourceIDs.STR_SEARCH);

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
	 * The offset of the selected key in the research pattern
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

	private XMPPClient xmppClient = XMPPClient.getInstance();

	private FTReceiver ftReceiver;

	public boolean cameraOn = false;

	public boolean micOn = false;

	private String highLightString = rm.getString(ResourceIDs.STR_HIGHLIGHTS);

	private UIHLayout header = null;
	private UILabel connData = null;
	private UILabel presenceLabel = null;
	public UIAccordion rosterAccordion = null;
	public UIAccordion searchAccordion = null;
	private UIContactGroup searchGroup = null;
	private int viewedAccordionIndex = -1;
	protected boolean priorityChecked = false;

	private Player mp3Player = null;

	/*
	 * These are default contacts that are added to the roster (agents, services, ...)
	 * without user intervention
	 */
	private Vector defaultContacts = new Vector();

	/*
	 * When loosing connection I save all the unread conversations here 
	 * so to retrieve them when going online again 
	 */
	private Vector savedConvs = null;

	private long playTime = 0;
	private boolean scheduledPlay = false;
	private Vector updateQueue = new Vector();
	private long updateTime = 0;
	private boolean updateScheduled = false;


	private PacketListener pubSubListener = new PacketListener() {

		public void packetReceived(Element e) {
			String from = e.getAttribute(Message.ATT_FROM);
			Contact c = xmppClient.getRoster().getContactByJid(from);
			if (c != null) {
				try {
					UICanvas.lock();
					// the contact must be created here in case it does not exist
					// (contact creation can be delayed due to bufferizing)
					dequeueContact(c, Contact.CH_STATUS);
				} catch (Exception ex) {
					// #mdebug
//@					Logger.log("In pubsub reception:" + ex.getClass());
//@					ex.printStackTrace();
					// #enddebug
				} finally {
					UICanvas.unlock();
				}
				UIContactGroup sg = UIServices.getGroup(rosterAccordion, false);
				UIContact uic = sg != null ? sg.getUIContact(c) : null;
				if (uic != null) {
					((PacketListener) uic).packetReceived(e);
				}
			}
		}
	};

	public static UIContactGroup getHighlightGroup(boolean allocate) {
		RosterScreen rs = RosterScreen.getInstance();
		return UIContactGroup.getContactGroup(rs.highLightString,
				rs.rosterAccordion, allocate);
	}

	private class RosterScreenListener extends IQResultListener {

		public static final int UNREGISTER_TYPE = 0;

		public final int LISTENER_TYPE;

		public RosterScreenListener(int type) {
			this.LISTENER_TYPE = type;
		}

		public void handleResult(Element e) {
			switch (LISTENER_TYPE) {

				case UNREGISTER_TYPE:
					handleUnregister(e);
					break;
			}
		}

		private void handleUnregister(Element e) {
			unsubscribe(e);
		}

		public void handleError(Element e) {
			if (this.LISTENER_TYPE == UNREGISTER_TYPE) {
				unsubscribe(e);
			}
		}

		private void unsubscribe(Element e) {
			try {
				UICanvas.lock();
				String toJid = e.getAttribute(Iq.ATT_FROM);
				Contact to = xmppClient.getRoster().getContactByJid(toJid);
				xmppClient.getRoster().unsubscribeContact(to);
				RosterScreen.this._removeContact(to);
				UIGatewayGroup ugg = UIGatewayGroup.getGroup(rosterAccordion,
						true);
				ugg.removeContact(to);
			} catch (Exception ex) {
				// #mdebug
//@				Logger.log("In deleting network:");
//@				ex.printStackTrace();
				// #enddebug
			} finally {
				UICanvas.unlock();
			}
		}

	}

	private static class FileReceiveScreen extends UIScreen {

		private UILabel cmd_exit = null;

		/*
		 * The listener related to this file trasnfer
		 */
		private OpenListener ftrp;

		private UIButton buttonNo;

		private UIButton buttonYes;

		private FileReceiveScreen(Contact c, OpenListener ftrp) {
			ResourceManager rm = RosterScreen.rm;

			this.setMenu(new UIMenu(""));
			cmd_exit = new UILabel(rm.getString(ResourceIDs.STR_CLOSE)
					.toUpperCase());
			this.getMenu().append(cmd_exit);
			this.setTitle(rm.getString(ResourceIDs.STR_FT));
			this.ftrp = ftrp;

			UILabel menuLabel = new UILabel(c.getPrintableName() + " "
					+ rm.getString(ResourceIDs.STR_ASK_FT) + ": "
					+ rm.getString(ResourceIDs.STR_FILE_NAME) + " - "
					+ ftrp.fileName + " \n"
					+ rm.getString(ResourceIDs.STR_DESC) + " - "
					+ ftrp.fileDesc + " \n"
					+ rm.getString(ResourceIDs.STR_SIZE) + " - "
					+ ftrp.fileSize + ". \n"
					+ rm.getString(ResourceIDs.STR_SUBSCRIPTION_ACCEPT));
			this.append(menuLabel);

			UIHLayout yesNo = new UIHLayout(2);
			yesNo.setGroup(false);
			buttonYes = new UIButton(rm.getString(ResourceIDs.STR_YES));
			buttonYes.setAnchorPoint(Graphics.HCENTER);
			buttonNo = new UIButton(rm.getString(ResourceIDs.STR_NO));
			buttonNo.setAnchorPoint(Graphics.HCENTER);
			yesNo.insert(buttonYes, 0, 50, UILayout.CONSTRAINT_PERCENTUAL);
			yesNo.insert(buttonNo, 1, 50, UILayout.CONSTRAINT_PERCENTUAL);
			this.append(yesNo);

			menuLabel.setWrappable(true, UICanvas.getInstance().getWidth() - 5);
			menuLabel.setFocusable(false);
			menuLabel.setSelected(false);
			yesNo.setSelectedItem(buttonYes);

		}

		public void menuAction(UIMenu menu, UIItem cmd) {
			if (cmd == cmd_exit) UICanvas.getInstance().close(this);
		}

		public void itemAction(UIItem item) {
			if (item == buttonYes) {
				this.ftrp.answerFT(true);
				FTScreen.startFtreceive(ftrp);
				updateFT();
			} else if (item == buttonNo) {
				this.ftrp.answerFT(false);
				updateFT();
			}
		}

		private void updateFT() {
			FTScreen frs = FTScreen.getInstance();
			UICanvas.getInstance().close(this);
			UICanvas.getInstance().open(frs, true, this);
		}
	}

	private class AdHocCommandsHandler extends IQResultListener {

		private WaitScreen ws;
		private EventQueryRegistration eqr;

		public AdHocCommandsHandler(UIScreen returnScreen) {
			ws = new WaitScreen(rm.getString(ResourceIDs.STR_CMDSCREEN_TITLE),
					returnScreen);
			eqr = EventDispatcher.addDelayedListener(ws, true);
			UICanvas.getInstance().open(ws, true);
		}

		public void handleError(Element e) {
			handlePacket(e, true);
			showAlert(AlertType.INFO, rm.getString(ResourceIDs.STR_LISTCMD), rm
					.getString(ResourceIDs.STR_COMMAND_ERROR), null);
		}

		public void handleResult(Element e) {
			handlePacket(e, false);
		}

		private void handlePacket(Element e, boolean error) {
			try {
				UICanvas.lock();
				if (!error) {
					xmppClient.handleClientCommands(e);
					String from = e.getAttribute(Stanza.ATT_FROM);
					if (from == null) return;
					Contact c = xmppClient.getRoster().getContactByJid(from);
					if (c == null) return;
					Element q = e.getChildByName(XMPPClient.NS_IQ_DISCO_ITEMS,
							Iq.QUERY);
					if (q != null) {
						_updateContact(c, Contact.CH_TASK_NEW);
						CommandListScreen cmdscr = new CommandListScreen(c,
								from);
						cmdscr.setReturnScreen(ws.getReturnScreen());
						UICanvas.getInstance().open(cmdscr, true);
					}
				}
			} catch (Exception ex) {
				// #mdebug
//@				Logger.log("In handling cmd error:");
//@				ex.printStackTrace();
				// #enddebug
			} finally {
				UICanvas.unlock();
			}
			EventDispatcher.dispatchDelayed(eqr, AdHocCommandsHandler.this);
		}
	}

	class RegisterHandler extends IQResultListener {

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

		private EventQueryRegistration eqr;

		public RegisterHandler(EventQueryRegistration eqr) {
			this.eqr = eqr;
		}

		public void handleError(Element e) {
			EventDispatcher.dispatchDelayed(eqr, RegisterHandler.this);
			RosterScreen.this.registerError(e);
		}

		public void handleResult(Element e) {
			try {
				UICanvas.lock();
				//closeWaitingScreen();
				Element q = e.getChildByName(XMPPClient.IQ_REGISTER, Iq.QUERY);
				if (q != null) {
					/* Parse the dataform if present */
					this.e = e;
					UIScreen screen = null;
					Element form = q.getChildByName(DataForm.NAMESPACE,
							DataForm.X);
					// new style gateway registration with form
					if (form != null) {
						DataForm df = new DataForm(form);
						this.df = df;
						RegisterDataFormExecutor rdf = new RegisterDataFormExecutor(
								this);
						screen = new DataFormScreen(df, rdf, -1);
						this.dfs = screen;
					} // old style gateway registration with specific handler
					else if (q.getChildByName(null, "username") != null) {
						screen = new GatewayRegisterScreen(e);
					}

					if (screen != null) UICanvas.getInstance().open(screen,
							true, RosterScreen.getInstance());
					else
						UICanvas.getInstance().open(RosterScreen.this, true);
				}
			} catch (Exception ex) {
				// #mdebug
//@				Logger.log("In handling registration:");
//@				ex.printStackTrace();
				// #enddebug
			} finally {
				UICanvas.unlock();
			}
			EventDispatcher.dispatchDelayed(eqr, RegisterHandler.this);
		}
	}

	public class RegisterDataFormExecutor extends IQResultListener implements
			DataFormListener {

		private RegisterHandler registerHandler;

		public RegisterDataFormExecutor(RegisterHandler registerHandler) {
			this.registerHandler = registerHandler;
		}

		public void execute(int cmd) {
			if (cmd == DataFormListener.CMD_SUBMIT) {
				String from = getFrom();
				Iq reply = new Iq(from, Iq.T_SET);
				reply.setAttribute(Stanza.ATT_FROM, registerHandler.e
						.getAttribute(Stanza.ATT_TO));
				Element query = new Element(XMPPClient.IQ_REGISTER, Iq.QUERY);
				reply.addElement(query);
				DataForm df = registerHandler.df;
				df.type = DataForm.TYPE_SUBMIT;
				query.addElement(df.getResultElement());
				xmppClient.sendIQ(reply, this);
				UICanvas.getInstance().close(registerHandler.dfs);
			} else if (cmd == DataFormListener.CMD_CANCEL) {
				UICanvas.getInstance().close(registerHandler.dfs);
			}
		}

		public void handleError(Element e) {
			RosterScreen.this.registerError(e);
		}

		public void handleResult(Element e) {
			try {
				UICanvas.lock();
				String from = registerHandler.e.getAttribute(Stanza.ATT_FROM);
				Object[] nameImg = (Object[]) gateways.get(from);
				String name = (String) nameImg[0];
				String selectedText = name + ": ";
				UILabel regLabel = new UILabel(selectedText + " "
						+ rm.getString(ResourceIDs.STR_REG_GATEWAYS));
				UIMenu regMenu = UIUtils.easyMenu(rm
						.getString(ResourceIDs.STR_GATEWAYS), 10, 20,
						RosterScreen.this.getWidth() - 20, regLabel, "", rm
								.getString(ResourceIDs.STR_CONTINUE)
								.toUpperCase());
				regLabel.setWrappable(true, regMenu.getWidth());
				RosterScreen.this.addPopup(regMenu);
			} catch (Exception ex) {
				// #mdebug
//@				ex.printStackTrace();
				// #enddebug
			} finally {
				UICanvas.unlock();
			}
		}

		public String getFrom() {
			// TODO Auto-generated method stub
			return this.registerHandler.e.getAttribute(Stanza.ATT_FROM);
		}

		public void setCel(CommandExecutorListener cel) {
			//this.eqr=eqr;
		}

		public boolean needWaiting(int comm) {
			// TODO Auto-generated method stub
			return false;
		}
	}

	public class DiscoExplorer extends IQResultListener {

		public static final int ITEMS = 0;
		public static final int ITEM = 1;

		private int discoType = ITEMS;
		private String serverAddress = null;
		private int[] queriedGateways = null;


		public void handleError(Element e) {
			switch (discoType) {

				case DiscoExplorer.ITEM:
					queriedGateways[0]--;
					break;


				default:
					break;
			}
		}

		public void handleResult(Element e) {
			switch (discoType) {
				case DiscoExplorer.ITEMS:
					queriedItems(e);
					break;

				case DiscoExplorer.ITEM:
					queriedItem(e);
					break;

				default:
					break;
			}
		}


		private void queriedItem(Element e) {
			String type = null;
			String name = "";
			String from = e.getAttribute(Stanza.ATT_FROM);

			Element q = e.getChildByName(XMPPClient.NS_IQ_DISCO_INFO, Iq.QUERY);

			Element identity = e.getPath(new String[] {
					XMPPClient.NS_IQ_DISCO_INFO, XMPPClient.NS_IQ_DISCO_INFO },
					new String[] { Iq.QUERY, "identity" });
			Element[] feature = null;
			if (q != null) {
				feature = q.getChildrenByNameAttrs(null, XMPPClient.FEATURE,
						new String[] { "var" },
						new String[] { XMPPClient.NS_MUC });
			}
			e.getPath(new String[] { XMPPClient.NS_IQ_DISCO_INFO,
					XMPPClient.NS_IQ_DISCO_INFO }, new String[] { Iq.QUERY,
					XMPPClient.NS_MUC });

			if (identity != null) {
				type = identity.getAttribute("type");
				String category = identity.getAttribute("category");
				if (category.compareTo(XMPPClient.CONFERENCE) == 0
						&& type.compareTo(XMPPClient.TEXT) == 0
						&& feature.length > 0) {
					mucJid = from;
					// the mucJid is changed toggle the menus
					UICanvas.lock();
					try {
						RosterScreen.this.toggleMenus();
					} finally {
						UICanvas.unlock();
					}
				}

				if (category.compareTo("store") == 0
						&& type.compareTo("file") == 0 && FTSender.supportFT(q)) {
					uploadJid = from;
					getBasePath(q);
				}

				name = identity.getAttribute("name");
			} else {
				name = from;
			}

			Element features[] = null;
			if (q != null) features = q.getChildrenByNameAttrs(
					XMPPClient.NS_IQ_DISCO_INFO, XMPPClient.FEATURE,
					new String[] { "var" },
					new String[] { XMPPClient.IQ_REGISTER });
			String category = "";

			if (identity != null) category = identity.getAttribute("category");

			if (features.length > 0 && "gateway".equals(category)) {
				addGateway(gateways, name, from, type, serverAddress);
				// to update contact icons by means of gateway icon
				Enumeration en = XMPPClient.getInstance().getRoster().contacts
						.elements();
				UICanvas.lock();
				try {
					while (en.hasMoreElements()) {
						Contact c = (Contact) en.nextElement();
						_updateContact(c, Contact.CH_STATUS);
					}
					askRepaint();
				} catch (Exception ex) {
					// #mdebug
//@					ex.printStackTrace();
					// #enddebug
				} finally {
					UICanvas.unlock();
				}
			}
			queriedGateways[0]--;
		}

		/**
		 * @param e
		 */
		private void queriedItems(Element e) {
			Element q = e
					.getChildByName(XMPPClient.NS_IQ_DISCO_ITEMS, Iq.QUERY);
			if (q != null) {
				Element items[] = q.getChildrenByName(
						XMPPClient.NS_IQ_DISCO_ITEMS, "item");
				queriedGateways[0] = items.length;
				for (int i = 0; i < items.length; i++) {
					Element ithItem = items[i];
					queryInfo(serverAddress, queriedGateways, ithItem);
				}
			}
		}

		private void getBasePath(Element q) {
			Element x = q.getChildByName(XMPPClient.JABBER_X_DATA, "x");
			if (x == null) return;

			Element[] pathEl = x.getChildrenByNameAttrs(null, "field",
					new String[] { "var" }, new String[] { "url" });
			if (pathEl.length > 0) basePath = pathEl[0].getChildByName(null,
					"value").getText();

			pathEl = x.getChildrenByNameAttrs(null, "field",
					new String[] { "var" }, new String[] { "suffix" });
			if (pathEl.length > 0) uploadSuffix = pathEl[0].getChildByName(
					null, "value").getText();
		}

		/**
		 * @param serverAddress
		 * @param queriedGateways
		 * @param ithItem
		 */
		public void queryInfo(String serverAddress, int[] queriedGateways,
				Element ithItem) {
			String ithJid = ithItem.getAttribute("jid");
			DiscoExplorer de = new DiscoExplorer(DiscoExplorer.ITEM,
					serverAddress, queriedGateways);

			Iq iq = new Iq(ithJid, Iq.T_GET);
			iq.addElement(XMPPClient.NS_IQ_DISCO_INFO, Iq.QUERY);
			xmppClient.sendIQ(iq, de, 240000);
		}

		public DiscoExplorer(int type, final String serverAddress,
				int[] queriedGateways) {
			this.discoType = type;
			this.serverAddress = serverAddress;
			this.queriedGateways = queriedGateways;
		}

	}

	// #ifdef SCREENSAVER
	// @ private long last_key_time;
	// @ private static long SCREENSAVER_DELAY = 10000;
	// @ private TimerTask screensaver_starter = null;
	// #endif

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

	public Hashtable getGateways() {
		return gateways;
	}

	/*
	 * Some update operations dealing with changes performed in options screen
	 */
	public void updateScreen() {
		// update UIAccordion colors
		int gBgColor = UIUtils.colorize(UIConfig.bg_color, -10);
		rosterAccordion.setLabelColor(gBgColor);
		rosterAccordion.setLabelGradientColor(UIUtils.colorize(gBgColor, -3));
		rosterAccordion.setLabelGradientSelectedColor(UIUtils.colorize(
				UIConfig.header_bg, -8));
		rosterAccordion.setLabelSelectedColor(UIConfig.header_bg);
		searchAccordion.setLabelColor(gBgColor);
		searchAccordion.setLabelGradientColor(UIUtils.colorize(gBgColor, -3));
		searchAccordion.setLabelGradientSelectedColor(UIUtils.colorize(
				UIConfig.header_bg, -8));
		searchAccordion.setLabelSelectedColor(UIConfig.header_bg);

		UIItem[] uicg = rosterAccordion.getItemLabels();
		for (int i = 0; i < uicg.length; i++) {
			UIContactGroup item = (UIContactGroup) uicg[i];
			item.updateColors();
		}

		// update colors of UIContacts fields
		UIContact.textLabelSelectedColor = UIUtils.colorize(UIConfig.bg_color,
				-20);
		UIContact.textLabelFontColor = 0x000000;
		UIRosterItem.contactSelectedColor = UIUtils.colorize(
				UIConfig.header_bg, +20);
		this.rosterAccordion.setSepSelectedColor(UIConfig.bb_color);
		this.searchAccordion.setSepSelectedColor(UIConfig.bb_color);

		//		Enumeration en = UIGroup.uiGroups.elements();
		//		while (en.hasMoreElements()) {
		//			UIGroup ithGroup = (UIGroup) en.nextElement();
		//			ithGroup.initGroupData();
		//		}
		this.toggleMenus();

		// change the header color
		header.setBg_color(UIUtils.colorize(UIConfig.header_bg, +20));
		connData.setFg_color(UIConfig.menu_title);
		presenceLabel.setFg_color(UIConfig.menu_title);

		// updatetitlebar in chatscreen
		Enumeration en = chatScreenList.elements();
		while (en.hasMoreElements()) {
			ChatScreen cs = (ChatScreen) en.nextElement();
			cs.headerImg.setBg_color(UIConfig.header_bg);
			cs.headerImg.setFg_color(UIConfig.menu_title);
			cs.headerStatus.setBg_color(UIConfig.header_bg);
			cs.headerStatus.setFg_color(UIConfig.menu_title);
		}

		en = UICanvas.getInstance().getScreenList().elements();
		while (en.hasMoreElements()) {
			UIScreen ithScreen = (UIScreen) en.nextElement();
			// update footers and headers
			updateLabels(ithScreen);
		}
		updateLabels(this);
	}

	/**
	 * @param ithScreen
	 */
	private void updateLabels(UIScreen ithScreen) {
		UILabel[] lbls = new UILabel[] { ithScreen.footerLeft,
				ithScreen.footerRight, ithScreen.titleLabel };
		for (int i = 0; i < lbls.length; i++) {
			UILabel label = lbls[i];
			label.setBg_color(UIConfig.header_bg);
			label.setFg_color(UIConfig.menu_title);
			label.setFont(UIConfig.font_title);
		}
	}

	private RosterScreen() {
// #ifndef GLIDER
																defaultContacts.addElement(new Object[] { Config.LAMPIRO_AGENT,
																		"Lampiro Agent" });
		// #endif
		UIHLayout buttonLayout = new UIHLayout(2);
		buttonLayout.setGroup(false);
		buttonLayout.insert(cmdMucNo, 0, 50, UILayout.CONSTRAINT_PERCENTUAL);
		buttonLayout.insert(cmdMucYes, 1, 50, UILayout.CONSTRAINT_PERCENTUAL);
		this.confirmDeleteLabel.setWrappable(true, UICanvas.getInstance()
				.getWidth() - 30);
		confirmDeleteLabel.setFocusable(false);
		this.deleteMucMenu.append(buttonLayout);

		Config cfg = Config.getInstance();
		volume = Integer.parseInt(cfg.getProperty(Config.TONE_VOLUME, "50"));
		play_flags = Utils.str2flags(cfg.getProperty(
				Config.VIBRATION_AND_TONE_SETTINGS, "15"), 0, 4);

		setMenu(new UIMenu(""));
		f_u = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN,
				Font.SIZE_SMALL);

		this.setFreezed(true);
		header = new UIHLayout(2);
		header.setGroup(false);
		header.setFocusable(false);
		connData = new UILabel("");
		presenceLabel = new UILabel(null, "");
		presenceLabel.setAnchorPoint(Graphics.RIGHT);
		header.insert(connData, 0, 100, UILayout.CONSTRAINT_PERCENTUAL);
		header.insert(presenceLabel, 1, 20, UILayout.CONSTRAINT_PERCENTUAL);
		connData.setFg_color(UIConfig.menu_title);
		presenceLabel.setFg_color(UIConfig.menu_title);
		header.setBg_color(UIUtils.colorize(UIConfig.header_bg, +20));

		this.append(header);
		UISeparator sep = new UISeparator(2);
		sep.setFg_color(0xCCCCCC);
		this.append(sep);
		this.setSelectedIndex(2);
		rosterAccordion = new UIAccordion();
		rosterAccordion.setMaxHeight(-1);
		rosterAccordion.setSepSize(1);
		rosterAccordion.setSepColor(0x00CCCCCC);

		//		int gBgColor = UIUtils.colorize(UIConfig.bg_color, -10);
		//		rosterAccordion.setLabelColor(gBgColor);
		//		rosterAccordion.setLabelGradientColor(UIUtils.colorize(gBgColor, -3));
		//		rosterAccordion.setLabelGradientSelectedColor(UIUtils.colorize(
		//				UIConfig.header_bg, -8));
		//		rosterAccordion.setLabelSelectedColor(UIConfig.header_bg);

		rosterAccordion.setModal(true);
		this.append(rosterAccordion);
		viewedAccordionIndex = this.indexOf(rosterAccordion);

		searchAccordion = new UIAccordion();
		searchAccordion.setMaxHeight(-1);
		searchAccordion.setSepSize(1);
		//		searchAccordion.setSepColor(0x00CCCCCC);
		//		searchAccordion.setLabelColor(gBgColor);
		//		searchAccordion.setLabelGradientColor(UIUtils.colorize(gBgColor, -3));
		//		searchAccordion.setLabelGradientSelectedColor(UIUtils.colorize(
		//				UIConfig.header_bg, -8));
		//		searchAccordion.setLabelSelectedColor(UIConfig.header_bg);
		searchAccordion.setModal(true);
		searchGroup = UIContactGroup.getContactGroup(searchString,
				searchAccordion, true);

		this.setFreezed(false);
		this.setDirty(true);
		this.askRepaint();

		this.rosterAccordion.setSelectedIndex(0);
		// section to detect if camera is available
		try {
			String supports = System.getProperty("video.snapshot.encodings");
			if (supports != null && supports.length() > 0) {
				this.cameraOn = true;
			} else {
				this.cameraOn = false;
			}
		} catch (Exception ioe) {
			this.cameraOn = false;
		}

		try {
			String supports = System.getProperty("audio.encodings");
			if (supports != null && supports.length() > 0) {
				this.micOn = true;
			} else {
				this.micOn = false;
			}
		} catch (Exception ioe) {
			this.micOn = false;
		}

		// setup the menu
		//		this.toggleMenus();

		UIItem[] contactMenuItems = new UIItem[] { UIContact.cmd_details,
				UIContact.cmd_groups, UIContact.cmd_delc,
				UIContact.cmd_resend_auth, UIContact.cmd_rerequest_auth,
				UIContact.cmd_active_sessions, UIContact.cmd_change_nick };
		for (int i = 0; i < contactMenuItems.length; i++) {
			UIItem array_element = contactMenuItems[i];
			array_element.setFocusable(true);
			array_element.setBg_color(UIConfig.menu_color);
		}

		try {
			InputStream is = getClass().getResourceAsStream("/beep.mp3");
			mp3Player = Manager.createPlayer(is, "audio/mpeg");
			mp3Player.realize();
			// get volume control for player and set volume to max
			VolumeControl vc = (VolumeControl) mp3Player
					.getControl("VolumeControl");
			if (vc != null) {
				vc.setLevel(volume);
			}
			mp3Player.prefetch();
		} catch (Exception e) {
			mp3Player = null;
		}
		this.updateScreen();
	}

	private void updateHeader() {
		int bytes[] = XMPPClient.getTraffic();
		String byteTrans = "";
		if (online) byteTrans = rm.getString(ResourceIDs.STR_TRAFFIC) + ": "
				+ (bytes[0] + bytes[1]) / 1000 + " Kb";
		else
			byteTrans = rm.getString(ResourceIDs.STR_OFFLINE).toUpperCase();
		if (byteTrans.compareTo(this.connData.getText()) != 0) {
			this.connData.setText(byteTrans);
		}
		if (sel_pattern.length() > 0) {
			this.connData.setText("sel: " + sel_pattern);
		}
		Contact myContact = xmppClient.getMyContact();
		if (myContact != null) {
			Image pimg = xmppClient.getPresenceIcon(myContact, null, myContact
					.getAvailability());
			// contacts with unread messages are always at the top
			UIGroup highLightGroup = getHighlightGroup(false);
			if (highLightGroup != null) {
				Enumeration ithGroup = rosterAccordion
						.getSubPanelElements(highLightGroup);
				if (ithGroup != null && ithGroup.hasMoreElements()) {
					UIContact firstContact = (UIContact) ithGroup.nextElement();
					if (firstContact.c.unread_msg()) {
						pimg = UIContact.img_msg;
					} else if (this.rosterAccordion.getOpenedLabel() != highLightGroup) {
						UIHLayout con = ((UIHLayout) highLightGroup
								.getContainer());
						UILabel imgLabel = ((UILabel) con.getItem(0));
						imgLabel.setImg(rosterAccordion.closeImage);
					}
				}
			}
			this.presenceLabel.setImg(pimg);
			int totalSize = 0;
			Enumeration en2 = UIContactGroup.uiGroups.elements();
			while (en2.hasMoreElements()) {
				UIItem ithLabel = (UIItem) en2.nextElement();
				if (ithLabel instanceof UIContactGroup
						&& ithLabel != highLightGroup) {
					UIAccordion currentAccordion = this.getAccordion();
					if (currentAccordion != null) {
						int size = currentAccordion.getPanelSize(ithLabel);
						totalSize += size;
					}
				}
			}
			String newTitle = rm.getString(ResourceIDs.STR_ROSTER_TITLE) + "("
					+ totalSize + ")";
			if (newTitle.compareTo(this.getTitle()) != 0) this
					.setTitle(newTitle);
		}
	}

	public static RosterScreen getInstance() {
		if (_instance == null) {
			_instance = new RosterScreen();
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
		this.replace(viewedAccordionIndex, rosterAccordion);
		filterContacts(false);
		this.setFreezed(false);
	}

	private void toggleMenus() {
		// add or remove commands only if there is a selected user

		UIMenu menu = getMenu();

		boolean needReopen = menu.isOpenedState();
		UIItem oldSelitem = null;
		if (needReopen) {
			oldSelitem = menu.getSelectedItem();
			menu.setOpenedState(false);
			this.askRepaint();
		}
		menu.clear();
		// #mdebug
//@		menu.append(cmd_debug);
		// #enddebug
		UIItem sepLayout = this.getSeparator();
		menu.append(cmd_state);
		menu.append(cmd_addc);
		if (this.mucJid != null) menu.append(cmd_mucs);
		menu.append(cmd_refresh_roster);
		menu.append(sepLayout);

		menu.append(cmd_album);
		menu.append(cmd_fts);
		if (this.cameraOn) menu.append(cmd_capture_img);
		if (this.micOn) menu.append(cmd_capture_aud);
		menu.append(sepLayout);

		menu.append(gateways_discovery);
		menu.append(toggle_offline);
		menu.append(cmd_options);
		menu.append(sepLayout);

		menu.append(cmd_about);
		menu.append(cmd_help);
		menu.append(sepLayout);

		if (online) menu.append(cmd_logout);
		else
			menu.append(cmd_login);
		menu.append(cmd_exit);

		sepLayout.setFocusable(false);

		UIItem[] contactMenuItems = new UIItem[] { UIContact.cmd_details,
				UIContact.cmd_groups, UIContact.cmd_delc,
				UIContact.cmd_resend_auth, UIContact.cmd_rerequest_auth,
				UIContact.cmd_active_sessions, UIContact.cmd_change_nick };
		for (int i = 0; i < contactMenuItems.length; i++)
			contactMenuItems[i].setBg_color(UIConfig.menu_color);

		if (needReopen) {
			menu.setSelectedItem(oldSelitem);
			this.keyPressed(UICanvas.MENU_RIGHT);
		}
	}

	public UIItem getSeparator() {
		UIHLayout sepLayout = new UIHLayout(3);
		sepLayout.setGroup(false);
		UIItem dummySep = new UISeparator(0);
		UISeparator sep = new UISeparator(1);
		sep.setFg_color(0x999999);
		sepLayout.insert(dummySep, 0, 5, UILayout.CONSTRAINT_PIXELS);
		if (UIConfig.menu_3d == true) {
			UIVLayout vlayout = new UIVLayout(2, 2);
			UISeparator sep2 = new UISeparator(1);
			sep2.setFg_color(0xFFFFFF);
			vlayout.insert(sep, 0, 1, UILayout.CONSTRAINT_PIXELS);
			vlayout.insert(sep2, 1, 1, UILayout.CONSTRAINT_PIXELS);
			vlayout.setGroup(false);
			sepLayout.insert(vlayout, 1, 100, UILayout.CONSTRAINT_PERCENTUAL);
		} else {
			sepLayout.insert(sep, 1, 100, UILayout.CONSTRAINT_PERCENTUAL);
		}
		sepLayout.insert(dummySep, 2, 5, UILayout.CONSTRAINT_PIXELS);
		return sepLayout;
	}

	/*
	*   Raised when a drag is made
	*/
	public void startDrag(UIItem draggedItem) {
		if (draggedItem instanceof UIContactGroup
				&& UIContactGroup.movingGroup == null) {
			((UIContactGroup) draggedItem).startMoving();
		}
	}

	/*
	*   Raised when a drag is made
	*/
	public void endDrag() {
		if (UIContactGroup.movingGroup != null) {
			UIContactGroup.movingGroup.stopMoving();
			this.askRepaint();
		}
	}

	/**
	 * Handle key events
	 * 
	 * @param kc
	 *            the pressed key
	 */
	public boolean keyPressed(int kc) {
		// #ifdef UI_DEBUG
		//@		Logger.log("Roster screen keypressed :" + kc);
		// #endif
		if (this.popupList.size() == 0
				& this.getMenu().isOpenedState() == false) {
			if (UICanvas.getInstance().hasQwerty()) {
				if ((kc >= 'A' && kc <= 'Z') || (kc >= 'a' && kc <= 'z')
						|| (kc >= '0' && kc <= '9')) {
					this.setFreezed(true);
					sel_pattern = sel_pattern + (char) kc;
					this.replace(viewedAccordionIndex, searchAccordion);
					filterContacts(true);
					this.setFreezed(false);
					if (this.rosterAccordion.getItems().size() > 0) {
						this.rosterAccordion.setSelectedIndex(0);
					}
					this.askRepaint();
					return true;
				}
			}

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
								+ Utils.itu_keys[key_num][sel_key_offset];
						this.replace(viewedAccordionIndex, searchAccordion);
						filterContacts(true);
					} else {
						// shifted key
						sel_key_offset += 1;
						if (sel_key_offset >= Utils.itu_keys[key_num].length) sel_key_offset = 0;
						sel_pattern = sel_pattern.substring(0, sel_pattern
								.length() - 1)
								+ Utils.itu_keys[key_num][sel_key_offset];
						filterContacts(false);
						this.rosterAccordion.setDirty(true);
					}
					sel_last_ts = t;
					this.setFreezed(false);
					if (this.rosterAccordion.getItems().size() > 0) {
						this.rosterAccordion.setSelectedIndex(0);
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

			// any of the delete characters
			if (kc == UICanvas.MENU_CANCEL || kc == 8) {
				if (sel_pattern.length() > 0) {
					cutPattern();
					return true;
				}
			}
			if (kc == Canvas.KEY_POUND
					&& this.getAccordion() == rosterAccordion) {
				UIContactGroup highLightGroup = getHighlightGroup(false);
				if (highLightGroup != null) {
					this.rosterAccordion.openLabel(highLightGroup);
				}
			}
		}
		return super.keyPressed(kc);
	}

	private void cutPattern() {
		this.setFreezed(true);
		sel_pattern = sel_pattern.substring(0, sel_pattern.length() - 1);
		if (sel_pattern.length() > 0) this.replace(viewedAccordionIndex,
				searchAccordion);
		else if (sel_pattern.length() == 0) this.replace(viewedAccordionIndex,
				rosterAccordion);
		filterContacts(false);
		this.setFreezed(false);
		askRepaint();
	}

	public void itemAction(UIItem item) {
		if (item instanceof UIRosterItem) {
			((UIRosterItem) item).executeAction();
		}
	}

	/**
	 * Handle a command
	 * 
	 * @param c
	 *            the selected command
	 * @param d
	 *            the object on which the command has been invoked
	 * 
	 */
	public void menuAction(UIMenu menu, UIItem c) {
		if (c == UIContactGroup.groupActionsLabel) {
			UIContactGroup selGroup = getSelectedUIGroup();
			if (selGroup != null) selGroup.openGroupMenu();
		} else if (c == UIContactGroup.openLabel) {
			UIGroup selGroup = getSelectedUIGroup();
			if (selGroup != null) {
				this.getAccordion().openLabel(selGroup);
			}
		} else if (c == UIContactGroup.moveLabel) {
			UIContactGroup selGroup = getSelectedUIGroup();
			if (selGroup != null) {
				selGroup.startMoving();
			}
		} else if (c == UIContact.actionsLabel) {
			this.openContactMenu();
		} else if (c == cmd_logout) {
			mucJid = null;
			xmppClient.setPresence(Contact.AV_UNAVAILABLE, null);
		} else if (c == cmd_login) {
			RegisterScreen rs = RegisterScreen.getInstance();
			UICanvas.getInstance().open(rs, true);
			if (rs.contains(rs.logLayout)) rs.login(true);
		} else if (c == cmd_exit) {
			LampiroMidlet.exit();
		} else if (c == this.deleteQuestion) {
			Contact contact = (Contact) this.deleteQuestion.getStatus();
			_removeContact(contact);
			xmppClient.getRoster().unsubscribeContact(contact);
		} else if (c == UIContact.cmd_delc) {
			this.removePopup(UIContact.optionsMenu);
			Contact cont = getSelectedContact();
			deleteQuestion = new UILabel(rm
					.getString(ResourceIDs.STR_DELETE_CONTACT)
					+ ": " + cont.jid + "?");
			UIMenu deleteMenu = UIUtils.easyMenu(rm
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
			deleteQuestion.setStatus(cont);
			this.addPopup(deleteMenu);
		} else if (c == cmd_help) {
			boolean oldFreezed = this.isFreezed();
			this.setFreezed(true);
			String help = rm.getString(ResourceIDs.STR_HELP_TEXT);
			help = help.replace('<', '\n');
			UITextField helpField = new UITextField("", help, help.length(),
					TextField.UNEDITABLE);
			helpField.setWrappable(true);
			helpField.setExpandable(false);
			UIMenu helpMenu = UIUtils.easyMenu(rm
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
		} else if (c == cmd_album) {
			AlbumScreen alb = AlbumScreen.getInstance();
			// I use here the currentScreen since it could be 
			// even different from this
			UICanvas.getInstance().open(alb, true,
					UICanvas.getInstance().getCurrentScreen());
		} else if (c == cmd_fts) {
			FTScreen fts = FTScreen.getInstance();
			UICanvas.getInstance().open(fts, true, this);
		} else if (c == cmd_addc) {
			AddContactScreen acs = new AddContactScreen();
			UICanvas.getInstance().open(acs, true, this);
		} else if (c == UIContact.cmd_send) {
			Contact user = getSelectedContact();
			String fullJid = this.getActionJid();
			MessageComposerScreen ms = new MessageComposerScreen(user, fullJid,
					MessageComposerScreen.MESSAGE);
			UICanvas.getInstance().open(ms, true, this);
			this.removePopup(UIContact.optionsMenu);
		} else if (c == UIContactGroup.groupMessage) {
			UIContactGroup group = getSelectedUIGroup();
			if (group == null) return;
			GrpMessageComposerScreen ms = new GrpMessageComposerScreen(group,
					MessageComposerScreen.MESSAGE);
			UICanvas.getInstance().open(ms, true, this);
			this.removePopup(UIContact.optionsMenu);
		} else if (c == UIContactGroup.chgGroupName) {
			UIContactGroup group = getSelectedUIGroup();
			if (group == null) return;
			ChgGroupNameScreen scr = new ChgGroupNameScreen(group);
			UICanvas.getInstance().open(scr, true, this);
			this.removePopup(UIContact.optionsMenu);
		} else if (c == gateways_discovery) {
			UICanvas.getInstance().open(new GatewayScreen(null), true, this);
		} else if (c == toggle_offline) {
			this.show_offlines = !this.show_offlines;
			this.setDirty(true);
			this.setFreezed(true);
			Enumeration en = xmppClient.roster.contacts.elements();
			while (en.hasMoreElements()) {
				Contact ithContact = (Contact) en.nextElement();
				this._updateContact(ithContact, Contact.CH_STATUS);
			}
			if (show_offlines) toggle_offline.setText(rm
					.getString(ResourceIDs.STR_HIDE_OFFLINE));
			else
				toggle_offline.setText(rm
						.getString(ResourceIDs.STR_SHOW_OFFLINE));
			this.setFreezed(false);
			this.askRepaint();
		} else if (c == UIContact.cmd_chat) {
			String fullJid = this.getActionJid();
			chatWithSelected(fullJid);
			this.removePopup(UIContact.optionsMenu);
			// } else if (c == cmd_reload) {
			// Roster.getInstance().updateRoster();
		} else if (c == UIContact.cmd_change_nick) {
			AddContactScreen acs = new AddContactScreen();
			Contact cont = this.getSelectedContact();
			acs.changeNickSetup(cont.jid);
			UICanvas.getInstance().open(acs, true, this);
			this.removePopup(UIContact.optionsMenu);
		} else if (c == cmd_state) {
			StatusScreen ssc = new StatusScreen();
			UICanvas.getInstance().open(ssc, true, this);
			// #mdebug
//@		} else if (c == cmd_debug) {
//@			DebugScreen debugScreen = new DebugScreen();
//@			UICanvas.getInstance().open(debugScreen, true);
			// #enddebug
		} else if (c == UIContact.cmd_contact_capture_aud) {
			String fullJid = this.getActionJid();
			this.captureMedia(fullJid, Config.AUDIO_TYPE);
			this.removePopup(UIContact.optionsMenu);
		} else if (c == UIContact.cmd_contact_capture_img) {
			String fullJid = this.getActionJid();
			captureMedia(fullJid, Config.IMG_TYPE);
			this.removePopup(UIContact.optionsMenu);
		} else if (c == cmd_capture_aud) {
			this.captureMedia(null, Config.AUDIO_TYPE);
		} else if (c == cmd_refresh_roster) {
			try {
				removeAllContacts();
				TimerTask tt = new TimerTask() {
					public void run() {
						xmppClient.roster.cleanAndRetrieve();
					}
				};
				Utils.tasks.schedule(tt, 200);
			} catch (Exception e) {
				// #mdebug
//@				e.printStackTrace();
				// #enddebug
			}
		} else if (c == cmd_capture_img) {
			captureMedia(null, Config.IMG_TYPE);
		} else if (c == UIContact.cmd_querycmd) {
			this.removePopup(UIContact.optionsMenu);
			Contact usr = getSelectedContact();
			String fullJid = this.getActionJid();
			usr.cmdlist = null;
			queryCmd(fullJid, this);
		} else if (c == UIContact.cmd_send_file) {
			String fullJid = this.getActionJid();
			AlbumScreen alb = AlbumScreen.getInstance(fullJid);
			UICanvas.getInstance().open(alb, true);
			this.removePopup(UIContact.optionsMenu);
		} else if (c == cmd_mucs || c == UIContactGroup.createMUC) {
			HandleMucScreen cms = new HandleMucScreen(null, this.mucJid,
					HMC_CONSTANTS.CREATE | HMC_CONSTANTS.JOIN_NOW);
			UICanvas.getInstance().open(cms, true, this);
			this.removePopup(UIContact.optionsMenu);
		} else if (c == cmd_about) {
			AboutScreen as = new AboutScreen();
			// I use here the currentScreen since it could be 
			// even different from this
			UICanvas.getInstance().open(as, true,
					UICanvas.getInstance().getCurrentScreen());
		} else if (c == cmd_options) {
			OptionsScreen os = new OptionsScreen();
			UICanvas.getInstance().open(os, true,
					UICanvas.getInstance().getCurrentScreen());
		} else if (c == UIContact.cmd_tasks) {
			this.removePopup(UIContact.optionsMenu);
			Contact usr = getSelectedContact();
			Task tasks[] = usr.getTasks();
			if (tasks.length == 1) {
				// #ifdef UI
				tasks[0].display();
				// #endif
			} else if (tasks.length > 1) {
				TaskListScreen taskListScreen = new TaskListScreen(tasks);
				UICanvas.getInstance().open(taskListScreen, true, this);
			}
		} else if (c == UIContact.cmd_details) {
			Contact cont = this.getSelectedContact();
			if (cont != null) {
				ContactInfoScreen ci = new ContactInfoScreen(cont);
				UICanvas.getInstance().open(ci, true, this);
			}
			this.removePopup(UIContact.optionsMenu);
		} else if (c == UIContact.cmd_resend_auth) {
			Contact cont = this.getSelectedContact();
			if (cont != null) {
				Presence p = new Presence(cont.jid, Presence.T_SUBSCRIBED,
						null, null, -1);
				xmppClient.sendPacket(p);
			}
			this.removePopup(UIContact.optionsMenu);
		} else if (c == UIContact.cmd_rerequest_auth) {
			Contact cont = this.getSelectedContact();
			if (cont != null) {
				Presence p = new Presence(cont.jid, Presence.T_SUBSCRIBE, null,
						null, -1);
				if (cont.name != null && cont.name.length() > 0) {
					Element nick = new Element(XMPPClient.NS_NICK, "nick");
					nick.addText(cont.name);
					p.addElement(nick);
				}
				xmppClient.sendPacket(p);
			}
			this.removePopup(UIContact.optionsMenu);
		} else if (c == UIContact.cmd_groups) {
			Contact cont = this.getSelectedContact();
			if (cont != null) {
				GroupsScreen ci = new GroupsScreen(cont);
				UICanvas.getInstance().open(ci, true,
						RosterScreen.getInstance());
			}
			this.removePopup(UIContact.optionsMenu);
		} else if (c == UIContact.cmd_close_muc) {
			UIContact uiMuc = this.getSelectedUIContact();
			this.deleteMucMenu.setStatus(uiMuc);
			this.removePopup(UIContact.optionsMenu);
			this.addPopup(this.deleteMucMenu);
		} else if (c == this.cmdMucYes) {
			UIContact uiMuc = (UIContact) this.deleteMucMenu.getStatus();
			if (uiMuc != null) {
				MUC muc = (MUC) uiMuc.c;

				// saving without persistence is equivalent to removing!
				xmppClient.getRoster().saveMUC(muc, false, false, false);

				MUCScreen ms = (MUCScreen) chatScreenList.get(muc.jid);
				if (ms != null) ms.closeMe();
				Iq iq = new Iq(muc.jid, Iq.T_SET);
				Element query = iq
						.addElement(XMPPClient.NS_MUC_OWNER, Iq.QUERY);
				query.addElement(null, "destroy");
				xmppClient.getRoster().contacts.remove(muc.jid);
				this._removeContact(muc);
				xmppClient.sendIQ(iq, null);
			}
		} else if (c == UIContact.cmd_manage_muc) {
			this.removePopup(UIContact.optionsMenu);
			String actionJid = this.getActionJid();
			handleMuc(actionJid, this);
		} else if (c == UIContact.cmd_enter_muc) {
			String jid = (String) UIContact.cmd_enter_muc.getStatus();
			enterMuc(jid);
			this.removePopup(UIContact.optionsMenu);
		} else if (c == UIGateway.cmd_log) {
			String toString = (String) UIContact.optionsMenu.getStatus();
			Contact to = xmppClient.getRoster().getContactByJid(toString);
			Presence pres = null;
			// i am logged in change presence to unavailable
			if (to.getAllPresences() != null && to.getAllPresences().length > 0) {
				pres = new Presence();
				pres.setAttribute(Presence.ATT_TYPE, Presence.T_UNAVAILABLE);
			} else {
				pres = new Presence(xmppClient.getMyContact().getPresence(null));
			}
			pres.setAttribute(Stanza.ATT_TO, to.jid);
			xmppClient.sendPacket(pres);
			this.removePopup(UIContact.optionsMenu);
		} else if (c == UIGateway.cmd_remove_network) {
			String toString = (String) UIContact.optionsMenu.getStatus();
			final Contact to = xmppClient.getRoster().getContactByJid(toString);
			if (to == null) return;
			Iq iq = new Iq(to.jid, Iq.T_SET);
			iq.addElement(XMPPClient.IQ_REGISTER, Iq.QUERY).addElement(null,
					XMPPClient.REMOVE);
			xmppClient.sendIQ(iq, new RosterScreenListener(
					RosterScreenListener.UNREGISTER_TYPE));
			this.removePopup(UIContact.optionsMenu);
		} else if (c == UIContact.cmd_exit_muc) {
			//UIAccordion ca = this.getAccordion();
			//UIGroup selGroup = (UIGroup) ca.getOpenedLabel();
			Presence pres = new Presence();
			UIContact uiMuc = this.getSelectedUIContact();
			if (uiMuc != null) {
				MUC muc = (MUC) uiMuc.c;
				MUCScreen ms = (MUCScreen) chatScreenList.get(muc.jid);
				if (ms != null) {
					ms.closeMe();
					UICanvas.getInstance().open(this, true);
				}
				pres.setAttribute(Stanza.ATT_TO, muc.jid + "/"
						+ Contact.user(xmppClient.my_jid));
				pres.setAttribute(Stanza.ATT_TYPE, Presence.T_UNAVAILABLE);
				xmppClient.sendPacket(pres);
				//ca.removePanelItem(selGroup, uiMuc);
			}
			this.removePopup(UIContact.optionsMenu);
		}
	}

	public void handleMuc(String actionJid, UIScreen nextScreen) {
		String jid = Contact.userhost(actionJid);
		HandleMucScreen cms = new HandleMucScreen(jid, Contact
				.domain(actionJid), 0);
		cms.infoLabel.setText(rm.getString(ResourceIDs.STR_MANAGE_GC));
		cms.muc_name_field.setText(Contact.user(jid));
		UICanvas.getInstance().open(cms, true, nextScreen);
	}

	public void enterMuc(String jid) {
		Contact c = xmppClient.getRoster().getContactByJid(jid);
		if (c == null) return;
		MUC muc = (MUC) c;

		HandleMucScreen.createMUC(Contact.user(jid), Contact.domain(jid),
				muc.nick, muc.pwd, null, true, false);
	}

	public void queryCmd(String fullJid, UIScreen returnScreen) {
		Iq iq = new Iq(fullJid, Iq.T_GET);
		Element query = iq.addElement(XMPPClient.NS_IQ_DISCO_ITEMS, Iq.QUERY);
		query.setAttribute("node", "http://jabber.org/protocol/commands");
		AdHocCommandsHandler handler = new AdHocCommandsHandler(returnScreen);
		xmppClient.sendIQ(iq, handler);
	}

	private String getActionJid() {
		if (UIContact.optionsAccordion != null) {
			return (String) UIContact.optionsAccordion.getOpenedLabel()
					.getStatus();
		} else {
			return (String) UIContact.optionsMenu.getStatus();
		}
	}

	private void openContactMenu() {
		// TODO Auto-generated method stub
		// setting the options menu
		UIItem selContact = getAccordion().getSelectedItem();
		if (selContact == null || selContact instanceof UIContact == false) return;

		((UIContact) selContact).openContactMenu();
	}

	/*
	 * Query the server for the instant  messaging transports
	 */
	void queryDiscoItems(String serverAddress, int[] queriedGateways,
			int duration) {
		DiscoExplorer de = new DiscoExplorer(DiscoExplorer.ITEMS,
				serverAddress, queriedGateways);

		Iq iq = new Iq(serverAddress, Iq.T_GET);
		iq.addElement(XMPPClient.NS_IQ_DISCO_ITEMS, Iq.QUERY);
		xmppClient.sendIQ(iq, de, duration);
	}

	private void addGateway(Hashtable gws, String name, String from,
			String type, String serverJid) {
		//		Enumeration en = gateways.keys();
		//		while (en.hasMoreElements()) {
		//			String ithFrom = (String) en.nextElement();
		//			Object[] ithData = (Object[]) gateways.get(ithFrom);
		//			String ithType = (String) ithData[2];
		//			if (ithType.compareTo(type) == 0) {
		//				if (from.indexOf(Config.BLUENDO_SERVER) >= 0) return;
		//				else {
		//					gateways.remove(from);
		//					break;
		//				}
		//			}
		//		}
		gws.put(from, new Object[] { name, type, serverJid });
	}

	private UIContact getSelectedUIContact() {
		UIAccordion accordion = this.getAccordion();
		if (accordion == null) return null;
		UIItem selContact = accordion.getSelectedItem();
		if (selContact instanceof UIContact) return ((UIContact) selContact);
		else
			return null;
	}

	public Contact getSelectedContact() {
		UIContact selUIContact = this.getSelectedUIContact();
		if (selUIContact != null) return selUIContact.c;
		return null;
	}

	private UIContactGroup getSelectedUIGroup() {
		UIAccordion accordion = this.getAccordion();
		if (accordion == null) return null;
		UIItem selUIGroup = accordion.getSelectedItem();
		if (selUIGroup instanceof UIContactGroup) return (UIContactGroup) selUIGroup;
		else
			return null;
	}

	public UIAccordion getAccordion() {
		if (this.indexOf(rosterAccordion) > 0) return rosterAccordion;
		if (this.indexOf(searchAccordion) > 0) return searchAccordion;
		return null;
	}

	public void chatWithSelected(String preferredJid) {
		Contact user = getSelectedContact();
		if (user == null) return;
		chatWithContact(user, preferredJid);
	}

	void chatWithContact(Contact user, String preferredJid) {
		// add it to the highlight group
		UIContactGroup highLightGroup = getHighlightGroup(true);
		highLightGroup.updateContact(user, Contact.CH_STATUS);

		boolean unread_msg = user.unread_msg();
		if (preferredJid == null && user instanceof MUC == false) preferredJid = user
				.getPresence(null).getAttribute(Message.ATT_FROM);
		if (user instanceof MUC) preferredJid = user.jid;
		String key = (preferredJid != null ? preferredJid : user.jid);
		//		if (user.unread_msg() || force_chat) {
		ChatScreen ms = (ChatScreen) chatScreenList.get(key);
		boolean newScreen = false;
		if (ms == null) {
			newScreen = true;
			if (user instanceof MUC == true) {
				ms = new MUCScreen(user);
				chatScreenList.put(key, ms);
			} else {
				ms = new ChatScreen(user, preferredJid);
				chatScreenList.put(key, ms);
			}
		} else {
			if (user instanceof MUC == false) {
				ms.preferredResource = preferredJid;
			}
		}
		UICanvas.getInstance().open(ms, true, this);
		if (newScreen) {
			ms.fillScreen();
		}
		if (unread_msg == false && ms.getCurrent_conversation().size() == 0
				&& online) ms.openComposer();

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

	/**
	 * Update the (global) status of a contact and repaint the roster
	 * accordingly to the new situation
	 * 
	 * @param c
	 */
	public void updateContact(Contact c, int reason) {
		//return;
		// #ifdef TIMING
		// @ long t1 = System.currentTimeMillis();
		// #endif
		boolean needRepaint = false;
		try {
			UICanvas.lock();
			needRepaint = _updateContact(c, reason);
			if (needRepaint) {
				// rosterAccordion.setDirty(true);
				askRepaint();
				UIScreen cs = UICanvas.getInstance().getCurrentScreen();
				if (cs != this && cs != null) cs.askRepaint();
			}
			//			if (c != null && reason == Contact.CH_STATUS
			//					&& Contact.userhost(xmppClient.my_jid).equals(c.jid)
			//					&& c.getAllPresences() != null
			//					&& c.getAllPresences().length > 1) {
			//				checkPriority(c);
			//			}
			// #ifdef TIMING
			// @ System.out.println("New sort time: " + (System.currentTimeMillis()
			// @ // - t1));
			// #endif
		} catch (Exception e) {
			// #mdebug
//@			e.printStackTrace();
			// #enddebug
		} finally {
			UICanvas.unlock();
		}
	}

	public void registerError(Element e) {
		String errorText = rm.getString(ResourceIDs.STR_REG_ERROR);
		Element error = e.getChildByName(null, Iq.T_ERROR);
		if (error != null) {
			String code = error.getAttribute(XMPPClient.CODE);
			if (code != null) errorText += (": " + XMPPClient
					.getErrorString(code));
		}
		showAlert(AlertType.INFO, rm.getString(ResourceIDs.STR_REGISTER),
				errorText, null);
	}

	public void showNotify() {
	}

	//	private void checkPriority(Contact c) {
	//		try {
	//			Config cfg = Config.getInstance();
	//			int myPriority = xmppClient.getMyContact().getAllPresences()[0]
	//					.getPriority();
	//			String myResource = cfg.getProperty(Config.YUP_RESOURCE, "");
	//			Presence[] allPresences = c.getAllPresences();
	//			for (int i = 0; i < allPresences.length; i++) {
	//				Presence ithPresence = allPresences[i];
	//				String otherResource = Contact.resource(ithPresence
	//						.getAttribute(Presence.ATT_FROM));
	//				int otherPriority = ithPresence.getPriority();
	//				if (otherPriority > myPriority) {
	//					if (priorityChecked == false) {
	//						String toJid = c.jid + "/" + myResource;
	//						Message msg = new Message(toJid, Message.CHAT);
	//						msg.setAttribute(Message.ATT_FROM, toJid);
	//						msg.setBody(rm.getString(ResourceIDs.STR_LOW_PRIORITY)
	//								+ ". " + otherResource + ":" + otherPriority
	//								+ " " + myResource + ":" + myPriority);
	//						c.addMessageToHistory(toJid, msg);
	//						this._updateContact(c, Contact.CH_MESSAGE_NEW);
	//					}
	//					priorityChecked = true;
	//					return;
	//				}
	//			}
	//			priorityChecked = false;
	//		} catch (Exception e) {
	//			// #mdebug
	//			Logger.log("In checking priority:");
	//			e.printStackTrace();
	//			System.out.println(c.jid);
	//			// #enddebug
	//		}
	//	}

	private class UpdateTask extends TimerTask {
		public void run() {
			updateContact(null, -1);
		}
	}

	public boolean _updateContact(Contact c, int reason) {
		boolean retVal = false;
		if (c != null && reason >= 0) {
			// normal update
			updateQueue.addElement(new Object[] { c, new Integer(reason) });
		} else {
			// update called from timer task
			updateTime = 0;
		}

		if (System.currentTimeMillis() - updateTime < 1000) {
			if (updateScheduled == false) {
				updateScheduled = true;
				UICanvas.getTimer().schedule(new UpdateTask(), 1000);
			}
			return retVal;
		} else {
			// #mdebug
//@			//			long startTime = System.currentTimeMillis();
//@			//			Logger.log("Start update loop: " + startTime + " "
//@			//					+ updateQueue.size());
			// #enddebug
			updateScheduled = false;
			Enumeration en = this.updateQueue.elements();
			int count = 0;
			updateTime = System.currentTimeMillis();
			while (en.hasMoreElements()
					&& (System.currentTimeMillis() - updateTime) < 250) {
				Object[] uEl = (Object[]) en.nextElement();
				Contact ithC = (Contact) uEl[0];
				int ithReason = ((Integer) uEl[1]).intValue();
				retVal |= dequeueContact(ithC, ithReason);
				count++;
			}
			// only a part was updated force a new update after releasing lock
			if (count < updateQueue.size()) {
				Vector tempQueue = new Vector(updateQueue.size() - count);
				en = updateQueue.elements();
				while (count > 0) {
					en.nextElement();
					count--;
				}
				while (en.hasMoreElements()) {
					tempQueue.addElement(en.nextElement());
				}
				updateQueue = tempQueue;
				if (updateScheduled == false) {
					updateScheduled = true;
					UICanvas.getTimer().schedule(new UpdateTask(), 250);
				}
			} else {
				this.updateQueue.removeAllElements();
				this.updateQueue.trimToSize();
				updateTime = System.currentTimeMillis();
			}
			// #mdebug
//@			//			long endTime = System.currentTimeMillis();
//@			//			Logger.log("End update loop: " + endTime + " "
//@			//					+ (endTime - startTime));
			// #enddebug
		}

		return retVal;
	}

	/**
	 * @param c
	 * @param reason
	 * @return
	 */
	private boolean dequeueContact(Contact c, int reason) {
		boolean needRepaint = false;
		// A special check for a gateway contact
		Object gatewayData = xmppClient.getRoster().registeredGateways
				.get(c.jid);
		if (gatewayData != null) {
			UIGatewayGroup networkGroup = UIGatewayGroup.getGroup(
					rosterAccordion, true);
			needRepaint |= networkGroup.updateContact(c, reason);
		}
		// if a status is changed i must update the headers of 
		// all the chatscreens and not mucscreens and repaint only the painted screen
		if (reason == Contact.CH_STATUS) {
			Enumeration en = chatScreenList.keys();
			while (en.hasMoreElements()) {
				String ithJid = (String) en.nextElement();
				if (Contact.userhost(ithJid).compareTo(c.jid) == 0) {
					ChatScreen contactScreen = ((ChatScreen) chatScreenList
							.get(ithJid));
					contactScreen.updateResource();
					if (UICanvas.getInstance().getCurrentScreen() == contactScreen) needRepaint = true;
				}
			}
		}
		if (reason == Contact.CH_GROUP) {
			// if a contact has changed a group 
			// it must be removed from all the groups
			Enumeration en = UIGroup.uiGroups.keys();
			while (en.hasMoreElements()) {
				String ithGroup = (String) en.nextElement();
				if (ithGroup.equals(highLightString) == false) {
					UIGroup ithUIGroup = UIGroup.getGroup(ithGroup,
							rosterAccordion, true);
					if (ithUIGroup instanceof UIContactGroup) ((UIContactGroup) ithUIGroup)
							.removeContact(c);
				}
			}
			return true;
		}
		UIContactGroup ithGroupLabel = null;
		String[] contactGroups = c.getGroups();
		// first check if it is a "Special contact" like one of the services
		if (contactGroups.length == 0) {
			ithGroupLabel = UIContactGroup.getContactGroup(
					UIContactGroup.ungrouped, rosterAccordion, false);
			boolean allowReorganize = c.isVisible() || ithGroupLabel != null
					|| this.show_offlines;
			if (allowReorganize) {
				if (ithGroupLabel == null) ithGroupLabel = UIContactGroup
						.getContactGroup(UIContactGroup.ungrouped,
								rosterAccordion, true);
				needRepaint |= ithGroupLabel.updateContact(c, reason);
			}
		} else {
			for (int i = 0; i < contactGroups.length; i++) {
				String ithGroup = contactGroups[i];
				ithGroupLabel = UIContactGroup.getContactGroup(ithGroup,
						rosterAccordion, false);
				boolean allowReorganize = c.isVisible()
						|| ithGroupLabel != null || this.show_offlines;
				if (allowReorganize) {
					if (ithGroupLabel == null) ithGroupLabel = UIContactGroup
							.getContactGroup(ithGroup, rosterAccordion, true);
					needRepaint |= ithGroupLabel.updateContact(c, reason);
				}
			}
		}
		// The higlight is a special group; it needs some extra check 
		boolean highlightNewCheck = (reason == Contact.CH_MESSAGE_NEW || reason == Contact.CH_TASK_NEW);
		boolean highlightCheck = highlightNewCheck;
		// in the following cases i must check if the user is in the highlight group
		// and if the groups itself exists
		UIContactGroup highLightGroup = getHighlightGroup(false);
		highlightCheck |= ((reason == Contact.CH_MESSAGE_READ
				|| reason == Contact.CH_TASK_REMOVED || reason == Contact.CH_STATUS)
				&& highLightGroup != null && highLightGroup.getUIContact(c) != null);
		if (highlightCheck) {
			if (highLightGroup == null) highLightGroup = getHighlightGroup(true);
			boolean highLightRepaint = highLightGroup.updateContact(c, reason);
			// if the highlightGroup is closed update the message status;
			if (highlightNewCheck
					&& this.rosterAccordion.getOpenedLabel() != highLightGroup) {
				UIHLayout con = ((UIHLayout) highLightGroup.getContainer());
				UILabel imgLabel = ((UILabel) con.getItem(0));
				if (imgLabel.getImg() != UIContact.img_msg) {
					imgLabel.setImg(UIContact.img_msg);
					needRepaint = true;
				}
			}
			needRepaint |= highLightRepaint;
		}
		return needRepaint;
	}

	public boolean askRepaint() {
		boolean retVal = true;
		try {
			this.updateHeader();
			retVal = super.askRepaint();
		} catch (Exception e) {
			// #mdebug
//@			e.printStackTrace();
			// #enddebug
		}
		return retVal;
	}

	private boolean filtering = false;

	/*
	 * Filter the contacts by means of the letters
	 * selected by user in the rosterscreen.
	 * 
	 * @param reuse If false restart filtering from scratch
	 * @return true is something has changed
	 */
	public boolean filterContacts(boolean reuse) {
		// sometimes i must repaint here and sometimes not
		// i hence must save the freeze status
		boolean needRepaint = false;
		boolean oldFreezed = this.isFreezed();
		setFiltering(true);
		this.setFreezed(true);

		try {
			int oldSelectedIndex = searchAccordion.getSelectedIndex();
			// it could have been removed hence check for it again
			searchGroup = UIContactGroup.getContactGroup(searchString,
					searchAccordion, true);
			if (sel_pattern.length() == 0) {
				Enumeration en = searchGroup.contacts.elements();
				while (en.hasMoreElements()) {
					UIItem ithUicontact = (UIItem) en.nextElement();
					ithUicontact.setContainer(rosterAccordion);
				}
			}
			searchGroup.contacts.clear();
			searchAccordion.clearPanel(searchGroup);
			if (sel_pattern.length() > 0) {
				// cannot use enumeration !!!
				Vector goodContacts = new Vector();
				Enumeration en = UIContactGroup.uiGroups.elements();
				while (en.hasMoreElements()) {
					UIGroup ithGroup = (UIGroup) en.nextElement();
					if (ithGroup instanceof UIContactGroup) {
						Enumeration en2 = ((UIContactGroup) ithGroup).contacts
								.elements();
						while (en2.hasMoreElements()) {
							UIContact uic = (UIContact) en2.nextElement();
							String contactName = uic.getText().toLowerCase();
							if (contactName.indexOf(sel_pattern) == 0) {
								goodContacts.addElement(uic);
							}
						}
					}
				}
				en = goodContacts.elements();
				while (en.hasMoreElements()) {
					UIContact contactToReorganize = ((UIContact) en
							.nextElement());
					try {
						searchGroup = UIContactGroup.getContactGroup(
								searchString, searchAccordion, true);
						if (searchGroup.contacts
								.containsKey(contactToReorganize.c) == false) {
							searchGroup.contacts.put(contactToReorganize.c,
									contactToReorganize);
							searchAccordion.insertPanelItem(searchGroup,
									contactToReorganize, 0);
							searchGroup.updateContact(contactToReorganize.c,
									Contact.CH_STATUS);
						}
					} catch (Exception e) {
						// #mdebug
//@						Logger.log("In filtering contacts :");
//@						e.printStackTrace();
//@						System.out.println(contactToReorganize.c.jid);
						// #enddebug
					}
				}
				this.searchAccordion.openLabel(searchGroup);
				needRepaint = true;
			}
			if (oldSelectedIndex != -1) searchAccordion
					.setSelectedIndex(oldSelectedIndex);
		} catch (Exception e) {
			// #mdebug
//@			// nothing special but since this is done 
//@			// within a lock / freeze it is mandatory to avoid 
//@			// raising exception
//@			Logger.log("In filtering contacts :");
//@			e.printStackTrace();
			// #enddebug
		}
		this.setFreezed(oldFreezed);
		setFiltering(false);
		return needRepaint;
	}

	public void removeContact(Contact c, boolean unsubscribed) {
		try {
			UICanvas.lock();
			_removeContact(c);
		} catch (Exception e) {
			// #mdebug
//@			e.printStackTrace();
//@			Logger.log("In removing contact: " + e.getClass());
			// #enddebug
		} finally {
			UICanvas.unlock();
		}
	}

	public void _removeContact(Contact c) {
		String[] cGroups = c.getGroups();
		for (int i = 0; i < cGroups.length; i++) {
			String ithGroup = cGroups[i];
			UIContactGroup ithUIGroup = UIContactGroup.getContactGroup(
					ithGroup, rosterAccordion, false);
			if (ithUIGroup != null) ithUIGroup.removeContact(c);
		}
		if (cGroups.length == 0) {
			UIContactGroup ithUIGroup = UIContactGroup.getContactGroup(
					UIContactGroup.ungrouped, rosterAccordion, false);
			if (ithUIGroup != null) ithUIGroup.removeContact(c);
		}
		UIContactGroup highLightGroup = getHighlightGroup(false);
		if (highLightGroup != null) highLightGroup.removeContact(c);

		Presence[] pres = c.getAllPresences();
		for (int l = 0; pres != null && l < pres.length; l++)
			chatScreenList.remove(pres[l]);
		chatScreenList.remove(c.jid);

		if (isFiltering() == false) filterContacts(true);

		askRepaint();
	}

	public void removeAllContacts() {
		//		while (chatScreenList.size() > 0) {
		//			Object key = chatScreenList.keys().nextElement();
		//			ChatScreen cs = (ChatScreen) chatScreenList.get(key);
		//			cs.updateConversation();
		//			UICanvas.getInstance().close(cs);
		//			chatScreenList.remove(key);
		//		}
		this.rosterAccordion.removeAllItems();
		this.searchAccordion.removeAllItems();
		UIContactGroup.uiGroups.clear();
	}

	/*
	 * I want to recompute it every time
	 */
	public static boolean isOnline() {
		return online;
	}

	public void packetReceived(Element e) {
		String userHost = Contact.userhost(e.getAttribute(Iq.ATT_FROM));

		// a presence has arrived for a MUC
		if (e.name.compareTo("presence") == 0) {
			String type = e.getAttribute("type");
			MUC presenceMUC = (MUC) xmppClient.getRoster().getContactByJid(
					userHost);
			if (presenceMUC != null) MUCScreen.handlePresence(presenceMUC, e,
					type);
			return;
		}

		// Dispatch the topic to the correct MUC
		if (e.name.compareTo("message") == 0) {
			Element subject = e.getChildByName(null, "subject");
			Contact muc = xmppClient.roster.getContactByJid(userHost);
			if (muc != null) {
				try {
					UICanvas.lock();
					MUCScreen mucScreen = (MUCScreen) chatScreenList
							.get(muc.jid);
					if (subject != null && muc instanceof MUC) {
						((MUC) muc).topic = subject.getText();
						if (mucScreen != null) {
							UILabel mucName = (UILabel) mucScreen.header
									.getItem(0);
							mucName.setText(rm.getString(ResourceIDs.STR_TOPIC)
									+ ": " + subject.getText());
							mucScreen.askRepaint();
						}
						return;
					}
				} catch (Exception ex) {
					// #mdebug
//@					ex.printStackTrace();
//@					Logger.log("In rosterscreen packet received: "
//@							+ e.getClass());
					// #enddebug
				} finally {
					UICanvas.unlock();
				}
			}
		}

		String invitedMuc = e.getAttribute(Message.ATT_FROM);
		String mucName = Contact.user(invitedMuc);
		String inviterName = e.getChildByName(XMPPClient.NS_MUC_USER, "x")
				.getChildByName(null, "invite").getAttribute(Message.ATT_FROM);
		if (inviterName == null) return;
		Contact c = xmppClient.roster.getContactByJid(inviterName);
		String printableName = "";
		if (c != null) printableName = c.getPrintableName();
		else
			printableName = inviterName;

		try {
			UICanvas.lock();
			HandleMucScreen cms = new HandleMucScreen(null, Contact
					.domain(invitedMuc), HMC_CONSTANTS.JOIN_NOW);
			cms.infoLabel.setText(rm
					.getString(ResourceIDs.STR_GROUP_CHAT_INVITATION)
					+ " " + printableName + "?");
			cms.muc_name_field.setText(mucName);
			UICanvas.getInstance().open(cms, true, this);
		} catch (Exception ex) {
			// #mdebug
//@			ex.printStackTrace();
//@			Logger.log("In roster packet received: " + e.getClass());
			// #enddebug
		} finally {
			UICanvas.unlock();
		}
	}

	/**
	 * @return the chatScreenList
	 */
	public static Hashtable getChatScreenList() {
		return chatScreenList;
	}

	public void dataReceived(byte[] data, String fileName, String fileDesc,
			OpenListener ftrp) {
		UICanvas.lock();
		try {
			UIScreen imScreen = new ShowMMScreen(data, fileName, fileDesc);
			UICanvas.getInstance().open(imScreen, true);
		} catch (Exception e) {
			String textAck = rm.getString(ResourceIDs.STR_DECODE_ERROR) + " "
					+ fileName;
			ftNotification(true, textAck, textAck);
		} finally {
			UICanvas.unlock();
		}
		FTScreen.ftFinished(ftrp);
	}

	public void reqFT(String contactName, OpenListener ftrp) {
		playSmartTone();
		try {
			UICanvas.lock();
			Contact c = xmppClient.getRoster().getContactByJid(contactName);
			if (c == null) {
				ftrp.answerFT(false);
				return;
			}
			FileReceiveScreen frs = new FileReceiveScreen(c, ftrp);
			UICanvas.getInstance().open(frs, true);
		} catch (Exception ex) {
			// #mdebug
//@			ex.printStackTrace();
//@			Logger.log("In asking ft accept: " + ex.getClass());
			// #enddebug
		} finally {
			UICanvas.unlock();
		}
	}

	public void fileAcceptance(Contact c, String fileName, boolean accept,
			FTSender sender) {
		FTScreen.ftAccept(sender, accept);
	}

	/*
	 * A helper function deputed to notify the user about file transfer status
	 * 
	 * @param accept
	 *            A flag to identify if it is an "ack" or a "nack"
	 *            
	 * @param textAck
	 *            The text to show if it is an "ack"
	 *            
	 * @param textAck
	 *            The text to show if it is a "nack"
	 *            
	 */
	private void ftNotification(boolean accept, String textAck, String textNack) {
		String title = rm.getString(ResourceIDs.STR_FT);
		UILabel acceptLabel = null;
		if (accept == true) acceptLabel = new UILabel(textAck);
		else
			acceptLabel = new UILabel(textNack);
		acceptLabel.setWrappable(true, this.getWidth() - 20);
		UIMenu acceptMenu = UIUtils.easyMenu(title, 10, 40,
				this.getWidth() - 20, acceptLabel);
		acceptMenu.cancelMenuString = "";
		acceptMenu.selectMenuString = rm.getString(ResourceIDs.STR_CONTINUE)
				.toUpperCase();
		UICanvas.getInstance().getCurrentScreen().addPopup(acceptMenu);
	}

	public void sessionInitated(Contact contactByJid, String fileName,
			FTSender sender) {
		FTScreen.addFileSend(sender, fileName);
	}

	public void fileSent(Contact c, String fileName, boolean success,
			FTSender sender) {
		FTScreen.ftFinished(sender);
		//		String textAck = fileName + " " + rm.getString(ResourceIDs.STR_FT_SENT)
		//				+ " " + c.getPrintableName();
		//		String textNack = fileName + " " + rm.getString(ResourceIDs.STR_FT_NOT)
		//				+ " " + rm.getString(ResourceIDs.STR_FT_SENT) + " "
		//				+ c.getPrintableName();
		//		ftNotification(success, textAck, textNack);
	}

	public void chunkSent(int sentBytes, int length, FTSender sender) {
		FTScreen.chunkTransferred(sentBytes, length, sender);

	}

	public void chunkReceived(int bytesReceived, int length,
			OpenListener openListener) {
		FTScreen.chunkTransferred(bytesReceived, length, openListener);

	}

	public void fileError(Contact c, String fileName, Element e) {
		Element error = e.getChildByName(null, Message.ERROR);
		if (error != null) {
			error.getChildByName(null, "feature-not-implemented");
		}

		String textAck = rm.getString(ResourceIDs.STR_ERROR) + ". "
				+ c.getPrintableName() + " "
				+ rm.getString(ResourceIDs.STR_FT_ERROR) + " " + fileName;

		UICanvas.lock();
		try {
			ftNotification(true, textAck, textAck);
		} catch (Exception ex) {
			// #mdebug
//@			ex.printStackTrace();
//@			Logger.log("In ft notify: " + ex.getClass());
			// #enddebug
		} finally {
			UICanvas.unlock();
		}
	}

	public void authenticated() {
		saveConvs();
		removeAllContacts();
		gatewaysServer = Config.DEFAULT_SERVER;

		// setup here all the needed listeners
		// registration to get notified of MUC invite
		EventQuery q = new EventQuery("message", null, null);
		EventQuery x = new EventQuery("x", new String[] { "xmlns" },
				new String[] { XMPPClient.NS_MUC_USER });
		q.child = x;
		EventQuery invite = new EventQuery("invite", null, null);
		x.child = invite;
		BasicXmlStream.addPacketListener(q, this);

		// registration to handle people left/join within MUCs
		q = new EventQuery("presence", null, null);
		x = new EventQuery("x", new String[] { "xmlns" },
				new String[] { XMPPClient.NS_MUC_USER });
		q.child = x;
		invite = new EventQuery("item", null, null);
		x.child = invite;
		BasicXmlStream.addPacketListener(q, this);

		// listen for all incoming messages with subject
		// this is used to set the topic for MUC
		// listen here and dispatch to the correct MUC
		/// XXX: something better ?
		q = new EventQuery("message", new String[] { Iq.ATT_TYPE },
				new String[] { Message.GROUPCHAT });
		q.child = new EventQuery("subject", null, null);
		BasicXmlStream.addPacketListener(q, this);

		this.ftReceiver = new FTReceiver((FTREventHandler) this);
		//= Contact.domain(XMPPClient.getInstance().my_jid);

		q = new EventQuery("message", null, null);
		EventQuery event = new EventQuery(XMPPClient.EVENT,
				new String[] { "xmlns" },
				new String[] { XMPPClient.NS_PUBSUB_EVENT });
		q.child = event;
		BasicXmlStream.addPacketListener(q, this.pubSubListener);

		firstLoginIntro();
		online = true;
		try {
			UICanvas.lock();
// #ifndef GLIDER
																								UICanvas.getInstance().open(this, true);
			// #endif
			this.toggleMenus();
		} catch (Exception ex) {
			// #mdebug
//@			ex.printStackTrace();
//@			Logger.log("In rosterx subscription: " + ex.getClass());
			// #enddebug
		} finally {
			UICanvas.unlock();
		}
	}


	private void firstLoginIntro() {
		Config cfg = Config.getInstance();
		// check first login
		if (Config.FALSE.equals(cfg.getProperty(Config.CLIENT_INITIALIZED))) {
			cfg.setProperty(Config.CLIENT_INITIALIZED, Config.TRUE);
			cfg.saveToStorage();

			// the gateway hint at startup is commented
			//			String hintText = rm.getString(ResourceIDs.STR_GATEWAY_HINT) + " "
			//					+ rm.getString(ResourceIDs.STR_SCARY_GMAIL);
			//			hintText = hintText.replace('<', '\n');
			//			UITextField gatewayHint = new UITextField("", hintText, hintText
			//					.length(), TextField.UNEDITABLE);
			//			gatewayHint.setWrappable(true);
			//			gatewayHint.setAutoUnexpand(false);
			//			gatewayHint.setExpandable(false);
			//			int canvasWidth = UICanvas.getInstance().getWidth() - 20;
			//			UIMenu firstLogin = UIUtils.easyMenu(rm
			//					.getString(ResourceIDs.STR_INSTRUCTIONS), 10, 20,
			//					canvasWidth, gatewayHint);
			//			firstLogin.setSelectedIndex(1);
			//			firstLogin.cancelMenuString = "";
			//			firstLogin.selectMenuString = rm
			//					.getString(ResourceIDs.STR_CONTINUE).toUpperCase();
			//			UIHLayout gatewayLayout = new UIHLayout(5);
			//			Vector images = new Vector(5);
			//			images.addElement(UICanvas.getUIImage("/transport/msn.png"));
			//			images.addElement(UICanvas.getUIImage("/transport/icq.png"));
			//			images.addElement(UICanvas.getUIImage("/transport/aim.png"));
			//			images.addElement(UICanvas.getUIImage("/transport/yahoo.png"));
			//			images.addElement(UICanvas.getUIImage("/transport/transport.png"));
			//			Enumeration en = images.elements();
			//			int i = 0;
			//			while (en.hasMoreElements()) {
			//				UILabel ithLabel = new UILabel((Image) en.nextElement());
			//				ithLabel.setAnchorPoint(Graphics.HCENTER);
			//				gatewayLayout.insert(ithLabel, i, 25,
			//						UILayout.CONSTRAINT_PERCENTUAL);
			//				i++;
			//			}
			//
			//			firstLogin.replace(0, gatewayLayout);
			//			this.addPopup(firstLogin);
			//			//			UICanvas.getInstance().open(this, true);
			//			this.askRepaint();
			//			gatewayHint.expand();
		}
	}

	public void rosterXsubscription(Element e) {
		try {
			UICanvas.lock();

			Contact fromContact = xmppClient.getRoster().getContactByJid(
					e.getAttribute(Iq.ATT_FROM));
			if (fromContact == null) { return; }
			Element x = e.getChildByName(null, "x");
			Element[] items = x.getChildrenByName(null, "item");
			SubscribeScreen sScreen = SubscribeScreen
					.getComponentSubscription(fromContact);
			boolean rosterModified = false;
			for (int i = 0; i < items.length; i++) {
				Element ithItem = items[i];
				String name = ithItem.getAttribute("name");
				String jid = ithItem.getAttribute("jid");
				String group = ithItem.getChildByName(null, "group").getText();
				String groups[] = null;
				if (group != null && group.length() > 0) {
					groups = new String[] { group };
				}
				String action = ithItem.getAttribute(XMPPClient.ACTION);
				Contact c = xmppClient.getRoster().getContactByJid(
						Contact.userhost(jid));
				if (c != null && action.compareTo("delete") == 0) rosterModified |= sScreen
						.addSubscription(c, SubscribeScreen.DELETE);
				// if I have a previous presence check to remove it and create a new one!		
				if (action.compareTo("add") == 0) {
					if (c == null
							|| !(c.subscription.equals(Contact.SUB_BOTH) || c.subscription
									.equals(Contact.SUB_TO))) {
						if (c != null) {
							xmppClient.getRoster().contacts.remove(c);
							this._removeContact(c);
						}
						c = new Contact(jid, name, null, groups);
						rosterModified |= sScreen.addSubscription(c,
								SubscribeScreen.ADD);
					}

				}
			}

			if (e.name.compareTo(Iq.IQ) == 0) {
				Element answer = e;
				e.setAttributes(new String[] { Iq.ATT_TO, Iq.ATT_FROM,
						Iq.ATT_TYPE }, new String[] {
						e.getAttribute(Iq.ATT_FROM), e.getAttribute(Iq.ATT_TO),
						Iq.T_RESULT });
				e.removeAllElements();
				xmppClient.sendPacket(answer);
				//System.out.println(new String(answer.toXml()));
			}
			Config cfg = Config.getInstance();
			String agString = cfg.getProperty(Config.ACCEPTED_GATEWAYS, "");
			byte[] agb = Utils.getBytesUtf8(agString);
			Element agEl = null;
			Element parsedAgb = null;
			try {
				parsedAgb = BProcessor.parse(agb);
			} catch (Exception ex) {

			}
			agEl = (agb != null && agb.length > 0 && parsedAgb != null ? parsedAgb
					: new Element("", "agb"));
			boolean accepted = false;
			for (int i = 0; i < agEl.getChildren().length; i++) {
				if (agEl.getChildren()[i].getText().equals(fromContact.jid)) {
					accepted = true;
					break;
				}
			}

			// I must also check if this is a "server trusted gateway"
			Hashtable gwc = xmppClient.gatewayConfig;
			if (accepted == false && gwc != null) {
				Object[] trusted = (Object[]) gwc.get("trusted");
				for (int i = 0; trusted != null && i < trusted.length; i++) {
					String str = (String) trusted[i];
					if (fromContact.jid.equals(str)) {
						accepted = true;
						break;
					}
				}
			}

			if (accepted) {
				sScreen.handlePushes();
			} else if (rosterModified) {
				// if the SubscribeScreen has been modified
				// show it otherwise forget it: it had nothing to show
				UICanvas.getInstance().open(sScreen, true);
			}

		} catch (Exception ex) {
			// #mdebug
//@			ex.printStackTrace();
//@			Logger.log("In rosterx subscription: " + ex.getClass());
			// #enddebug
		} finally {
			UICanvas.unlock();
		}
	}

	public void playSmartTone() {
		UICanvas.lock();
		try {
			if (System.currentTimeMillis() - playTime < 2000) {
				if (scheduledPlay == false) {
					scheduledPlay = true;
					UICanvas.getTimer().schedule(new TimerTask() {
						public void run() {
							playSmartTone();
						}
					}, 2000);
				}
				return;
			}

			playTime = System.currentTimeMillis();
			scheduledPlay = false;
			boolean shown = UICanvas.getInstance().getCurrentScreen() == RosterScreen
					.getInstance();
			boolean vibrate = (shown && play_flags[1])
					|| ((!shown) && play_flags[0]);
			boolean play = (shown && play_flags[3])
					|| ((!shown) && play_flags[2]);
			// #mdebug
//@			Logger.log(vibrate + "" + play);
			// #enddebug
			if (vibrate) {
				LampiroMidlet.disp.vibrate(200);
			}
			if (play) {
				if (mp3Player != null) {
					try {
						mp3Player.start();
						return;
					} catch (Exception e) {
						// #mdebug
//@						e.printStackTrace();
//@						Logger.log("In playing file 1: " + e.getClass());
						// #enddebug
					}
				}
				// if the mp2 fails to sound at least play a tone
				Manager.playTone(85, 100, volume);
				Manager.playTone(86, 100, volume);
				Manager.playTone(88, 300, volume);
			}
		} catch (Exception e2) {
			// #mdebug
//@			e2.printStackTrace();
//@			Logger.log("In playing file 3: " + e2.getClass());
			// #enddebug
		} finally {
			UICanvas.unlock();
		}
	}

	public void askSubscription(Contact u) {
		try {
			UICanvas.lock();
			SubscribeScreen scs = SubscribeScreen.getUserSubscription();
			scs.addSubscription(u, SubscribeScreen.ADD);
			UICanvas.getInstance().open(scs, true);
		} catch (Exception e) {
			// #mdebug
//@			Logger.log("In asking subscription:" + e.getClass());
//@			e.printStackTrace();
			// #enddebug
		} finally {
			UICanvas.unlock();
		}
	}

	private void loadConvs() {
		if (savedConvs == null) return;
		Enumeration oldConvs = savedConvs.elements();
		while (oldConvs.hasMoreElements()) {
			Object[] ithConv = (Object[]) oldConvs.nextElement();
			String jid = (String) ithConv[0];
			String resource = Contact.resource(jid);
			Contact c = xmppClient.getRoster().getContactByJid(jid);
			Vector messages = (Vector) ithConv[1];
			Enumeration msgEn = messages.elements();
			while (c != null && msgEn.hasMoreElements()) {
				String[] data = (String[]) msgEn.nextElement();
				Message msg = new Message(data[0], data[3]);
				msg.setAttribute(Message.ATT_FROM, data[0]);
				msg.setBody(rm.getString(ResourceIDs.STR_OLD_MESSAGE) + "["
						+ data[2] + "]: " + data[1]);
				c.addMessageToHistory(resource, msg);
				this._updateContact(c, Contact.CH_MESSAGE_NEW);
			}
		}
		this.savedConvs = null;
	}

	private void saveConvs() {
		Vector tempSavedConvs = new Vector();
		Enumeration contacts = getHighlightGroup(true).contacts.keys();
		while (contacts.hasMoreElements()) {
			Contact c = (Contact) contacts.nextElement();
			Vector ithConv = c.getAllConvs();
			Enumeration convs = ithConv.elements();
			while (convs.hasMoreElements()) {
				Object[] convCouple = (Object[]) convs.nextElement();
				if (((Vector) convCouple[1]).size() > 0) {
					tempSavedConvs.addElement(convCouple);
				}
			}
		}
		this.savedConvs = tempSavedConvs;
	}

	public void connectionLost() {
		try {
			UICanvas.lock();
			_connectionLost();
		} catch (Exception e) {
			// #mdebug
//@			e.printStackTrace();
//@			Logger.log("In connection Lost : " + e.getClass());
			// #enddebug
		} finally {
			UICanvas.unlock();
		}
	}

	/**
	 * 
	 */
	void _connectionLost() {
		online = false;
		UIContactGroup hg = getHighlightGroup(false);
		removeAllContacts();
		this.askRepaint();
		if (hg != null) {
			Enumeration en = hg.contacts.keys();
			while (en.hasMoreElements()) {
				Contact c = (Contact) en.nextElement();
				c.resetAllPresences();
				if (c.pending_tasks || c.unread_msg()) getHighlightGroup(true)
						.updateContact(c, Contact.CH_STATUS);
			}
		}
		this.toggleMenus();
// #ifndef GLIDER
																UICanvas.getInstance().open(RegisterScreen.getInstance(), true);
																this.askRepaint();
		// #endif
		//UICanvas.getInstance().close(this);
	}

	public void showAlert(AlertType type, int titleCode, int textCode,
			String additionalText) {
		int resourceTitleCode = ResourceIDs.STR_ERROR;
		int resourceTextCode = ResourceIDs.STR_ERROR;
		switch (titleCode) {

			case Config.ALERT_COMMAND_INFO:
				resourceTitleCode = ResourceIDs.STR_COMMAND_INFO;
				break;

			case Config.ALERT_DATA:
				resourceTitleCode = ResourceIDs.STR_DATA;
				break;

			case Config.ALERT_CONNECTION:
				resourceTitleCode = ResourceIDs.STR_CONNECTION;
				break;

			default:
				break;
		}
		switch (textCode) {
			case Config.ALERT_WAIT_COMMAND:
				resourceTextCode = ResourceIDs.STR_DATA_SUBMITTED;
				break;

			case Config.ALERT_CANCELED_COMMAND:
				resourceTextCode = ResourceIDs.STR_COMMAND_CANCELING;
				break;

			case Config.ALERT_CANCELING_COMMAND:
				resourceTextCode = ResourceIDs.STR_COMMAND_CANCELED;
				break;

			case Config.ALERT_FINISHED_COMMAND:
				resourceTextCode = ResourceIDs.STR_TASK_FINISHED;
				break;

			case Config.ALERT_ERROR_COMMAND:
				resourceTextCode = ResourceIDs.STR_TASK_ERROR;
				break;

			case Config.ALERT_NOTE:
				resourceTextCode = ResourceIDs.STR_NOTE;
				break;

			case Config.ALERT_CONNECTION:
				resourceTextCode = ResourceIDs.STR_CONNECTION_LOST;
				break;

			default:
				break;
		}

		String textString = rm.getString(resourceTextCode);
		textString = additionalText == null ? textString : textString + ": "
				+ additionalText;
		showAlert(type, rm.getString(resourceTitleCode), textString, null);
	}

	public void showAlert(final AlertType type, final String title,
			final String text, final Object next_screen) {
		// the alert should never block the calling thread
		// it may be called on the UICanvas thread
		new Thread() {
			public void run() {
				try {
					UICanvas.lock();
					if (next_screen != null) {
						UICanvas.getInstance().open((UIScreen) next_screen,
								true);
					}
					UICanvas.showAlert(type, title, text);
				} catch (Exception ex) {
					// #mdebug
//@					ex.printStackTrace();
//@					Logger.log("In show alert: " + ex.getClass());
					// #enddebug
				} finally {
					UICanvas.unlock();
				}
			};
		}.start();
	}

	public void handleTask(Task task) {
		try {
			UICanvas.lock();
			_handleTask(task);
		} catch (Exception e) {
			// #mdebug
//@			Logger.log("In displaying task:" + e.getClass());
//@			e.printStackTrace();
			// #enddebug
		} finally {
			UICanvas.unlock();
		}
	}

	// Update the data pertaining the contact
	// and decides if showing a screen is needed
	public void _handleTask(Object obj) {
		Task task = null;
		task = (Task) obj;
		Contact user = xmppClient.getRoster().getContactByJid(task.getFrom());
		if (user != null) {
			_updateContact(user, task.getEnableNew() ? Contact.CH_TASK_NEW
					: Contact.CH_TASK_REMOVED);
		}
		if (obj instanceof Task == false) return;

		if (task.getEnableDisplay()) {
			// #ifdef UI
			task.display();
			// #endif
// #ifndef UI
			//@			Displayable cur = LampiroMidlet.disp.getCurrent();
			//@ task.display(LampiroMidlet.disp, cur);
			// #endif
		}
	}

	public Object handleDataForm(DataForm df, byte type, DataFormListener dfl,
			int cmds) {
		UIScreen scr = null;
		if (type == Task.CMD_INPUT) {
			scr = new DataFormScreen(df, dfl, cmds);
		} else if (type == Task.CMD_FINISHED) {
			scr = new DataResultScreen(df, dfl);
		}

		return scr;
	}

	public void showCommand(Object screen) {
		UICanvas.getInstance().open((UIScreen) screen, true);
	}


	public boolean isMicOn() {
		return micOn;
	}

	public boolean isCameraOn() {
		return cameraOn;
	}

	public void captureMedia(final String fullJid, final int mmType) {
		this.setFreezed(true);
		Thread t = new Thread() {
			public void run() {
				try {
					UICanvas.lock();
					MMScreen tempCanvas = new MMScreen(fullJid);
					Display.getDisplay(LampiroMidlet._lampiro).setCurrent(
							tempCanvas);
					if (mmType == Config.AUDIO_TYPE) {
						tempCanvas.showAudio();
					} else {
						tempCanvas.showCamera();
					}
				} catch (Exception e) {
					// #mdebug
//@					e.printStackTrace();
//@					Logger.log("In capture mm:" + e.getClass());
					// #enddebug
				} finally {
					UICanvas.unlock();
				}
			}
		};
		t.start();
	}

	static Vector getOrderedContacts(boolean includeMuc) {
		Roster roster = XMPPClient.getInstance().getRoster();
		Vector tempOrderedContacts = new Vector(roster.contacts.size());
		for (Enumeration en = roster.contacts.elements(); en.hasMoreElements();) {
			Contact c = (Contact) en.nextElement();
			// to select contact:
			// i select all the online contacts 
			// if the MUCS are included I check that the resources are different from null
			if (c.isVisible()
					&& (c instanceof MUC == false || (includeMuc == true && c
							.getAllPresences() != null))) {
				Enumeration en2 = tempOrderedContacts.elements();
				int index = 0;
				while (en2.hasMoreElements()) {
					if (Utils.compareTo((Contact) en2.nextElement(), c)) index++;
					else
						break;
				}
				tempOrderedContacts.insertElementAt(c, index);
			}
		}
		return tempOrderedContacts;
	}

	public void rosterRetrieved() {
		// Handle subscription to the default contacts 
		Enumeration en = defaultContacts.elements();
		Roster roster = xmppClient.getRoster();
		while (en.hasMoreElements()) {
			Object[] ithContact = (Object[]) en.nextElement();
			String jid = (String) ithContact[0];
			String name = (String) ithContact[1];
			Contact c = roster.getContactByJid(jid);
			if (c == null || !Contact.SUB_BOTH.equals(c.subscription)) {
				c = new Contact(jid, name, null, null);
				subscribeContact(c, false);
			}
		}
		String localServer = Contact.domain(xmppClient.my_jid);
		queryDiscoItems(localServer, new int[] { 0 }, 240000);
		if (localServer.equals(Config.DEFAULT_SERVER) == false) {
			queryDiscoItems(Config.DEFAULT_SERVER, new int[] { 0 }, 240000);
		}
		// #ifdef TEST_COMPONENTS
//@		// // to test the test component
//@		queryDiscoItems(testServer, new int[] { 0 }, 240000);
		//  #endif 
		try {
			UICanvas.lock();
			// allocate the network group to be sure it is always visible
			UIGatewayGroup.getGroup(rosterAccordion, true);
			// load again all the lost conversations;
			this.loadConvs();

		} catch (Exception e) {
			// #mdebug
//@			Logger.log("In roster retrieved:");
//@			e.printStackTrace();
			// #enddebug
		} finally {
			UICanvas.unlock();
		}
	}

	void subscribeContact(Contact c, boolean accept) {
		xmppClient.getRoster().subscribeContact(c, accept);
	}

	/*
	 *  A helper used to know if the Screen can be right/left scrolled
	 */
	public static boolean makeRoll(int kc, UIScreen screen) {
		if (screen.getPopupList().size() > 0
				|| (screen.getMenu() != null && screen.getMenu()
						.isOpenedState() == true)) { return false; }

		int ga = UICanvas.getInstance().getGameAction(kc);
		switch (ga) {
			case Canvas.RIGHT: {
				showNextScreen(screen);
				return true;
			}
			case Canvas.LEFT: {
				showPreviousScreen(screen);
				return true;
			}

			default: {
				break;
			}
		}
		return false;
	}

	public void handlePresenceError(Presence presence) {
		String jid = presence.getAttribute(Iq.ATT_FROM);
		if (jid == null) return;
		Contact c = xmppClient.getRoster().getContactByJid(jid);
		if (c == null || c instanceof MUC == false) return;

		MUC muc = ((MUC) c);

		String myJid = muc.jid + "/" + muc.nick;
		Element error = presence.getChildByName(null, Iq.T_ERROR);
		if (error != null) {
			Message msg = new Message(myJid, Message.ERROR);
			msg.setAttribute(Message.ATT_FROM, myJid);
			msg.addElement(error);
			muc.addMessageToHistory(myJid, msg);
			try {
				UICanvas.lock();
				this._updateContact(muc, Contact.CH_MESSAGE_NEW);
				this.askRepaint();
				UIScreen ms = (UIScreen) RosterScreen.getChatScreenList().get(
						muc.jid);
				if (ms != null) ms.askRepaint();
			} catch (Exception e) {
				// #mdebug
//@				Logger.log("In handling presence error:");
//@				e.printStackTrace();
				// #enddebug
			} finally {
				UICanvas.unlock();
			}
		}
	}

	public void setShow_offlines(boolean show_offlines) {
		this.show_offlines = show_offlines;
	}

	public boolean isShow_offlines() {
		return show_offlines;
	}

	public void setFiltering(boolean filtering) {
		this.filtering = filtering;
	}

	public boolean isFiltering() {
		return filtering;
	}
}
