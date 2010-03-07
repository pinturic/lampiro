/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: Roster.java 2002 2010-03-06 19:02:12Z luca $
*/

package it.yup.xmpp;

// #mdebug
//@
//@import it.yup.util.Logger;
//@
// #enddebug

import it.yup.xml.BProcessor;
import it.yup.xml.Element;
import it.yup.xmlstream.BasicXmlStream;
import it.yup.xmlstream.EventQuery;
import it.yup.xmlstream.PacketListener;
import it.yup.xmpp.XMPPClient.XmppListener;
import it.yup.xmpp.packets.DataForm;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Presence;
import it.yup.xmpp.packets.Stanza;
import it.yup.util.RMSIndex;
import it.yup.util.Utils;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.AlertType;

public class Roster implements PacketListener {

	private class RosterIqListener extends IQResultListener {

		public static final int BOOKMARK = 0;
		public static final int ROSTER = 1;
		public static final int SUBSCRIBE = 2;
		public static final int UPDATE_GATEWAY = 3;
		private int LISTENER_TYPE = 0;
		public boolean go_online;
		public boolean accept;
		public Contact c;

		public RosterIqListener(int LISTENER_TYPE) {
			this.LISTENER_TYPE = LISTENER_TYPE;
		}

		public void handleError(Element e) {
			switch (LISTENER_TYPE) {
				case BOOKMARK:
					serverStorage = false;
					break;

				default:
					break;
			}
			// #mdebug
//@			System.out.println(e.toXml());
			// #enddebug
		}

		public void handleResult(Element e) {
			switch (LISTENER_TYPE) {
				case BOOKMARK:
					setupBookmark(e);
					break;
				case ROSTER:
					setupRoster(e);
					break;
				case SUBSCRIBE:
					handleSubscribe();
					break;
				case UPDATE_GATEWAY:
					updateGateway(e);
					break;
				default:
					break;
			}

		}

		/**
		 * @param e
		 */
		private void updateGateway(Element e) {
			String type = null;
			String name = "";
			String from = e.getAttribute(Stanza.ATT_FROM);

			Element identity = e.getPath(new String[] {
					XMPPClient.NS_IQ_DISCO_INFO, XMPPClient.NS_IQ_DISCO_INFO },
					new String[] { Iq.QUERY, "identity" });
			if (identity != null) {
				type = identity.getAttribute("type");
				String category = identity.getAttribute("category");
				name = identity.getAttribute("name");
				if (category.compareTo("gateway") == 0) {
					Roster.this.registeredGateways.put(from, new String[] {
							type, name });
					saveGateways();

					// to notify the RosterScreen of the gateway presence 
					if (client.getXmppListener() != null) client
							.getXmppListener().updateContact(c,
									Contact.CH_STATUS);
				}
			}
		}

		/**
		 * 
		 */
		private void handleSubscribe() {
			Presence psub;
			if (accept) {
				psub = new Presence(Presence.T_SUBSCRIBED, null, null, -1);
				psub.setAttribute(Stanza.ATT_TO, c.jid);
				client.sendPacket(psub);
			}
			psub = new Presence(Presence.T_SUBSCRIBE, null, null, -1);
			psub.setAttribute(Stanza.ATT_TO, c.jid);
			client.sendPacket(psub);
		}

		/**
		 * @param e
		 */
		private void setupRoster(Element e) {
			recreateRoster(e, true);
			saveToStorage();
			if (go_online) {
				client.setPresence(-1, null);
			}

			XmppListener listener = XMPPClient.getInstance().getXmppListener();
			if (listener != null) listener.rosterRetrieved();
		}

