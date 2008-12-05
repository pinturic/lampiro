/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: XMPPClient.java 1017 2008-11-28 21:57:46Z luca $
*/

package it.yup.xmpp;

import it.yup.transport.BaseChannel;
import it.yup.transport.SocketChannel;



// #mdebug
//@import it.yup.util.Logger; // 
// #enddebug

import it.yup.util.Utils;
import it.yup.xmlstream.AccountRegistration;
import it.yup.xmlstream.BasicXmlStream;
import it.yup.xmlstream.Element;
import it.yup.xmlstream.EventQuery;
import it.yup.xmlstream.EventQueryRegistration;
import it.yup.xmlstream.PacketListener;
import it.yup.xmlstream.SocketStream;
import it.yup.xmlstream.StreamEventListener;


import it.yup.xmpp.packets.DataForm;
import it.yup.xmpp.packets.IQResultListener;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Message;
import it.yup.xmpp.packets.Presence;
import it.yup.xmpp.packets.Stanza;
import lampiro.LampiroMidlet;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import it.yup.util.ResourceManager;
import org.bouncycastle.util.encoders.Base64;

import com.sun.perseus.j2d.Point;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;

// #ifdef UI 
import it.yup.ui.UICanvas;
import it.yup.ui.UIScreen;
import lampiro.screens.CommandListScreen;
import lampiro.screens.DataFormScreen;
import lampiro.screens.DataResultScreen;
import lampiro.screens.RegisterScreen;
import lampiro.screens.RosterScreen;
import lampiro.screens.SubscriptionConfirmScreen;

// #endif
// #ifndef UI
//@
//@import it.yup.screens.CommandListScreen;
//@import it.yup.screens.DataFormScreen;
//@import it.yup.screens.DataResultScreen;
//@import it.yup.screens.RegisterScreen;
//@import it.yup.screens.RosterScreen;
//@import it.yup.screens.SubscriptionConfirmAlert;
//@import javax.microedition.lcdui.Display;
//@
// #endif

public class XMPPClient {

	private static ResourceManager rm = ResourceManager.getManager("common",
			"en");

	/*
	 * The features published by Lampiro ordered as specified here:
	 * http://tools.ietf.org/html/rfc4790#section-9.3
	 */
	private String[] features = new String[] { MIDP_PLATFORM, NS_COMMANDS,
			NS_IQ_DISCO_INFO, NS_MUC, JABBER_X_DATA };

	/** the client instance */
	private static XMPPClient xmppInstance;

	// /** the authID value obtained during stream initialization */
	// public String _authID;

	public Roster roster;

	/** myself */
	private Contact me;

	/** my jabber id */
	public String my_jid;

	/** the used XmlStream */
	private BasicXmlStream xmlStream = null;

	/** The actual connection with the Server */
	private BaseChannel connection = null;

	/** true when the stream is valid */
	private boolean valid_stream = false;

	private int volume;
	private boolean play_flags[];

	/** reference to the possible alert screen */
	private Alert alert;


	// /** send the subscribe at most once per session */
	// private boolean lampiro_subscribe_sent = false;

	private Image presence_icons[];

	/*
	 * The number of sent bytes over the socket
	 */
	//public static int bytes_sent = 0;
	/*
	 * The number of received bytes over the socket
	 */
	//public static int bytes_received = 0;
	/*
	 * A flag used to enable or disable compression
	 */
	public boolean addCompression = false;

	/*
	 * A flag used to enable or disable compression
	 */
	public boolean addTLS = false;

	/**
	 * Get the total amount of traffic on the GPRS connection
	 * 
	 * @return an array with two elements: in / out traffic
	 */
	public static int[] getTraffic() {
		return new int[] { BaseChannel.bytes_received, BaseChannel.bytes_sent };
	}

