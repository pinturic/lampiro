/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: MUCScreen.java 1136 2009-01-28 11:25:30Z luca $
*/

package lampiro.screens;

import it.yup.ui.UIButton;
import it.yup.ui.UICombobox;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UIMenu;
import it.yup.ui.UIPanel;
import it.yup.ui.UITextField;
import it.yup.util.ResourceIDs;
import it.yup.xmlstream.Element;
import it.yup.xmlstream.EventQuery;
import it.yup.xmlstream.EventQueryRegistration;
import it.yup.xmlstream.PacketListener;
import it.yup.xmpp.Contact;
import it.yup.xmpp.MUC;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.DataForm;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Message;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.TextField;

import lampiro.screens.RosterScreen.UIContact;

public class MUCScreen extends ChatScreen implements PacketListener {

	private UICombobox rosterCombo;
	private Vector mucCandidates = new Vector();
	private UILabel addUser = new UILabel(rm
			.getString(ResourceIDs.STR_ADD_USER));
	private EventQueryRegistration presenceReg = null;
	private EventQueryRegistration subjectReg = null;
	private Vector mucParticipants = new Vector(5);
	private UILabel cmd_topic = new UILabel(rm
			.getString(ResourceIDs.STR_SET_TOPIC));
	private UITextField topic_name_field = new UITextField("Topic name", "",
			50, TextField.ANY);
	private UIButton topic_button = new UIButton(rm
			.getString(ResourceIDs.STR_SUBMIT));

	public MUCScreen(Contact u) {
		super(u);
		//this.setFreezed(true);
		this.getMenu().append(addUser);
		this.getMenu().append(cmd_topic);
		setTitle(rm.getString(ResourceIDs.STR_GROUP_CHAT) + " "
				+ user.getPrintableName());
		this.rosterCombo = new UICombobox(rm
				.getString(ResourceIDs.STR_ADD_USER), true);
		this.rosterCombo.setSubmenu(this.closeMenu);
		RosterScreen roster = RosterScreen.getInstance();
		UIPanel rosterPanel = roster.rosterPanel;
		Vector orderedContacts = new Vector(rosterPanel.getItems().size());
		for (Enumeration en = rosterPanel.getItems().elements(); en
				.hasMoreElements();) {
			RosterScreen.UIContact item = (RosterScreen.UIContact) en
					.nextElement();
			if (item.c instanceof MUC == false) {
				this.addContact(orderedContacts, item);
			}
		}

		for (Enumeration en = orderedContacts.elements(); en.hasMoreElements();) {
			RosterScreen.UIContact item = (RosterScreen.UIContact) en
					.nextElement();
			if (item.c instanceof MUC == false) {
				String printableName = item.c.getPrintableName();
				this.rosterCombo.append(printableName);
				this.mucCandidates.addElement(item.c);
			}
		}
		this.insert(this.indexOf(chatPanel), rosterCombo);
		this.rosterCombo.setSelected(false);
		if (chatPanel.getItems().size() > 4) {
			// remember the separator
			chatPanel.setSelectedIndex(chatPanel.getItems().size() - 2);
			this.setSelectedIndex(this.indexOf(chatPanel));
		} else {
			// chatPanel.setSelectedIndex(0);
			chatPanel.setSelected(false);
			this.setSelectedIndex(this.indexOf(this.rosterCombo));
		}
		chatPanel.setDirty(true);
		UILabel mucName = (UILabel) header.getItem(0);
		mucName.setText(rm.getString(ResourceIDs.STR_TOPIC) + ": "
				+ ((MUC) this.user).topic);
		//this.setFreezed(false);
		this.askRepaint();

		// registration to handle people left/join
		EventQuery q = new EventQuery("presence", null, null);
		EventQuery x = new EventQuery("x", new String[] { "xmlns" },
				new String[] { XMPPClient.NS_MUC_USER });
		q.child = x;
		EventQuery invite = new EventQuery("item", null, null);
		x.child = invite;
		presenceReg = XMPPClient.getInstance().registerListener(q, this);

	}

	private void addContact(Vector orderedContacts, UIContact item) {
		Enumeration en = orderedContacts.elements();
		int index = 0;
		while (en.hasMoreElements()) {
			if (compareTo((UIContact) en.nextElement(), item)) index++;
		}
		orderedContacts.insertElementAt(item, index);
	}

	public boolean compareTo(UIContact left, UIContact right) {
		return left.c.getPrintableName().toLowerCase().compareTo(
				right.c.getPrintableName().toLowerCase()) < 0;
	}

	public void menuAction(UIMenu menu, UIItem c) {
		if (c == cmd_exit) {
			// for MUCs the conversation is downloaded again at any reconnection
			// conversations.remove(user.jid);
			user.lastResource = null;
			if (presenceReg != null) {
				presenceReg.remove();
				presenceReg = null;
			}
			if (subjectReg != null) {
				subjectReg.remove();
				subjectReg = null;
			}
		} else if (c == topic_button) {
			String topicName = this.topic_name_field.getText();
			XMPPClient client = XMPPClient.getInstance();
			Message msg = new Message(Contact.userhost(this.user.jid),
					"groupchat");
			msg.addElement("", "subject").content = topicName;
			client.sendPacket(msg);
			this.topic_name_field.setText("");
			return;
		} else if (c == cmd_topic) {
			UIMenu topicNameMenu = UIMenu.easyMenu(rm
					.getString(ResourceIDs.STR_CHOOSE_NAME), 10, 20, this.getWidth() - 20, topic_name_field);
			topicNameMenu.append(topic_button);
			topicNameMenu.setDirty(true);
			topicNameMenu.setSelectedIndex(topicNameMenu
					.indexOf(topic_name_field));
			this.addPopup(topicNameMenu);
			return;
		} else if (c == addUser) {
			this.setSelectedIndex(this.indexOf(this.rosterCombo));
			chatPanel.setSelected(false);
			chatPanel.setDirty(true);
			this.rosterCombo.openMenu();
			return;
		}
		super.menuAction(menu, c);
	}