		/**
		 * @param e
		 */
		private void setupBookmark(Element e) {
			serverStorage = true;
			Element storage = e.getPath(new String[] { null,
					XMPPClient.NS_BOOKMARKS }, new String[] { Iq.QUERY,
					XMPPClient.STORAGE });
			if (storage != null) {
				privateStorage.removeChild(XMPPClient.NS_BOOKMARKS,
						XMPPClient.STORAGE);
				privateStorage.addElement(storage);
			}

			Element exStorage = e.getPath(new String[] { null,
					XMPPClient.NS_STORAGE_LAMPIRO }, new String[] { Iq.QUERY,
					XMPPClient.STORAGE });
			if (exStorage != null) {
				privateStorage.removeChild(XMPPClient.NS_STORAGE_LAMPIRO,
						XMPPClient.STORAGE);
				privateStorage.addElement(exStorage);
			}

			Element[] conferences = storage.getChildrenByName(null,
					XMPPClient.CONFERENCE);
			for (int i = 0; i < conferences.length; i++) {
				Element el = conferences[i];
				String jid = el.getAttribute("jid");
				Element nickEl = el.getChildByName(null, "nick");
				Element pwdEl = el.getChildByName(null, "password");
				String nick = nickEl != null ? nickEl.getText() : null;
				String pwd = pwdEl != null ? pwdEl.getText() : null;
				String autoJoinString = el.getAttribute(MUC.AUTO_JOIN);
				boolean autoJoin = false;
				if (autoJoinString != null && autoJoinString.equals("true")) autoJoin = true;

				Element[] extEl = exStorage.getChildrenByNameAttrs(null,
						XMPPClient.CONFERENCE, new String[] { "jid" },
						new String[] { jid });

				boolean lampiroAutoJoin = autoJoin;
				if (extEl.length > 0) {
					if (extEl[0].getAttribute(MUC.LAMPIRO_AUTO_JOIN).equals(
							"false")) {
						lampiroAutoJoin = false;
					} else {
						lampiroAutoJoin = true;
					}
				}

				Contact c = getContactByJid(Contact.user(jid));
				if (c != null && c instanceof MUC == false) {
					contacts.remove(c.jid);
					if (client.getXmppListener() != null) client
							.getXmppListener().removeContact(c);
				}
				MUC muc = createMuc(jid, Contact.user(jid), nick, pwd,
						lampiroAutoJoin);
				if (client.getXmppListener() != null) client.getXmppListener()
						.updateContact(muc, Contact.CH_STATUS);

			}
		}

	}

	/*
	 *	Implements the XEP for roster push 
	 */
	class RosterX implements PacketListener {
		public RosterX() {
			EventQuery q = new EventQuery("message", null, null);
			EventQuery x = new EventQuery("x", new String[] { "xmlns" },
					new String[] { XMPPClient.NS_ROSTERX });
			q.child = x;
			BasicXmlStream.addEventListener(q, this);

			q = new EventQuery("iq", new String[] { Iq.ATT_TYPE },
					new String[] { Iq.T_SET });
			q.child = x;
			BasicXmlStream.addEventListener(q, this);
		}

		public void packetReceived(Element e) {
			//System.out.println(new String(e.toXml()));
			// check the packet sender
			// check what to do with contacts 
			// answer in case it is an Iq
			if (client.getXmppListener() != null) client.getXmppListener()
					.rosterXsubscription(e);
		}
	}

	/*
	 * The roster version
	 */
	String rosterVersion = "0";

	/** All contacts */
	public Hashtable contacts = new Hashtable();

	public boolean serverStorage = false;
	/** All contacts */
	private Element privateStorage = null;

	private XMPPClient client;

	private RosterX rosterX;

	public static String unGroupedCode = new String(
			new char[] { ((char) 0x08) });

	public Hashtable registeredGateways = new Hashtable(5);

	Roster(XMPPClient _client) {
		client = _client;
		privateStorage = new Element(XMPPClient.NS_PRIVATE, Iq.QUERY);
		privateStorage.addElement(new Element(XMPPClient.NS_BOOKMARKS,
				XMPPClient.STORAGE));
		privateStorage.addElement(new Element(XMPPClient.NS_STORAGE_LAMPIRO,
				XMPPClient.STORAGE));
	}

	public void streamInitialized() {
		EventQuery eq = new EventQuery(Iq.IQ, new String[] { "type" },
				new String[] { "set" });
		eq.child = new EventQuery(Iq.QUERY, new String[] { "xmlns" },
				new String[] { XMPPClient.NS_IQ_ROSTER });
		BasicXmlStream.addEventListener(eq, this);
		this.rosterX = new RosterX();
	}

	RMSIndex rosterStore;

	/*
	 * The configuration instance
	 */
	private Config cfg = Config.getInstance();

