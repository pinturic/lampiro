/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: FTSender.java 1858 2009-10-16 22:42:29Z luca $
*/
package it.yup.xmpp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.bouncycastle.util.encoders.Base64;

import it.yup.util.Utils;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Presence;
import it.yup.xml.Element;
import it.yup.xmlstream.BasicXmlStream;
import it.yup.xmlstream.EventQuery;
import it.yup.xmlstream.EventQueryRegistration;
import it.yup.xmlstream.PacketListener;

// #mdebug
//@
//@import it.yup.util.Logger;
//@
// #enddebug
public class FTSender extends IQResultListener implements PacketListener {

	/*
	 * Initiate a session with jingle
	 */
	public static String SESSION_INITIATE = "session-initiate";

	/*
	 * Initiate a session with jingle
	 */
	public static String SESSION_ACCEPT = "session-accept";

	/*
	 * Initiate a session with jingle
	 */
	public static String SESSION_TERMINATE = "session-terminate";

	/*
	 * Jingle related jingle
	 */
	public static String JINGLE = "jingle";

	/*
	 * Jingle related jingle
	 */
	public static String DECLINE = "decline";

	/*
	 * Jingle related action
	 */
	public static String CONTENT = "content";

	/*
	 * Jingle related description
	 */
	public static String DESCRIPTION = "description";

	/*
	 * Jingle related description
	 */
	public static String DESC = "desc";

	/*
	 * Jingle related description
	 */
	public static String OFFER = "offer";

	/*
	 * Jingle related name
	 */
	public static String NAME = "name";

	/*
	 * Jingle related name
	 */
	public static String BLOCK_SIZE = "block-size";

	/*
	 * Jingle related creator
	 */
	public static String CREATOR = "creator";

	/*
	 * Jingle related initiator
	 */
	public static String INITIATOR = "initiator";

	/*
	 * Jingle related SID
	 */
	public static String SID = "sid";

	/*
	 * IBB related Seq
	 */
	public static String SEQ = "seq";

	/*
	 * Jingle related file
	 */
	public static String FILE = "file";

	/*
	 * Jingle related file
	 */
	//public static String OPEN = "open";

	/*
	 * Jingle related file
	 */
	//public static String CLOSE = "close";

	/*
	 * Jingle related file
	 */
	public static String TRANSPORT = "transport";

	/*
	 * Jingle related size
	 */
	public static String SIZE = "size";

	/*
	 * The name of the file to transfer
	 */
	public String fileName = "";

	/*
	 * The xmpp client
	 */
	private XMPPClient xmppClient = XMPPClient.getInstance();

	/*
	 * The data bytes of the file 
	 */
	private byte[] fileData = null;

	/*
	 * the chunk length
	 */
	private int chunk_length = 4096;

	/*
	 * The receiver of the file
	 */
	private String to = null;

	/*
	 * The description associated to the file
	 */
	private String desc = null;

	/*
	 * The Session id related to this section
	 */
	private String jingleSid = "";

	/*
	 * The Session id related to this section
	 */
	private String transportSid = "";

//	/*
//	 * The session must be opened and the client must have sent session acept
//	 * before sending file
//	 */
//	private boolean sessionOpened = false;

	private FTSEventHandler eh;

	/*
	 * 
	 */
	private EventQueryRegistration terminateReg;

	public static boolean supportFT(String fullJid) {
		Contact c = XMPPClient.getInstance().getRoster().getContactByJid(
				fullJid);
		if (c == null || c instanceof MUC) return false;
		Presence p = c.getPresence(fullJid);
		if (p == null) return false;
		Element caps = c.getCapabilities(p);
		return supportFT(caps);
	}

	public static boolean supportFT(Element caps) {
		if (caps == null) return false;
		Element[] features = caps.getChildrenByName(null, XMPPClient.FEATURE);
		Vector vars = new Vector(features.length);
		for (int i = 0; i < features.length; i++) {
			Element ithFeature = features[i];
			vars.addElement(ithFeature.getAttribute("var"));
		}
		if (vars.contains(XMPPClient.FILE_TRANSFER) == false
				|| vars.contains(XMPPClient.JINGLE) == false
				|| vars.contains(XMPPClient.JINGLE_FILE_TRANSFER) == false
				|| vars.contains(XMPPClient.JINGLE_IBB_TRANSPORT) == false) return false;
		return true;
	}

	public interface FTSEventHandler {

		public void fileAcceptance(Contact c, String fileName, boolean accept,
				FTSender sender);

		public void fileError(Contact c, String fileName, Element e);

		public void fileSent(Contact c, String fileName, boolean success,
				FTSender sender);

		public void chunkSent(int sentBytes, int length, FTSender sender);

		public void sessionInitated(Contact contactByJid, String fileName,
				FTSender sender);
	}

