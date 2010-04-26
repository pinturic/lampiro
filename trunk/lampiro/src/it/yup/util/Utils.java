/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: Utils.java 2045 2010-04-01 09:14:22Z luca $
*/

package it.yup.util;

import it.yup.xml.Element;
import it.yup.xmpp.Contact;
import it.yup.xmpp.packets.Iq;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.Vector;

import org.bouncycastle.crypto.digests.GeneralDigest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;

public class Utils {

	/**
	 * Global event scheduler
	 */
	static public Timer tasks = new Timer();

	public static boolean has_utf8 = false;

	static {
		try {
			// XXX don't waht will happen with optimizer && obfuscator
			"".getBytes("UTF-8");
			has_utf8 = true;
		} catch (UnsupportedEncodingException usx) {
			has_utf8 = false;
		}
	}

	/**
	 * Compute a digest of a message
	 * @param msg 
	 * 	The message whose digest must be computed. The encoding must be utf-8
	 * @param digestType 
	 * 	sha1 or md5
	 * @return
	 * 	a string representing the digest
	 */
	public static String hexDigest(String msg, String digestType) {
		return bytesToHex(digest(msg, digestType));
	}

	static public byte[] digest(String msg, String digestType) {
		return digest(getBytesUtf8(msg), digestType);
	}

	/**
	 * 
	 * @param data
	 * @param digestType
	 * @return the digest or null if the requested digest is not supported
	 */
	static public byte[] digest(byte data[], String digestType) {
		GeneralDigest digest = null;
		if (digestType.equals("sha1")) {
			digest = new SHA1Digest();
		} else if (digestType.equals("md5")) {
			digest = new MD5Digest();
		} else {
			return null;
		}

		// XXX too many copies of data, modify the hash functions so that they write
		// the result to a byte array
		digest.update(data, 0, data.length);
		// some emulators fail on calling getByteLength  
		byte out[] = null;
		try {
			out = new byte[digest.getByteLength()];
		} catch (Error e) {
			out = new byte[64];
		}

		int len = digest.doFinal(out, 0);
		byte result[] = new byte[len];
		System.arraycopy(out, 0, result, 0, len);
		return result;
	}

	private static void hexDigit(PrintStream p, byte x) {
		char c;

		c = (char) ((x >> 4) & 0xf);
		if (c > 9) c = (char) ((c - 10) + 'a');
		else
			c = (char) (c + '0');
		p.write(c);
		c = (char) (x & 0xf);
		if (c > 9) c = (char) ((c - 10) + 'a');
		else
			c = (char) (c + '0');
		p.write(c);
	}

	public static String bytesToHex(byte digestBits[]) {
		ByteArrayOutputStream ou = new ByteArrayOutputStream();
		PrintStream p = new PrintStream(ou);
		for (int i = 0; i < digestBits.length; i++)
			hexDigit(p, digestBits[i]);

		return (ou.toString());
	}

