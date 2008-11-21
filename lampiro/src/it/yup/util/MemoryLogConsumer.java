/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: MemoryLogConsumer.java 846 2008-09-11 12:20:05Z luca $
*/

package it.yup.util;

import java.util.Vector;

public class MemoryLogConsumer implements LogConsumer {

	public Vector messages = new Vector();
	public int max_size = 10;
	private static MemoryLogConsumer consumer = null;
	
	private MemoryLogConsumer(){};
	
	public static MemoryLogConsumer getConsumer(){
		if(consumer == null) {
			consumer = new MemoryLogConsumer();
		}
		return consumer;
	};
	
	public void gotMessage(String message, int level) {
		messages.addElement(message);
		if(messages.size()>max_size) {
			messages.removeElementAt(0);
		}
	}

	public void setExiting() {
		// TODO Auto-generated method stub	
	}
}