	/*
	 * The constructor that initiate a Jingle file transfer
	 */
	public FTSender(String fileName, byte[] fileData, String to, String desc,
			FTSEventHandler eh) {
		this.fileName = fileName;
		this.fileData = fileData;
		this.to = to;
		this.desc = desc;
		this.eh = eh;
	}

	public void sessionInitiate() {
		Iq sessionInitiateIq = new Iq(this.to, Iq.T_SET);
		Element jingle = sessionInitiateIq.addElement(XMPPClient.JINGLE,
				FTSender.JINGLE);
		jingle.setAttribute(XMPPClient.ACTION, SESSION_INITIATE);
		jingle.setAttribute(INITIATOR, xmppClient.my_jid);
		String encodedSid = Utils.hexDigest(System.currentTimeMillis() + "",
				"sha1");
		jingleSid = new String(encodedSid);
		jingle.setAttribute(SID, jingleSid);
		Element content = jingle.addElement(null, CONTENT);
		content.setAttribute(CREATOR, INITIATOR);
		content.setAttribute(NAME, "a-file-transfer" + jingleSid);
		Element description = content.addElement(
				XMPPClient.JINGLE_FILE_TRANSFER, DESCRIPTION);
		Element offer = description.addElement(null, OFFER);
		Element file = offer.addElement(XMPPClient.FILE_TRANSFER, FILE);
		file.setAttribute(NAME, this.fileName);
		if (fileData != null) file
				.setAttribute(SIZE, this.fileData.length + "");
		if (desc != null && desc.length() > 0) file.addElement(null, DESC)
				.addText(this.desc);
		Element transport = content.addElement(XMPPClient.JINGLE_IBB_TRANSPORT,
				TRANSPORT);
		transport.setAttribute(FTSender.BLOCK_SIZE, this.chunk_length + "");
		encodedSid = Utils.hexDigest((System.currentTimeMillis() + 1) + "",
				"sha1");
		transportSid = new String(encodedSid);
		transport.setAttribute(FTSender.SID, transportSid);

		PacketListener terminateListener = new PacketListener() {
			public void packetReceived(Element e) {
				isAccepted(e);
				if (terminateReg != null) {
					BasicXmlStream.removeEventListener(terminateReg);
					terminateReg = null;
				}
			}
		};
		//this.encodedData.setLength(fileSize*2);

		EventQuery eq = new EventQuery(Iq.IQ, new String[] { Iq.ATT_FROM,
				Iq.ATT_TYPE }, new String[] { this.to, Iq.T_SET });
		EventQuery eqChild = eq.child = new EventQuery(FTSender.JINGLE,
				new String[] { XMPPClient.ACTION },
				new String[] { FTSender.SESSION_TERMINATE });
		eqChild.child = new EventQuery(FTSender.DECLINE, null, null);
		terminateReg = BasicXmlStream.addEventListener(eq, terminateListener);

		xmppClient.sendIQ(sessionInitiateIq, this);

		eh.sessionInitated(XMPPClient.getInstance().getRoster()
				.getContactByJid(this.to), fileName, this);
	}

	private void initiateInteraction() {
		EventQuery eq = new EventQuery(Iq.IQ, new String[] { Iq.ATT_TYPE },
				new String[] { Iq.T_SET });
		eq.child = new EventQuery(FTSender.JINGLE, new String[] { "xmlns",
				XMPPClient.ACTION, FTSender.SID }, new String[] {
				XMPPClient.JINGLE, FTSender.SESSION_ACCEPT, this.jingleSid });

		BasicXmlStream.addOnetimeEventListener(eq, this);

		//		Iq initiateInteraction = new Iq(this.to, Iq.T_SET);
		//		Element open = initiateInteraction.addElement(XMPPClient.NS_IBB, OPEN);
		//		open.setAttribute(SID, transportSid);
		//		open.setAttribute(BLOCK_SIZE, chuck_length + "");
		//
		//		xmppClient.sendIQ(initiateInteraction, this);
	}

	private int fileOffset = 0;
	//private String encodedData = "";
	private int seq = 0;

	private void sendFile() {
		this.fileOffset = 0;
		sendChunk();
	}

