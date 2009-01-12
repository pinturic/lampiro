/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: EventQueryRegistration.java 1028 2008-12-09 15:44:50Z luca $
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
        listeners.removeElement(o);
    }
}  