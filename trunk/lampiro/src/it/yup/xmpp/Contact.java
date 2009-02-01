/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: Contact.java 1144 2009-01-30 17:26:43Z luca $
*/

package it.yup.xmpp;

import it.yup.util.Utils;
import it.yup.xmlstream.Element;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Message;
import it.yup.xmpp.packets.Presence;
import it.yup.xmpp.packets.Stanza;
import it.yup.xmpp.packets.IQResultListener;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Un contatto.
 */
public class Contact extends IQResultListener{

	/* possible availability status */
	public static final int AV_CHAT = 0;
	public static final int AV_ONLINE = 1;
	public static final int AV_DND = 2;
	public static final int AV_AWAY = 3;
	public static final int AV_XA = 4;
	public static final int AV_UNAVAILABLE = 5;

	/* reason for status change */
	public static final int CH_MESSAGE_NEW = 0;
	public static final int CH_MESSAGE_READ = 1;
	public static final int CH_STATUS = 2;
	public static final int CH_TASK_NEW = 3;
	public static final int CH_TASK_REMOVED = 4;

	/*
	 * The last resource associated to this user that sent a message
	 */
	/** mapping presence availability constants */
	public static String availability_mapping[] = { Presence.SHOW_CHAT, // AV_CHAT
			"online", // AV_ONLINE
			Presence.SHOW_DND, // AV_DND
			Presence.SHOW_AWAY, // AV_AWAY
			Presence.SHOW_XA, // AV_XA
			Presence.T_UNAVAILABLE // AV_UNAVAILABLE
	};

	/** the messages history */
	private Vector conv = null;

	/** pending commands */
	private Vector tasks = null;
	// XXX to array
	/** the command list; array of String pairs (node/name) */
	public String cmdlist[][] = null;

	/** if true the conversation has unread messages */
	public boolean unread_msg = false;
	public boolean pending_tasks = false;

	public String jid;
	public String name;
	public String groups[];
	public String subscription;
	protected Presence[] resources = null;

	/** cached availability: cached for speeding up sorting */
	int availability = AV_UNAVAILABLE;

	/*
	 * the last (in time order) resource associated to this user
	 */
	public String lastResource = null;

	public Contact(String jid, String name, String subscription,
			String groups[]) {
		this.jid = jid;

		if (name == null) {
			this.name = "";
		} else {
			this.name = name;
		}

		if (subscription == null || "".equals(subscription)) {
			this.subscription = "none";
		} else {
			this.subscription = subscription;
		}

		if (groups == null) {
			this.groups = new String[0];
		} else {
			this.groups = groups;
		}

		// System.out.println("name ---->" + this.name + "(" + this.jid + ")");
	}

	/**
	 * Build a contact reading the values from the record store
	 * 
	 * @throws IOException
	 */
	public Contact(DataInputStream in) throws IOException {
		this.jid = in.readUTF();
		this.name = in.readUTF();
		this.subscription = in.readUTF();
		this.groups = new String[in.readShort()];
		for (int i = 0; i < this.groups.length; i++) {
			this.groups[i] = in.readUTF();
		}
	}

	public void store(DataOutputStream out) throws IOException {
		out.writeUTF(jid);
		out.writeUTF(name);
		out.writeUTF(subscription);
		out.writeShort(groups.length);
		for (int i = 0; i < groups.length; i++) {
			out.writeUTF(groups[i]);
		}
	}

	public void addMessageToHistory(Message msg) {

		if (conv == null) {
			conv = new Vector();
		}

		String body = msg.getBody();
		String to = userhost(msg.getAttribute(Stanza.ATT_TO));
		String from = msg.getAttribute(Stanza.ATT_FROM);
		Element subjectEl = msg.getChildByName(null, "subject");
		String subject = null;
		if (subjectEl != null) subject = subjectEl.content;
		if (subject != null && subject.length() > 0) {
			body = subject + ": " + body;
		}
		// / Should / Could be reused ?
		Calendar cal;
		long date;
		String arriveTime = "";

		date = System.currentTimeMillis();
		cal = Calendar.getInstance();
		cal.setTime(new Date(date));

		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		arriveTime = new String((hour < 10 ? "0" : "") + hour + ":"
				+ (minute < 10 ? "0" : "") + minute);

		if (from != null) lastResource = resource(from);
		if (body != null && body.length() > 0) {
			if (!this.jid.equals(to)) {
				unread_msg = true;
			}
			conv
					.addElement(new String[] { to, body, lastResource,
							arriveTime });
		}
	}

	public Vector getMessageHistory() {
		return conv;
	}

	public int getHistoryLength() {
		if (conv == null) { return 0; }
		return conv.size();
	}

	public void resetMessageHistory() {
		conv = null;
	}

