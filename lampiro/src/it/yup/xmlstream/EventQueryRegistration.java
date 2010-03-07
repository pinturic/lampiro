/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: EventQueryRegistration.java 2002 2010-03-06 19:02:12Z luca $
*/

package it.yup.xmlstream;

import java.util.Vector;

public class EventQueryRegistration {
	private Object o;
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