	private void sendChunk() {
		Iq chunkIq = new Iq(this.to, Iq.T_SET);
		Element data = chunkIq.addElement(XMPPClient.NS_IBB, XMPPClient.DATA);
		data.setAttribute(SID, transportSid);
		data.setAttribute(SEQ, seq + "");
		seq++;
		if (seq == 65536) seq = 0;
		int endIndex = Math
				.min(fileOffset + chunk_length, fileData.length);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			Base64.encode(this.fileData,fileOffset,endIndex-fileOffset,baos);
		} catch (IOException e) {
			// #mdebug 
//@			Logger.log("error in sending file");
//@			e.printStackTrace();
			// #enddebug
		}
		String encodedData = new String(baos.toByteArray());
		fileOffset += chunk_length;
		data.addText(encodedData);
		xmppClient.sendIQ(chunkIq, this);
		eh.chunkSent(fileOffset, fileData.length, this);
	}

	/**
	 * @param e
	 */
	private boolean isAccepted(Element e) {
		Element decline = e.getPath(new String[] { XMPPClient.JINGLE, null },
				new String[] { FTSender.JINGLE, FTSender.DECLINE });

		if (decline != null) {
			eh.fileAcceptance(XMPPClient.getInstance().getRoster()
					.getContactByJid(e.getAttribute(Iq.ATT_FROM)), fileName,
					false, this);
			return false;
		}
		return true;
	}

	// FT machine state variables 
	public static final int FT_RESET = 0;
	public static final int FT_INITIATED = 1;
	public static final int FT_SENDING = 2;
	public static final int FT_WAITING_CLOSE = 3;
	public static final int FT_WAITING_TERMINATE = 4;

	private int FT_STATE = FT_RESET;

	public void handleError(Element e) {
		switch (FT_STATE) {
			case FT_RESET:
				eh.fileError(XMPPClient.getInstance().getRoster()
						.getContactByJid(e.getAttribute(Iq.ATT_FROM)),
						fileName, e);
				FT_STATE = FT_RESET;
				break;

			case FT_INITIATED:
				eh.fileAcceptance(XMPPClient.getInstance().getRoster()
						.getContactByJid(e.getAttribute(Iq.ATT_FROM)),
						fileName, false, this);
				FT_STATE = FT_RESET;
				break;

			case FT_SENDING:
				FT_STATE = FT_RESET;
				break;

			case FT_WAITING_CLOSE:
				FT_STATE = FT_RESET;
				eh.fileSent(XMPPClient.getInstance().getRoster()
						.getContactByJid(e.getAttribute(Iq.ATT_FROM)),
						fileName, true, this);
				break;

			default:
				break;
		}

	}

	public void handleResult(Element e) {
		switch (FT_STATE) {
			case FT_RESET:
				if (isAccepted(e)) this.initiateInteraction();
				FT_STATE = FT_INITIATED;
				break;

//			case FT_INITIATED:
//				if (sessionOpened) this.sendFile();
//				else
//					
//				// this result must have arrived and the packet received below
//				sessionOpened = true;
//				FT_STATE = FT_SENDING;
//				break;

			case FT_SENDING:
				//				if (fileOffset < encodedData.length()) {
				//					FT_STATE = FT_SENDING;
				//					this.sendChunk();
				//				} else {
				//					FT_STATE = FT_WAITING_CLOSE;
				//					this.sendFooter();
				//				}
				FT_STATE = FT_SENDING;
				if (fileOffset< fileData.length) {
					this.sendChunk();
				}
				else
				{
					Iq closeSession = new Iq(e.getAttribute(Iq.ATT_FROM),
							Iq.T_SET);
					Element jingleClose = closeSession.addElement(
							XMPPClient.JINGLE, FTSender.JINGLE);
					jingleClose.setAttribute(XMPPClient.ACTION,
							FTSender.SESSION_TERMINATE);
					jingleClose.setAttribute(FTSender.SID, jingleSid);
					jingleClose.addElement(null, "reason").addElement(null,
							"success");
					xmppClient.sendPacket(closeSession);
					eh.fileSent(XMPPClient.getInstance().getRoster()
							.getContactByJid(e.getAttribute(Iq.ATT_FROM)),
							fileName, true, this);
					FT_STATE = FT_RESET;
				}
				break;

			case FT_WAITING_CLOSE:
//				sessionOpened = false;
				eh.fileSent(XMPPClient.getInstance().getRoster()
						.getContactByJid(e.getAttribute(Iq.ATT_FROM)),
						fileName, true, this);
				FT_STATE = FT_WAITING_TERMINATE;
				break;

			default:
				break;
		}

	}

	public void packetReceived(Element e) {

		Iq reply = Utils.easyReply(e);

		switch (FT_STATE) {
			case FT_RESET:
			case FT_INITIATED:
			case FT_SENDING:
				eh.fileAcceptance(XMPPClient.getInstance().getRoster()
						.getContactByJid(e.getAttribute(Iq.ATT_FROM)),
						fileName, true, this);
				xmppClient.sendPacket(reply);
//				if (sessionOpened) this.sendFile();
//				else
//					this.encodedData = new String(Base64.encode(this.fileData));
				//this.encodedData = new String(Base64.encode(this.fileData));
				this.sendFile();
				// this iq must have arrived and the packet received below
//				sessionOpened = true;
				FT_STATE = FT_SENDING;
				break;

			case FT_WAITING_CLOSE:
				xmppClient.sendPacket(reply);
				FT_STATE = FT_RESET;
				break;

			case FT_WAITING_TERMINATE:
				xmppClient.sendPacket(reply);
				FT_STATE = FT_RESET;
				break;
		}
	}
}