	public static String NS_IQ_DISCO_INFO = "http://jabber.org/protocol/disco#info";
	public static String NS_IQ_DISCO_ITEMS = "http://jabber.org/protocol/disco#items";
	public static String NS_COMMANDS = "http://jabber.org/protocol/commands";
	public static String NS_CAPS = "http://jabber.org/protocol/caps";
	public static String NS_MUC = "http://jabber.org/protocol/muc";
	public static String NS_MUC_USER = "http://jabber.org/protocol/muc#user";
	public static String NS_MUC_OWNER = "http://jabber.org/protocol/muc#owner";
	public static String MIDP_PLATFORM = "http://bluendo.com/midp#platform";
	public static String JABBER_X_DATA = "jabber:x:data";
	public static String JABBER_IQ_GATEWAY = "jabber:iq:gateway";

	private XMPPClient() {
		Config cfg = Config.getInstance();

		roster = new Roster(this);

		volume = Integer.parseInt(cfg.getProperty(Config.TONE_VOLUME, "50"));
		play_flags = Utils.str2flags(cfg.getProperty(
				Config.VIBRATION_AND_TONE_SETTINGS, "1"), 0, 4);

		// preoload the presence icons
		String mapping[] = Contact.availability_mapping;
		presence_icons = new Image[mapping.length];
		for (int i = 0; i < presence_icons.length; i++) {
			try {
				presence_icons[i] = Image.createImage("/icons/presence_"
						+ mapping[i] + ".png");
			} catch (IOException e) {
				presence_icons[i] = Image.createImage(16, 16);
			}
		}
	};

	/**
	 * Get the XMPP client (a singleton)
	 * 
	 * @return the unique instance of the client
	 * 
	 */
	public static XMPPClient getInstance() {
		if (xmppInstance == null) {
			xmppInstance = new XMPPClient();
		}
		return xmppInstance;
	}

	public void startClient() {
		// #ifndef UI 
		//@				LampiroMidlet.disp.setCurrent(RegisterScreen.getInstance());
		// #endif
	}

	/** close the connection XXX i don't like this name */
	public void stopClient() {
		// saveToStorage();
	}

	/**
	 * Add a listener for XMPP packets
	 * 
	 * @param _q
	 *            the xpath like query
	 * @param _l
	 *            the listener
	 * @return The registration object to be used with
	 *         {@link #unregisterListener(EventQueryRegistration)} for removing
	 *         the listener
	 */
	public EventQueryRegistration registerListener(EventQuery _q, Object _l) {
		return xmlStream.addEventListener(_q, _l);
	}

	/**
	 * Add a one time listener for XMPP packets
	 * 
	 * @param _q
	 *            the xpath like query
	 * @param _l
	 *            the listener
	 * @return The registration object to be used with
	 *         {@link #unregisterListener(EventQueryRegistration)} for removing
	 *         the listener
	 * 
	 */
	public EventQueryRegistration registerOneTimeListener(EventQuery _q,
			Object _l) {
		return xmlStream.addOnetimeEventListener(_q, _l);
	}

	/**
	 * Remove a registered listener
	 * 
	 * @param reg
	 *            the registration obtained with
	 *            {@link #registerListener(EventQuery, PacketListener)}
	 * 
	 */
	public void unregisterListener(EventQueryRegistration reg) {
		xmlStream.removeEventListener(reg);
	}

	/**
	 * Queue a packet into the send queue
	 * 
	 * @param pack
	 *            the packet to be sent
	 */
	public void sendPacket(Element pack) {
		xmlStream.send(pack, Config.TIMEOUT);
	}

	/**
	 * Send an Iq packet and register the packet listener for the answer
	 * 
	 * @param iq
	 * @param listener
	 *            (may be null)
	 */
	public void sendIQ(Iq iq, IQResultListener listener) {
		if (listener != null) {
			EventQuery eq = new EventQuery("iq", new String[] { "id" },
					new String[] { iq.getAttribute("id") });
			registerOneTimeListener(eq, listener);
		}
		sendPacket(iq);
	}

	public Contact getMyContact() {
		return me;
	}