	/**
	 * Read the contacts from the RMS
	 * 
	 */
	protected synchronized void readFromStorage() {
		try {
			rosterStore.open();
			byte[] rosterData = rosterStore.load(Utils.getBytesUtf8("roster"));
			if (rosterData != null) {
				Element rosterEl = BProcessor.parse(rosterData);
				rosterVersion = rosterEl.getAttribute("ver");
				if (rosterVersion == null) rosterVersion = "null";
				Element[] children = rosterEl.getChildrenByName(null, "group");
				for (int i = 0; i < children.length; i++) {
					Element ithChild = children[i];
					String gName = ithChild.getText();
					byte[] gData = rosterStore.load(Utils.getBytesUtf8(gName));
					if (gData != null) {
						Element gEl = BProcessor.parse(gData);
						Element[] gChildren = gEl.getChildrenByName(null,
								"item");
						for (int j = 0; j < gChildren.length; j++) {
							Element item = gChildren[j];
							this.updateRosterItem(item);
						}
					}
				}
			}
		} catch (Exception e) {
			// #mdebug
//@			Logger.log("Error in reading from storage: " + e.getMessage(),
//@					Logger.DEBUG);
//@			e.printStackTrace();
			// #enddebug
			client.showAlert(AlertType.ERROR, Config.ALERT_DATA,
					Config.ALERT_DATA, e.getClass().toString());
		} finally {
			rosterStore.close();
		}
		// #mdebug
//@		Logger.log("Finish read from storage:" + System.currentTimeMillis());
		// #enddebug
	}

	/**
	 * Save the roster to the RMS
	 * 
	 */
	protected synchronized void saveToStorage() {
		try {
			rosterStore.open();
			Element rosterEl = new Element(XMPPClient.NS_IQ_ROSTER, "roster");
			if (rosterVersion == null) rosterVersion = "null";
			rosterEl.setAttribute("ver", this.rosterVersion);
			Hashtable groups = Group.getGroups();
			Enumeration en = groups.elements();
			while (en.hasMoreElements()) {
				Group g = (Group) en.nextElement();
				byte[] groupData = BProcessor.toBinary(g.store());
				rosterStore.store(Utils.getBytesUtf8(g.name), groupData);
				Element groupEl = new Element(XMPPClient.NS_IQ_ROSTER, "group");
				groupEl.addText(g.name);
				rosterEl.addElement(groupEl);
			}
			rosterStore.store(Utils.getBytesUtf8("roster"), BProcessor
					.toBinary(rosterEl));
		} catch (Exception e) {
			// #mdebug
//@			Logger.log("Error in saving to storage: " + e.getMessage(),
//@					Logger.DEBUG);
			// #enddebug
			client.showAlert(AlertType.ERROR, Config.ALERT_DATA,
					Config.ALERT_DATA, e.getClass().toString());
		} finally {
			rosterStore.close();
		}
	}

	public void packetReceived(Element e) {
		// #mdebug
//@		Logger.log("RosterHandler: received packet: " + new String(e.toXml()),
//@				Logger.DEBUG);
		// #enddebug

		Element query = e.getChildByName(null, Iq.QUERY);
		Element items[] = query.getChildrenByName(null, "item");
		for (int i = 0; i < items.length; i++) {
			updateRosterItem(items[i]);
		}
		String tempVer = query.getAttribute("ver");
		if (tempVer != null) this.rosterVersion = tempVer;
		saveToStorage();
	}

	/**
	 * Send a roster query
	 * 
	 * @param go_online
	 *            if true we go online when received the roster
	 */
	public void retrieveRoster(final boolean go_online, boolean purge) {
		//ask the roster and after the bookmarks
		Iq iq_roster = new Iq(null, Iq.T_GET);
		Element query = iq_roster.addElement(XMPPClient.NS_IQ_ROSTER, Iq.QUERY);
		query.setAttribute("ver", this.rosterVersion);
		RosterIqListener rosterListener = new RosterIqListener(
				RosterIqListener.ROSTER);
		rosterListener.go_online = go_online;
		client.sendIQ(iq_roster, rosterListener,240000);
	}

