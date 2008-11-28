/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: Roster.java 880 2008-10-01 15:44:07Z luca $
*/

package it.yup.xmpp;

// #ifdef UI 
import it.yup.ui.UICanvas;
import lampiro.screens.RegisterScreen;
import lampiro.screens.RosterScreen;

// #endif
// #ifndef UI
//@
//@import lampiro.LampiroMidlet;
//@import it.yup.screens.RosterScreen;
//@
// #endif

// #debug
//@import it.yup.util.Logger;
import it.yup.xmlstream.BasicXmlStream;
import it.yup.xmlstream.Element;
import it.yup.xmlstream.EventQuery;
import it.yup.xmlstream.PacketListener;
import it.yup.xmpp.packets.IQResultListener;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Presence;
import it.yup.xmpp.packets.Stanza;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.AlertType;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreNotFoundException;

import com.sun.perseus.model.Set;

public class Roster implements PacketListener {

	/** All contacts */
	public Hashtable contacts = new Hashtable();

	/** the group list */
	public Vector groups = new Vector();

	private XMPPClient client;

	public static String NS_IQ_ROSTER = "jabber:iq:roster";

	Roster(XMPPClient _client) {
		client = _client;
	}

	protected void associateWithStream(BasicXmlStream stream) {
		// Register the handler for roster packets of type 'set'
		EventQuery eq = new EventQuery(Iq.IQ, new String[] { "type" },
				new String[] { "set" });
		eq.child = new EventQuery(Iq.QUERY, new String[] { "xmlns" },
				new String[] { NS_IQ_ROSTER });
		stream.addEventListener(eq, this);
	}

