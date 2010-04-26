/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: XMPPClient.java 2055 2010-04-12 10:22:05Z luca $
*/

package it.yup.xmpp;

import it.yup.transport.BaseChannel;
import it.yup.transport.SocketChannel;



// #mdebug
//@
//@import it.yup.util.Logger;
//@
//#enddebug

import it.yup.util.Utils;
import it.yup.xml.Element;
import it.yup.xml.SystemConfig;
import it.yup.xmlstream.AccountRegistration;
import it.yup.xmlstream.BasicXmlStream;
import it.yup.xmlstream.EventQuery;
import it.yup.xmlstream.EventQueryRegistration;
import it.yup.xmlstream.PacketListener;
import it.yup.xmlstream.SocketStream;


import it.yup.xmpp.packets.DataForm;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Message;
import it.yup.xmpp.packets.Presence;
import it.yup.xmpp.packets.Stanza;
import it.yup.util.CountInputStream;
import it.yup.util.CountOutputStream;
import it.yup.util.EventDispatcher;
import it.yup.util.EventListener;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimerTask;
import java.util.Vector;
import org.bouncycastle.util.encoders.Base64;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Image;

public class XMPPClient implements EventListener {

	/*
	 * Few methods used to communicate to the application of xmpp events 
	 */
	public interface XmppListener {

		/**
		 * Removes the contact from the roster; the contact may be removed for many reasons 
		 * (temporary modifications, contact to MUC upgrade, roster recreation, unsubscription )
		 * 
		 * @param c the contact that has been removed
		 * @param unsubscribed true if the contact has unsibscribed
		 */
		void removeContact(Contact c, boolean unsubscribed);

		void removeAllContacts();

		void updateContact(Contact c, int chStatus);

		void authenticated();

		void rosterXsubscription(Element e);

		void playSmartTone();

		void askSubscription(Contact u);

		void connectionLost();

		void showAlert(AlertType type, int titleCode, int textCode,
				String additionalText);

		void handleTask(Task task);

		Object handleDataForm(DataForm df, byte type, DataFormListener dfl,
				int cmds);


		void showCommand(Object screen);

		void rosterRetrieved();

		void handlePresenceError(Presence presence);
	}

	private Config cfg = Config.getInstance();

	/*
	 * The features published by Lampiro are ordered as specified here:
	 * http://tools.ietf.org/html/rfc4790#section-9.3
	 */
	private String[] features = new String[] { MIDP_PLATFORM, NS_CAPS,
			NS_COMMANDS, NS_IQ_DISCO_INFO, NS_IBB, NS_MUC, NS_PUBSUB_EVENT,
			NS_ROSTERX, FILE_TRANSFER, NS_JABBER_VERSION, JABBER_X_DATA,
			JINGLE, JINGLE_FILE_TRANSFER, JINGLE_IBB_TRANSPORT };

	public static final String J2MEVER = "J2ME/"
			+ System.getProperty("microedition.platform");

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

	private EventQueryRegistration lostConnReg = null;


	// /** send the subscribe at most once per session */
	// private boolean lampiro_subscribe_sent = false;

	protected Image presence_icons[];
	protected Image presence_phone_icons[];

	/*
	 * This task is used to retrieve asynchronously the roster
	 * after going online. 
	 */
	private TimerTask rosterRetrieveTask = null;

	/*
	 * The time after rosterRetrieveTask is scheduled after receiving
	 * a presence
	 */
	private int rosterRetrieveTime = 5000;

	/*
	 * The time at which the last presence has been received
	 */
	private long lastPresenceTime;

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
	 * A flag used to enable or disable TLS
	 */
	public boolean addTLS = false;

	/*
	 * the gateways whose contacts must be autoaccepted
	 * i.e. the gateways whose presence has been subscripted 
	 * within the current session
	 */
	public Vector autoAcceptGateways = new Vector();

	private XMPPClient.XmppListener xmppListener;

	/*
	 * Used to notify the XmppClient that the user jid and/or password
	 * are changed from last login
	 */
	private boolean newCredentials = false;

	/* 
	 * The registration initializer
	 */
	private AccountRegistration accountRegistration;

	/*
	 * Contains some additional info about the server gateway config
	 */
	public Hashtable gatewayConfig = new Hashtable();

	/*
	 * A flag used to communicate if the client must go online when authenticated
	 */
	private boolean goOnline = false;

	/**
	 * Get the total amount of traffic on the GPRS connection
	 * 
	 * @return an array with two elements: in / out traffic
	 */
	public static int[] getTraffic() {
		CountInputStream cis = BaseChannel.countInputStream;
		int readBytes = cis != null ? cis.getCount() : 0;
		CountOutputStream cos = BaseChannel.countOutputStream;
		int sentBytes = cos != null ? cos.getCount() : 0;
		return new int[] { readBytes, sentBytes };
	}

