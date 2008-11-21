/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: ContactInfoScreen.java 924 2008-10-25 16:10:40Z luca $
*/

/**
 * 
 */
package lampiro.screens;

import it.yup.ui.UICanvas;
import it.yup.ui.UIHLayout;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UILayout;
import it.yup.ui.UIMenu;
import it.yup.ui.UIPanel;
import it.yup.ui.UIScreen;
import it.yup.ui.UISeparator;
import it.yup.ui.UITextField;
import it.yup.ui.UIVLayout;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.xmpp.Contact;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.Presence;
import it.yup.xmpp.packets.Stanza;

import javax.microedition.lcdui.TextField;

/**
 * @author luca
 * 
 */
public class ContactInfoScreen extends UIScreen {

	private static ResourceManager rm = ResourceManager.getManager("common",
			"en");

	private UILabel close = new UILabel(rm.getString(ResourceIDs.STR_CLOSE)
			.toUpperCase());

	/**
	 * 
	 */
	public ContactInfoScreen(Contact contact) {
		super();

		setMenu(new UIMenu(""));
		this.getMenu().append(close);
		this.setFreezed(true);

		UIPanel contactPanel = new UIPanel();
		contactPanel.setMaxHeight(-1);
		this.append(contactPanel);

		UITextField JID = new UITextField("JID", contact.jid, 50,
				TextField.UNEDITABLE);
		JID.setWrappable(true);
		contactPanel.addItem(JID);

		if (contact.name != null && contact.name.length() > 0) {
			UITextField tf_nick = new UITextField("Nick", contact.name, 50,
					TextField.UNEDITABLE);
			tf_nick.setWrappable(true);
			contactPanel.addItem(tf_nick);
		}

		UITextField tf_sub = new UITextField("Subscription",
				contact.subscription, 50, TextField.UNEDITABLE);
		contactPanel.addItem(tf_sub);

		UILabel si_rres = new UILabel("Resources");
		contactPanel.addItem(si_rres);
		contactPanel.setFocusable(true);

		Presence[] resources = contact.getAllPresences();
		for (int i = 0; resources != null && i < resources.length; i++) {

			String jid = resources[i].getAttribute(Stanza.ATT_FROM);
			UIHLayout labelLayout = new UIHLayout(2);
			labelLayout.setGroup(false);

			UILabel ii_img = new UILabel(XMPPClient.getInstance()
					.getPresenceIcon(contact.getAvailability(jid)));
			UIVLayout resVl = new UIVLayout(3, 120);
			resVl.setGroup(false);

			UILabel ii_res = new UILabel(jid);
			ii_res.setWrappable(true, this.getWidth());
			ii_res.setFocusable(true);
			resVl.insert(ii_res, 0, 50, UILayout.CONSTRAINT_PERCENTUAL);
			labelLayout.insert(ii_img, 0, 20, UILayout.CONSTRAINT_PIXELS);
			UISeparator sep = new UISeparator(2);
			sep.setBg_color(0xCCCCCC);
			resVl.insert(sep, 1, 1, UILayout.CONSTRAINT_PIXELS);
			String status = resources[i].getStatus();
			if (status != null && status.length() > 0) {
				UILabel si_status = new UILabel(status);
				resVl.insert(si_status, 2, 50, UILayout.CONSTRAINT_PERCENTUAL);
				si_status.setWrappable(true, this.getWidth());
				si_status.setFocusable(true);
			} else {
				resVl.insert(new UILabel(""), 2, 50,
						UILayout.CONSTRAINT_PERCENTUAL);
			}
			labelLayout.insert(resVl, 1, 100, UILayout.CONSTRAINT_PERCENTUAL);
			contactPanel.addItem(labelLayout);
		}
		this.setFreezed(false);
	}

	public void menuAction(UIMenu menu, UIItem cmd) {
		if (cmd == this.close) {
			UICanvas.getInstance().close(this);
		}
	}

}
