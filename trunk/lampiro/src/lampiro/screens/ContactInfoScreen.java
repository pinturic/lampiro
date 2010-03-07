/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: ContactInfoScreen.java 2002 2010-03-06 19:02:12Z luca $
*/

/**
 * 
 */
package lampiro.screens;

import it.yup.ui.UICanvas;
import it.yup.ui.UIConfig;
import it.yup.ui.UIHLayout;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UILayout;
import it.yup.ui.UIMenu;
import it.yup.ui.UIPanel;
import it.yup.ui.UIScreen;
import it.yup.ui.UISeparator;
import it.yup.ui.UIUtils;
import it.yup.ui.UIVLayout;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.xml.Element;
import it.yup.xmpp.Contact;
import it.yup.xmpp.IQResultListener;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Presence;
import it.yup.xmpp.packets.Stanza;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import org.bouncycastle.util.encoders.UrlBase64;

/**
 * @author luca
 * 
 */
public class ContactInfoScreen extends UIScreen {

	private static ResourceManager rm = ResourceManager.getManager();

	private UILabel close = new UILabel(rm.getString(ResourceIDs.STR_CLOSE)
			.toUpperCase());

	private UIPanel contactPanel;

	private UILabel imgLabel = null;

	private UILabel loadingLabel = null;

	private UIVLayout fnLayout = null;
	private UIVLayout nnLayout = null;
	private UIVLayout eeLayout = null;
	private UIVLayout jidLayout = null;
	private UIVLayout nickLayout = null;
	private UIVLayout subLayout = null;

	private Font gFont = null;

	private boolean changeColor = false;

	private int diffColor = UIUtils.colorize(UIConfig.bg_color, -10);

	private String unavailableString = "<"
			+ rm.getString(ResourceIDs.STR_UNAVAILABLE) + ">";

	private UILabel fnLabel = new UILabel(unavailableString);

	private UILabel nnLabel = new UILabel(unavailableString);

	private UILabel eeLabel = new UILabel(unavailableString);