	/**
	 * Start the XML Stream using the current configuration
	 * 
	 * @param register
	 *            set true if the stream must create the account
	 * @return
	 */
	public BasicXmlStream createStream(boolean register) {

		buildSocketConnection();


		// connection = new SimpleBTConnector(
		// Config.HTTP_GW_HOST,
		// Config.HTTP_GW_PATH,
		// xmlStream
		// );

		// Associate the roster with the stream
		this.roster.associateWithStream(xmlStream);

		// Register the handler for incoming messages
		EventQuery eq = new EventQuery(Message.MESSAGE, null, null);
		eq.child = new EventQuery(Message.BODY, null, null);
		xmlStream.addEventListener(eq, new MessageHandler());

		// Register the presence handler
		eq = new EventQuery(Presence.PRESENCE, null, null);
		xmlStream.addEventListener(eq, new PresenceHandler());

		// Register the disco handler
		eq = new EventQuery(Iq.IQ, new String[] { "type" },
				new String[] { "get" });
		eq.child = new EventQuery(Iq.QUERY, new String[] { "xmlns" },
				new String[] { NS_IQ_DISCO_INFO });
		xmlStream.addEventListener(eq, new DiscoHandler());

		// Register the handler for dataforms (both as <iq/> and <message/>)
		// payloads
		DataFormHandler dh = new DataFormHandler();
		eq = new EventQuery(Message.MESSAGE, null, null);
		eq.child = new EventQuery(DataForm.X, new String[] { "xmlns" },
				new String[] { DataForm.NAMESPACE });
		xmlStream.addEventListener(eq, dh);
		eq = new EventQuery(Iq.IQ, null, null);
		eq.child = new EventQuery(DataForm.X, new String[] { "xmlns" },
				new String[] { DataForm.NAMESPACE });
		xmlStream.addEventListener(eq, dh);

		/* register handler for ad hoc command announcements */
		PacketListener ashc_listener = new PacketListener() {
			public void packetReceived(Element e) {
				handleClientCommands(e, false);
			}

		};

		// XXX useful with messages? I don't think so
		// eq = new EventQuery(Message.MESSAGE, null, null);
		// eq.child = new EventQuery(Iq.QUERY, new String[] { "xmlns" },
		// new String[] { "http://jabber.org/protocol/disco#items" } );
		// xmlStream.addEventListener(eq, adch);

		// XXX here we *must* use client capabilities
		/* register handler for ad hoc command presence announce */
		eq = new EventQuery(Presence.PRESENCE, null, null);
		eq.child = new EventQuery(Iq.QUERY, new String[] { "xmlns" },
				new String[] { "http://jabber.org/protocol/disco#items" });
		xmlStream.addEventListener(eq, ashc_listener);

		if (register) {
			xmlStream.addInitializer(new AccountRegistration(), 0);
		}

		return xmlStream;
	}

	/**
	 * Build the low level connection based on plain sockets
	 */
	private void buildSocketConnection() {
		Config cfg = Config.getInstance();
		xmlStream = new SocketStream();
		// #ifndef BT_PLAIN_SOCKET
		connection = new SocketChannel("socket://"
				+ cfg.getProperty(Config.CONNECTING_SERVER), xmlStream);
		// #endif
		((SocketChannel) connection).KEEP_ALIVE = Long.parseLong(cfg
				.getProperty(Config.KEEP_ALIVE));
	}


	public void openStream() {
		Config config = Config.getInstance();

		String resource = config.getProperty(Config.YUP_RESOURCE, "Lampiro");
		xmlStream.initialize(config.getProperty(Config.USER) + "@"
				+ config.getProperty(Config.SERVER) + "/" + resource, config
				.getProperty(Config.PASSWORD));


		if (!connection.isOpen()) {
			connection.open();
		}
	}

	public void closeStream() {
		if (connection.isOpen()) {
			connection.close();
		}
	}

