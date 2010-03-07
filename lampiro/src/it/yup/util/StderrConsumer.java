/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: StderrConsumer.java 2002 2010-03-06 19:02:12Z luca $
*/

package it.yup.util;

public class StderrConsumer implements LogConsumer {

	public void gotMessage(String message, int level) {
		System.err.println(message);
	}

	public void setExiting() {
		// just ignore
	}

}
