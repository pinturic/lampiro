/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: StderrConsumer.java 846 2008-09-11 12:20:05Z luca $
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