	/*
	 * XXX: perch� questo non diventa un initializer o listener sullo stream?
	 * qui c'� un pezzo di "view", non sarebbe pi� opportuno separare le cose in
	 * modo da isolare le varie cose? in fondo va detto che se vogliamo che
	 * XMPPClient diventi una libreria...
	 */
	public void stream_authenticated(boolean new_credentials) {

		Config cfg = Config.getInstance();

		// create the self contact and setup the initialial presence
		my_jid = xmlStream.jid;

		me = new Contact(Contact.userhost(my_jid), null, null, null);
		Presence p = new Presence();
		p.setAttribute("from", my_jid);
		String show = cfg.getProperty(Config.LAST_PRESENCE_SHOW);
		if (show != null && !"online".equals(show)) {
			p.setShow(show);
		}
		String msg = cfg.getProperty(Config.LAST_STATUS_MESSAGE);
		String tempPriority = cfg.getProperty(Config.LAST_PRIORITY, "0");
		p.setPriority(Integer.parseInt(tempPriority));
		p.setStatus(msg);
		// set capabilities
		String uri = NS_CAPS;
		Element cap = p.addElement(uri, "c", uri);
		cap.setAttribute("node", "http://bluendo.com/lampiro/caps");
		cap.setAttribute("hash", "sha-1");
		cap.setAttribute("ver", getCapVer());

		// XXX I don't like this, it could be better to send capabilities with a
		// different hash in the version
		Element x = p.addElement(JABBER_X_DATA, "x", JABBER_X_DATA);
		x.setAttribute("type", "result");
		Element field = x.addElement(JABBER_X_DATA, "field", JABBER_X_DATA);
		field.setAttribute("var", "FORM_TYPE");
		field.setAttribute("type", "hidden");
		field.addElement(JABBER_X_DATA, "value", JABBER_X_DATA).content = MIDP_PLATFORM;

		field = x.addElement(JABBER_X_DATA, "field", JABBER_X_DATA);
		field.setAttribute("var", "microedition.platform");
		field.addElement(JABBER_X_DATA, "value", JABBER_X_DATA).content = System
				.getProperty("microedition.platform");

		me.updatePresence(p);

		// we are connected, set the stream as valid
		valid_stream = true;

		// Listen for lost connections
		xmlStream.addEventListener(new EventQuery(
				BasicXmlStream.STREAM_TERMINATED, null, null),
				new StreamEventListener() {

					public void gotStreamEvent(String event, Object source) {
						valid_stream = false;
						closeStream();
						showAlert(AlertType.ERROR, "Connection lost",
								"Connection with the server lost",
								RegisterScreen.getInstance());
						/* XXX: should close all screens and open the RegisterScreen */
					}
				});

		// now go online
		// XXX: perch� if(true)?
		if (true || new_credentials) {
			roster.retrieveRoster(true);
		} else {
			// subscribe_to_agent();
			// #ifdef UI
			UICanvas.getInstance().open(RosterScreen.getInstance(), true);
			UICanvas.getInstance().close(RegisterScreen.getInstance());
			// #endif
// #ifndef UI
			//@						LampiroMidlet.disp.setCurrent(RosterScreen.getInstance());
			// #endif
			// UICanvas.getInstance().open(new RosterScreen(), true);
		}
	}

	private String getCapVer() {
		Config cfg = Config.getInstance();
		Vector ss = new Vector();
		ss.addElement("client/");
		ss.addElement("phone/");
		ss.addElement("/")/* XXX should be the lang here */;
		ss.addElement("Lampiro " + cfg.getProperty(Config.VERSION) + "<");
		for (int i = 0; i < features.length; i++) {
			ss.addElement(features[i]);
			ss.addElement("<");
		}
		Enumeration en = ss.elements();
		String S = "";
		while (en.hasMoreElements()) {
			S += en.nextElement();
		}
		S = new String(Base64.encode(Utils.digest(S, "sha1")));
		return S;
	}

	private class MessageHandler implements PacketListener {

