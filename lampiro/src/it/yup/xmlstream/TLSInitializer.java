/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: TLSInitializer.java 2039 2010-03-31 07:29:31Z luca $
*/

// #ifdef TLS
//@package it.yup.xmlstream;
//@
//@import it.yup.util.EventDispatcher;
//@import it.yup.xml.Element;
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
//@		Element starttls = new Element(this.namespace, "starttls");
//@		EventQuery pq = new EventQuery(EventQuery.ANY_PACKET, null, null);
//@		BasicXmlStream.addOnetimePacketListener(pq, this);
//@		stream.send(starttls, -1);
//@	}
//@
//@	public void packetReceived(Element e) {
//@		if("proceed".equals(e.name)) {
//@			try {
//@				((SocketStream) stream).startTLS();
//@				EventDispatcher.dispatchEvent(EventDispatcher.TLS_INITIALIZED, null);
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
