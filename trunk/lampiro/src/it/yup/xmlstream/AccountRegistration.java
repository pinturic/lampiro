/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: AccountRegistration.java 1132 2009-01-26 16:05:01Z luca $
*/

package it.yup.xmlstream;

import it.yup.xmpp.Contact;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Stanza;

/**
 * Class registering an account with a XMPP server <br/>
 * The registration process is simplified, since we don't gather the required fields,
 * and we directly send the chosen username and password. This may lead to
 * compatibility issues with some servers, however we don't care at present.
 *
 */
public class AccountRegistration extends Initializer implements PacketListener {
	
	private static final String IQ_REGISTER =  "jabber:iq:register";
	
	public AccountRegistration() {
		// when used the registration is mandatory, if it fails
		// we cannot go on
		super("http://jabber.org/features/iq-register", false);
	}
	
	public void start(BasicXmlStream xmlStream) {
		this.stream = xmlStream;
		Iq iq_register = new Iq(null, Iq.T_SET);
        Element query = iq_register.addElement(IQ_REGISTER, "query");
        query.addElement(IQ_REGISTER, "username").content = Contact.user(stream.jid);
        query.addElement(IQ_REGISTER, "password").content = stream.password;
        EventQuery registerResult = new EventQuery("iq", 
        		new String[]{ Stanza.ATT_ID }, 
        		new String[]{ iq_register.getAttribute(Stanza.ATT_ID) });
		stream.addOnetimeEventListener(registerResult, this);
        stream.send(iq_register, -1);
	}

	public void packetReceived(Element e) {
		if(e.getAttribute("type").equals("result")) {
			stream.dispatchEvent(BasicXmlStream.STREAM_ACCOUNT_REGISTERED, null);
			stream.nextInitializer();
		} else {
			String errmsg = "Error creating account";
			Element err = e.getChildByName(null, "error");
			if(err != null) {
				for(int i = 0; i < err.children.size(); i++) {
					Element cld = (Element)err.children.elementAt(i);
					errmsg = cld.name;
				}
			}
			stream.dispatchEvent(BasicXmlStream.STREAM_ERROR, errmsg);
		}
	}

}