		public void packetReceived(Element p) {
			// Logger.log(
			// "MessageHandler: received packet: " + new String(p.toXml()),
			// Logger.DEBUG
			// );
			Message msg = Message.fromElement(p);

			// XXX: we will need to check the type
			String jid = msg.getAttribute(Stanza.ATT_FROM);
			// error packet sometimes do not have from
			if (jid == null) return;
			Contact u = roster.getContactByJid(jid);

			if (u == null) {
				Element group_elements[] = p.getChildrenByName(null, "group");
				String groups[] = new String[group_elements.length];
				for (int j = 0; j < groups.length; j++) {
					groups[j] = group_elements[j].content;
				}
				u = new Contact(Contact.userhost(jid), p.getAttribute("name"),
						p.getAttribute("subscription"), groups);
				roster.contacts.put(Contact.userhost(u.jid), u);
			}

			RosterScreen roster = RosterScreen.getInstance();
			u.addMessageToHistory(msg);
			roster.updateContact(u, Contact.CH_MESSAGE_NEW);
			playSmartTone();

		}
	}

	private class PresenceHandler implements PacketListener {

		public void packetReceived(Element e) {
			// #mdebug
//@			Logger.log("PresenceHandler: received packet: "
//@					+ new String(e.toXml()), Logger.DEBUG);
			// #enddebug
			String t = e.getAttribute(Stanza.ATT_TYPE);
			if (t == null || Presence.T_UNAVAILABLE.equals(t)) {
				Presence p = new Presence(e);

				String from = e.getAttribute(Stanza.ATT_FROM);
				Contact u = roster.getContactByJid(from);
				if (u == null) {
					// #ifdef MUC
					// first check if its a MUC
					Element[] xs = p.getChildrenByName(null, "x");
					for (int i = 0; xs != null && i < xs.length; i++) {
						if (xs[i].uri != null
								&& xs[i].uri.indexOf(XMPPClient.NS_MUC) >= 0) {
							u = new MUC(Contact.userhost(from), Contact
									.user(from));
						}
					}

					if (u == null) {
						// XXX Guess the subscription
						u = new Contact(Contact.userhost(from), null, "both",
								null);
					}
					// #endif
// #ifndef MUC
					//@										// XXX Guess the subscription
					//@										u = new Contact(Contact.userhost(from), null, "both", null);
					// #endif
					u.updatePresence(new Presence(e));
					roster.contacts.put(u.jid, u);
				} else {
					u.updatePresence(p);
				}
				RosterScreen.getInstance().updateContact(u, Contact.CH_STATUS);

			} else if (Presence.T_SUBSCRIBE.equals(t)) {
				handleSubscribe(new Presence(e));
			} else {
				// XXX At present ignore other cases, but when receiving
				// UNSUBCRIBED we should update the roster
			}
		}

		private void handleSubscribe(Presence p) {

			// try getting the contact (we may already have it)
			String jid = p.getAttribute(Stanza.ATT_FROM);
			Contact u = roster.getContactByJid(jid);
			if (u == null) {
				// we don't have the contact, create it
				u = new Contact(jid, null, null, null);
			}

			// subscription handling
			if ("both".equals(u.subscription) || "to".equals(u.subscription)
					|| Config.LAMPIRO_AGENT.equals(jid)) {
				// subscribe received: if already granted, I don't ask anything
				Presence pmsg = new Presence();
				pmsg.setAttribute(Stanza.ATT_TO, u.jid);
				pmsg.setAttribute(Stanza.ATT_TYPE, Presence.T_SUBSCRIBED);
				sendPacket(pmsg);
			} else {
				/*
				 * UIMenu confirmMenu = new UIMenu(rm
				 * .getString(ResourceIDs.STR_SUBSCRIPTION_CONFIRM)); UILabel
				 * confirmQuestion = new UILabel(rm
				 * .getString(ResourceIDs.STR_SUBSCRIPTION_REQUEST_FROM) + " " +
				 * u.jid + ". " +
				 * rm.getString(ResourceIDs.STR_SUBSCRIPTION_ACCEPT));
				 * confirmMenu.append(confirmQuestion);
				 * confirmQuestion.setFocusable(true);
				 * confirmMenu.setSelectedIndex(1);
				 * confirmMenu.setAbsoluteX(10);
				 * confirmMenu.setWidth(UICanvas.getInstance().getWidth() - 20);
				 * confirmQuestion.setWrappable(true, confirmQuestion.getWidth() -
				 * 5); confirmMenu.cancelMenuString =
				 * rm.getString(ResourceIDs.STR_NO);
				 * confirmMenu.selectMenuString = rm
				 * .getString(ResourceIDs.STR_YES); UIScreen currentScreen =
				 * UICanvas.getInstance() .getCurrentScreen(); Graphics cg =
				 * currentScreen.getGraphics(); int offset = (cg.getClipHeight() -
				 * confirmMenu.getHeight(cg)) / 2;
				 * confirmMenu.setAbsoluteY(offset); this.confirmContact =
				 * contact; currentScreen.addPopup(confirmMenu);
				 */

				// #ifdef UI
				SubscriptionConfirmScreen scs = new SubscriptionConfirmScreen(u);
				UICanvas.getInstance().open(scs, true);

				// #endif
// #ifndef UI
				//@								Display d = Display.getDisplay(LampiroMidlet._lampiro);
				//@								SubscriptionConfirmAlert scf = new SubscriptionConfirmAlert(u,
				//@										d.getCurrent());
				//@								d.setCurrent(scf);
				// #endif

			}
		}
	}

