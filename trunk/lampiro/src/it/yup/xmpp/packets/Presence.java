/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: Presence.java 906 2008-10-14 20:33:51Z luca $
*/

package it.yup.xmpp.packets;

import it.yup.xmlstream.Element;
import it.yup.xmpp.Config;

/**
 * 
 */
public class Presence extends Stanza {

	/* presence element names */
	public static final String PRESENCE = "presence";
	public static final String PRIORITY = "priority";
	public static final String RESOURCE = "resource";
	public static final String STATUS = "status";
	public static final String SHOW = "show";

	/* possible availability show */
	public static final String SHOW_CHAT = "chat";
	public static final String SHOW_DND = "dnd";
	public static final String SHOW_AWAY = "away";
	public static final String SHOW_XA = "xa";

	/* possible presence types */
	public static final String T_SUBSCRIBE = "subscribe";
	public static final String T_SUBSCRIBED = "subscribed";
	public static final String T_UNSUBSCRIBE = "unsubscribe";
	public static final String T_UNSUBSCRIBED = "unsubscribed";
	public static final String T_PROBE = "probe";
	public static final String T_UNAVAILABLE = "unavailable";

	public Presence() {
		super(PRESENCE, null, null, null);
	}

	public Presence(String to, String type, String show, String status,
			int priority) {
		super(PRESENCE, to, type, null);
		if (status != null) {
			setStatus(status);
		}
		// XXX Perhaps wrong, negative priority may have a meaning !!!
		if (priority >= 0) {
			setPriority(priority);
		}
		if (show != null) {
			setShow(show);
		}
	}

	public Presence(String type, String show, String status, int priority) {
		this(null, type, show, status, priority);
	}

	public Presence(Element e) {
		super(e);
	}

	public void setShow(String show) {
		Element el = getChildByName(NS_JABBER_CLIENT, SHOW);
		if (el == null) {
			el = addElement(NS_JABBER_CLIENT, SHOW, null);
		}
		el.content = show;
	}

	public String getShow() {
		Element el = getChildByName(NS_JABBER_CLIENT, SHOW);
		if (el != null) {
			return el.content;
		} else {
			return null;
		}
	}

	public void setPriority(int priority) {
		Element el = getChildByName(NS_JABBER_CLIENT, PRIORITY);
		if (el == null) {
			el = addElement(NS_JABBER_CLIENT, PRIORITY, null);
		}
		el.content = String.valueOf(priority);
	}

	public int getPriority() {
		Element el = getChildByName(NS_JABBER_CLIENT, PRIORITY);
		if (el != null) {
			return Integer.parseInt(el.content);
		} else {
			return 0;
		}
	}

	public String getResource() {
		Element el = getChildByName(NS_JABBER_CLIENT, RESOURCE);
		if (el != null) {
			return el.content;
		} else {
			return Config.getInstance().getProperty(Config.YUP_RESOURCE, "Lampiro");
		}
	}

	public void setStatus(String status) {
		Element el = getChildByName(NS_JABBER_CLIENT, STATUS);
		if (el == null) {
			el = addElement(NS_JABBER_CLIENT, STATUS, null);
		}
		el.content = status;
	}

	public String getStatus() {
		Element el = getChildByName(NS_JABBER_CLIENT, STATUS);
		if (el != null) {
			return el.content;
		} else {
			return null;
		}
	}
}
