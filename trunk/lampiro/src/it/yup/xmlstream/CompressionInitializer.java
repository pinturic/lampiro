/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: CompressionInitializer.java 1002 2008-11-18 14:26:17Z luca $
*/

// #ifdef COMPRESSION
//@package it.yup.xmlstream;
//@
//@public class CompressionInitializer extends Initializer implements PacketListener {
//@	
//@	BasicXmlStream stream;
//@	
//@	protected CompressionInitializer() {
//@		super("http://jabber.org/features/compress", true);
//@	}
//@
//@	public void start(BasicXmlStream stream) {
//@		
//@		this.stream = stream;
//@		
//@		Element methods = (Element) stream.features.get(namespace);
//@		boolean found = false;
//@		for(int i=0; i<methods.children.size(); i++) {
//@			Element method = (Element) methods.children.elementAt(i);
//@			if("method".equals(method.name) && "zlib".equals(method.content)) {
//@				found = true;
//@				break;
//@			}
//@		}
//@		
//@		if(found) {
//@			Element compress = new Element("http://jabber.org/protocol/compress", "compress", "http://jabber.org/protocol/compress");
//@			compress.addElement("http://jabber.org/protocol/compress", "method", null).content = "zlib";
//@			EventQuery pq = new EventQuery(EventQuery.ANY_PACKET, null, null);
//@			stream.addOnetimeEventListener(pq, this);
//@			stream.send(compress, -1);
//@		} else {
//@			stream.nextInitializer();
//@		}
//@	}
//@
//@	public void packetReceived(Element e) {
//@		if("compressed".equals(e.name)) {
//@			((SocketStream) stream).startCompression();
//@			stream.restart();
//@		} else {
//@			stream.nextInitializer();
//@		}
//@	}
//@
//@}
// #endif