	// XXX The roster handler should always listen to any iq:roster packet for
	// supporting roster push and any roster update when logged in

	private class DataFormHandler implements PacketListener {

		public void packetReceived(Element p) {
			updateTask(new SimpleDataFormExecutor(p));
			playSmartTone();
		}
	}

	private class DiscoHandler implements PacketListener {

		public void packetReceived(Element p) {
			Element q = p.getChildByName(NS_IQ_DISCO_INFO, Iq.QUERY);
			Iq reply = new Iq(p.getAttribute(Stanza.ATT_FROM), Iq.T_RESULT);
			reply.setAttribute(Stanza.ATT_ID, p.getAttribute(Stanza.ATT_ID));
			String node = q.getAttribute("node");
			Element qr = reply.addElement(NS_IQ_DISCO_INFO, Iq.QUERY,
					NS_IQ_DISCO_INFO);

			if (node == null) {
				Element identity = qr.addElement(NS_IQ_DISCO_INFO, "identity",
						NS_IQ_DISCO_INFO);
				identity.setAttribute("category", "client");
				identity.setAttribute("type", "phone");
				identity.setAttribute("name", "Lampiro");
				for (int i = 0; i < features.length; i++) {
					Element feature = identity.addElement(NS_IQ_DISCO_INFO,
							"feature", NS_IQ_DISCO_INFO);
					feature.setAttribute("var", features[i]);
				}

			} else if (MIDP_PLATFORM.equals(node)) {
				qr.setAttribute("node", MIDP_PLATFORM);
				Element x = qr.addElement(JABBER_X_DATA, "x", JABBER_X_DATA);
				x.setAttribute("type", "result");
				Element field = x.addElement(JABBER_X_DATA, "field",
						JABBER_X_DATA);
				field.setAttribute("var", "microedition.platform");
				field.addElement(JABBER_X_DATA, "value", JABBER_X_DATA).content = System
						.getProperty("microedition.platform");

			}
			sendPacket(reply);
		}
	}

	public void playSmartTone() {
		// #ifdef UI
		boolean shown = UICanvas.getInstance().getCurrentScreen() == RosterScreen
				.getInstance();
		// #endif
// #ifndef UI
		//@				boolean shown = RosterScreen.getInstance().isShown();
		// #endif
		boolean vibrate = (shown && play_flags[1])
				|| ((!shown) && play_flags[0]);
		boolean play = (shown && play_flags[3]) || ((!shown) && play_flags[2]);

		if (vibrate) {
			// #ifdef UI
			LampiroMidlet.disp.vibrate(200);
			// #endif
// #ifndef UI
			//@						LampiroMidlet.disp.vibrate(200);
			// #endif
		}

		if (play) {
			try {
				Manager.playTone(40, 100, volume);
				Manager.playTone(50, 100, volume);
				Manager.playTone(30, 100, volume);
			} catch (MediaException e1) {

			}
		}
	}