	/**
	 * Parse a  line with values separated by delim
	 * 
	 */
	public static Vector tokenize(String line, char delim) {
		Vector tokens = new Vector();
		StringBuffer token = new StringBuffer();
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == delim) {
				tokens.addElement(token.toString());
				token.setLength(0);
			} else {
				token.append(c);
			}
		}
		tokens.addElement(token.toString());
		return tokens;
	}

	/**
	 * Parse a  line with values separated by delim
	 * @param line the string that is being parsed
	 * @param delims the set of delimiters
	 * @param keep if true keep delimiters
	 */
	public static Vector tokenize(String line, String delims[], boolean keep) {
		Vector tokens = new Vector();
		StringBuffer token = new StringBuffer();
		int i = 0;
		boolean in_delim = true;
		while (i < line.length()) {
			int l = -1;
			// find the longest delimiter
			int di = -1;
			for (int j = 0; j < delims.length; j++) {
				if (delims[j].length() > l && line.startsWith(delims[j], i)) {
					l = delims[j].length();
					di = j;
				}
			}

			if (in_delim) {
				if (l < 0) {
					token.append(line.charAt(i));
					in_delim = false;
				} else if (keep) {
					tokens.addElement(delims[di]);
				}
			} else {
				if (l >= 0) {
					tokens.addElement(token.toString());
					token.setLength(0);
					if (keep) {
						tokens.addElement(delims[di]);
					}
					;
					in_delim = true;
				} else {
					token.append(line.charAt(i));
				}
			}

			i += Math.max(l, 1);
		}
		if (token.length() > 0) {
			tokens.addElement(token.toString());
		}
		return tokens;
	}

	public final static String replace(String text, String searchString,
			String replacementString) {
		StringBuffer sBuffer = new StringBuffer();
		int pos = 0;
		while ((pos = text.indexOf(searchString)) != -1) {
			sBuffer.append(text.substring(0, pos) + replacementString);
			text = text.substring(pos + searchString.length());
		}
		sBuffer.append(text);
		return sBuffer.toString();
	}

	public static boolean is_jid(String s) {
		Vector parts = Utils.tokenize(s, '.');
		return (parts.size() >= 2)
				&& ((String) parts.elementAt(0)).length() > 0
				&& ((String) parts.elementAt(1)).length() > 0;
	}

	public static String jabberify(String id, int maxLength) {
		id = id.toLowerCase();
		char[] res = id.toCharArray();
		char[] invChar = new char[] { 0x20, 0x22, 0x26, 0x27, 0x2F, 0x3A, 0x3C,
				0x3E, 0x40 };

		for (int i = 0; i < res.length; i++) {
			for (int j = 0; j < invChar.length; j++)
				if (res[i] == invChar[j]) {
					res[i] = '_';
				}
		}
		id = new String(res);
		if (maxLength >= 0 && id.length() > maxLength) {
			id = id.substring(0, maxLength);
		}
		return id;
	}

	public static boolean is_email(String s) {
		Vector parts = Utils.tokenize(s, '@');
		return (parts.size() == 2)
				&& ((String) parts.elementAt(0)).length() > 0
				&& ((String) parts.elementAt(1)).length() > 0;
	}

	public static boolean[] str2flags(String s, int start, int length) {
		int flags = Integer.parseInt(s);
		boolean v[] = new boolean[length];
		flags >>= start;
		for (int i = 0; i < length; i++) {
			int mask = 0x01 << i;
			v[i] = ((flags & mask) == mask);
		}
		return v;
	};

	//	public static int readExactly (InputStream is, byte [] buf , int start , int length) throws IOException{
	//		int tempLength = 0;
	//		int n = 0;
	//		do {
	//			n = is.read(buf, tempLength,buf.length - tempLength);
	//			tempLength += n;
	//		} while (n >= 0 && tempLength < length);
	//		return tempLength;
	//	}

	public static String flags2str(boolean v[], int offset) {
		int flags = 0;
		for (int i = 0; i < v.length; i++) {
			flags |= (v[i] ? (0x01 << i) : (0));
		}
		flags <<= offset;
		return "" + flags;
	}

	public static Vector find_urls(String s) {
		Vector v = new Vector();
		int i = 0;
		int url_start, url_end;
		while (i < s.length()) {
			if ((url_start = s.indexOf("http://", i)) >= 0) {
				url_end = s.indexOf(' ', url_start + 7);
				if (url_end < 0) url_end = s.length();
				v.addElement(s.substring(url_start, url_end));
				i = url_end;
			} else if ((url_start = s.indexOf("www.", i)) >= 0) {
				url_end = s.indexOf(' ', url_start + 4);
				if (url_end < 0) url_end = s.length();
				v.addElement("http://" + s.substring(url_start, url_end));
				i = url_end;
			} else {
				break;
			}
		}
		return v;
	}

	public static final String getStringUTF8(byte in[], int len) {
		//		if (has_utf8) {
		//			try {
		//				return new String(in, 0, len, "UTF-8");
		//			} catch (UnsupportedEncodingException usx) {
		//				// shouldnt...
		//			}
		//		}
		int max = len;
		int outLen = len;
		for (int i = 0; i < max; i++) {
			if ((in[i] & 0x80) == 0) {
				;
			} else if ((in[i] & 0xe0) == 0xc0) {
				outLen--;
			} else if ((in[i] & 0xf0) == 0xe0) {
				outLen -= 2;
			} else if ((in[i] & 0xf8) == 0xf0) {
				outLen -= 3;
			} else {
				;
			}
		}
		char[] outChar = new char[outLen];
		int outIndex = 0;
		for (int i = 0; i < len; i++) {
			char c = 0;
			if ((in[i] & 0x80) == 0) {
				c = (char) in[i];
			} else if ((in[i] & 0xe0) == 0xc0) {
				c |= ((in[i] & 0x1f) << 6);
				i++;
				c |= ((in[i] & 0x3f) << 0);
			} else if ((in[i] & 0xf0) == 0xe0) {
				c |= ((in[i] & 0x0f) << 12);
				i++;
				c |= ((in[i] & 0x3f) << 6);
				i++;
				c |= ((in[i] & 0x3f) << 0);
			} else if ((in[i] & 0xf8) == 0xf0) {
				c |= ((in[i] & 0x07) << 18);
				i++;
				c |= ((in[i] & 0x3f) << 12);
				i++;
				c |= ((in[i] & 0x3f) << 6);
				i++;
				c |= ((in[i] & 0x3f) << 0);
			} else {

			}
			outChar[outIndex] = c;
			outIndex++;
		}
		return new String(outChar);

	}

	public static final String getStringUTF8(byte in[]) {
		return getStringUTF8(in, in.length);
	}

	public static final byte[] getBytesUtf8(String str) {
		//		if (has_utf8) {
		//			try {
		//				return str.getBytes("UTF-8");
		//			} catch (UnsupportedEncodingException usx) {
		//				// shouldnt...
		//			}
		//		}
		char[] chars = str.toCharArray();
		int vlen = chars.length;
		for (int i = 0; i < chars.length; i++) {
			char ch = chars[i];
			if (ch >= 0 && ch <= 0x07F) {
				;
			} else if (ch >= 0x080 && ch <= 0x07FF) {
				vlen++;
			} else if ((ch >= 0x0800 && ch <= 0x0D7FF)
					|| (ch >= 0x00E000 && ch <= 0x00FFFD)) {
				vlen += 2;
			} else if (ch >= 0x010000 && ch <= 0x10FFFF) {
				vlen += 3;
			} else {
				/* invalid char, ignore */
				vlen--;
			}
		}
		byte[] buf = new byte[vlen];
		int j = 0;
		for (int i = 0; i < chars.length; i++) {
			char ch = chars[i];
			if (ch >= 0 && ch <= 0x07F) {
				buf[j++] = (byte) (ch & 0x07F);
			} else if (ch >= 0x080 && ch <= 0x07FF) {
				buf[j++] = (byte) (0xC0 | ((ch & 0x07C0) >> 6));
				buf[j++] = (byte) (0x80 | ((ch & 0x003F)));
			} else if ((ch >= 0x0800 && ch <= 0x0D7FF)
					|| (ch >= 0x00E000 && ch <= 0x00FFFD)) {
				buf[j++] = (byte) (0xE0 | ((ch & 0x0F000) >> 12));
				buf[j++] = (byte) (0x80 | ((ch & 0x00FC0) >> 6));
				buf[j++] = (byte) (0x80 | ((ch & 0x0003F)));
			} else if (ch >= 0x010000 && ch <= 0x10FFFF) {
				/* non dovrebbero essere usate */
				buf[j++] = (byte) (0xE0 | ((ch & 0x1C0000) >> 18));
				buf[j++] = (byte) (0x80 | ((ch & 0x03F000) >> 12));
				buf[j++] = (byte) (0x80 | ((ch & 0x000FC0) >> 6));
				buf[j++] = (byte) (0x80 | ((ch & 0x00003F)));
			}
		}
		return buf;
	}

	public static Iq easyReply(Element fromIq) {
		Iq replIq = new Iq(fromIq.getAttribute(Iq.ATT_FROM), Iq.T_RESULT);
		replIq.setAttribute(Iq.ATT_ID, fromIq.getAttribute(Iq.ATT_ID));
		return replIq;
	}

	public static boolean compareTo(Contact left, Contact right) {
		return left.getPrintableName().toLowerCase().compareTo(
				right.getPrintableName().toLowerCase()) < 0;
	}

	//	public static int indexOf(char[] source, char[] target, int fromIndex,
	//			int sourceCount) {
	//		if (fromIndex >= sourceCount) { return (target.length == 0 ? sourceCount
	//				: -1); }
	//		if (fromIndex < 0) {
	//			fromIndex = 0;
	//		}
	//		if (target.length == 0) { return fromIndex; }
	//
	//		char first = target[0];
	//		int max = (sourceCount - target.length);
	//		for (int i = fromIndex; i <= max; i++) {
	//			/* Look for first character. */
	//			if (source[i] != first) {
	//				while (++i <= max && source[i] != first)
	//					;
	//			}
	//			if (i <= max) {
	//				int j = i + 1;
	//				int end = j + target.length - 1;
	//				for (int k = 1; j < end && source[j] == target[k]; j++, k++)
	//					;
	//
	//				if (j == end) {
	//					/* Found whole string. */
	//					return i;
	//				}
	//			}
	//		}
	//		return -1;
	//	}

	/**
	 * The lookup table used to memorize letters for search pattern
	 */
	public static char[][] itu_keys = { { ' ', '0' }, { '1' },
			{ 'a', 'b', 'c', 'à', '2' }, { 'd', 'e', 'f', 'è', 'é', '3' },
			{ 'g', 'h', 'i', 'ì', '4' }, { 'j', 'k', 'l', '5' },
			{ 'm', 'n', 'o', 'ò', '6' }, { 'p', 'q', 'r', 's', '7' },
			{ 't', 'u', 'v', 'ù', '8' }, { 'w', 'x', 'y', 'z', '9' } };

}
