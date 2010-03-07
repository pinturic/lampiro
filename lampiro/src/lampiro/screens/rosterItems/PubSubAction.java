/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: PubSubAction.java 1858 2009-10-16 22:42:29Z luca $
*/
package lampiro.screens.rosterItems;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import lampiro.screens.AlbumScreen;
import lampiro.screens.ContactInfoScreen;
import lampiro.screens.HandleMucScreen;
import lampiro.screens.MessageComposerScreen;
import lampiro.screens.RosterScreen;

import it.yup.ui.UICanvas;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.util.Utils;
import it.yup.xml.Element;
import it.yup.xmpp.CommandExecutor;
import it.yup.xmpp.Contact;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Presence;

public class PubSubAction {

	public static final int COMMAND = 0;
	public static final int ROSTER = 1;
	public static final int SEND_FILE = 2;
	public static final int SEND_MESSAGE = 3;
	public static final int JOIN_MUC = 4;
	public static final int VCARD = 5;
	public static final int PUBSUB = 6;

	private static ResourceManager rm = ResourceManager.getManager();

	public static PubSubAction parse(String text) {
		Vector header = Utils.tokenize(text, ':');
		if ("xmpp".equals(header.elementAt(0)) == false) return null;
		Vector uri = Utils.tokenize((String) header.elementAt(1), '?');
		String jid = (String) uri.elementAt(0);
		Vector pars = Utils.tokenize((String) uri.elementAt(1), ';');

		String actionTypeStr = (String) pars.elementAt(0);
		int actionType = 0;
		if (actionTypeStr.equals(XMPPClient.COMMAND)) {
			actionType = PubSubAction.COMMAND;
		} else if (actionTypeStr.equals("roster")) {
			actionType = PubSubAction.ROSTER;
		} else if (actionTypeStr.equals("sendfile")) {
			actionType = PubSubAction.SEND_FILE;
		} else if (actionTypeStr.equals("message")) {
			actionType = PubSubAction.SEND_MESSAGE;
		} else if (actionTypeStr.equals("join")) {
			actionType = PubSubAction.JOIN_MUC;
		} else if (actionTypeStr.equals("vcard")) {
			actionType = PubSubAction.VCARD;
		} else if (actionTypeStr.equals(XMPPClient.PUBSUB)) {
			actionType = PubSubAction.PUBSUB;
		}

		pars.removeElementAt(0);

		Hashtable parHash = new Hashtable();
		Enumeration en = pars.elements();
		while (en.hasMoreElements()) {
			String object = (String) en.nextElement();
			Vector couple = Utils.tokenize(object, '=');
			parHash.put(couple.elementAt(0), couple.elementAt(1));
		}

		return new PubSubAction(jid, actionType, parHash);
	}

	String jid;
	int action;
	Hashtable parameters;

	private PubSubAction(String jid, int actionType, Hashtable parHash) {
		this.jid = jid;
		this.action = actionType;
		this.parameters = parHash;
	}

	public String get(String par) {
		return (String) this.parameters.get(par);
	}

	public void execute() {
		switch (this.action) {
			case PubSubAction.COMMAND:

				CommandExecutor cmdEx = null;
				String[] selCmd = new String[] { this.get(XMPPClient.NODE) };

				cmdEx = new CommandExecutor(selCmd, this.jid, null);
				RosterScreen.getInstance()._handleTask(cmdEx);
				cmdEx.setupCommand();

				break;

			case PubSubAction.JOIN_MUC:
				HandleMucScreen cms = new HandleMucScreen(null, Contact
						.domain(this.jid),
						HandleMucScreen.HMC_CONSTANTS.JOIN_NOW);
				cms.infoLabel.setText(rm
						.getString(ResourceIDs.STR_GROUP_CHAT_INVITATION));
						
//						+ " "
//						+ XMPPClient.getInstance().getRoster().getContactByJid(
//								UIUIDReader.UID_JID) + "?");
				cms.muc_name_field.setText(Contact.user(jid));
				cms.menuAction(null, cms.cmd_save);
				break;

			case PubSubAction.SEND_FILE:
				UICanvas.getInstance().open(AlbumScreen.getInstance(this.jid),
						true, UICanvas.getInstance().getCurrentScreen());
				break;

			case PubSubAction.SEND_MESSAGE:
				Contact user = XMPPClient.getInstance().getRoster()
						.getContactByJid(jid);
				String fullJid = user.jid;
				Object subject = parameters.get("subject");
				Object body = parameters.get("body");
				MessageComposerScreen ms = new MessageComposerScreen(user,
						fullJid, MessageComposerScreen.MESSAGE);
				if (subject != null) ms.tf_subject.setText((String) subject);
				if (subject != null) ms.tf_body.setText((String) body);
				UICanvas.getInstance().open(ms, true,
						UICanvas.getInstance().getCurrentScreen());
				break;

			case PubSubAction.ROSTER:
				Contact c = XMPPClient.getInstance().getRoster()
						.getContactByJid(jid);
				if (c != null && Contact.SUB_BOTH.equals(c.subscription)) {
					return;
				} else if (c == null) {
					c = new Contact(jid, null, null, null);
				}
				XMPPClient.getInstance().getRoster().subscribeContact(c, false);
				break;

			case PubSubAction.VCARD:
				ContactInfoScreen ci = new ContactInfoScreen(XMPPClient
						.getInstance().getRoster().getContactByJid(this.jid));
				UICanvas.getInstance().open(ci, true);
				break;

			case PubSubAction.PUBSUB:
				Iq iq = new Iq(this.jid, Iq.T_SET);
				Element subscribe = iq.addElement(XMPPClient.NS_PUBSUB,
						XMPPClient.PUBSUB).addElement(null,
						Presence.T_SUBSCRIBE);
				subscribe.setAttribute(XMPPClient.NODE, this
						.get(XMPPClient.NODE));
				subscribe.setAttribute("jid", Contact.userhost(XMPPClient
						.getInstance().my_jid));
				XMPPClient.getInstance().sendIQ(iq, null);
				break;
		}
	}

}