	public void retrieveBookmarks() {
		Element query;
		Iq iq_bookMarks = new Iq(null, Iq.T_GET);
		query = iq_bookMarks.addElement(XMPPClient.NS_PRIVATE, Iq.QUERY);
		query.addElement(XMPPClient.NS_BOOKMARKS, XMPPClient.STORAGE);
		query.addElement(XMPPClient.NS_STORAGE_LAMPIRO, XMPPClient.STORAGE);
		IQResultListener bookmarkListener = new RosterIqListener(
				RosterIqListener.BOOKMARK);
		client.sendIQ(iq_bookMarks, bookmarkListener,240000);
	}

	/**
	 * Subscribe to a contact. Adding a contact fires the transmission of two
	 * messages: an iq of type set for updating the roster, and a presence of
	 * type subscribe
	 * @param c: the contact to be subscribed
	 * @param accept: true if this is a response to a subscribe request
	 */
	public void subscribeContact(Contact c, final boolean accept) {
		contacts.put(c.jid, c);
		Iq iq_roster = new Iq(null, Iq.T_SET);
		Element query = iq_roster.addElement(XMPPClient.NS_IQ_ROSTER, Iq.QUERY);
		Element item = query.addElement(XMPPClient.NS_IQ_ROSTER, "item");
		item.setAttribute("jid", c.jid);
		if (c.name.length() > 0) {
			item.setAttribute("name", c.name);
		}
		for (int i = 0; i < c.getGroups().length; i++) {
			item.addElement(XMPPClient.NS_IQ_ROSTER, "group").addText(
					c.getGroups()[i]);
		}
		if (c.getGroups().length == 0) this.addGatewayGroup(c, item);

		RosterIqListener subscribeListener = new RosterIqListener(
				RosterIqListener.SUBSCRIBE);
		subscribeListener.accept = accept;
		subscribeListener.c = c;
		client.sendIQ(iq_roster, subscribeListener);
		// recreateGroups();
	}

	private void addGatewayGroup(Contact c, Element item) {
		Enumeration en = registeredGateways.keys();
		while (en.hasMoreElements()) {
			String from = (String) en.nextElement();
			String domain = Contact.domain(c.jid);
			if (from.equals(domain)) {
				item.addElement(XMPPClient.NS_IQ_ROSTER, "group").addText(
						((String[]) (registeredGateways.get(from)))[0]);
				break;
			}
		}
	}

	/** remove a contact */

	public void unsubscribeContact(Contact c) {
		contacts.remove(c.jid);
		Iq iq_roster = new Iq(null, Iq.T_SET);
		Element query = iq_roster.addElement(XMPPClient.NS_IQ_ROSTER, Iq.QUERY);
		Element item = query.addElement(XMPPClient.NS_IQ_ROSTER, "item");
		item.setAttribute("jid", c.jid);
		item.setAttribute("subscription", "remove");
		client.sendPacket(iq_roster);
		// recreateGroups();
	}

