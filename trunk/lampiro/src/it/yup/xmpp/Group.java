/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: Group.java 846 2008-09-11 12:20:05Z luca $
*/

package it.yup.xmpp;


import java.util.Vector;

/**
 * A group of contatcs
 */
public class Group {
	
    /** the group name */
    public String name;
    /** the contacts of this group */
    public Vector contacts;
    
    public Group(String name) {
        this.name = name;
        contacts = new Vector();
    }
    
    /**
     * Adding a contact to a group
     * TODO: sorting of contacts using the nickname
     * @param u
     */
    public boolean addContact(Contact u) {
        contacts.addElement(u);
        return true;
    }

}