	/**
	 * 
	 */
	public ContactInfoScreen(Contact contact) {
		super();

		Font xFont = UIConfig.font_body;
		gFont = Font.getFont(xFont.getFace(), Font.STYLE_BOLD, xFont.getSize());

		this.setTitle(contact.getPrintableName());

		setMenu(new UIMenu(""));
		this.getMenu().append(close);
		this.setFreezed(true);

		contactPanel = new UIPanel();
		contactPanel.setMaxHeight(-1);
		contactPanel.setModal(true);
		contactPanel.setFocusable(true);
		this.append(contactPanel);

		loadingLabel = new UILabel(rm.getString(ResourceIDs.STR_LOADING));
		loadingLabel.setAnchorPoint(Graphics.HCENTER);
		addToPanel(loadingLabel);
		imgLabel = new UILabel(UICanvas.getUIImage("/icons/empty_avatar.png"));
		imgLabel.setFocusable(true);
		imgLabel.setAnchorPoint(Graphics.HCENTER);
		imgLabel.setSelectedColor(0xFFFFFF);
		addToPanel(imgLabel);
		imgLabel.setBg_color(0xFFFFFF);
		UISeparator sep = new UISeparator(1);
		sep.setFg_color(0xAAAAAA);
		contactPanel.addItem(sep);

		UILabel fullName = new UILabel(rm.getString(ResourceIDs.STR_NAME) + ":");
		fnLayout = contactLayout(fullName, fnLabel);
		this.contactPanel.addItem(fnLayout);

		UILabel nickName = new UILabel(rm.getString(ResourceIDs.STR_NICK_NAME)
				+ ":");
		nnLayout = contactLayout(nickName, nnLabel);
		this.contactPanel.addItem(nnLayout);

		UILabel email = new UILabel(rm.getString(ResourceIDs.STR_E_MAIL) + ":");
		eeLayout = contactLayout(email, eeLabel);
		this.contactPanel.addItem(eeLayout);

		UILabel JID = new UILabel(rm.getString(ResourceIDs.STR_JID) + ":");
		jidLayout = contactLayout(JID, new UILabel(contact.jid));
		this.contactPanel.addItem(jidLayout);

		if (contact.name != null && contact.name.length() > 0) {
			UILabel nic = new UILabel(rm
					.getString(ResourceIDs.STR_DISPLAYED_NAME));
			nickLayout = contactLayout(nic, new UILabel(contact.name));
			this.contactPanel.addItem(nickLayout);
		}

		UILabel subscription = new UILabel(rm
				.getString(ResourceIDs.STR_SUBSCRIPTION)
				+ ":");
		int subText = 0;
		if (contact.subscription.equals(Contact.SUB_BOTH)) {
			subText = ResourceIDs.STR_SUB_BOTH;
		} else if (contact.subscription.equals(Contact.SUB_TO)) {
			subText = ResourceIDs.STR_SUB_TO;
		} else if (contact.subscription.equals(Contact.SUB_FROM)) {
			subText = ResourceIDs.STR_SUB_FROM;
		} else if (contact.subscription.equals(Contact.SUB_NONE)) {
			subText = ResourceIDs.STR_SUB_NONE;
		} else {
			subText = ResourceIDs.STR_SUB_UNKNOWN;
		}
		subLayout = contactLayout(subscription, new UILabel(rm
				.getString(subText)));
		this.contactPanel.addItem(subLayout);

		Presence[] resources = contact.getAllPresences();
		if (resources != null) {
			sep = new UISeparator(1);
			sep.setFg_color(0xAAAAAA);
			contactPanel.addItem(sep);
			UILabel si_rres = new UILabel(rm
					.getString(ResourceIDs.STR_RESOURCES)
					+ ":");
			si_rres.setAnchorPoint(Graphics.HCENTER);
			addToPanel(si_rres);
			for (int i = 0; i < resources.length; i++) {
				String status = resources[i].getStatus();
				String jid = resources[i].getAttribute(Stanza.ATT_FROM);
				Image presenceIcon = XMPPClient.getInstance().getPresenceIcon(
						contact, jid, contact.getAvailability(jid));
				UIVLayout resVl = null;
				if (status != null && status.length() > 0) resVl = new UIVLayout(
						2, 120);
				else
					resVl = new UIVLayout(1, 120);
				resVl.setFocusable(true);

				Graphics g = UICanvas.getInstance().getCurrentScreen()
						.getGraphics();
				String resource = Contact.resource(jid);
				resource = resource != null ? resource : "";
				UILabel ii_res = new UILabel(presenceIcon, resource);
				ii_res.setWrappable(true, this.getWidth() - 30);
				resVl.insert(ii_res, 0, ii_res.getHeight(g),
						UILayout.CONSTRAINT_PIXELS);
				int statusHeight = 0;
				if (status != null && status.length() > 0) {
					UIHLayout uhl = new UIHLayout(2);
					// 1 pixel for the img alignement in the Label
					uhl.insert(new UISeparator(0), 0,
							presenceIcon.getWidth() + 1,
							UILayout.CONSTRAINT_PIXELS);
					status = resources[i].getStatus();
					UILabel si_status = new UILabel(status);
					si_status.setWrappable(true, this.getWidth() - 30);
					statusHeight = si_status.getHeight(g);
					uhl.insert(si_status, 1, 100,
							UILayout.CONSTRAINT_PERCENTUAL);
					resVl.insert(uhl, 1, statusHeight,
							UILayout.CONSTRAINT_PIXELS);
				}
				resVl.setHeight(ii_res.getHeight(g) + sep.getHeight(g)
						+ statusHeight);
				addToPanel(resVl);
			}
		}
		this.setFreezed(false);

		// ask avatars
		Iq iq = new Iq(contact.jid, Iq.T_GET);
		iq.addElement(XMPPClient.VCARD_TEMP, XMPPClient.VCARD);
		XMPPClient.getInstance().sendIQ(iq, new IQResultListener() {

			public void handleError(Element e) {
				// TODO Auto-generated method stub

			}

			public void handleResult(Element e) {
				UICanvas.lock();
				try {
					ContactInfoScreen ci = ContactInfoScreen.this;
					ci.contactPanel.removeItem(loadingLabel);

					Element vCard = e.getChildByName(null, XMPPClient.VCARD);
					if (vCard == null) {
						ci.askRepaint();
						return;
					}

					// full name 
					Element FN = vCard.getChildByName(null, XMPPClient.FN);
					updateContactLayout(fnLayout, FN);

					// nickName
					Element NICK = vCard.getChildByName(null,
							XMPPClient.NICKNAME);
					updateContactLayout(nnLayout, NICK);

					// email
					Element EMAIL = vCard
							.getChildByName(null, XMPPClient.EMAIL);
					if (EMAIL != null) updateContactLayout(eeLayout, EMAIL
							.getChildByName(null, XMPPClient.USERID));

					// photo

					Element BINVAL = vCard
							.getPath(new String[] { null, null }, new String[] {
									XMPPClient.PHOTO, XMPPClient.BINVAL });
					if (BINVAL != null) {
						String vCardString = BINVAL.getText();
						byte[] vCardBytes = UrlBase64.decode(vCardString
								.getBytes());
						Image img = Image.createImage(vCardBytes, 0,
								vCardBytes.length);
						ContactInfoScreen.this.imgLabel.setImg(img);
					}
					ci.askRepaint();
				} catch (Exception ex) {
					// #mdebug
//@										System.out.println(ex.getMessage());
//@										ex.printStackTrace();
					// #enddebug
				} finally {
					UICanvas.unlock();
				}
			}

		});

	}

