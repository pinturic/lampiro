/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: VCardManager.java 1858 2009-10-16 22:42:29Z luca $
*/
package lampiro.screens.rosterItems;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import it.yup.ui.UIAccordion;
import it.yup.ui.UICanvas;
import it.yup.ui.UIConfig;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UIMenu;
import it.yup.ui.UIScreen;
import it.yup.ui.UIUtils;
import it.yup.util.ResourceIDs;
import it.yup.xmpp.Config;
import it.yup.xmpp.Contact;
import it.yup.xmpp.MUC;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Message;
import it.yup.xmpp.packets.Presence;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import lampiro.screens.AlbumScreen;
import lampiro.screens.RosterScreen;

public class UIContact extends UIRosterItem {

	public static Image img_msg = UICanvas.getUIImage("/icons/message.png");
	public static Image img_cmd = UICanvas.getUIImage("/icons/gear.png");
	public static Image img_task = UICanvas.getUIImage("/icons/task.png");

	private static Image subscriptionTo = UIMenu.menuImage;
	private static Image subscriptionFrom;

	public static UILabel cmd_details = new UILabel(rm
			.getString(ResourceIDs.STR_SEE_DETAILS));
	public static UILabel cmd_resend_auth = new UILabel(rm
			.getString(ResourceIDs.STR_RESEND_AUTH));
	public static UILabel cmd_rerequest_auth = new UILabel(rm
			.getString(ResourceIDs.STR_REREQUEST_AUTH));
	public static UILabel cmd_groups = new UILabel(rm
			.getString(ResourceIDs.STR_HANDLE_GROUPS));
	public static UILabel cmd_delc = new UILabel(rm
			.getString(ResourceIDs.STR_DELETE_CONTACT));
	public static UILabel cmd_send = new UILabel(rm
			.getString(ResourceIDs.STR_SEND_MESSAGE));
	public static UILabel cmd_chat = new UILabel(rm
			.getString(ResourceIDs.STR_CHAT));
	public static UILabel cmd_send_file = new UILabel(rm
			.getString(ResourceIDs.STR_SEND_FILE));
	public static UILabel cmd_contact_capture_img = new UILabel(rm
			.getString(ResourceIDs.STR_SEND_IMAGE));
	public static UILabel cmd_contact_capture_aud = new UILabel(rm
			.getString(ResourceIDs.STR_SEND_AUDIO));
	public static UILabel cmd_querycmd = new UILabel(rm
			.getString(ResourceIDs.STR_QUERYCMD));
	public static UILabel cmd_tasks = new UILabel(rm
			.getString(ResourceIDs.STR_PENDINGTASK));
	public static UILabel cmd_close_muc = new UILabel(rm
			.getString(ResourceIDs.STR_CLOSE_MUC));
	public static UILabel cmd_manage_muc = new UILabel(rm
			.getString(ResourceIDs.STR_MANAGE_GC));
	public static UILabel cmd_exit_muc = new UILabel(rm
			.getString(ResourceIDs.STR_EXIT_MUC));
	public static UILabel cmd_enter_muc = new UILabel(rm
			.getString(ResourceIDs.STR_ENTER_MUC));
	public static UILabel cmd_active_sessions = new UILabel(rm
			.getString(ResourceIDs.STR_ACTIVE_SESSIONS)
			+ ":");
	public static UILabel cmd_change_nick = new UILabel(rm
			.getString(ResourceIDs.STR_CHANGE_NICK));

	public static Image jabberImg = UICanvas
			.getUIImage("/transport/jabber.png");

	public Contact c;

	/*
	 * The text pertaining the status of the user
	 */
	private UILabel statusText = new UILabel("");

	public static int textLabelSelectedColor = UIUtils.colorize(
			UIConfig.bg_color, -20);
	public static int textLabelFontColor = 0x000000;

