/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: Config.java 1135 2009-01-27 23:07:46Z luca $
*/

package it.yup.xmpp;

// #debug
//@import it.yup.util.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.lcdui.AlertType;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreNotFoundException;

/**
 * Client Configuration
 */
public class Config {

	private static String version = "00.03";

	/** name of the record store */
	public static final String RMS_NAME = "yuprms";

	/** config index in the record store */
	public static final int RNUM_CONFIG = 1;

	/** roster index in the record store */
	public static final int RNUM_ROSTER = 2;

	private Hashtable properties = new Hashtable();

	/** Connection url used by the GPRS connector */
	// public static final String GPRS_CONNECTION_URL =
	// "socket://www.bluendo.com:5280";
	public static final String GPRS_CONNECTION_URL = "socket://localhost:10080";
	// public static final String GPRS_CONNECTION_URL =
	// "socket://bosh.bluendo.com:10080";

	/** URL (host/port) of the GPRS/HTTP gateway */
	// public static final String HTTP_GW_HOST = "dalek";
	public static final String HTTP_GW_HOST = "bosh.bluendo.com";

	/** path of the GPRS/HTTP gateway */
	public static final String HTTP_GW_PATH = "/httpb";

	/** path of the GPRS/HTTP gateway */
	public static final String SRV_QUERY_PATH = "http://services.bluendo.com/srv/?domain=";

	/**
	 * time the server should wait before sending a response if no data is
	 * available
	 */
	public static final int WAIT_TIME = 30;

	// /** default value keepalive of the plain socket */
	// XXX We may keep this but the transport should not read it from Config
	private static final int SO_KEEPALIVE = 60 * 15 * 1000;

	/** Config instance */
	private static Config instance;

	public static String TRUE = "t";
	public static String FALSE = "f";

	// constants for values saved in the record store
	/** server name */
	public static short SERVER = 0x0000;
	/** user name */
	public static short USER = 0x0001;
	/** password */
	public static short PASSWORD = 0x0002;
	/** mail address */
	public static short EMAIL = 0x0003;
	/** connecting server (after a SRV_RECORD query ) */
	public static short CONNECTING_SERVER = 0x0004;
	/** sw version */
	public static short VERSION = 0x0005;
	/** sw version */
	public static short SILENT = 0x0006;
	/** logged once */
	public static short LOGGED_ONCE = 0x0007;
	/** keaplive for plain sockets */
	public static short KEEP_ALIVE = 0x0008;
	/** flag which is true after the first succesful login and roster update */
	public static short CLIENT_INITIALIZED = 0x0009;
	/** last "show" used in the presence */
	public static short LAST_PRESENCE_SHOW = 0x0010;
	/** last status message */
	public static short LAST_STATUS_MESSAGE = 0x0011;
	/** last compression settings used */
	public static short COMPRESSION = 0x0019;
	/** last TLS settings used */
	public static short TLS = 0x0020;
	/** last priority */
	public static short LAST_PRIORITY = 0x0017;
	/** XMPP resource */
	public static short YUP_RESOURCE = 0x0021;


	/**
	 * Using bit masks
	 * 
	 * vibration settings:
	 * <ul>
	 * <li>0x00: none </li>
	 * <li>0x01: only when hidden</li>
	 * <li>0x02: only when shown</li>
	 * <li>0x03: always </li>
	 * </ul>
	 * 
	 * tone settings:
	 * <ul>
	 * <li>0x00: none </li>
	 * <li>0x04: only when hidden</li>
	 * <li>0x08: only when shown</li>
	 * <li>0x0C: always </li>
	 * </ul> *
	 * 
	 */
	public static short VIBRATION_AND_TONE_SETTINGS = 0x0012;

	/** The theme associated to the Application */
	public static short COLOR = 0x0013;

	/** tone volume */
	public static short TONE_VOLUME = 0x0014;

	/**
	 * UICanvas keys for left and right. String is a comma-separated couple of
	 * integers representing (in order) left and right key
	 */
	public static short CANVAS_KEYS = 0x0015;

	/*
	 * Font Size for roster and chat
	 */
	public static short FONT_SIZE = 0x0016;

	/*
	 * Font Size for roster and chat
	 */
	public static short HISTORY_SIZE = 0x0018;
	
	/*
	 * The accepted gateways
	 */
	public static short ACCEPTED_GATEWAYS = 0x0022;

	/** the bluendo assistent */
	public static final String LAMPIRO_AGENT = "lampiro@golem.jabber.bluendo.com";

	/** maximum wait time for a packet (should we let configure this ) */
	public static final int TIMEOUT = -1;

	/**
	 * Get the configuration using the stored values (if any), or use the
	 * default values
	 */
	public synchronized static Config getInstance() {
		if (instance == null) {
			instance = new Config();
			instance.loadFromStorage();
		}
		return instance;
	}

	/** Make the constructur private -> singleton */
	private Config() {
	}

