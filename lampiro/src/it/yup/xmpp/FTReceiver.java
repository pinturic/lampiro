package it.yup.xmpp;

import org.bouncycastle.util.encoders.Base64;

//#mdebug
//@
//@import it.yup.util.Logger;
//@
// #enddebug

import it.yup.util.Utils;
import it.yup.xml.Element;
import it.yup.xmlstream.BasicXmlStream;
import it.yup.xmlstream.EventQuery;
import it.yup.xmlstream.EventQueryRegistration;
import it.yup.xmlstream.PacketListener;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Stanza;

public class FTReceiver implements PacketListener {

	/*
	 * The xmpp client
	 */
	private XMPPClient xmppClient = XMPPClient.getInstance();

	/*
	 * A file transfer receiver event handler
	 */
	private FTREventHandler eh;

	public interface FTREventHandler {

		public void dataReceived(byte[] data, String fileName, String fileDesc,
				OpenListener ftrp);

		public void reqFT(String contactName, OpenListener ftrp);

		public void chunkReceived(int length, int fileSize,
				OpenListener openListener);
	}

	public class OpenListener implements PacketListener {
		public Element e_jingle;

		private int block_size = 4096;
		//private StringBuffer encodedData= new StringBuffer(block_size);
		private StringBuffer encodedData = new StringBuffer();
		private byte[] decodedData;

		private EventQueryRegistration dataListenerEq;

		// the file size
		public int fileSize;

		// the file size
		public String fileName;

		public String fileDesc = "";

		public void answerFT(boolean accept) {
			// If accept send a correct reply and register a listener
			// to open the jingle stream
			if (accept) {
				this.acceptSession();
			} else {
				Iq reply = new Iq(this.e_jingle.getAttribute(Iq.ATT_FROM),
						Iq.T_SET);
				Element jingle = reply.addElement(XMPPClient.JINGLE,
						FTSender.JINGLE);
				jingle.setAttribute(XMPPClient.ACTION,
						FTSender.SESSION_TERMINATE);
				jingle.addElement(null, FTSender.DECLINE);
				xmppClient.sendPacket(reply);
			}
		}

		public void packetReceived(Element e) {

			//this.encodedData.setLength(fileSize*2);

			Element child = e.getChildByName(null, XMPPClient.DATA);
			if (child != null) {
				handleData(e);
				return;
			}

//			child = e.getChildByName(null, FTSender.CLOSE);
//			if (child != null) {
//				handleClose(e);
//				return;
//			}

//			child = e.getChildByName(null, FTSender.OPEN);
//			if (child != null) {
//				handleOpen(e);
//				return;
//			}
		}

//		private void handleOpen(Element e) {
//			EventQuery eq = new EventQuery(Iq.IQ, new String[] { Iq.ATT_FROM,
//					Iq.ATT_TYPE }, new String[] { e.getAttribute(Iq.ATT_FROM),
//					Iq.T_SET });
//			Element openElement = e.getChildByName(null, FTSender.OPEN);
//			eq.child = new EventQuery(XMPPClient.DATA, new String[] {
//					FTSender.SID, "xmlns" }, new String[] {
//					openElement.getAttribute(FTSender.SID), XMPPClient.NS_IBB });
//			this.dataListenerEq = BasicXmlStream.addEventListener(eq, this);
//
//			eq = new EventQuery(Iq.IQ,
//					new String[] { Iq.ATT_FROM, Iq.ATT_TYPE }, new String[] {
//							e.getAttribute(Iq.ATT_FROM), Iq.T_SET });
//			eq.child = new EventQuery(FTSender.CLOSE, new String[] {
//					FTSender.SID, "xmlns" }, new String[] {
//					openElement.getAttribute(FTSender.SID), XMPPClient.NS_IBB });
//			BasicXmlStream.addOnetimeEventListener(eq, this);
//
//			block_size = Integer.parseInt(openElement
//					.getAttribute(FTSender.BLOCK_SIZE));
//
//			Stanza reply = new Iq(e.getAttribute(Iq.ATT_FROM), Iq.T_RESULT);
//			reply.setAttribute(Iq.ATT_ID, e.getAttribute(Iq.ATT_ID));
//			xmppClient.sendPacket(reply);
//		}

