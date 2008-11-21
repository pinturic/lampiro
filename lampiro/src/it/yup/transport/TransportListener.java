/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: TransportListener.java 846 2008-09-11 12:20:05Z luca $
*/

package it.yup.transport;

/**
 * Listening to transport events (data and connection events)
 *
 */
public interface TransportListener  {
	
	/** Called when a connection has been established */
	public void connectionEstablished(BaseChannel connection);
	
	/** Called when we couldn't establish a connection */
	public void connectionFailed(BaseChannel connection);
	
	/** Called when a connection has been lost */
	public void connectionLost(BaseChannel connection);
}
