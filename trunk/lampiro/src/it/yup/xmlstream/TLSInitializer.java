/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: TLSInitializer.java 1028 2008-12-09 15:44:50Z luca $
*/

// #ifdef TLS
//@package it.yup.xmlstream;
//@
//@import java.io.IOException;
//@
//@public class TLSInitializer extends Initializer implements PacketListener {
//@	
//@	BasicXmlStream stream;
//@	
//@	protected TLSInitializer() {
//@		super("urn:ietf:params:xml:ns:xmpp-tls", true);
//@	}
//@
//@	public void start(BasicXmlStream stream) {
//@		
//@		this.stream = stream;
//@
//@		Element starttls = new Element(this.namespace, "starttls", this.namespace);
//@		EventQuery pq = new EventQuery(EventQuery.ANY_PACKET, null, null);
//@		stream.addOnetimeEventListener(pq, this);
//@		stream.send(starttls, -1);
//@	}
//@
//@	public void packetReceived(Element e) {
//@		if("proceed".equals(e.name)) {
//@			try {
//@				((SocketStream) stream).startTLS();
//@			} catch (IOException e1) {
//@				// notify error
//@				e1.printStackTrace();
//@			}
//@			stream.restart();
//@		} else {
//@			stream.nextInitializer();
//@		}
//@	}
//@
//@}
// #endif
