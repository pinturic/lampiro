/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: SubscriptionConfirmScreen.java 873 2008-09-28 15:24:27Z luca $
*/

package lampiro.screens;

import it.yup.ui.UIButton;
import it.yup.ui.UICanvas;
import it.yup.ui.UIHLayout;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UILayout;
import it.yup.ui.UIPanel;
import it.yup.ui.UIScreen;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.xmpp.Contact;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.Presence;
import it.yup.xmpp.packets.Stanza;

public class SubscriptionConfirmScreen extends UIScreen {

	private static ResourceManager rm = ResourceManager.getManager("common",
			"en");
	private UIButton cmd_yes = new UIButton(rm.getString(ResourceIDs.STR_YES));
	private UIButton cmd_no = new UIButton(rm.getString(ResourceIDs.STR_NO));
	private Contact contact;

	public SubscriptionConfirmScreen(Contact contact) {
		super();
		this.setFreezed(true);
		this.setTitle(rm.getString(ResourceIDs.STR_SUBSCRIPTION_CONFIRM));
		this.contact = contact;
		UIPanel requestPanel = new UIPanel();
		requestPanel.setMaxHeight(-1);
		this.append(requestPanel);
		UILabel instr = new UILabel(rm
				.getString(ResourceIDs.STR_SUBSCRIPTION_REQUEST_FROM)
				+ " " + contact.jid + ". ");
		instr.setWrappable(true, this.getWidth() - 10);
		requestPanel.addItem(instr);
		instr = new UILabel(rm.getString(ResourceIDs.STR_SUBSCRIPTION_ACCEPT));
		requestPanel.addItem(instr);
		UIHLayout uhl = new UIHLayout(2);
		uhl.setGroup(false);
		uhl.insert(cmd_yes, 0, 50, UILayout.CONSTRAINT_PERCENTUAL);
		uhl.insert(cmd_no, 1, 50, UILayout.CONSTRAINT_PERCENTUAL);
		requestPanel.addItem(uhl);
		this.setSelectedIndex(this.indexOf(requestPanel));
		requestPanel.setSelected(true);
		requestPanel.setSelectedIndex(requestPanel.getItems().indexOf(uhl));
		this.setFreezed(false);
		this.askRepaint();
	}

	public void itemAction(UIItem c) {
		XMPPClient client = XMPPClient.getInstance();
		Presence pmsg = new Presence();
		pmsg.setAttribute(Stanza.ATT_TO, contact.jid);
		if (c == cmd_yes) {
			Contact cont = client.roster.getContactByJid(contact.jid);
			if (cont == null) {
				cont = new Contact(contact.jid, contact.name, "from",
						contact.groups);
				// xmpp.addContact(c);
			}
			pmsg.setAttribute(Stanza.ATT_TYPE, Presence.T_SUBSCRIBED);
			client.sendPacket(pmsg);
			pmsg.setAttribute(Stanza.ATT_TYPE, Presence.T_SUBSCRIBE);
			client.sendPacket(pmsg);
		} else if (c == cmd_no) {
			pmsg.setAttribute(Stanza.ATT_TYPE, Presence.T_UNSUBSCRIBED);
			client.sendPacket(pmsg);
		}

		UICanvas.getInstance().close(this);
	}

}
