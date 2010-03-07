/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: TransportListener.java 2002 2010-03-06 19:02:12Z luca $
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
