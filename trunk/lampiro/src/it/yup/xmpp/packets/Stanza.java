/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: Stanza.java 846 2008-09-11 12:20:05Z luca $
*/

package it.yup.xmpp.packets;

import it.yup.xmlstream.Element;

public class Stanza extends Element {

	public static final String NS_JABBER_CLIENT = "jabber:client";
	public static final String ATT_TO   = "to";
	public static final String ATT_FROM = "from";
	public static final String ATT_TYPE = "type";
	public static final String ATT_ID   = "id";
	//public static final String ATT_JID  = "jid";

	protected Stanza(String name, String to, String type, String id) {
		super(NS_JABBER_CLIENT, name, NS_JABBER_CLIENT);
		if (to != null) {
			attributes.addElement(new String[] { ATT_TO, to });
		}
		if (type != null) {
			attributes.addElement(new String[] { ATT_TYPE, type });
		}
		if (id != null) {
			attributes.addElement(new String[] { ATT_ID, id });
		}
	}

	public Stanza(Element e) {
		super(e);
	}
	
//	public void setType(String type) {
//		setAttribute("type", type);
//	}
//	
//	public String getType() {
//		return getAttribute("type");
//	}
//	
//	public void setId(String id) {
//		setAttribute("id", id);
//	}
//	
//	public String getId() {
//		return getAttribute("id");
//	}
//	
//	public void setFrom(String from) {
//		setAttribute("from", from);
//	}
//	
//	public String getFrom() {
//		return getAttribute("from");
//	}
//	
//	public void setTo(String to) {
//		setAttribute("to", to);
//	}
//	
//	public String getTo() {
//		return getAttribute("to");
//	}
}