	public static String NS_IQ_DISCO_INFO = "http://jabber.org/protocol/disco#info";
	public static String NS_IQ_DISCO_ITEMS = "http://jabber.org/protocol/disco#items";
	public static String NS_COMMANDS = "http://jabber.org/protocol/commands";
	public static String NS_CAPS = "http://jabber.org/protocol/caps";
	public static String NS_BLUENDO_CAPS = "http://bluendo.com/protocol/caps";
	public static String NS_BLUENDO_CFG = "xmpp:bluendo:cfg";
	public static String NS_BLUENDO_PUBLISH = "bluendo:http:publish:0";
	public static String NS_BOOKMARKS = "storage:bookmarks";
	public static String NS_IBB = "http://jabber.org/protocol/ibb";
	public static String NS_MUC = "http://jabber.org/protocol/muc";
	public static String NS_PRIVATE = "jabber:iq:private";
	public static String NS_JABBER_VERSION = "jabber:iq:version";
	public static String NS_PUBSUB = "http://jabber.org/protocol/pubsub";
	public static String NS_PUBSUB_EVENT = "http://jabber.org/protocol/pubsub#event";
	public static String NS_ROSTERX = "http://jabber.org/protocol/rosterx";
	public static String NS_MUC_USER = "http://jabber.org/protocol/muc#user";
	public static String NS_MUC_OWNER = "http://jabber.org/protocol/muc#owner";
	public static String NS_NICK = "http://jabber.org/protocol/nick";
	public static String NS_STORAGE_LAMPIRO = "storage:lampiro";
	public static String NS_VCARD_UPDATE = "vcard-temp:x:update";
	public static String NS_UUID = "urn:bluendo:uuid:0";
	public static String NS_MEDIA_ELEMENT = "urn:xmpp:media-element";
	public static String NS_BOB = "urn:xmpp:bob";

	public static String XML_NS = "http://www.w3.org/XML/1998/namespace";
	public static String STORAGE = "storage";
	public static String MIDP_PLATFORM = "http://bluendo.com/midp#platform";
	public static String JABBER_X_DATA = "jabber:x:data";
	public static String JABBER_IQ_GATEWAY = "jabber:iq:gateway";
	public static String NS_IQ_ROSTER = "jabber:iq:roster";
	public static String IQ_REGISTER = "jabber:iq:register";
	public static String JINGLE = "urn:xmpp:jingle:1";
	public static String JINGLE_FILE_TRANSFER = "urn:xmpp:jingle:apps:file-transfer:1";
	public static String FILE_TRANSFER = "http://jabber.org/protocol/si/profile/file-transfer";
	public static String JINGLE_IBB_TRANSPORT = "urn:xmpp:jingle:transports:ibb:0";
	public static String BLUENDO_PUBLISH = "bluendo:http:publish:0";
	public static String USERID = "USERID";
	public static String VCARD_TEMP = "vcard-temp";
	public static String VCARD = "vCard";
	public static String BINVAL = "BINVAL";
	public static String NICKNAME = "NICKNAME";
	public static String PHOTO = "PHOTO";
	public static String FN = "FN";
	public static String EMAIL = "EMAIL";
	public static String BLUENDO_XMLRPC = "bluendo:bxmlrpc:0";
	public static String BLUENDO_REGISTER = "bluendo:register:0";
	public static String NS_DELAY = "urn:xmpp:delay";
	public static String DELAY = "delay";
	public static String CONFERENCE = "conference";
	public static String SECTION = "section";
	public static String PUBSUB = "pubsub";
	public static String PUBLISH = "publish";
	public static String ITEMS = "items";
	public static String ITEM = "item";
	public static String NODE = "node";
	public static String LAMPIRO_CONFIG = "lampiro_config";
	public static String EVENT = "event";
	public static String COMMAND = "command";
	public static String ACTION = "action";
	public static String DATA = "data";
	public static String UUID = "uuid";
	public static String FEATURE = "feature";
	public static String URI = "uri";
	public static String MEDIA = "media";
	public static String TEXT = "text";
	public static String CODE = "code";
	public static String REMOVE = "remove";
	public static String SUBSCRIPTION = "subscription";
	public static String FULL_NODE = "fullnode";


	public static String[][] errorCodes = new String[][] {
			{ "400", "Bad request" }, { "401", "Not authorized" },
			{ "403", "Forbidden" }, { "404", "Item not found" },
			{ "405", "Operation not allowed" }, { "406", "Not acceptable" },
			{ "408", "Request timeout" }, { "407", "Registration required" },
			{ "409", "Conflict" }, { "500", "Internal server error" },
			{ "501", "Feature not implemented" },
			{ "502", "Remote server error" }, { "503", "Service unavailable" },
			{ "504", "Remote server timeout" }, { "510", "Disconnected" },

	};

	public Image getPresenceIcon(Contact c, String preferredResource,
			int availability) {
		Presence p = null;
		if (c != null) {
			// first check if it is a MUC
			if (c instanceof MUC == true) {
				if (c.resources != null) return this.presence_icons[Contact.MUC_IMG];
				else
					return this.presence_icons[Contact.MUC_IMG_OFFLINE];
			}
			// next the usual ones
			else if (preferredResource != null) p = c
					.getPresence(preferredResource);
			else
				p = c.getPresence(null);
		}
		if (availability >= 0 && availability < this.presence_icons.length) {
			if (p == null || p.pType == Presence.PC) {
				return this.presence_icons[availability];
			} else if (p.pType == Presence.PHONE) { return this.presence_phone_icons[availability]; }
		}
		return null; // maybe we could return an empty image
	}