	public String getPrintableName() {
		if (!"".equals(name)) {
			return name;
		} else {
			return jid;
		}
	}

	/**
	 * Returns the user full jid (if online) or null if offline. The presence
	 * with the highest pririty is used
	 * 
	 * @return The user full Jid.
	 */
	public String getFullJid() {
		if (resources != null) { return resources[0]
				.getAttribute(Stanza.ATT_FROM); }
		return null;
	}

	public boolean isVisible() {
		if (unread_msg || (conv != null && conv.size() > 0)) {
			/* if the contact has messages in his chat, it will be shown */
			return true;
		}
		if ("none".equals(subscription)) {// || "to".equals(subscription)) {
			// ignoring the contacts whose status hasn't to be displayed
			// XXX: we should let the user configure this behavior
			return false;
		}
		if (resources == null) { return false; }
		return true;
	}

	public void addTask(Task t) {
		if (tasks == null) {
			tasks = new Vector();
			pending_tasks = true;
		}
		if (!tasks.contains(t)) {
			tasks.addElement(t);
			pending_tasks = true;
		}
	}

	public void removeTask(Task t) {
		if (tasks == null) { return; // shouldn't happen, but...
		}
		tasks.removeElement(t);
		if (tasks.size() == 0) {
			tasks = null;
			pending_tasks = false;
		}
	}

	public Task[] getTasks() {
		Task[] tasks;
		if (this.tasks != null) {
			tasks = new Task[this.tasks.size()];
			this.tasks.copyInto(tasks);
		} else {
			tasks = new Task[0];
		}
		return tasks;
	}

	/**
	 * Get the presence of the best resource (in order of visibility and
	 * priority)
	 * 
	 * @return
	 */
	public Presence getPresence() {
		if (resources == null) return null;
		return resources[0];
	}

	/**
	 * Get all presences for each resource
	 * 
	 * @return
	 */
	public Presence[] getAllPresences() {
		// XXX it should be safer to return a copy
		return resources;
	}

	/**
	 * Update the presence for the resource that has sent it
	 * 
	 * @param p
	 */
	public void updatePresence(Presence p) {

		if (Presence.T_UNAVAILABLE.equals(p.getAttribute(Stanza.ATT_TYPE))) {
			if (resources == null) {
				return;
			} else if (resources.length == 1) {

				// remove the resource array if the only resource is going
				// offline
				String old_jid = resources[0].getAttribute(Stanza.ATT_FROM);

				if (old_jid.equals(p.getAttribute(Stanza.ATT_FROM))) {
					resources = null;
					availability = AV_UNAVAILABLE;
				}
				return;
			} else {
				updateExistingPresence(p);
			}
		} else {
			// available presence, update the list and resort
			if (resources == null) {
				// first resource create the list
				resources = new Presence[] { p };
			} else {
				// add or update and finally sort
				String jid = p.getAttribute(Stanza.ATT_FROM);
				boolean found = false;
				// check if we can just update
				for (int i = 0; i < resources.length; i++) {
					if (jid.equals(resources[i].getAttribute(Stanza.ATT_FROM))) {
						resources[i] = p;
						found = true;
						break;
					}
				}

				if (!found) {
					// new resource found, add it
					Presence v[] = new Presence[resources.length + 1];
					v[0] = p;
					for (int i = 0; i < resources.length; i++) {
						v[i + 1] = resources[i];
					}
					resources = v;
				}

				// presence order may have changed sort the resources
				if (resources.length > 1) {
					for (int i = 0; i < resources.length - 1; i++) {
						for (int j = 1; j < resources.length; j++) {
							Presence ri = resources[i];
							Presence rj = resources[j];
							int diff = mapAvailability(ri.getShow())
									- mapAvailability(rj.getShow());
							if (diff > 0) {
								resources[i] = rj;
								resources[j] = ri;
							} else if (diff == 0) {
								// check priority
								int pdiff = ri.getPriority() - rj.getPriority();
								// higher priority comes first
								if (pdiff < 0) {
									resources[i] = rj;
									resources[j] = ri;
								}
							}
						}
					}
				}
			}
		}

		// cache the new availability
		availability = mapAvailability(resources[0].getShow());
	}
	
	public Vector getCapabilities (){
		if (this.resources[0] == null)
			return null;
		Element c = this.resources[0].getChildByName(XMPPClient.NS_CAPS,"c");
		if (c== null){
			return null;
		}
		String node = c.getAttribute("node");
		String ver = c.getAttribute("ver");
		return XMPPClient.getCapabilities(node, ver);
	}
	
