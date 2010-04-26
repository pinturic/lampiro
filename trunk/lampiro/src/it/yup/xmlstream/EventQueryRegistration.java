/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: EventQueryRegistration.java 2047 2010-04-06 20:24:41Z luca $
*/

package it.yup.xmlstream;

import java.util.Vector;

public class EventQueryRegistration {
	public Object o;
	private Vector listeners;

	public EventQueryRegistration(Object o, Vector listeners) {
		this.o = o;
		this.listeners = listeners;
	}

	public void remove() {
		synchronized (listeners) {
			listeners.removeElement(o);
		}
	}
}