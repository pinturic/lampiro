/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: Logger.java 2002 2010-03-06 19:02:12Z luca $
*/

package it.yup.util;

import java.util.Vector;

public class Logger {
	
	public static int DEBUG = 0; 

	private static Vector consumers = new Vector();
	
	private Logger() {}; // forbid direct instantiation
	
	public static void addConsumer(LogConsumer consumer) {
		Logger.consumers.addElement(consumer);
	}
	
	public static void removeConsumer(LogConsumer consumer) {
		Logger.consumers.removeElement(consumer);
	}
	
	public static void log(String message, int level) {
		for(int i=0; i<Logger.consumers.size(); i++) {
			((LogConsumer)Logger.consumers.elementAt(i)).gotMessage(message, level);
		}
	}
	
	public static void log(String message) {
		log(message, Logger.DEBUG);
	}
}
