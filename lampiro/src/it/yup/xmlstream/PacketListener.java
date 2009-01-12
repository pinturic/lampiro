/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: PacketListener.java 1028 2008-12-09 15:44:50Z luca $
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