	/*
	 * Set the current priority of the client (store and send it).
	 * After setting the priority calls setpresence.
	 * 
	 * @param priority The priority to set
	 */
	public void setPresence(int availability, String status, int priority) {
		Presence p = me.getPresence(my_jid);
		p.setPriority(priority);
		this.setPresence(availability, status);
	}

	/**
	 * Set the current presence of the client (store and send it)
	 * 
	 * @param availability
	 * @param status
	 */
	public void setPresence(int availability, String status) {

		Presence p = me.getPresence(my_jid);
		Presence new_p = new Presence();

		new_p.setAttribute("from", p.getAttribute("from"));

		if (availability >= 0) {
			if (Contact.AV_ONLINE == availability) {

			} else if (Contact.AV_UNAVAILABLE == availability) {
				new_p.setAttribute(Stanza.ATT_TYPE, Presence.T_UNAVAILABLE);
			} else {
				new_p.setShow(Contact.availability_mapping[availability]);
			}
		}

		if (status != null) {
			new_p.setStatus(status);
		} else {
			new_p.setStatus(p.getStatus());
		}

		new_p.children.addElement(p.getChildByName(null, "x"));
		new_p.children.addElement(p.getChildByName(null, "c"));
		new_p.setPriority(p.getPriority());
		me.updatePresence(new_p);

		System.out.println(new String(new_p.toXml()));
		sendPacket(new_p);

		if (Presence.T_UNAVAILABLE.equals(new_p.getAttribute(Stanza.ATT_TYPE))) {
			closeStream();
		}
	}

	/**
	 * Handle an incoming command list
	 * 
	 * @param e
	 *            the received element with commands
	 * @param show
	 *            when true show the command list screen
	 */
	public void handleClientCommands(Element e, boolean show) {
		String from = e.getAttribute(Stanza.ATT_FROM);
		if (from == null) return;
		Contact c = roster.getContactByJid(from);
		if (c == null) return;
		Element q = e.getChildByName("http://jabber.org/protocol/disco#items",
				Iq.QUERY);
		if (q != null) {
			Element items[] = q.getChildrenByName(
					"http://jabber.org/protocol/disco#items", "item");
			c.cmdlist = new String[items.length][2];
			for (int i = 0; i < items.length; i++) {
				c.cmdlist[i][0] = items[i].getAttribute("node");
				c.cmdlist[i][1] = items[i].getAttribute("name");
			}

			RosterScreen.getInstance().updateContact(c, Contact.CH_TASK_NEW);

			if (show) {
				CommandListScreen cmdscr = new CommandListScreen(c);
				// #ifdef UI
				UICanvas.getInstance().open(cmdscr, true);
				// #endif
// #ifndef UI
				//@								LampiroMidlet.disp.setCurrent(cmdscr);
				// #endif 
			}
		} // XXX we could add an alert if it's empty and we have to show
	}

	/**
	 * Show an error screen, if multiple errors occur only append the message
	 * 
	 * @param type
	 * @param title
	 *            Title of the screen
	 * @param text
	 *            Displayed error message
	 * @param next_screen
	 *            the screen where we have to return to
	 */
	public void showAlert(AlertType type, String title, String text,
			final Object next_screen) {
		// #ifdef UI
		if (next_screen != null) {
			UICanvas.getInstance().open((UIScreen) next_screen, true);
		}
		UICanvas.showAlert(type, title, text);
		// #endif
// #ifndef UI
		//@		
		//@				Displayable cur = LampiroMidlet.disp.getCurrent();
		//@				if (cur.equals(alert)) {
		//@					alert.setString(alert.getString() + "\n" + text);
		//@					return;
		//@				}
		//@		
		//@				Image img;
		//@				try {
		//@					if (AlertType.INFO.equals(type)) {
		//@						img = Image.createImage("/icons/warning.png");
		//@					} else if (AlertType.ERROR.equals(type)) {
		//@						img = Image.createImage("/icons/error.png");
		//@					} else {
		//@						img = Image.createImage("/icons/error.png");
		//@					}
		//@		
		//@				} catch (IOException e) {
		//@					img = null;
		//@				}
		//@		
		//@				alert = new Alert(title, text, img, type);
		//@		
		//@				alert.setType(type);
		//@				alert.setTimeout(Alert.FOREVER);
		//@		
		//@				LampiroMidlet.disp.setCurrent(alert, (Displayable) next_screen);
		//#endif
	}

