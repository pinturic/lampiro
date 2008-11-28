/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: BasicXmlStream.java 1002 2008-11-18 14:26:17Z luca $
*/

package it.yup.xmlstream;

import it.yup.transport.TransportListener; // #debug
import it.yup.util.Logger;
import it.yup.xmpp.Contact;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.Iq;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public abstract class BasicXmlStream implements TransportListener {

	/** packets waiting for being sent */
	protected Vector sendQueue = new Vector(10);

	/** Storing XPath like queries and relative packet listeners */
	public Vector eventListeners = new Vector(10);

	/* EVENT CONSTANTS */
	public static String STREAM_CONNECTED = "_01";
	// public static String STREAM_AUTHENTICATED = "_02";
	public static String STREAM_INITIALIZED = "_02";
	// public static String STREAM_DISCONNECTED = "_03";
	public static String STREAM_ERROR = "_04";
	// public static String STREAM_RESOURCE_BOUND = "_05";
	// public static String STREAM_SESSION_OPENED = "_06";
	public static String STREAM_ACCOUNT_REGISTERED = "_07";
	public static String CONNECTION_LOST = "_08";
	public static String STREAM_TERMINATED = "_09";
	// public static String AUTHENTICACTION_FAILED = "_09";
	public static String REGISTRATION_FAILED = "_10";
	public static String CONNECTION_FAILED = "_11";
	//public static String STREAM_REGISTRATION_ERROR = "_08";

	/** Session ID for this stream */
	protected String SID = null;

	/* configuration properties */
	public static final String USERNAME = "1";
	public static final String PASSWORD = "2";

	/* User related data */
	/** Session jid */
	public String jid = null;
	/** Password used for authentication */
	protected String password = null;

	/**
	 * Stream features, mapping namespace to relevant dom Element 
	 */
	protected Hashtable features = new Hashtable();

	/** Initializers */
	protected Vector initializers = new Vector();

	/** Iterate through initializers in subsequent {@link BasicXmlStream#nextInitializer()}
	 * calls */
	protected Enumeration initializerInterator = null;

	/**
	 * Class used for associating packet listeners and queries
	 */
	protected class ListenerRegistration {
		public EventQuery query;
		public Object listener;
		public boolean oneTime;

		public ListenerRegistration(EventQuery query, Object listener,
				boolean oneTime) {
			this.query = query;
			this.listener = listener;
			this.oneTime = oneTime;
		}
	}

	protected BasicXmlStream() {
		// prepare the default initializers
		initializers.addElement(new SASLAuthenticator());
		initializers.addElement(new ResourceBinding());
		initializers.addElement(new SessionOpener());
	}

	/** Initialize the stream 
	 * @param jid 
	 * 		jid with or without resource; in the first case the resource is taken as a request 
	 * 		(the server may override it)
	 * @param domain
	 * @param password
	 * */
	public abstract void initialize(String jid, String password);

	/**
	 * Send a XMPP packet. It's possible to set a maximum wait time in order to send a packet
	 * also when cheap connections aren't available.
	 * 
	 * @param packetToSend
	 * 			the XMPP packet to send
	 * @param maxWait 
	 * 			maximum time a packet can wait before sending it (-1 for sending it 
	 * 			only when a cheap connection is available). This paramenter is only for
	 * 			compatibility with future extensions
	 */
	public void send(Element packetToSend, int maxWait) {
		// prepare the packet to send
		packetToSend.queueTime = new Date().getTime();
		packetToSend.maxWait = maxWait;

		synchronized (sendQueue) {
			this.sendQueue.addElement(packetToSend);
		}
		tryToSend();
	}

	/** Restart a stream (used during initialization)*/
	protected abstract void restart();

	protected Vector getPacketsToSend(boolean onlyUrgent) {
		Vector packetsToSend = new Vector();

		synchronized (sendQueue) {
			if (onlyUrgent) {
				// try to send the most urgent
				Enumeration en = sendQueue.elements();

				// the packets due in the next second
				long aBitLater = (new Date()).getTime() + 1000;
				while (en.hasMoreElements()) {
					Element ithPacket = ((Element) en.nextElement());
					if (ithPacket.maxWait > 0
							&& (ithPacket.queueTime + 1000 * ithPacket.maxWait) > aBitLater) {
						packetsToSend.addElement(ithPacket);
						// this is the first place to look for an error
						sendQueue.removeElement(ithPacket);
					}
				}
			} else {
				Enumeration en = sendQueue.elements();

				while (en.hasMoreElements()) {
					packetsToSend.addElement(en.nextElement());
				}
				sendQueue.removeAllElements();
			}
		}

		return packetsToSend;
	}

	/** 
	 * Method starting the send process, only if necessary
	 * */
	protected abstract void tryToSend();

	/**
	 * Add an event listener, it may be either a {@link PacketListener} or a {@link StreamEventListener}
	 * @param query
	 * @param listener either a {@link PacketListener} or a {@link StreamEventListener}
	 * @return the registration object that may be used for unregistering the listener
	 */
	public EventQueryRegistration addEventListener(EventQuery query,
			Object listener) {
		ListenerRegistration ld = new ListenerRegistration(query, listener,
				false);
		synchronized (this.eventListeners) {
			this.eventListeners.addElement(ld);
		}

		return new EventQueryRegistration(ld, eventListeners);
	}

	/**
	 * Remove an event listener passing the {@link EventQueryRegistration} received from 
	 * {@link BasicXmlStream#addEventListener(EventQuery, Object)} or
	 * {@link BasicXmlStream#addOnetimeEventListener(EventQuery, Object)} 
	 * @param registration
	 */
	public void removeEventListener(EventQueryRegistration registration) {
		registration.remove();
	}

	/**
	 * Add an event listener that can be fired only one, it may be either a 
	 * {@link PacketListener} or a {@link StreamEventListener}
	 * @param query
	 * @param listener either a {@link PacketListener} or a {@link StreamEventListener}
	 * @return the registration object that may be used for unregistering the listener
	 */
	public EventQueryRegistration addOnetimeEventListener(EventQuery query,
			Object listener) {
		ListenerRegistration ld = new ListenerRegistration(query, listener,
				true);
		synchronized (this.eventListeners) {
			this.eventListeners.addElement(ld);
		}
		return new EventQueryRegistration(ld, eventListeners);
	}

	/**
	 * Call the packet listeners registered for this packet
	 * @param stanza
	 */
	protected void promotePacket(Element stanza) {
		try {
			// #ifdef TIMING    		
			//@    		long t1 = System.currentTimeMillis();
			// #endif

			// 			XXX transform into a preprocessor macro    		
			//    		Uncomment for logging the number of listeners
			//    		System.out.println("---->" + eventListeners.size());

			Enumeration enPacketListener = null;
			synchronized (this.eventListeners) {
				enPacketListener = this.eventListeners.elements();
			}
			while (enPacketListener.hasMoreElements()) {
				ListenerRegistration listenerData = (ListenerRegistration) enPacketListener
						.nextElement();

				//    			Uncomment for dumping registered listeners
				//    			EventQuery q = listenerData.query;
				//    			String tab = ">>";
				//    			while(q!=null) {
				//    				System.out.println(tab + listenerData.query.event);
				//    				if(q.tagAttrNames != null) {
				//    					for(int i=0; i<q.tagAttrNames.length; i++) {
				//    						System.out.println(tab +">" + q.tagAttrNames[i] +": " + q.tagAttrValues[i]);
				//    					}
				//    				}
				//    				q = q.child;
				//
				//    				tab += ">>";
				//
				//    			}

				if (areMatching(stanza, listenerData.query)) {
					// #ifdef TIMING    				
					//@    				long t2 = System.currentTimeMillis();
					// #endif
					((PacketListener) listenerData.listener)
							.packetReceived(stanza);
					if (listenerData.oneTime == true) {
						synchronized (this.eventListeners) {
							this.eventListeners.removeElement(listenerData);
						}
					}
					// #ifdef TIMING
					//@    				EventQuery q = listenerData.query; 				
					//@    				System.out.println("L " + q.event + ":" + (System.currentTimeMillis() - t2));
					// #endif    				
				}
			}
			// #ifdef TIMING
			//@    		System.out.println("Promote: " + (System.currentTimeMillis() - t1));
			// #endif

		} catch (RuntimeException e) {

			// XXX don't knwow if here we must do something like closing the stream
			e.printStackTrace();
			// #mdebug
//@			    		e.printStackTrace();
//@			    		Logger.log("[BasicXmlStream::promotePacket] RuntimeException: " + e.getClass().getName() + "\n" + e.getMessage());
			// #enddebug

		}
	}

	/**
	 * Verify if a packet matches a query 
	 * @param receivedPacket
	 * @param query
	 * @return
	 */
	protected boolean areMatching(Element receivedPacket, EventQuery query) {

		/* better stating first a condition that fails immediatly the check 
		 * (just readability issue) */
		if (!query.event.equals(receivedPacket.name)
				&& !query.event.equals(EventQuery.ANY_PACKET)) { return false; }

		// then check all the attributes if the query has any 
		if (query.tagAttrNames != null) {
			boolean matched = false;
			for (int l = 0; l < query.tagAttrNames.length; l++) {
				matched = false;
				String lthName = query.tagAttrNames[l];
				String lthValue = query.tagAttrValues[l];
				if ("xmlns".equals(lthName)
						&& lthValue.equals(receivedPacket.uri)) {
					matched = true;
				} else if (receivedPacket.attributes != null) {
					for (int i = 0; i < receivedPacket.attributes.size(); i++) {
						String[] ithAttr = ((String[]) receivedPacket.attributes
								.elementAt(i));
						if ((ithAttr[0].equals(lthName) && ithAttr[1]
								.equals(lthValue))) {
							matched = true;
							break;
						}
					}
				}
			}
			if (matched == false) { return false; }
		}

		/* a packet with no child doesn't match a query with a child sub-query */
		if (query.child != null && receivedPacket.children != null
				&& receivedPacket.children.size() == 0) { return false; }

		// all attributes verified, check the children
		if (query.child != null) {
			for (int i = 0; i < receivedPacket.children.size(); i++) {
				Element ithChild = (Element) receivedPacket.children
						.elementAt(i);
				if (areMatching(ithChild, query.child)) { return true; }
			}
			return false;
		}

		return true;
	}

	/**
	 * Dispatch an XmlStream event
	 * */
	protected void dispatchEvent(String event, Object source) {
		Enumeration en = eventListeners.elements();
		while (en.hasMoreElements()) {
			ListenerRegistration listenerData = (ListenerRegistration) en
					.nextElement();
			if (listenerData.query.event.equals(EventQuery.ANY_EVENT)
					|| event.equals(listenerData.query.event)) {
				((StreamEventListener) listenerData.listener).gotStreamEvent(
						event, source);
				if (listenerData.oneTime) {
					synchronized (eventListeners) {
						eventListeners.removeElement(listenerData);
					}

				}
			}
		}
	}

	/**
	 * Start the feature chain
	 * @param features
	 */
	protected void processFeatures(Vector features) {
		this.features.clear();
		this.initializerInterator = null;
		for (int i = 0; i < features.size(); i++) {
			Element e = (Element) features.elementAt(i);
			this.features.put(e.uri, e);
		}
		// received a set of features trigger the stream initialization
		nextInitializer();
	}

	/**
	 * Call the next stream initialiazer.
	 * Dispatch {@link XmlStream#STREAM_INITIALIZATION_FINISHED} when all the 
	 * initializers have been processed
	 */
	public void nextInitializer() {
		if (initializerInterator == null) {
			initializerInterator = initializers.elements();
		}
		while (initializerInterator.hasMoreElements()) {
			Initializer initializer = (Initializer) initializerInterator
					.nextElement();
			if (initializer.matchFeatures(features)) {
				initializer.start(this);
				return;
			}
		}
		initializerInterator = null;
		dispatchEvent(BasicXmlStream.STREAM_INITIALIZED, null);
	}

	public void addInitializer(Initializer initializer, int position) {
		this.initializers.insertElementAt(initializer, position);
	}

	/**
	 *	Initializer that binds a resource
	 */
	private class ResourceBinding extends Initializer implements PacketListener {

		public ResourceBinding() {
			super("urn:ietf:params:xml:ns:xmpp-bind", false);
		}

		public void start(BasicXmlStream xmlStream) {
			this.stream = xmlStream;
			Iq iq = new Iq(null, "set");
			Element bind = new Element(namespace, "bind", namespace);
			String s = Contact.resource(xmlStream.jid);
			if (s != null) {
				Element resource = new Element(namespace, "resource", namespace);
				resource.content = s;
				bind.children.addElement(resource);
			}
			iq.children.addElement(bind);
			EventQuery q = new EventQuery("iq", new String[] { "id" },
					new String[] { iq.getAttribute("id") });
			stream.addOnetimeEventListener(q, this);
			stream.send(iq, -1);
		}

		public void packetReceived(Element e) {
			if ("result".equals(e.getAttribute("type"))) {
				Element bind = e.getChildByName(null, "bind");
				Element jid = null;
				if (bind != null
						&& (jid = bind.getChildByName(null, "jid")) != null
						&& jid.content != null) {
					stream.jid = jid.content;
				}
				stream.nextInitializer();
			} else {
				stream.dispatchEvent(BasicXmlStream.STREAM_ERROR,
						"cannot bind resource");
			}
		}

	}

	/** 
	 * Initialiazer that opens a session
	 * */
	private class SessionOpener extends Initializer implements PacketListener {

		public SessionOpener() {
			super("urn:ietf:params:xml:ns:xmpp-session", true);
		}

		public void start(BasicXmlStream xmlStream) {
			this.stream = xmlStream;
			Iq iq = new Iq(null, "set");
			Element session = new Element(namespace, "session", namespace);
			iq.children.addElement(session);
			EventQuery q = new EventQuery("iq", new String[] { "id" },
					new String[] { iq.getAttribute("id") });
			stream.addOnetimeEventListener(q, this);
			stream.send(iq, -1);
		}

		public void packetReceived(Element e) {
			if ("result".equals(e.getAttribute("type"))) {
				stream.nextInitializer();
			} else {
				stream.dispatchEvent(BasicXmlStream.STREAM_ERROR,
						"cannot start session");
			}
		}
	}

}