	private void updateContactLayout(UIVLayout layout, Element el) {
		String content = "";
		if (el != null && el.getText().length() > 0) content = el.getText();
		else
			content = unavailableString;
		((UILabel) layout.getItem(1)).setText(content);
		layout.setHeight(layout.getItem(0).getHeight(this.getGraphics())
				+ layout.getItem(1).getHeight(this.getGraphics()));

	}

	public UIVLayout contactLayout(UILabel typeItem, UILabel valItem) {
		Graphics g = UICanvas.getInstance().getCurrentScreen().getGraphics();
		UIVLayout conLayout = new UIVLayout(2, typeItem.getHeight(g)
				+ valItem.getHeight(g));
		int tempWidth = (UICanvas.getInstance().getCurrentScreen().getWidth() - 10);
		typeItem.setAnchorPoint(Graphics.HCENTER);
		typeItem.setWrappable(true, tempWidth);
		typeItem.setFocusable(false);
		this.setColor(typeItem);
		valItem.setFont(gFont);
		valItem.setAnchorPoint(Graphics.HCENTER);
		valItem.setWrappable(true, tempWidth);
		this.setColor(typeItem);
		conLayout.setGroup(true);
		conLayout.setFocusable(true);
		conLayout.insert(typeItem, 0, typeItem.getHeight(g),
				UILayout.CONSTRAINT_PIXELS);
		conLayout.insert(valItem, 1, valItem.getHeight(g),
				UILayout.CONSTRAINT_PIXELS);
		conLayout.setHeight(typeItem.getHeight(g) + valItem.getHeight(g));
		return conLayout;
	}

	private void addToPanel(UIItem item) {
		this.contactPanel.addItem(item);
		setColor(item);
	}

	private void setColor(UIItem item) {
		if (changeColor) item.setBg_color(diffColor);
		changeColor = !changeColor;
	}

	public void menuAction(UIMenu menu, UIItem cmd) {
		if (cmd == this.close) {
			UICanvas.getInstance().close(this);
		}
	}

}