	/**
	 * Update the status of a task. Queue it if this is the first time it's
	 * status is updated
	 * 
	 * @param task
	 */
	public void updateTask(Task task) {

		Contact user = roster.getContactByJid(task.getFrom());
		user.addTask(task);
		System.out.println("Tsk: " + Integer.toHexString(task.getStatus()));
		// #ifdef UI 
		Displayable cur = LampiroMidlet.disp.getCurrent();
		// #endif
// #ifndef UI
		//@				Displayable cur = LampiroMidlet.disp.getCurrent();
		// #endif

		byte type = task.getStatus();

		// true if we should display the command
		boolean display = false;
		boolean removed = false;

		if ((type & Task.CMD_MASK) == Task.CMD_MASK) {
			switch (type) {
				case Task.CMD_INPUT:
					display = true;
					break;
				case Task.CMD_EXECUTING:
					// do nothing, just wait for an answer
					break;
				case Task.CMD_CANCELING:
					// do nothing, just wait for an answer
					break;
				case Task.CMD_CANCELED:
					display = true;
					removed = true;
					user.removeTask(task);
					break;
				case Task.CMD_FINISHED:
					// tasks.removeElement(task);
					display = true;
					break;
				case Task.CMD_ERROR:
					// XXX
					break;
				case Task.CMD_DESTROY:
					removed = true;
					user.removeTask(task);
					break;
			}
		} else { // simple data form
			switch (type) {
				case Task.DF_FORM:
					display = true;
					break;
				case Task.DF_SUBMITTED:
					removed = true;
					user.removeTask(task);
					break;
				case Task.DF_CANCELED:
					removed = true;
					user.removeTask(task);
					break;
				case Task.DF_RESULT:
					display = true;
					break;
				case Task.DF_ERROR:
					// XXX
					break;
				case Task.DF_DESTROY:
					removed = true;
					user.removeTask(task);
					break;
			}
		}

		// update contact position in the roster
		if (removed) {
			RosterScreen.getInstance().updateContact(user,
					Contact.CH_TASK_REMOVED);
		} else {
			RosterScreen.getInstance().updateContact(user, Contact.CH_TASK_NEW);
		}

		// Display a task only if no other task is currently displayed
		Class klass = cur.getClass();
		if (display && !DataFormScreen.class.equals(klass)
				&& !DataResultScreen.class.equals(klass)) {
			// #ifdef UI
			task.display();
			// #endif
// #ifndef UI
			//@						task.display(LampiroMidlet.disp, cur);
			// #endif
		}
	};

	// private void subscribe_to_agent() {
	// if(lampiro_subscribe_sent){
	// return;
	// }
	// lampiro_subscribe_sent = true;
	//		
	// Contact c = getContactByJid(Config.LAMPIRO_AGENT);
	// if(c == null) {
	// c = new Contact(Config.LAMPIRO_AGENT, "Lampiro Agent", "none", null);
	// subscribeContact(c);
	// } else if(!"both".equals(c.subscription)) {
	// // XXX resend presesence of type subscribe!
	// }
	// }

	/**
	 * Get the icon for a presence show
	 * 
	 * @param i
	 *            a {@link@Presence} AV_* constant
	 * @return
	 */
	public Image getPresenceIcon(int availability) {
		if (availability >= 0 && availability < presence_icons.length) { return presence_icons[availability]; }
		return null; // maybe we could return an empty image
	}

	public Roster getRoster() {
		return roster;
	}

	// #ifdef SEND_DEBUG1
	// @ public void sendDebug(String msg) {
	// @ Message m = new Message("ff@jabber.bluendo.com", "chat");
	// @ m.setBody(msg);
	// @ sendPacket(m);
	// @ }
	// #endif

}
