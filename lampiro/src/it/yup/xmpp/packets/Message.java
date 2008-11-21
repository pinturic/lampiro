/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: Message.java 846 2008-09-11 12:20:05Z luca $
*/

package it.yup.xmpp.packets;

import it.yup.xmlstream.Element;

public class Message extends Stanza {

	public static final String MESSAGE  = "message";
	public static final String BODY     = "body";
	public static final String SUBJECT  = "subject";
	public static final String THREAD   = "thread";
	public static final String ERROR    = "error";
	
	public Message(String to, String type) {
		super(MESSAGE, to, type, null);
	}

	public void setBody(String body) {
		Element el = getChildByName(NS_JABBER_CLIENT, BODY);
		if (el != null)
			el.content = body;
		else
			el = addElement(NS_JABBER_CLIENT, BODY, null);
			el.content = body;
	}

	public String getBody() {
		Element el = getChildByName(NS_JABBER_CLIENT, BODY);
		if (el != null)
			return el.content;
		return null;
	}

	public String getErrorText() {
		Element el = getChildByName(NS_JABBER_CLIENT, ERROR);
		if (el == null)
			return null;
		
		Element txt = el.getChildByName("urn:ietf:params:xml:ns:xmpp-stanzas", "text");
		if(txt == null) {
			return null;
		}
		
		return txt.content;
	}

	public static Message fromElement(Element src) {

		String to = src.getAttribute(ATT_TO);
		String type = src.getAttribute(ATT_TYPE);

		/* copy entire tree of data. watch out for possible GC issues here */
		Message m = new Message(to, type);
		m.attributes = src.attributes;
		m.children = src.children;
		
		return m;
	}

}