	private XMPPClient() {
		//		for (int i = 0; i < features.length; i++) {
		//			String array_element = features[i];
		//			System.out.println(array_element);	
		//		}

		roster = new Roster(this);

		// preload the presence icons
		String mapping[] = Contact.availability_mapping;
		presence_icons = new Image[mapping.length];
		presence_phone_icons = new Image[mapping.length];
		try {
			presence_icons[0] = Image.createImage("/icons/presence_"
					+ mapping[1] + ".png");
			presence_phone_icons[0] = Image.createImage("/icons/presence_"
					+ mapping[1] + "_phone.png");
			presence_icons[1] = Image.createImage("/icons/presence_"
					+ mapping[1] + ".png");
			presence_phone_icons[1] = Image.createImage("/icons/presence_"
					+ mapping[1] + "_phone.png");
			presence_icons[2] = Image.createImage("/icons/presence_"
					+ mapping[2] + ".png");
			presence_phone_icons[2] = Image.createImage("/icons/presence_"
					+ mapping[2] + "_phone.png");
			presence_icons[3] = Image.createImage("/icons/presence_"
					+ mapping[3] + ".png");
			presence_phone_icons[3] = Image.createImage("/icons/presence_"
					+ mapping[3] + "_phone.png");
			presence_icons[4] = Image.createImage("/icons/presence_"
					+ mapping[3] + ".png");
			presence_phone_icons[4] = Image.createImage("/icons/presence_"
					+ mapping[3] + "_phone.png");
			presence_icons[5] = Image.createImage("/icons/presence_"
					+ mapping[5] + ".png");
			presence_phone_icons[5] = Image.createImage("/icons/presence_"
					+ mapping[5] + "_phone.png");
			presence_icons[6] = Image.createImage("/icons/muc.png");
			presence_icons[7] = Image.createImage("/icons/muc-offline.png");

		} catch (Exception e) {
			// should not happen
		}

		//		presence_icons = new Image[mapping.length];
		//		for (int i = 0; i < presence_icons.length; i++) {
		//			try {
		//				presence_icons[i] = Image.createImage("/icons/presence_"
		//						+ mapping[i] + ".png");
		//			} catch (IOException e) {
		//				presence_icons[i] = Image.createImage(16, 16);
		//			}
		//		}

	}

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

	}

	/** close the connection XXX i don't like this name */
	public void stopClient() {
		// saveToStorage();
	}

	//	/**
	//	 * Add a listener for XMPP packets
	//	 * 
	//	 * @param _q
	//	 *            the xpath like query
	//	 * @param _l
	//	 *            the listener
	//	 * @return The registration object to be used with
	//	 *         {@link #unregisterListener(EventQueryRegistration)} for removing
	//	 *         the listener
	//	 */
	//	public EventQueryRegistration registerListener(EventQuery _q, Object _l) {
	//		return BasicXmlStream.addEventListener(_q, _l);
	//	}
	//
	//	/**
	//	 * Add a one time listener for XMPP packets
	//	 * 
	//	 * @param _q
	//	 *            the xpath like query
	//	 * @param _l
	//	 *            the listener
	//	 * @return The registration object to be used with
	//	 *         {@link #unregisterListener(EventQueryRegistration)} for removing
	//	 *         the listener
	//	 * 
	//	 */
	//	public EventQueryRegistration registerOneTimeListener(EventQuery _q,
	//			Object _l) {
	//		return BasicXmlStream.addOnetimeEventListener(_q, _l);
	//	}
	//
	//	/**
	//	 * Remove a registered listener
	//	 * 
	//	 * @param reg
	//	 *            the registration obtained with
	//	 *            {@link #registerListener(EventQuery, PacketListener)}
	//	 * 
	//	 */
	//	public void unregisterListener(EventQueryRegistration reg) {
	//		BasicXmlStream.removeEventListener(reg);
	//	}

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
		sendIQ(iq, listener, -1);
	}

	/**
	 * Send an Iq packet and register the packet listener for the answer
	 * 
	 * @param iq the iq
	 * @param listener the listener
	 * @param duration the duration before expire in milliseconds
	 */
	public void sendIQ(Iq iq, IQResultListener listener, int duration) {
		if (listener != null) {
			IqManager.getInstance().addRegistration(iq, listener, duration);
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
	 * @param newCredentials 
	 * @return
	 */
	public BasicXmlStream createStream(boolean register, boolean newCredentials) {
		this.newCredentials = newCredentials;
		// this must be done to clean the Roster after a reconnect
		this.roster.purge();
		buildSocketConnection();


		// connection = new SimpleBTConnector(
		// Config.HTTP_GW_HOST,
		// Config.HTTP_GW_PATH,
		// xmlStream
		// );

		// XXX useful with messages? I don't think so
		// eq = new EventQuery(Message.MESSAGE, null, null);
		// eq.child = new EventQuery(Iq.QUERY, new String[] { "xmlns" },
		// new String[] { "http://jabber.org/protocol/disco#items" } );
		// xmlStream.addEventListener(eq, adch);

		if (register) {

			EventQuery qReg = new EventQuery(
					EventDispatcher.STREAM_ACCOUNT_REGISTERED, null, null);
			/*
			 * The registration used to be notified of the registration
			 */
			EventDispatcher.addEventListener(qReg, this);

			accountRegistration = new AccountRegistration();
			xmlStream.addInitializer(accountRegistration, 0);
		}

		return xmlStream;
	}

	/**
	 * Build the low level connection based on plain sockets
	 */
	private void buildSocketConnection() {
		xmlStream = new SocketStream();
		// #ifndef BT_PLAIN_SOCKET
		connection = new SocketChannel("socket://"
				+ cfg.getProperty(Config.CONNECTING_SERVER), xmlStream);
		// #endif
		((SocketChannel) connection).KEEP_ALIVE = Long.parseLong(cfg
				.getProperty(Config.KEEP_ALIVE));
	}


	public void openStream(boolean goOnline) {
		this.goOnline = goOnline;
		String resource = cfg.getProperty(Config.YUP_RESOURCE,
				Config.CLIENT_NAME);
		xmlStream.initialize(cfg.getProperty(Config.USER) + "@"
				+ cfg.getProperty(Config.SERVER) + "/" + resource, cfg
				.getProperty(Config.PASSWORD));

		EventQuery qAuth = new EventQuery(EventDispatcher.STREAM_INITIALIZED,
				null, null);
		/*
		 * The registration used to be notified of the authentication
		 */
		EventDispatcher.addEventListener(qAuth, this);


		if (!connection.isOpen()) {
			connection.open();
		}
	}

	public void closeStream() {
		if (connection.isOpen()) {
			connection.close();
		}
	}

	public void gotStreamEvent(String event, Object source) {
		if (EventDispatcher.STREAM_INITIALIZED.equals(event)) {

			// all these registration are made here 
			// Register the handler for incoming messages
			EventQuery eq = new EventQuery(Message.MESSAGE, null, null);
			eq.child = new EventQuery(Message.BODY, null, null);
			BasicXmlStream.addPacketListener(eq, new MessageHandler(false));

			// a handler only for error type messages
			eq = new EventQuery(Message.MESSAGE,
					new String[] { Message.ATT_TYPE },
					new String[] { Message.ERROR });
			BasicXmlStream.addPacketListener(eq, new MessageHandler(true));

			// Register the presence handler
			eq = new EventQuery(Presence.PRESENCE, null, null);
			BasicXmlStream.addPacketListener(eq, new PresenceHandler());

			// Register the disco handler
			eq = new EventQuery(Iq.IQ, new String[] { "type" },
					new String[] { "get" });
			eq.child = new EventQuery(Iq.QUERY, new String[] { "xmlns" },
					new String[] { NS_IQ_DISCO_INFO });
			BasicXmlStream.addPacketListener(eq, new DiscoHandler());

			// Register the UUID handler
			eq = new EventQuery(Iq.IQ, new String[] { "type" },
					new String[] { "get" });
			eq.child = new EventQuery(XMPPClient.UUID,
					new String[] { "xmlns" }, new String[] { NS_UUID });
			BasicXmlStream.addPacketListener(eq, new UUIDHandler());

			// Register the disco handler
			eq = new EventQuery(Iq.IQ, new String[] { "type" },
					new String[] { "get" });
			eq.child = new EventQuery(Iq.QUERY, new String[] { "xmlns" },
					new String[] { NS_JABBER_VERSION });
			BasicXmlStream.addPacketListener(eq, new PacketListener() {

				public void packetReceived(Element e) {
					Iq reply = Utils.easyReply(e);
					Element query = reply.addElement(NS_JABBER_VERSION,
							Iq.QUERY);
					query
							.addElementAndContent(null, "name",
									Config.CLIENT_NAME);
					query.addElementAndContent(null, "version", cfg
							.getProperty(Config.VERSION));
					query.addElementAndContent(null, "os", "J2ME/"
							+ System.getProperty("microedition.platform"));
					XMPPClient.this.sendPacket(reply);
				}
			});

			// Register the handler for dataforms (both as <iq/> and <message/>)
			// payloads
			DataFormHandler dh = new DataFormHandler();
			eq = new EventQuery(Message.MESSAGE, null, null);
			eq.child = new EventQuery(DataForm.X, new String[] { "xmlns" },
					new String[] { DataForm.NAMESPACE });
			BasicXmlStream.addPacketListener(eq, dh);
			eq = new EventQuery(Iq.IQ, null, null);
			eq.child = new EventQuery(DataForm.X, new String[] { "xmlns" },
					new String[] { DataForm.NAMESPACE });
			BasicXmlStream.addPacketListener(eq, dh);

			//			/* register handler for ad hoc command announcements */
			//			PacketListener ashc_listener = new PacketListener() {
			//				public void packetReceived(Element e) {
			//					handleClientCommands(e, false);
			//				}
			//			};
			//
			//			// XXX here we *must* use client capabilities
			//			/* register handler for ad hoc command presence announce */
			//			eq = new EventQuery(Presence.PRESENCE, null, null);
			//			eq.child = new EventQuery(Iq.QUERY, new String[] { "xmlns" },
			//					new String[] { "http://jabber.org/protocol/disco#items" });
			//			BasicXmlStream.addEventListener(eq, ashc_listener);

			IqManager.getInstance().streamInitialized();
			roster.streamInitialized();

			stream_authenticated();
		} else if (EventDispatcher.STREAM_ACCOUNT_REGISTERED.equals(event)) {
			xmlStream.removeInitializer(accountRegistration);
		}
	}

	public void stream_authenticated() {
		// create the self contact and setup the initial presence
		my_jid = xmlStream.jid;

		me = new Contact(Contact.userhost(my_jid), null, null, null);
		Presence p = new Presence();
		p.setAttribute(Stanza.ATT_FROM, my_jid);
		p.setAttribute(XMPPClient.XML_NS, "lang", Config.lang);
		p.toXml();
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
		Element cap = p.addElement(uri, "c");
		cap.setAttribute("node", XMPPClient.NS_BLUENDO_CAPS);
		cap.setAttribute("hash", "sha-1");
		cap.setAttribute("ver", getCapVer());

		// XXX I don't like this, it could be better to send capabilities with a
		// different hash in the version
		//		Element x = p.addElement(JABBER_X_DATA, "x");
		//		x.setAttribute("type", Iq.T_RESULT);
		//		Element field = x.addElement(JABBER_X_DATA, "field");
		//		field.setAttribute("var", "FORM_TYPE");
		//		field.setAttribute("type", "hidden");
		//		field.addElement(JABBER_X_DATA, "value").addText(MIDP_PLATFORM);
		//
		//		field = x.addElement(JABBER_X_DATA, "field");
		//		field.setAttribute("var", "microedition.platform");
		//		field.addElement(JABBER_X_DATA, "value").addText(
		//				System.getProperty("microedition.platform"));

		me.updatePresence(p);

		// we are connected, set the stream as valid
		valid_stream = true;

		// Listen for lost connections
		lostConnReg = EventDispatcher.addEventListener(new EventQuery(
				EventDispatcher.STREAM_TERMINATED, null, null),
				new EventListener() {

					public void gotStreamEvent(String event, Object source) {
						if (rosterRetrieveTask != null) rosterRetrieveTask
								.cancel();
						valid_stream = false;
						closeStream();
						if (xmppListener != null) xmppListener.connectionLost();
						showAlert(AlertType.ERROR, Config.ALERT_CONNECTION,
								Config.ALERT_CONNECTION, null);
						EventDispatcher.removeEventListener(lostConnReg);
						/* XXX: should close all screens and open the RegisterScreen */
					}
				});

		// if this is the first login it is better to ask the roster and go online suddenly
		// otherwise i only go online and the ask the roster after a while
		// however first try to load the roster from db
		roster.setupStore(Contact.userhost(my_jid));
		roster.loadGateways();
		roster.readFromStorage();
		roster.retrieveBookmarks();

		checkTrustedServices();

		// if already have a UUID I can go online
		// otherwise i ask the UUID and then i will go 
		// online within the chckUUID function
		String myUid = cfg.getProperty(Config.UUID, "-1");
		if (myUid.equals("-1")) {
			checkUUID();
		} else if (goOnline) {
			askRoster();
		}

		if (this.xmppListener != null) xmppListener.authenticated();
	}

	/**
	 * 
	 */
	public void askRoster() {
		if (newCredentials || !"null".equals(this.roster.rosterVersion)) {
			roster.retrieveRoster(true, false);
		} else {
			this.setPresence(-1, null);

			lastPresenceTime = System.currentTimeMillis();
			rosterRetrieveTask = new TimerTask() {
				public void run() {
					if (lastPresenceTime + rosterRetrieveTime < System
							.currentTimeMillis()) {
						roster.retrieveRoster(false, false);
						//roster.retrieveBookmarks();
						this.cancel();
					}
				}
			};
			Utils.tasks.schedule(rosterRetrieveTask, rosterRetrieveTime,
					rosterRetrieveTime);
		}
	}

	/**
	 * @param myUid
	 */
	private void checkUUID() {
		// if i don't have a UUID I ask the server for it
		// check the trusted services
		String myUid = cfg.getProperty(Config.UUID, "-1");
		if (myUid.equals("-1")) {
			Iq UUIDReq = new Iq(Config.LAMPIRO_AGENT + "/lampiro", Iq.T_SET);
			Element command = UUIDReq.addElement(XMPPClient.NS_COMMANDS,
					XMPPClient.COMMAND);
			command.setAttribute("node", "uuid_gen");
			command.setAttribute(XMPPClient.ACTION, "execute");

			IQResultListener resultListener = new XmppIqListener(
					XmppIqListener.UUID);
			this.sendIQ(UUIDReq, resultListener, 240000);
		}
	}

	/**
	 * 
	 */
	private void checkTrustedServices() {
		// check the trusted services
		Iq systemConfigIq = new Iq(Config.LAMPIRO_AGENT, Iq.T_GET);
		Element pubsub = systemConfigIq.addElement(NS_PUBSUB, PUBSUB);
		Element items = pubsub.addElement(null, ITEMS);
		items.setAttribute(NODE, LAMPIRO_CONFIG);
		XmppIqListener resultListener = new XmppIqListener(
				XmppIqListener.TRUSTED_SERVICES);

		this.sendIQ(systemConfigIq, resultListener, 240000);
	}

	private String getCapVer() {
		Vector ss = new Vector();
		ss.addElement("client/");
		ss.addElement("phone/");
		ss.addElement(Config.lang + "/");
		ss.addElement(Config.CLIENT_NAME + " "
				+ cfg.getProperty(Config.VERSION) + " " + J2MEVER + "<");
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

		private boolean handleError = false;

		public MessageHandler(boolean handleError) {
			this.handleError = handleError;
		}

		public void packetReceived(Element p) {

			// different behaviours depending on type

			String type = p.getAttribute(Message.ATT_TYPE);
			if (type == null) type = Message.NORMAL;
			if (handleError == false && type.equals(Message.ERROR)) return;

			// e.g. normal are used to receive invite for MUC;
			// if the MUC user is created here it result in a normal Contact!!!
			// be careful for that 
			Element x = p.getChildByName(XMPPClient.NS_MUC_USER, "x");
			if (x != null && type.equals(Message.GROUPCHAT)) {
				Element invite = x.getChildByName(null, "invite");
				if (invite != null) return;
			}

			Message msg = new Message(p);
			// XXX: we will need to check the type
			String jid = msg.getAttribute(Stanza.ATT_FROM);
			// error packet sometimes do not have from
			if (jid == null) return;
			Contact u = roster.getContactByJid(jid);

			if (u == null) {
				Element group_elements[] = p.getChildrenByName(null, "group");
				String groups[] = new String[group_elements.length];
				for (int j = 0; j < groups.length; j++) {
					groups[j] = group_elements[j].getText();
				}
				u = new Contact(Contact.userhost(jid), p.getAttribute("name"),
						p.getAttribute(XMPPClient.SUBSCRIPTION), groups);
				roster.contacts.put(Contact.userhost(u.jid), u);
			}

			// at this manner only the first msg triggers an update
			u.addMessageToHistory(jid, msg);
			if (xmppListener != null) xmppListener.updateContact(u,
					Contact.CH_MESSAGE_NEW);
			playSmartTone();
		}
	}

	private class PresenceHandler implements PacketListener {

		public void packetReceived(Element e) {
			// #mdebug
//@			Logger.log("PresenceHandler: received packet: "
//@					+ new String(e.toXml()), Logger.DEBUG);
			// #enddebug

			lastPresenceTime = System.currentTimeMillis();
			String t = e.getAttribute(Stanza.ATT_TYPE);
			if (t == null || Presence.T_UNAVAILABLE.equals(t)) {
				Presence p = new Presence(e);

				String from = e.getAttribute(Stanza.ATT_FROM);
				Contact u = roster.getContactByJid(from);
				String type = e.getAttribute(Stanza.ATT_TYPE);
				if (u == null) {
					if (type != null
							&& type.compareTo(Presence.T_UNAVAILABLE) == 0) return;
					// XXX Guess the subscription
					u = new Contact(Contact.userhost(from), null,
							Contact.SUB_UNKNOWN, null);

					u.updatePresence(new Presence(e));
					roster.contacts.put(u.jid, u);
				} else {
					u.updatePresence(p);
				}
				if (xmppListener != null) xmppListener.updateContact(u,
						Contact.CH_STATUS);

			} else if (Presence.T_SUBSCRIBE.equals(t)) {
				handleSubscribe(new Presence(e));
			} else if (Iq.T_ERROR.equals(t) && xmppListener != null) {
				xmppListener.handlePresenceError(new Presence(e));
			} else {
				// XXX At present ignore other cases, but when receiving
				// UNSUBCRIBED we should update the roster
			}
		}

		private void handleSubscribe(Presence p) {
			// try getting the contact (we may already have it)
			String jid = Contact.userhost(p.getAttribute(Stanza.ATT_FROM));
			Contact u = roster.getContactByJid(jid);
			if (u == null) {
				// we don't have the contact, create it
				u = new Contact(jid, null, null, null);
			}

			// subscription handling
			if (Contact.SUB_BOTH.equals(u.subscription)
					|| Contact.SUB_TO.equals(u.subscription)
					|| Config.LAMPIRO_AGENT.equals(Contact.userhost(jid))) {
				// subscribe received: if already granted, I don't ask anything
				Presence pmsg = new Presence();
				pmsg.setAttribute(Stanza.ATT_TO, u.jid);
				pmsg.setAttribute(Stanza.ATT_TYPE, Presence.T_SUBSCRIBED);
				sendPacket(pmsg);
			} else {
				// add a nick only if a previous name has been added
				Element nick = p.getChildByName(XMPPClient.NS_NICK, "nick");
				if (nick != null) {
					String nickNameText = nick.getText();
					if (nickNameText != null && nickNameText.length() > 0
							&& (u.name == null || u.name.length() == 0)) {
						u.name = nickNameText;
					}
				}

				Enumeration en = XMPPClient.this.autoAcceptGateways.elements();
				while (en.hasMoreElements()) {
					String ithGateway = (String) en.nextElement();
					if (u.jid.indexOf(ithGateway) >= 0) {
						XMPPClient.this.roster.subscribeContact(u, true);
						return;
					}
				}
				if (xmppListener != null) {
					xmppListener.askSubscription(u);
				}
			}
		}
	}

	// XXX The roster handler should always listen to any iq:roster packet for
	// supporting roster push and any roster update when logged in

	private class DataFormHandler implements PacketListener {

		public void packetReceived(Element p) {
			SimpleDataFormExecutor task = new SimpleDataFormExecutor(p);
			updateTask(task);
			if (xmppListener != null) xmppListener.handleTask(task);
			playSmartTone();
		}
	}

	private class UUIDHandler implements PacketListener {

		public void packetReceived(Element p) {
			Iq reply = Utils.easyReply(p);
			Element e = reply.addElement(XMPPClient.NS_UUID, XMPPClient.UUID);
			String myUid = cfg.getProperty(Config.UUID, "-1");
			e.addText(myUid);
			sendPacket(reply);
		}
	}

	private class DiscoHandler implements PacketListener {

		public void packetReceived(Element p) {
			Element q = p.getChildByName(NS_IQ_DISCO_INFO, Iq.QUERY);
			Iq reply = new Iq(p.getAttribute(Stanza.ATT_FROM), Iq.T_RESULT);
			reply.setAttribute(Stanza.ATT_ID, p.getAttribute(Stanza.ATT_ID));
			String node = q.getAttribute("node");
			Element qr = reply.addElement(NS_IQ_DISCO_INFO, Iq.QUERY);
			String capDisco = XMPPClient.NS_BLUENDO_CAPS + "#"
					+ XMPPClient.this.getCapVer();
			if (node == null || node.compareTo(capDisco) == 0) {
				Element identity = qr.addElement(NS_IQ_DISCO_INFO, "identity");
				String lampiroName = Config.CLIENT_NAME + " "
						+ cfg.getProperty(Config.VERSION) + " "
						+ XMPPClient.J2MEVER;
				identity.setAttributes(new String[] { "category", "type",
						"name" },
						new String[] { "client", "phone", lampiroName });
				identity.setAttribute(XMPPClient.XML_NS, "lang", Config.lang);
				for (int i = 0; i < features.length; i++) {
					Element feature = qr.addElement(NS_IQ_DISCO_INFO,
							XMPPClient.FEATURE);
					feature.setAttribute("var", features[i]);
				}

			}
			//			else if (MIDP_PLATFORM.equals(node)) {
			//				qr.setAttribute("node", MIDP_PLATFORM);
			//				Element x = qr.addElement(JABBER_X_DATA, "x");
			//				x.setAttribute("type", Iq.T_RESULT);
			//				Element field = x.addElement(JABBER_X_DATA, "field");
			//				field.setAttribute("var", "microedition.platform");
			//				field.addElement(JABBER_X_DATA, "value").addText(
			//						System.getProperty("microedition.platform"));
			//			}
			if (node != null && node.compareTo(capDisco) == 0) {
				qr.setAttribute("node", capDisco);
			}
			sendPacket(reply);
		}
	}

	private class XmppIqListener extends IQResultListener {

		public static final int UUID = 0;
		public static final int TRUSTED_SERVICES = 1;
		public int LISTENER_TYPE;

		public XmppIqListener(int LISTENER_TYPE) {
			this.LISTENER_TYPE = LISTENER_TYPE;
		}

		public void handleError(Element e) {
			switch (LISTENER_TYPE) {
				case UUID:
					// i go online and ask roster after having the UUID
					// or receiving an error
					saveAndAskRoster(null);
					break;

				default:
					break;
			}
			// #mdebug
//@			System.out.println(e.toXml());
			// #enddebug
		}

		/**
		 * @param e
		 */
		private void setupTrustedServices(Element e) {
			Element config = e.getChildByName(NS_PUBSUB, PUBSUB);
			if (config != null) {
				Element[] items = config.getChildrenByNameAttrs(NS_PUBSUB,
						ITEMS, new String[] { NODE },
						new String[] { LAMPIRO_CONFIG });
				if (items.length > 0) {
					Element[] gateways = items[0].getChildrenByNameAttrs(null,
							ITEM, new String[] { "id" },
							new String[] { "gateways" });
					if (gateways.length > 0) {
						gatewayConfig = SystemConfig.parse(gateways[0]);
						if (gatewayConfig != null) gatewayConfig = (Hashtable) gatewayConfig
								.get("gateways");
					}
				}
			}
		}

		/**
		 * @param myUid
		 */
		private void saveAndAskRoster(String myUid) {
			// with a wrong myUID i use a default one
			if (myUid == null) myUid = System.currentTimeMillis() + "";
			cfg.setProperty(Config.UUID, myUid);
			cfg.saveToStorage();
			// i go online and ask roster after having the UUID
			if (goOnline) {
				askRoster();
			}
		}

		public void handleResult(Element e) {
			switch (LISTENER_TYPE) {
				case TRUSTED_SERVICES:
					setupTrustedServices(e);
					break;

				case UUID:
					saveUUID(e);
					break;

				default:
					break;
			}
		}

		/**
		 * @param e
		 */
		private void saveUUID(Element e) {
			Element x = e.getPath(new String[] { null, null }, new String[] {
					XMPPClient.COMMAND, "x" });
			String myUid = null;
			if (x != null) {
				Element[] fields = x.getChildrenByNameAttrs(null, "field",
						new String[] { "var" },
						new String[] { XMPPClient.UUID });
				if (fields.length > 0) {
					myUid = fields[0].getChildByName(null, "value").getText();

				}
			}
			saveAndAskRoster(myUid);
		}
	}

	public void playSmartTone() {
		if (this.xmppListener != null) {
			xmppListener.playSmartTone();
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
		if (p == null) return;
		Presence new_p = new Presence();

		// set my lang
		new_p.setAttribute(XMPPClient.XML_NS, "lang", Config.lang);
		new_p.setAttribute(Stanza.ATT_FROM, p.getAttribute(Stanza.ATT_FROM));

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

		//new_p.addElement(p.getChildByName(null, "x"));
		new_p.addElement(p.getChildByName(null, "c"));
		new_p.setPriority(p.getPriority());
		me.updatePresence(new_p);
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
	 * @param optionalArguments 
	 */
	public void handleClientCommands(Element e) {
		String from = e.getAttribute(Stanza.ATT_FROM);
		if (from == null) return;
		Contact c = roster.getContactByJid(from);
		if (c == null) return;
		Element q = e.getChildByName(XMPPClient.NS_IQ_DISCO_ITEMS, Iq.QUERY);
		if (q != null) {
			Element items[] = q.getChildrenByName(NS_IQ_DISCO_ITEMS, ITEM);
			c.cmdlist = new String[items.length][2];
			for (int i = 0; i < items.length; i++) {
				c.cmdlist[i][0] = items[i].getAttribute(NODE);
				c.cmdlist[i][1] = items[i].getAttribute("name");
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
	//	public void showAlert(AlertType type, String title, String text,
	//			final Object next_screen) {
	//		if (xmppListener != null) {
	//			xmppListener.showAlert(type, title, text, next_screen);
	//		}
	//	}
	public void showAlert(AlertType type, int titleCode, int textCode,
			String additionalText) {
		if (xmppListener != null) {
			xmppListener.showAlert(type, titleCode, textCode, additionalText);
		}
	}

	/**
	 * Update the status of a task. Queue it if this is the first time it's
	 * status is updated
	 * 
	 * @param task
	 */
	public void updateTask(Task task) {
		Contact user = roster.getContactByJid(task.getFrom());
		// I may wish to execute commands even on unknown contacts
		if (user == null) {
			user = new Contact(Contact.userhost(task.getFrom()), null, null,
					null);
			roster.contacts.put(Contact.userhost(user.jid), user);
		}
		user.addTask(task);
		// #mdebug
//@		System.out.println("Tsk: " + Integer.toHexString(task.getStatus()));
		//#enddebug
		byte type = task.getStatus();

		// true if we should display the command
		boolean display = false;
		boolean removed = false;

		if ((type & Task.CMD_MASK) == Task.CMD_MASK) {
			switch (type) {
				case Task.CMD_FORM_LESS:
					display = false;
					removed = true;
					user.removeTask(task);
					break;
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
					display = true;
					removed = true;
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
					display = true;
					removed = true;
					break;
				case Task.DF_DESTROY:
					removed = true;
					user.removeTask(task);
					break;
			}
		}

		task.setEnableDisplay(display);
		task.setEnableNew(!removed);

		// update contact position in the roster
		//		if (xmppListener != null) {
		//			xmppListener.handleTask(task, display,
		//					removed ? Contact.CH_TASK_REMOVED : Contact.CH_TASK_NEW);
		//		}
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

	public Roster getRoster() {
		return roster;
	}

	public void setXmppListener(XMPPClient.XmppListener xmppListener) {
		this.xmppListener = xmppListener;
	}

	public XMPPClient.XmppListener getXmppListener() {
		return xmppListener;
	}

	public static String getErrorString(String code) {
		for (int i = 0; i < XMPPClient.errorCodes.length; i++) {
			String[] ithCode = XMPPClient.errorCodes[i];
			if (ithCode[0].equals(code)) { return (ithCode[1]); }
		}
		return "";
	}

	public void setMUCGroup(String str) {
		MUC.GROUP_CHATS = str;
	}
}
