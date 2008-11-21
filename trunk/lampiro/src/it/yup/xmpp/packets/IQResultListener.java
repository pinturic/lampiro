/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: IQResultListener.java 846 2008-09-11 12:20:05Z luca $
*/

package it.yup.xmpp.packets;

import it.yup.xmlstream.Element;
import it.yup.xmlstream.PacketListener;

public abstract class IQResultListener implements PacketListener {

	public void packetReceived(Element e) {
		String type = e.getAttribute(Stanza.ATT_TYPE);
		if(Iq.T_RESULT.equals(type))
			handleResult(e);
		else
			handleError(e);
	}

	public abstract void handleResult(Element e);
	
	public abstract void handleError(Element e);
}