	public void packetReceived(Element e) {
		String userHost = Contact.userhost(e.getAttribute(Iq.ATT_FROM));
		if (e.name.compareTo("presence") == 0
				&& userHost.compareTo(this.user.jid) == 0) {
			String type = e.getAttribute("type");
			String jidName = Contact.resource(e.getAttribute(Message.ATT_FROM));
			handlePresence(jidName, type);
			return;
		}

		super.packetReceived(e);
	}

	private void handlePresence(String jidName, String type) {
		Message msg = null;
		msg = new Message(this.user.jid, "chat");
		msg.setAttribute(Message.ATT_FROM, null);
		String msgText = "";
		boolean send = false;
		String endString = " "
				+ rm.getString(ResourceIDs.STR_GROUP_CHAT).toLowerCase() + ".";
		if (type == null && mucParticipants.contains(jidName) == false) {
			send = true;
			mucParticipants.addElement(jidName);
			msgText = jidName + " " + rm.getString(ResourceIDs.STR_JOINED_MUC)
					+ endString;
		} else if (type != null && type.compareTo("unavailable") == 0) {
			send = true;
			msgText = jidName + " " + rm.getString(ResourceIDs.STR_LEFT_MUC)
					+ endString;
			mucParticipants.removeElement(jidName);
		}

		if (send == true) {
			msg.setBody(msgText);
			user.lastResource = null;
			user.addMessageToHistory(msg);
			this.askRepaint();
		}
	}

	private void sendInvite(Contact ithContact) {
		Message msg = new Message(user.jid, null);
		Element x = new Element(XMPPClient.NS_MUC_USER, DataForm.X);
		msg.children.addElement(x);
		Element invite = new Element("", "invite");
		invite.setAttribute(Message.ATT_TO, ithContact.getFullJid());
		x.children.addElement(invite);
		XMPPClient.getInstance().sendPacket(msg);

	}
	
	protected void  getPrintableHeight(Graphics g, int h) {
		super.getPrintableHeight(g,h);
		if (rosterCombo == null) {
			// this method could be called without rosterCombo 
			// being initialized.
			this.rosterCombo = new UICombobox(rm
					.getString(ResourceIDs.STR_ADD_USER), true);
		}
		this.printableHeight -= this.rosterCombo.getHeight(g);
	}

	ConversationEntry wrapMessage(String text[]) {

		// #ifdef TIMING
		// @ long t1 = System.currentTimeMillis();
		// #endif

		byte type = (text[2] != null && text[2].equals(Contact.user(text[0]))) ? ConversationEntry.ENTRY_TO
				: ConversationEntry.ENTRY_FROM;

		// #ifdef TIMING
		// @ System.out.println("wrap conv: " + (System.currentTimeMillis() -
		// @ // t1));
		// #endif

		String labelText = "";
		labelText += text[1];
		ConversationEntry convEntry = new ConversationEntry(labelText, type);
		if (type == ConversationEntry.ENTRY_FROM && text[2] != null) convEntry.from = text[2];
		if (text[3] != null) convEntry.arriveTime = text[3];

		return convEntry;
	}

	public void itemAction(UIItem c) {
		if (c == this.rosterCombo) {
			RosterScreen roster = RosterScreen.getInstance();
			UIPanel rosterPanel = roster.rosterPanel;
			int[] selectedIndeces = this.rosterCombo.getSelectedIndeces();
			for (int i = 0; i < selectedIndeces.length; i++) {
				for (Enumeration en = rosterPanel.getItems().elements(); en
						.hasMoreElements();) {
					RosterScreen.UIContact item = (RosterScreen.UIContact) en
							.nextElement();
					Contact ithContact = ((Contact) this.mucCandidates
							.elementAt(selectedIndeces[i]));
					if (item.c.getPrintableName().compareTo(
							ithContact.getPrintableName()) == 0) {
						this.sendInvite(ithContact);
					}
				}
			}
			this.setSelectedIndex(this.indexOf(this.chatPanel));
			boolean flags[] = new boolean[this.mucCandidates.size()];
			for (int i = 0; i < flags.length; i++)
				flags[i] = false;
			this.rosterCombo.setSelectedFlags(flags);
			this.chatPanel
					.setSelectedIndex(this.chatPanel.getItems().size() - 2);
			this.askRepaint();
			return;
		}
		super.itemAction(c);
	}

	String getLabelHeader(ConversationEntry entry) {
		String retString = "";
		int fromLength = entry.from.length();
		int arriveTimeLength = entry.arriveTime.length();
		if (arriveTimeLength > 0 || fromLength > 0) {
			retString = "[";
			if (fromLength > 0) retString += entry.from;
			if (fromLength > 0 && arriveTimeLength > 0) retString += " ";
			if (arriveTimeLength > 0) retString += entry.arriveTime;
			retString += "] ";
		}
		return retString;
	}

}