	private void recreateRoster(Element iq, boolean purge) {

		// XXX -> this should be run within a synchronized

		//		// Build a lookup table with roster
		//		Hashtable oldrst = new Hashtable();
		//		Enumeration en = contacts.elements();
		//		while (en.hasMoreElements()) {
		//			Contact c = (Contact) en.nextElement();
		//			oldrst.put(c.jid, c);
		//		}

		Element query = iq.getChildByName(null, Iq.QUERY);
		if (query == null) { return; }
		String tempVer = query.getAttribute("ver");
		if (tempVer != null) this.rosterVersion = tempVer;
		Element items[] = query.getChildrenByName(null, "item");
		if (purge) {
			Hashtable newContacts = new Hashtable();
			// the old contacts that have a presence but are not 
			// in the roster
			Vector oldUnRosterContacts = new Vector();
			for (int i = 0; i < items.length; i++) {
				Contact c = getContactByJid(items[i].getAttribute("jid"));
				if (c != null) newContacts.put(c.jid, c);
			}
			Enumeration en = this.contacts.keys();
			while (en.hasMoreElements()) {
				Object ithElem = en.nextElement();
				Contact contactToRemove = (Contact) this.contacts.get(ithElem);
				// Presence[] ps = contactToRemove.getAllPresences();
				if (newContacts.containsKey(ithElem) == false) {
					if (contactToRemove.isVisible() == false /*ps == null || ps.length == 0*/) {
						if (client.getXmppListener() != null) client
								.getXmppListener().removeContact(
										contactToRemove);
					} else {
						newContacts.put(contactToRemove.jid, contactToRemove);
						oldUnRosterContacts.addElement(contactToRemove);
					}
				}
			}
			this.contacts = newContacts;
			// these old contacts must be updated
			en = oldUnRosterContacts.elements();
			while (en.hasMoreElements()) {
				if (client.getXmppListener() != null) client.getXmppListener()
						.updateContact((Contact) en.nextElement(),
								Contact.CH_STATUS);
			}
		}

		for (int i = 0; i < items.length; i++) {
			updateRosterItem(items[i]);
		}

		// add the server contact
		// XXX: is it correct to do it here ?
		// and/or is it the nicest way to do it
		XMPPClient me = XMPPClient.getInstance();
		String myDomain = Contact.domain(me.my_jid);
		Contact c = getContactByJid(myDomain);
		if (c == null) {
			Element serverEl = new Element("", "serverEl");
			serverEl.setAttributes(new String[] { Iq.ATT_TO, "jid", "name",
					"subscription" }, new String[] { me.my_jid, myDomain,
					"Jabber Server", Contact.SUB_BOTH });
			updateRosterItem(serverEl);
			/// create a a fictitious presence
			Presence p = new Presence(me.my_jid, Presence.T_SUBSCRIBED,
					"online", "Jabber Server", 1);
			p.setAttribute(Presence.ATT_FROM, myDomain);
			c = getContactByJid(myDomain);
			c.updatePresence(p);
			updateRosterItem(serverEl);
		}
	}

	/**
	 * Update roster item
	 * 
	 * @param item
	 */
	private void updateRosterItem(Element item) {
		// XXX handle the case in which the subscription is "remove"
		// XXX: A lot of the group logic should be redone
		//	for example I don't like all the translations between group <--> String and so on.. ugly
		String jid = item.getAttribute("jid");
		boolean changedGroups = false;

		Element group_elements[] = item.getChildrenByName(null, "group");
		String groups[] = new String[group_elements.length];
		for (int j = 0; j < groups.length; j++) {
			groups[j] = group_elements[j].getText();
		}

		// "ungrouped" contact if no group assign the ungrouped
		Contact c = getContactByJid(jid);
		if (c == null) {
			c = new Contact(jid, item.getAttribute("name"), item
					.getAttribute("subscription"), groups);
		} else {
			// contact found, just update
			c.subscription = item.getAttribute("subscription");
			String name = item.getAttribute("name");
			if (name != null) {
				c.name = name;
			}
			changedGroups = c.setGroups(groups);
		}

		if (changedGroups) {
			if (client.getXmppListener() != null) client.getXmppListener()
					.updateContact(c, Contact.CH_GROUP);
		}

		// XXX not sure that is completely correct...
		String subscription = item.getAttribute("subscription");
		if (subscription != null && subscription.compareTo("remove") == 0) {
			// if the user has removed me from roster
			// there is nothing to do remove contacts and nothing all
			contacts.remove(c.jid);
			if (client.getXmppListener() != null) client.getXmppListener()
					.removeContact(c);
			return;
		}

		contacts.put(c.jid, c);
		// check if this contact is one of my registered gateways
		updateGateways(c);
		if (client.getXmppListener() != null) client.getXmppListener()
				.updateContact(c, Contact.CH_STATUS);
	}

	/*
	 * Load the registered gateways from recordStore
	 */
	public synchronized void loadGateways() {
		rosterStore.open();

		try {
			byte[] gwBytes = rosterStore.load(Utils
					.getBytesUtf8(Config.REGISTERED_GATEWAYS));

			// to check it is a valid xml
			if (gwBytes == null || gwBytes.length == 0) return;

			Element decodedPacket = null;
			try {
				decodedPacket = BProcessor.parse(gwBytes);
			} catch (Exception e) {
				// #mdebug
//@				e.printStackTrace();
//@				Logger.log("In loading gateways" + e.getClass().getName()
//@						+ "\n" + e.getMessage());
				//#enddebug
				return;
			}

			Element[] children = decodedPacket.getChildren();
			try {
				for (int i = 0; i < children.length; i++) {
					Element ithElem = children[i];
					String ithFrom = ithElem.getChildByName(null,
							Stanza.ATT_FROM).getText();
					String ithType = ithElem.getChildByName(null, "type")
							.getText();
					String ithName = ithElem.getChildByName(null, "name")
							.getText();
					this.registeredGateways.put(ithFrom, new String[] {
							ithType, ithName });
				}
			} catch (Exception e) {
				// corrupted configuration reset it
				cfg.setData(Config.REGISTERED_GATEWAYS.getBytes(),
						new byte[] {});
			}
		} catch (Exception e) {
			rosterStore.store(Utils.getBytesUtf8(Config.REGISTERED_GATEWAYS),
					new byte[] {});
		} finally {
			rosterStore.close();
		}
	}