	/*
	 * The label showing subscriptions 
	 */
	//	private UILabel subLabel = null;
	static {
		try {
			subscriptionFrom = Image.createImage("/icons/menuarrow.png");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public UIContact(Contact c) {
		super();
		this.c = c;
		statusText.setFont(Font.getFont(Font.FACE_PROPORTIONAL,
				Font.STYLE_PLAIN, Font.SIZE_SMALL));
		statusText.setFg_color(0xAAAAAA);
		this.updateContactData();
		//checkSubscription();
	}

	//	public void checkSubscription() {
	//		if (c.subscription.equals(Iq.ATT_FROM)) subLabel = new UILabel(
	//				subscriptionFrom);
	//		else if (c.subscription.equals(Iq.ATT_TO)) subLabel = new UILabel(
	//				subscriptionTo);
	//		else
	//			subLabel = null;
	//	}

	public boolean updateContactData() {
		boolean needRepaint = false;
		String uname = c.getPrintableName();
		Image pimg = null;

		pimg = getPresenceIcon();
		if (pimg == null) pimg = xmppClient.getPresenceIcon(null, null,
				Contact.AV_UNAVAILABLE);
		// setup the status text label
		if (contactLabel.getText().equals(uname) == false) {
			needRepaint = true;
			this.contactLabel.setText(uname);
		}
		String fixedStatus = "";
		if (this.c instanceof MUC == false) {
			String status = null;
			Presence[] resources = c.getAllPresences();
			if (resources != null && resources.length > 0) {
				status = resources[0].getStatus();
			}
			fixedStatus = status != null ? status : "";
		} else {
			fixedStatus = ((MUC) c).topic;
		}
		if (statusText.getText().equals(fixedStatus) == false) {
			needRepaint = true;
			this.statusText.setText(fixedStatus);
		}

		// if a message has arrived the icons replaces the normal ones
		if (c.unread_msg()) pimg = img_msg;

		if (this.statusLabel.getImg() != pimg) {
			needRepaint = true;
			this.statusLabel.setImg(pimg);
			statusLabel.setLayoutWidth(pimg.getWidth());
		}
		chooseInfoImgs();
		this.setDirty(needRepaint);
		return needRepaint;
	}

	protected Image getPresenceIcon() {
		return xmppClient.getPresenceIcon(c, null, c.getAvailability());
	}

	public int getHeight(Graphics g) {
		this.height = super.getHeight(g);
		// a minimum width in case it is 0 (and hence not painted yet)
		int minWidth = RosterScreen.getInstance().getWidth() - 25;
		if (this.isSelected() && minWidth > 25) this.statusText.setWrappable(
				true, minWidth);
		else
			this.statusText.setWrappable(false, -1);
		if (this.statusText.getText().length() > 0) this.height += statusText
				.getHeight(g);
		if (this.isSelected()) {
			if (infoImgs.size() > 0) {
				int infoHeight = 0;
				Enumeration en = this.infoImgs.elements();
				while (en.hasMoreElements()) {
					Image object = (Image) en.nextElement();
					if (object.getHeight() > infoHeight) infoHeight = object
							.getHeight();
				}
				this.height += infoHeight;
			}

		}
		return this.height;
	}

	private void chooseInfoImgs() {
		infoImgs.removeAllElements();

		//subscriptions
		if (c.subscription.equals(Iq.ATT_FROM)) {
			infoImgs.addElement(subscriptionFrom);
		} else if (c.subscription.equals(Iq.ATT_TO)) {
			infoImgs.addElement(subscriptionTo);
		}

		// contact type
		Hashtable gws = RosterScreen.getInstance().getGateways();
		Enumeration en = gws.keys();
		boolean found = false;
		while (en.hasMoreElements()) {
			String from = (String) en.nextElement();
			if (c.jid.indexOf(from) > 0) {
				Object[] data = (Object[]) gws.get(from);
				infoImgs.addElement((Image) UIGateway
						.getGatewayIcons((String) data[1]));
				found = true;
				break;
			}
		}
		if (found == false) {
			infoImgs.addElement(jabberImg);
		}

		Presence[] aps = c.getAllPresences();
		if (aps != null && aps.length > 0 && c.supportsMUC(aps[0])) {
			infoImgs.addElement(xmppClient.getPresenceIcon(null, null,
					Contact.MUC_IMG));
		}

		// command
		if (c.pending_tasks) infoImgs.addElement(img_task);
		if (c.cmdlist != null) infoImgs.addElement(img_cmd);
	}

	private Vector infoImgs = new Vector();

	protected void paint(Graphics g, int w, int h) {
		int yoffset = super.getHeight(g);
		int statusLabelWidth = statusLabel.getImg().getWidth();
		super.paint(g, w, h);
		// an additional offset if the subscription has been painted
		g.translate(0, yoffset);
		int orX = g.getTranslateX();
		int orY = g.getTranslateY();

		int infoHeight = 0;
		if (this.isSelected() && infoImgs.size() > 0) {
			Enumeration en = this.infoImgs.elements();
			while (en.hasMoreElements()) {
				Image object = (Image) en.nextElement();
				if (object.getHeight() > infoHeight) infoHeight = object
						.getHeight();
			}
			g.setColor(textLabelSelectedColor);
			g.fillRect(0, 0, w, infoHeight);
			g.translate(statusLabelWidth, 0);
			g.setColor(getBg_color() >= 0 ? getBg_color() : UIConfig.bg_color);
			en = infoImgs.elements();
			while (en.hasMoreElements()) {
				Image ithImg = (Image) en.nextElement();
				paintIthImage(g, ithImg);
			}
		}

		g.translate(orX - g.getTranslateX() + statusLabelWidth, orY
				- g.getTranslateY() + infoHeight);

		if (this.statusText.getText().length() > 0) {
			int statusTextHeight = statusText.getHeight(g);
			int oldStatusColor = statusText.getBg_color();
			int oldStatusFontColor = statusText.getFg_color();
			if (isSelected()) {
				int oldColor = g.getColor();
				g.setColor(textLabelSelectedColor);
				g.fillRect(-statusLabelWidth, 0, statusLabelWidth,
						statusTextHeight);
				g.setColor(oldColor);
				statusText.setBg_color(textLabelSelectedColor);
				statusText.setFg_color(textLabelFontColor);
			}
			statusText.paint0(g, w - statusLabelWidth, statusTextHeight);
			statusText.setBg_color(oldStatusColor);
			statusText.setFg_color(oldStatusFontColor);
		}
		//                      // Remove these elements because the pointerPressed must 
		//                      // find the UIContact 
                UIScreen cs = this.getScreen();
                if (cs!=null){
                cs.removePaintedItem(sep);
                cs.removePaintedItem(statusText);
	}
        }
	/**
	 * @param g
	 * @param ithImg
	 */
	private void paintIthImage(Graphics g, Image ithImg) {
		if (ithImg != null) {
			g.drawImage(ithImg, 0, 0, Graphics.TOP | Graphics.LEFT);
			g.translate(ithImg.getWidth(), 0);
		}
	}

	public void executeAction() {
		RosterScreen rs = RosterScreen.getInstance();
		Contact c = this.c;
		if (rs.getSelectedContact() != c) {
			rs.rosterAccordion.setSelectedItem(this);
		}
		if (c != null) {
			if (c.unread_msg()) {
				// at this manner the loop is made to all the resources
				// even the offline ones
				Vector allConvs = c.getAllConvs();
				Enumeration en = allConvs.elements();
				while (en.hasMoreElements()) {
					Object[] coupleConv = (Object[]) en.nextElement();
					String ithRes = (String) coupleConv[0];
					Vector messages = (Vector) coupleConv[1];
					if (messages.size() > 0) {
						rs.chatWithSelected(ithRes);
						return;
					}
				}
			}
			// join a muc 
			if (c instanceof MUC
					&& (c.getAllPresences() == null || c.getAllPresences().length == 0)) {
				if (RosterScreen.isOnline()) {
					rs.enterMuc(c.jid);
					return;
				}
			}

			Presence presence = c.getPresence(null);
			String toJid = (presence != null ? presence
					.getAttribute(Message.ATT_FROM) : c.jid);
			rs.chatWithSelected(toJid);
		}
	}

	public void openContactMenu() {
		Contact c = this.c;
		RosterScreen rs = RosterScreen.getInstance();

		if (c != null) {
			boolean isOnline = RosterScreen.isOnline();
			boolean isMuc = c instanceof MUC;
			optionsMenu = UIUtils.easyMenu(c.getPrintableName(), 10, (this)
					.getSubmenu().getAbsoluteY(), UICanvas.getInstance()
					.getWidth() - 20, null);
			optionsMenu.setAutoClose(false);
			optionsAccordion = null;
			Presence[] res = c.getAllPresences();
			if (res != null && res.length > 1 && isMuc == false) {
				optionsAccordion = new UIAccordion();
				optionsAccordion.setFocusable(true);
				optionsAccordion.setMaxHeight(0);
				optionsAccordion.setOneOpen(false);
				optionsAccordion.setModal(true);
				optionsAccordion.setBg_color(UIConfig.menu_color);

				optionsAccordion.addSpareItem(cmd_details);
				if (isOnline) {
					optionsAccordion.addSpareItem(cmd_change_nick);
					optionsAccordion.addSpareItem(cmd_groups);
					optionsAccordion.addSpareItem(cmd_resend_auth);
					optionsAccordion.addSpareItem(cmd_rerequest_auth);
					optionsAccordion.addSpareItem(cmd_delc);
				}
				optionsAccordion.addSpareItem(cmd_active_sessions);
				cmd_active_sessions.setFocusable(false);

				for (int i = 0; i < res.length; i++) {
					optionsVector = new Vector();

					String resString = null;
					resString = Contact.resource(res[i]
							.getAttribute(Iq.ATT_FROM));
					if (resString == null) resString = res[i]
							.getAttribute(Iq.ATT_FROM);
					Image img = xmppClient.getPresenceIcon(c, res[i]
							.getAttribute(Iq.ATT_FROM), c.getAvailability());
					optionsLabel = new UILabel(img, resString);
					optionsLabel.setWrappable(true, UICanvas.getInstance()
							.getWidth() - 30);
					optionsLabel.setStatus(res[i]
							.getAttribute(Message.ATT_FROM));
					optionsVector.addElement(cmd_chat);
					if (isOnline) {
						optionsVector.addElement(cmd_send);
						if (AlbumScreen.getCount(Config.IMG_TYPE) > 0
								|| AlbumScreen.getCount(Config.AUDIO_TYPE) > 0) {
							optionsVector.addElement(cmd_send_file);
						}
						if (rs.cameraOn) optionsVector
								.addElement(cmd_contact_capture_img);
						if (rs.micOn) optionsVector
								.addElement(cmd_contact_capture_aud);
						optionsVector.addElement(cmd_querycmd);
						if (c.pending_tasks) {
							optionsVector.addElement(cmd_tasks);
						}
					}
					optionsAccordion.addItem(optionsLabel, optionsVector);
				}
				optionsMenu.append(optionsAccordion);
				//optionsAccordion.openLabel(optionsAccordion.getItemLabels()[0]);
				optionsAccordion.setSelectedIndex(0);
				optionsMenu.setSelectedItem(cmd_details);
			} else {

				String toRes = (res != null && res.length >= 1 ? res[0]
						.getAttribute(Message.ATT_FROM) : c.jid);
				optionsMenu.setStatus(toRes);
				optionsMenu.append(UIContact.cmd_chat);
				optionsMenu.append(UIContact.cmd_change_nick);
				if (isOnline) {
					optionsMenu.append(UIContact.cmd_send);
					if (AlbumScreen.getCount(Config.IMG_TYPE) > 0
							|| AlbumScreen.getCount(Config.AUDIO_TYPE) > 0) {
						optionsMenu.append(UIContact.cmd_send_file);
					}
					if (rs.cameraOn) optionsMenu
							.append(UIContact.cmd_contact_capture_img);
					if (rs.micOn) optionsMenu
							.append(UIContact.cmd_contact_capture_aud);
				}
				optionsMenu.append(UIContact.cmd_details);
				if (isOnline) {
					optionsMenu.append(UIContact.cmd_resend_auth);
					optionsMenu.append(UIContact.cmd_rerequest_auth);
					optionsMenu.append(UIContact.cmd_groups);
					optionsMenu.append(UIContact.cmd_delc);
					optionsMenu.append(UIContact.cmd_querycmd);
					if (c.pending_tasks) {
						optionsMenu.append(UIContact.cmd_tasks);
					}
				}
				if (isMuc) {
					optionsMenu.remove(UIContact.cmd_delc);
					optionsMenu.remove(UIContact.cmd_resend_auth);
					optionsMenu.remove(UIContact.cmd_rerequest_auth);
					optionsMenu.remove(UIContact.cmd_groups);
					optionsMenu.remove(UIContact.cmd_querycmd);
					optionsMenu.remove(UIContact.cmd_change_nick);

					if (isOnline) {
						optionsMenu.insert(optionsMenu
								.indexOf(UIContact.cmd_chat) + 1,
								UIContact.cmd_manage_muc);
						optionsMenu.insert(optionsMenu
								.indexOf(UIContact.cmd_manage_muc) + 1,
								UIContact.cmd_close_muc);

						UIItem presenceLabel = null;
						UIItem orderLabel = null;
						if (c.getAllPresences() != null) {
							presenceLabel = cmd_exit_muc;
							orderLabel = cmd_manage_muc;
						} else {
							cmd_enter_muc.setStatus(c.jid);
							presenceLabel = cmd_enter_muc;
							orderLabel = (UIItem) optionsMenu.getItems()
									.elementAt(0);
							optionsMenu.remove(cmd_chat);
						}
						optionsMenu.insert(optionsMenu.indexOf(orderLabel) + 1,
								presenceLabel);
					}
				}
				optionsMenu.setSelectedItem(UIContact.cmd_chat);
			}

			rs.addPopup(optionsMenu);
		}
	}
}