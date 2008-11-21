/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: PacketListener.java 846 2008-09-11 12:20:05Z luca $
*/

package it.yup.xmlstream;


/**
 * Listening to XMPP packets
 *
 */
public interface PacketListener 
{
	/**
	 * Called when an XMPP element is received
	 * @param e
	 */
    public void packetReceived (Element e);
}