	/**
	 * Load the the configuration from the RMS
	 */
	private void loadFromStorage() {

		RecordStore recordStore = null;
		try {
			recordStore = RecordStore.openRecordStore(RMS_NAME, false);
			byte[] b = recordStore.getRecord(RNUM_CONFIG);

			if (b != null && b.length != 0) {
				DataInputStream in = new DataInputStream(
						new ByteArrayInputStream(b));
				while (in.available() > 0) {
					short code = in.readShort();
					String val = in.readUTF();
					properties.put(String.valueOf(code), val);
				}
				in.close();
			}

			String _version = getProperty(Config.VERSION);

			if (_version == null || _version.compareTo(Config.version) < 0) {
				// the software has been updated, handle here the "update" logic
				setDefaults();
			}
		} catch (RecordStoreNotFoundException rnfe) {
			// no memory, using the default values
			setDefaults();
		} catch (InvalidRecordIDException ire) {
			// the record does not exist, using the default value
			setDefaults();
		} catch (Exception e) {
			this.resetStorage(true);
			XMPPClient.getInstance().showAlert(
					AlertType.ERROR,
					"Config Error",
					"Error while loading config:\n" + e.getMessage()
							+ "\nConfig has been reset.", null);
		} finally {
			try {
				if (recordStore != null) {
					recordStore.closeRecordStore();
				}
			} catch (Exception e) {
			}
		}
	}

	private void setDefaults() {
		setProperty(Config.VERSION, Config.version);
		setDefault(Config.USER, "");
		setDefault(Config.SERVER, "");
		setDefault(Config.EMAIL, "");
		setDefault(Config.CONNECTING_SERVER, "");
		setDefault(Config.SILENT, "y");
		setDefault(Config.LOGGED_ONCE, "0");
		setDefault(Config.KEEP_ALIVE, "" + SO_KEEPALIVE);
		setDefault(Config.CLIENT_INITIALIZED, Config.FALSE);
		setDefault(Config.LAST_STATUS_MESSAGE,
				"Lampiro (http://lampiro.bluendo.com)");
		saveToStorage();
	}

	/**
	 * Reset the options. If hard is set to true even the login credentials are
	 * reset
	 * 
	 * @param hard
	 */
	public void resetStorage(boolean hard) {
		Config cfg;
		String user = null;
		String password = null;
		String server = null;
		String email = null;
		String connectingServer = null;
		if (hard == false) {
			cfg = Config.getInstance();
			try {
				user = cfg.getProperty(Config.USER);
				server = cfg.getProperty(Config.SERVER);
				password = cfg.getProperty(Config.PASSWORD);
				email = cfg.getProperty(Config.EMAIL);
				connectingServer = cfg.getProperty(Config.CONNECTING_SERVER);
			} catch (Exception e) {
				resetStorage(true);
				return;
			}
		}
		properties.clear();
		if (hard == false) {
			cfg = Config.getInstance();
			cfg.setProperty(Config.USER, user);
			cfg.setProperty(Config.PASSWORD, password);
			cfg.setProperty(Config.SERVER, server);
			cfg.setProperty(Config.EMAIL, email);
			cfg.setProperty(Config.CONNECTING_SERVER, connectingServer);
		}
		this.saveToStorage();
		// so that the new options are automatically reloaded
		this.loadFromStorage();
	}

	public void saveToStorage() {

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(baos);

			Enumeration en = properties.keys();
			while (en.hasMoreElements()) {
				String code = (String) en.nextElement();
				out.writeShort(Integer.parseInt(code));
				out.writeUTF((String) properties.get(code));
			}
			RecordStore rms = RecordStore.openRecordStore(RMS_NAME, true);
			byte[] data = baos.toByteArray();
			while (rms.getNumRecords() < RNUM_CONFIG) {
				rms.addRecord(null, 0, 0);
			}
			rms.setRecord(RNUM_CONFIG, data, 0, data.length);
		} catch (Exception e) {
			// #mdebug
			//@						Logger.log("Error in saving to storage: " + e.getMessage(),
			//@									Logger.DEBUG);
			//@			
			// #enddebug
			XMPPClient.getInstance().showAlert(AlertType.ERROR, "Config Error",
					"Error while saving config:\n" + e.getMessage(), null);
		}

	}

	public String getProperty(short code) {
		return (String) properties.get(String.valueOf(code));
	};

	public String getProperty(short code, String default_value) {
		String s = (String) properties.get(String.valueOf(code));
		return (s == null) ? default_value : s;
	};

	public void setProperty(short code, String value) {
		properties.put(String.valueOf(code), value);
	}

	/**
	 * Set the default value for a property if none is given
	 * 
	 * @param code
	 * @param default_value
	 */
	private void setDefault(short code, String default_value) {
		if (!this.properties.containsKey(String.valueOf(code))) {
			setProperty(code, default_value);
		}
	}

}
