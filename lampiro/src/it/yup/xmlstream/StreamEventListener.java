/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: StreamEventListener.java 2002 2010-03-06 19:02:12Z luca $
*/

package it.yup.xmlstream;

/**
 * Implement this interface in order to receive stream events
 */
public interface StreamEventListener {
	
	/**
	 * Called when some stream event happens
	 * @param event 
	 * 	The name of the event
	 * @param source 
	 * 	The source of the event. It may be either the object that has distpatched it or some
	 *  data that must be passed to the listener 
	 */
	public void gotStreamEvent(String event, Object source);
	
}
