/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: Iq.java 2002 2010-03-06 19:02:12Z luca $
*/

package it.yup.xmpp.packets;

public class Iq extends Stanza {

	public static final String T_GET = "get";
	public static final String T_SET = "set";
	public static final String T_RESULT = "result";
	public static final String T_ERROR = "error";

	public static final String IQ = "iq";
	public static final String QUERY = "query";
	public static final String PROMPT = "prompt";

	public Iq(String to, String type) {
		super(IQ, to, type,
				type.equals(T_SET) || type.equals(T_GET) ? createUniqueId()
						: null);
	}
}