	/*
	 * save the registered gateways to recordStore
	 */
	private synchronized void saveGateways() {
		Element el = new Element("", "gws");
		Enumeration en = this.registeredGateways.keys();
		while (en.hasMoreElements()) {
			String ithFrom = (String) en.nextElement();
			String[] data = (String[]) this.registeredGateways.get(ithFrom);
			String ithType = data[0];
			String ithName = data[1];
			Element gw = el.addElement(null, "gw");
			gw.addElement(null, Stanza.ATT_FROM).addText(ithFrom);
			gw.addElement(null, "type").addText(ithType);
			gw.addElement(null, "name").addText(ithName);
		}

		rosterStore.open();
		try {
			rosterStore.store(Utils.getBytesUtf8(Config.REGISTERED_GATEWAYS),
					BProcessor.toBinary(el));
		} catch (Exception e) {

		} finally {
			rosterStore.close();
		}
	}

	/*
	 * Check if contact is a gateway and in case 
	 * start the procedure to add it to the registered gateways
	 * 
	 * @param c
	 * 		The contact to check for
	 */
	private void updateGateways(final Contact c) {
		if (c.jid.indexOf('@') >= 0
				|| registeredGateways.containsKey(Contact.userhost(c.jid))) return;

		RosterIqListener gw = new RosterIqListener(
				RosterIqListener.UPDATE_GATEWAY);
		gw.c = c;

		Iq iq = new Iq(c.jid, Iq.T_GET);
		iq.addElement(XMPPClient.NS_IQ_DISCO_INFO, Iq.QUERY);
		XMPPClient.getInstance().sendIQ(iq, gw,240000);
	}

	public Contact getContactByJid(String jid) {
		return (Contact) contacts.get(Contact.userhost(jid));
	}

	public Element getBookmarkByJid(String jid, boolean extended) {
		// TODO Auto-generated method stub
		String ns = extended == false ? XMPPClient.NS_BOOKMARKS
				: XMPPClient.NS_STORAGE_LAMPIRO;
		Element storage = this.privateStorage.getChildByName(ns, "storage");
		Element[] conference = storage.getChildrenByNameAttrs(null,
				XMPPClient.CONFERENCE, new String[] { "jid" },
				new String[] { jid });
		if (conference.length > 0) return conference[0];
		return null;
	}

	public void purge() {
		this.contacts.clear();
	}

	public MUC createMuc(String mucJid, String mucName, String nick,
			String pwd, boolean joinNow) {
		// first check if for some reason a contact 
		// with that jid already exists
		MUC u = null;
		Contact myContact = client.getMyContact();
		Contact c = this.getContactByJid(mucJid);
		String roomNick = nick != null ? nick : Contact.user(myContact
				.getPrintableName());
		if (c != null && c instanceof MUC == true) {
			u = (MUC) c;
			// the nick could have changed
			u.nick = nick;
			u.pwd = pwd;
		}
		if (u == null) {
			u = new MUC(mucJid, mucName, nick, pwd);
			client.roster.contacts.put(u.jid, u);
		}
		if (joinNow == false) return u;

		Presence pres = new Presence(myContact.getPresence(null));
		pres.setAttribute(Stanza.ATT_TO, mucJid + "/" + roomNick);
		Element el = new Element(XMPPClient.NS_MUC, DataForm.X);
		pres.addElement(el);
		if (pwd != null) el.addElement(null, "password").addText(pwd);

		client.sendPacket(pres);

		return u;
	}