		/**
		 * @param e
		 */
		private void handleData(Element e) {
			try {
				String chunkData = e.getChildByName(null, XMPPClient.DATA)
						.getText();
				Iq replIq = new Iq(e.getAttribute(Iq.ATT_FROM), Iq.T_RESULT);
				replIq.setAttribute(Iq.ATT_ID, e.getAttribute(Iq.ATT_ID));
				xmppClient.sendPacket(replIq);
				encodedData.append(chunkData);
				// the data is base64 encoded 
				eh.chunkReceived((encodedData.length() * 3) / 4, fileSize,
						OpenListener.this);
				
				// finished receiving 
				if ((encodedData.length() * 3) / 4 >=fileSize){
					try {
						// when finishing file transfer 
						// the registration is removed by myself
						BasicXmlStream.removeEventListener(dataListenerEq);
						Iq closeSession = new Iq(e.getAttribute(Iq.ATT_FROM), Iq.T_SET);
						Element jingleClose = closeSession.addElement(
								XMPPClient.JINGLE, FTSender.JINGLE);
						jingleClose.setAttribute(XMPPClient.ACTION,
								FTSender.SESSION_TERMINATE);
						jingleClose.setAttribute(FTSender.SID, e_jingle.getChildByName(
								null, FTSender.JINGLE).getAttribute(FTSender.SID));
						jingleClose.addElement(null, "reason").addElement(null,
								"success");
						xmppClient.sendPacket(closeSession);
						String decString = encodedData.toString();
						decodedData = Base64.decode(decString);
						// #mdebug 
//@						Logger.log("File received kb: " + decodedData.length);
//@						// System.out.println(decString);
						// #enddebug
						eh.dataReceived(decodedData, fileName, fileDesc,
								OpenListener.this);
					} catch (Exception ex) {
						// #mdebug
//@						ex.printStackTrace();
//@						Logger.log("In closing session" + ex.getClass().getName()
//@								+ "\n" + ex.getMessage());
						// #enddebug
					}
				}
			} catch (Exception ex) {
				// #mdebug
//@				ex.printStackTrace();
//@				Logger.log("In receiving an IBB packet"
//@						+ ex.getClass().getName() + "\n" + ex.getMessage());
				// #enddebug
			}
		}

//		/**
//		 * @param e
//		 */
//		private void handleClose(Element e) {
//			try {
//				BasicXmlStream.removeEventListener(dataListenerEq);
//				Iq reply = Utils.easyReply(e);
//				xmppClient.sendPacket(reply);
//				Iq closeSession = new Iq(e.getAttribute(Iq.ATT_FROM), Iq.T_SET);
//				Element jingleClose = closeSession.addElement(
//						XMPPClient.JINGLE, FTSender.JINGLE);
//				jingleClose.setAttribute(XMPPClient.ACTION,
//						FTSender.SESSION_TERMINATE);
//				jingleClose.setAttribute(FTSender.SID, e_jingle.getChildByName(
//						null, FTSender.JINGLE).getAttribute(FTSender.SID));
//				jingleClose.addElement(null, "reason").addElement(null,
//						"success");
//				xmppClient.sendPacket(closeSession);
//				String decString = encodedData.toString();
//				decodedData = Base64.decode(decString);
//				// #mdebug 
//				Logger.log("File received kb: " + decodedData.length);
//				// System.out.println(decString);
//				// #enddebug
//				eh.dataReceived(decodedData, fileName, fileDesc,
//						OpenListener.this);
//			} catch (Exception ex) {
//				// #mdebug
//				ex.printStackTrace();
//				Logger.log("In closing session" + ex.getClass().getName()
//						+ "\n" + ex.getMessage());
//				// #enddebug
//			}
//		}

		private void acceptSession() {

			Element e = this.e_jingle;
			Element session_accept = new Iq(e.getAttribute(Iq.ATT_FROM),
					Iq.T_SET);
			Element jingle = this.e_jingle
					.getChildByName(null, FTSender.JINGLE);
			jingle.setAttribute(XMPPClient.ACTION, FTSender.SESSION_ACCEPT);
			session_accept.addElement(jingle);
			xmppClient.sendPacket(session_accept);
		}
	};

	public FTReceiver(FTREventHandler eh) {
		this.eh = eh;
		EventQuery eq = new EventQuery(Iq.IQ, new String[] { Iq.ATT_TYPE },
				new String[] { Iq.T_SET });
		eq.child = new EventQuery(FTSender.JINGLE, new String[] { "xmlns",
				XMPPClient.ACTION }, new String[] { XMPPClient.JINGLE,
				FTSender.SESSION_INITIATE });
		BasicXmlStream.addEventListener(eq, this);
	}

	public void packetReceived(Element e) {
		// file transfer receive protocol
		OpenListener ftrp = new OpenListener();
		ftrp.e_jingle = e;
		Element fileNode = e.getPath(new String[] { null, null, null, null,
				null }, new String[] { FTSender.JINGLE, FTSender.CONTENT,
				FTSender.DESCRIPTION, FTSender.OFFER, FTSender.FILE });

		ftrp.fileSize = Integer.parseInt(fileNode.getAttribute(FTSender.SIZE));
		ftrp.fileName = fileNode.getAttribute(FTSender.NAME);
		Element desc = fileNode.getChildByName(null, FTSender.DESC);
		if (desc != null) ftrp.fileDesc = desc.getText();
		Stanza reply = Utils.easyReply(e);
		xmppClient.sendPacket(reply);

		EventQuery eq = new EventQuery(Iq.IQ, new String[] { Iq.ATT_FROM,
				Iq.ATT_TYPE }, new String[] { e.getAttribute(Iq.ATT_FROM),
				Iq.T_SET });
		Element transport = e.getPath(new String[] { null, null, null },
				new String[] { FTSender.JINGLE, FTSender.CONTENT,
						FTSender.TRANSPORT });

		int block_size = Integer.parseInt(transport
				.getAttribute(FTSender.BLOCK_SIZE));
		
		ftrp.block_size = block_size;

		eq.child = new EventQuery(XMPPClient.DATA,
				new String[] { FTSender.SID }, new String[] { transport
						.getAttribute(FTSender.SID) });
		EventQueryRegistration eqr = BasicXmlStream.addEventListener(eq, ftrp);
		ftrp.dataListenerEq = eqr;
		
		// file transfer acceptance
		eh.reqFT(e.getAttribute(Iq.ATT_FROM), ftrp);
	}
}