	/**
	 * Read the contacts from the RMS
	 * 
	 */
	protected void readFromStorage() {
		RecordStore recordStore = null;
		try {
			recordStore = RecordStore.openRecordStore(Config.RMS_NAME, false);
			byte[] b = recordStore.getRecord(Config.RNUM_ROSTER);

			if (b.length == 0) { return; }
			DataInputStream in = new DataInputStream(
					new ByteArrayInputStream(b));
			short n = in.readShort();
			for (int i = 0; i < n; i++) {
				Contact c = new Contact(in);
				contacts.put(c.jid, c);
			}
			in.close();
		} catch (RecordStoreNotFoundException rnfe) {
			// no storage, use default values
		} catch (InvalidRecordIDException ire) {
			// record not found, use default values
		} catch (Exception e) {
			// XXX There is no display at this point
			// showAlert(
			// AlertType.ERROR,
			// "Exception",
			// "Error reading roster from storage:\n" +e,
			// null
			// );
		} finally {
			try {
				recordStore.closeRecordStore();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Save the roster to the RMS
	 * 
	 */
	protected void saveToStorage() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(baos);
			out.writeShort(contacts.size());

			Enumeration en = contacts.elements();
			while (en.hasMoreElements()) {
				Contact c = (Contact) en.nextElement();
				c.store(out);
			}

			RecordStore rms = RecordStore
					.openRecordStore(Config.RMS_NAME, true);
			byte[] data = baos.toByteArray();
			while (rms.getNumRecords() < Config.RNUM_ROSTER) {
				rms.addRecord(null, 0, 0);
			}
			rms.setRecord(Config.RNUM_ROSTER, data, 0, data.length);
		} catch (Exception e) {
			// #mdebug
//@						Logger.log("Error in saving to storage: " + e.getMessage(),
//@									Logger.DEBUG);
			// #enddebug
			client.showAlert(AlertType.ERROR, "Exception",
					"Error saving roster to storage:\n" + e, null);
		}
	}

	public void packetReceived(Element e) {
		// #mdebug
//@				Logger.log("RosterHandler: received packet: " + new String(e.toXml()),
//@							Logger.DEBUG);
		// #enddebug

		Element items[] = e.getChildByName(null, Iq.QUERY).getChildrenByName(
				null, "item");
		for (int i = 0; i < items.length; i++) {
			updateRosterItem(items[i]);
		}

		// recreateGroups();

		// saveToStorage();
	}

	/**
	 * Send a roster query
	 * 
	 * @param go_online
	 *            if true we go online when received the roster
	 */
	public void retrieveRoster(final boolean go_online) {
		Iq iq_roster = new Iq(null, Iq.T_GET);
		iq_roster.addElement(NS_IQ_ROSTER, Iq.QUERY, NS_IQ_ROSTER);
		client.sendIQ(iq_roster, new IQResultListener() {
			public void handleError(Element e) {
				System.out.println(e.toXml());
			}

			// XXX I don't link this method, we should study some events for
			// oing this
			public void handleResult(Element e) {
				recreateRoster(e);
				if (go_online) {
					client.setPresence(-1, null);
				}

				// Handle subscription to the agent
				Contact c = getContactByJid(Config.LAMPIRO_AGENT);
				if (c == null || !"both".equals(c.subscription)) {
					c = new Contact(Config.LAMPIRO_AGENT, "Lampiro Agent",
							null, null);
					subscribeContact(c);
				}
				// #ifdef UI 
				UICanvas.getInstance().open(RosterScreen.getInstance(), true);
				UICanvas.getInstance().close(RegisterScreen.getInstance());
				// #endif
// #ifndef UI
				//@								LampiroMidlet.disp.setCurrent(RosterScreen.getInstance());
				// #endif
			}
		});
	}

	/**
	 * Subscribe to a contact. Adding a contact fires the transmission of two
	 * messages: an iq of type set for updating the roster, and a presence of
	 * type subscribe
	 */
	public void subscribeContact(Contact c) {
		contacts.put(c.jid, c);
		Iq iq_roster = new Iq(null, Iq.T_SET);
		Element query = iq_roster.addElement(NS_IQ_ROSTER, Iq.QUERY,
				NS_IQ_ROSTER);
		Element item = query.addElement(NS_IQ_ROSTER, "item", null);
		item.setAttribute("jid", c.jid);
		if (c.name.length() > 0) {
			item.setAttribute("name", c.name);
		}
		for (int i = 0; i < c.groups.length; i++) {
			item.addElement(NS_IQ_ROSTER, "group", NS_IQ_ROSTER).content = c.groups[i];
		}
		client.sendIQ(iq_roster, null);

		Presence psub = new Presence(Presence.T_SUBSCRIBE, null, null, -1);
		psub.setAttribute(Stanza.ATT_TO, c.jid);
		client.sendPacket(psub);
		// recreateGroups();
	}

	/** remove a contact */

	public void unsubscribeContact(Contact c) {

		contacts.remove(c.jid);

		RosterScreen.getInstance().removeContact(c);
		Iq iq_roster = new Iq(null, Iq.T_SET);
		Element query = iq_roster.addElement(NS_IQ_ROSTER, Iq.QUERY,
				NS_IQ_ROSTER);
		Element item = query.addElement(NS_IQ_ROSTER, "item", null);
		item.setAttribute("jid", c.jid);
		item.setAttribute("subscription", "remove");
		client.sendPacket(iq_roster);
		// recreateGroups();
	}

	private void recreateRoster(Element iq) {

		// XXX -> this should be run within a synchronized

		// Build a lookup table with roster
		Hashtable oldrst = new Hashtable();
		Enumeration en = contacts.elements();
		while (en.hasMoreElements()) {
			Contact c = (Contact) en.nextElement();
			oldrst.put(c.jid, c);
		}

		contacts.clear();
		RosterScreen.getInstance().removeAllContacts();

		Element query = iq.getChildByName(null, Iq.QUERY);
		if (query == null) { return; }

		Element items[] = query.getChildrenByName(null, "item");

		for (int i = 0; i < items.length; i++) {
			updateRosterItem(items[i]);
		}

		// add the server contact
		// XXX: is it correct to do it here ?
		// and/or is it the nicest way to do it
		XMPPClient me = XMPPClient.getInstance();
		Element serverEl = new Element("", "serverEl", "");
		serverEl.setAttribute(Iq.ATT_TO, me.my_jid);
		String myDomain = Contact.domain(me.my_jid);
		serverEl.setAttribute("jid", myDomain);
		serverEl.setAttribute("name", "Jabber Server");
		serverEl.setAttribute("subscription", "both");
		updateRosterItem(serverEl);
		Contact c = getContactByJid(myDomain);
		/// create a a fictitious presence
		Presence p = new Presence(me.my_jid, Presence.T_SUBSCRIBED, "online",
				"Jabber Server", 1);
		p.setAttribute(Presence.ATT_FROM, myDomain);
		c.updatePresence(p);
		//c.resources = new Presence[1];
		//c.resources[0] = p;
		updateRosterItem(serverEl);

	}

	/**
	 * Update roster item
	 * 
	 * @param item
	 */
	private void updateRosterItem(Element item) {

		// XXX handle the case in which the subscription is "remove"

		String jid = item.getAttribute("jid");

		Element group_elements[] = item.getChildrenByName(null, "group");
		String groups[] = new String[group_elements.length];
		for (int j = 0; j < groups.length; j++) {
			groups[j] = group_elements[j].content;
		}

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
			c.groups = groups;

		}

		RosterScreen rosterScreen = RosterScreen.getInstance();
		// XXX not sure that is completely correct...
		String subscription = item.getAttribute("subscription");
		if (subscription != null && subscription.compareTo("remove") == 0) {
			// if the user has removed me from roster
			// there is nothing to do remove contacts and nothing all
			contacts.remove(c.jid);
			rosterScreen.removeContact(c);
			return;
		}

		contacts.put(c.jid, c);
		rosterScreen.updateContact(c, Contact.CH_STATUS);
	}

	public Contact getContactByJid(String jid) {
		return (Contact) contacts.get(Contact.userhost(jid));
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