	public void saveMUC(MUC u, boolean persistent, boolean autojoin,
			boolean lampiroauto_join) {
		Element storage = privateStorage.getChildByName(
				XMPPClient.NS_BOOKMARKS, XMPPClient.STORAGE);
		Element[] conferences = storage.getChildrenByNameAttrs(null,
				XMPPClient.CONFERENCE, new String[] { "jid" },
				new String[] { u.jid });
		Element conference = null;
		if (conferences.length == 0) {
			conference = new Element(XMPPClient.NS_BOOKMARKS,
					XMPPClient.CONFERENCE);
			storage.addElement(conference);
		} else {
			conference = conferences[0];
		}
		if (u.nick != null) {
			Element nickEl = conference.getChildByName(null, "nick");
			if (nickEl == null) nickEl = conference.addElement(
					XMPPClient.NS_BOOKMARKS, "nick");
			nickEl.resetText();
			nickEl.addText(u.nick);
		}
		if (u.pwd != null) {
			Element pwdEl = conference.getChildByName(null, "password");
			if (pwdEl == null) pwdEl = conference.addElement(
					XMPPClient.NS_BOOKMARKS, "password");
			pwdEl.resetText();
			pwdEl.addText(u.pwd);
		}

		if (persistent == false) storage.removeChild(conference);

		conference.setAttribute(MUC.AUTO_JOIN, autojoin ? "true" : "false");
		conference.setAttribute("jid", u.jid);
		conference.setAttribute("name", Contact.user(u.jid));

		storage = privateStorage.getChildByName(XMPPClient.NS_STORAGE_LAMPIRO,
				XMPPClient.STORAGE);
		conferences = storage.getChildrenByNameAttrs(null,
				XMPPClient.CONFERENCE, new String[] { "jid" },
				new String[] { u.jid });
		Element extConference = null;
		if (conferences.length == 0) {
			extConference = new Element(XMPPClient.NS_STORAGE_LAMPIRO,
					XMPPClient.CONFERENCE);
			storage.addElement(extConference);
		} else {
			extConference = conferences[0];
		}

		// if the conference is not persistent or it does not exist anymore i need to
		// remove even the ext conference
		if (persistent == false) {
			storage.removeChild(extConference);
		}

		extConference.setAttribute(MUC.LAMPIRO_AUTO_JOIN,
				lampiroauto_join ? "true" : "false");
		extConference.setAttribute("jid", u.jid);

		if (serverStorage == true) {
			Iq iq = new Iq(null, Iq.T_SET);
			iq.addElement(privateStorage);
			XMPPClient.getInstance().sendIQ(iq, null,240000);
		}

	}

	// XXX temporary removed
	// private void recreateGroups() {
	//	    
	// // unclassified users are group 0, remove all other groups
	// groups.removeAllElements();
	// Group ng = new Group("No Group");
	// groups.addElement(ng);
	//	    
	// Group gi;
	// Enumeration en = contacts.elements();
	// while(en.hasMoreElements()) {
	// Contact c = (Contact) en.nextElement();
	//
	// // the contact is not in any group
	// if(c.groups.length == 0) {
	// ng.addContact(c);
	// } else {
	//
	// // add a contact in all the pertaining groups
	// for(int p = 0; p < c.groups.length; p++) {
	// gi = findGroup(c.groups[p]);
	// gi.addContact(c);
	// }
	// }
	// }
	// }

	public void cleanAndRetrieve() {
		readFromStorage();
		retrieveBookmarks();
		retrieveRoster(false, true);
	}

	void setupStore(String my_jid) {
		String rmsName = getRecordStoreName(my_jid);
		this.rosterStore = new RMSIndex(rmsName);
	}


	/**
	 * @param my_jid
	 * @return
	 */
	public String getRecordStoreName(String my_jid) {
		String rmsName = Utils.jabberify("rstr_" + my_jid, 31);
		return rmsName;
	}

	// private Group findGroup(String gname) {
	// Group g = null;
	// for(int i = 1; i < groups.size(); i++) {
	// g = (Group)groups.elementAt(i);
	// if(g.name.equals(gname)) {
	// return g;
	// }
	// }
	//	    
	// /* arrivando qui, non ho trovato il gruppo */
	// g = new Group(gname);
	// groups.addElement(g);
	// return g;
	// }
}