	public void askCapabilities() {
		Element c = null;
		if (this.resources[0] != null) {
			c = this.resources[0].getChildByName(XMPPClient.NS_CAPS, "c");
		}
		Iq iq = new Iq(this.getFullJid(), Iq.T_GET);
		Element query = iq.addElement(XMPPClient.NS_IQ_DISCO_INFO, Iq.QUERY);
		if (c != null) {
			query.setAttribute("node", c.getAttribute("node")+"#"+c.getAttribute("ver"));
		}
		XMPPClient.getInstance().sendIQ(iq, this);
	}

	protected void updateExistingPresence(Presence p) {
		// More than one resource, remove only that resource (if
		// present)
		String from = p.getAttribute(Stanza.ATT_FROM);
		String from_cache[] = new String[resources.length];
		boolean found = false;
		for (int i = 0; i < resources.length; i++) {
			from_cache[i] = resources[i].getAttribute(Stanza.ATT_FROM);
			if (from.equals(from_cache[i])) {
				found = true;
			}
		}

		if (found) {
			Presence v[] = new Presence[resources.length - 1];
			for (int i = 0, j = 0; i < resources.length; i++) {
				if (!from.equals(from_cache[i])) {
					v[j++] = resources[i];
				}
			}
			resources = v;
		}
		// got to the end of the method, must update availability
	}

	/**
	 * Compare the contatcs for sorting them in the roster. The precedence is
	 * given by:
	 * <ol>
	 * <li> having unread messages </li>
	 * <li> having pending tasks </li>
	 * <li> the presence </li>
	 * <li> alphabetical order </li>
	 * </ol>
	 */
	public int compareTo(Contact d) {

		// check first unread messages
		if (d.unread_msg && !unread_msg) {
			return -1;
		} else if (unread_msg && !d.unread_msg) { return 1; }

		// then check for pending tasks
		if (d.pending_tasks && !this.pending_tasks) {
			return -1;
		} else if (!d.pending_tasks && this.pending_tasks) { return 1; }

		if (availability != d.availability) { return d.availability
				- availability; }

		// finally use the name if all the other tests failed
		return d.getPrintableName().toLowerCase().compareTo(
				getPrintableName().toLowerCase());
	}

	/**
	 * Get the availability of the highest scrored resource
	 * 
	 * @return one of the possible AV_* constants
	 */
	public int getAvailability() {
		return availability;
	}

	/**
	 * Get the availability of a given reosurce
	 * 
	 * @return one of the possible AV_* constants
	 */
	public int getAvailability(String jid) {
		for (int i = 0; i < resources.length; i++) {
			String ijid = resources[i].getAttribute(Stanza.ATT_FROM);
			if (jid.equals(ijid)) { return mapAvailability(resources[i]
					.getShow()); }
		}
		return AV_UNAVAILABLE;
	}

	public Presence getPresence(String jid) {
		if (resources != null) {
			for (int i = 0; i < resources.length; i++) {
				String ijid = resources[i].getAttribute(Stanza.ATT_FROM);
				if (jid.equals(ijid)) { return resources[i]; }
			}
		}
		return null;
	}

	// XXX -> move to the Utils?
	public static String userhost(String jid) {
		int spos = jid.indexOf('/');
		if (spos > 0) {
			return jid.substring(0, spos);
		} else {
			return jid;
		}
	}

	public static String resource(String jid) {
		int spos = jid.indexOf('/');
		if (spos > 0) {
			return jid.substring(spos + 1);
		} else {
			return null;
		}
	}

	public static String user(String jid) {
		int spos = jid.indexOf('@');
		if (spos > 0) {
			return jid.substring(0, spos);
		} else {
			return null;
		}
	}

	public static String domain(String jid) {
		String uh = Contact.userhost(jid);
		int spos = uh.indexOf('@');
		if (spos > 0) {
			return uh.substring(spos + 1);
		} else {
			return null;
		}
	}

	/**
	 * Getting the constant for a given availability string
	 * 
	 * @param s
	 *            the availability string (if null mapped to "online")
	 * @return
	 */
	int mapAvailability(String s) {
		if (s == null) s = "online";
		for (int i = 0; i < availability_mapping.length; i++) {
			if (availability_mapping[i].equals(s)) { return i; }
		}
		return -1;
	}

	public void handleError(Element e) {
		// TODO Auto-generated method stub
		
	}

	public void handleResult(Element e) {
		Element query = e.getChildByName(XMPPClient.NS_IQ_DISCO_INFO,Iq.QUERY);
		String fullNode = query.getAttribute("node");
		Element[] features = query.getChildrenByName(null, "feature");
		if (fullNode != null){
			Vector fn = Utils.tokenize(fullNode, '#');
			String node = (String)fn.elementAt(0);
			String ver = (String)fn.elementAt(1);
			XMPPClient.saveCapabilities (node,ver,features);
		}
		
	}
}
