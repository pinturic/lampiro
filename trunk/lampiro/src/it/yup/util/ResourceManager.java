/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: ResourceManager.java 1891 2009-11-03 17:43:52Z luca $
*/

package it.yup.util;

import it.yup.xmpp.Config;
import it.yup.xmpp.XMPPClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.AlertType;

public class ResourceManager {

	private static ResourceManager manager = null;

	private Hashtable resources = new Hashtable();


	private ResourceManager(String name, String locale) {
// #ifndef GLIDER
					InputStream is = this.getClass().getResourceAsStream(
							name + "." + locale);
			// #endif
			try {

				// the max length that the common file can contain
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int b;
				while ((b = is.read()) != -1) {
					byte c = (byte) b;
					if (b == '\n') {
						String str = Utils.getStringUTF8(baos.toByteArray());
						Vector tokens = Utils.tokenize(str, '\t');
						Object key = tokens.elementAt(0);
						Object element = tokens.elementAt(1);
						resources.put(key, element);
						baos.reset();
					} else {
						baos.write(c);
					}
				}
				is.close();
			} catch (IOException e) {
				// XXX we should launch an exception and trap it outside, without using the XMPPClient
				XMPPClient.getInstance().showAlert(AlertType.ERROR,
						Config.ALERT_DATA,Config.ALERT_DATA,e.getMessage());

			}
	}


	public static ResourceManager getManager(String name, String locale) {
		if (ResourceManager.manager == null) {
			manager = new ResourceManager(name, locale);
		}
		return manager;
	}

	public static ResourceManager getManager(String name) {
		return getManager(name, Config.lang);
	}

	public static ResourceManager getManager() {
		return getManager("/locale/common", Config.lang);
	}

	public String getString(int id) {
		return (String) resources.get("" + id);
	}
}
