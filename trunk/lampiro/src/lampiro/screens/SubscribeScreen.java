/**
 * 
 */
package lampiro.screens;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import it.yup.ui.UIButton;
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
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.xmpp.Config;
import it.yup.xmpp.Contact;
import it.yup.xmpp.XMPPClient;

/**
 * @author luca
 *
 */
public class SubscribeScreen extends UIScreen {

	public static final int ADD=0;
	public static final int DELETE=1;
	public static final int MODIFY=2;

	private UIPanel subscribePanel;

	private UILabel cmd_yes;

	private UIMenu denyMenu;

	static ResourceManager rm = ResourceManager.getManager("common", "en");

	public UIButton acceptAll = new UIButton(rm
			.getString(ResourceIDs.STR_ACCEPT_ALL));
	
	private UIButton acceptAlways = new UIButton(rm
			.getString(ResourceIDs.STR_ACCEPT_ALWAYS));

	private UIButton close = new UIButton(rm.getString(ResourceIDs.STR_CLOSE));

	private UILabel cmd_no;
	
	private UILabel sub_text = new UILabel("");

	private Contact fromContact = null;
	
	UIHLayout acceptLayout= null;
	
	public SubscribeScreen (Contact fromContact){
		this();
		this.fromContact = fromContact;
		this.sub_text.setText(fromContact.getPrintableName()+ " " +rm.getString(ResourceIDs.STR_SUBSCRIPTION_REQUEST_FROM));
		int acceptIndex = subscribePanel.getItems().indexOf(acceptLayout);
		UIHLayout newAcceptLayout= null;
		newAcceptLayout = UIHLayout.easyCenterLayout(acceptAlways, 110);
		acceptAlways.setFont(UIConfig.small_font);
		subscribePanel.insertItemAt(newAcceptLayout,acceptIndex+1);
	}
	/**
	 * 
	 */
	public SubscribeScreen() {
		sub_text.setWrappable(true, UICanvas.getInstance().getWidth()-10);
		cmd_yes = new UILabel(rm.getString(ResourceIDs.STR_YES).toUpperCase());
		UIMenu menu = new UIMenu("");
		menu.append(cmd_yes);
		setMenu(menu);
		setTitle(rm.getString(ResourceIDs.STR_SUBSCRIPTION_CONFIRM));
		subscribePanel = new UIPanel();
		subscribePanel.setMaxHeight(-1);
		subscribePanel.setFocusable(true);
		this.append(subscribePanel);
		this.sub_text.setText(rm.getString(ResourceIDs.STR_SUBSCRIPTION_REQUEST));
		subscribePanel.addItem(this.sub_text);
		acceptLayout = UIHLayout.easyCenterLayout(acceptAll, 110);
		acceptLayout.setSelectedItem(acceptAll);
		acceptAll.setImg(UICanvas.getUIImage("/icons/contact_add_all.png"));
		acceptAll.setFont(UIConfig.small_font);
		acceptAlways.setImg(UICanvas.getUIImage("/icons/contact_add_always.png"));
		subscribePanel.addItem(acceptLayout);
		acceptAll.setFocusable(true);
		UISeparator sep = new UISeparator(2);
		sep.setBg_color(0xCCCCCC);
		subscribePanel.addItem(sep);
		sep = new UISeparator(2);
		sep.setBg_color(0xCCCCCC);
		subscribePanel.addItem(sep);
		UIHLayout closeLayout = UIHLayout.easyCenterLayout(close, 80);
		subscribePanel.addItem(closeLayout);
		denyMenu = new UIMenu("");
		cmd_no = new UILabel(rm.getString(ResourceIDs.STR_NO).toUpperCase());
		denyMenu.append(cmd_no);
		subscribePanel.setSelected(true);
		this.setSelectedIndex(0);
		subscribePanel.setSelectedIndex(1);
		acceptAll.setSelected(true);
	}

	private Hashtable subscriptions = new Hashtable(5);

	/*
	 * add a subscription request
	 */
	public boolean addSubscription(Contact c, int action) {
		// first check if the contact is already online
		Enumeration en = this.subscriptions.keys();
		while (en.hasMoreElements()) {
			UILabel selLabel = (UILabel) en.nextElement();
			Object[] objects = (Object[]) this.subscriptions.get(selLabel);
			Contact ithC = (Contact) objects[0];
			if (Contact.userhost(ithC.jid).compareTo(c.jid) == 0) return false;
		}
		// then insert it
		String upAction = "";
		Image image = null;
		if (action == SubscribeScreen.ADD){
			upAction = rm.getString(ResourceIDs.STR_ADD_CONTACT);
			image = UICanvas.getUIImage("/icons/contact_add.png");
		}
		if (action == SubscribeScreen.DELETE){
			upAction = rm.getString(ResourceIDs.STR_DELETE_CONTACT);
			image = UICanvas.getUIImage("/icons/contact_delete.png");
		}
		UIHLayout uhl = new UIHLayout(2);
		uhl.setGroup(false);
		UILabel imgLabel = new UILabel(image);
		uhl.insert(imgLabel, 0, image.getWidth(), UILayout.CONSTRAINT_PIXELS);
		imgLabel.setFocusable(false);
		UILabel ithSubscription = new UILabel(upAction + " "
				+ c.getPrintableName());
		uhl.insert(ithSubscription, 1, 100, UILayout.CONSTRAINT_PERCENTUAL);
		ithSubscription.setSubmenu(this.denyMenu);
		ithSubscription.setWrappable(true,
				UICanvas.getInstance().getWidth() - 20);
		subscriptions.put(ithSubscription, new Object[] { c, new Integer(action) });
		this.subscribePanel.insertItemAt(uhl, this.subscribePanel.getItems().size()-2);
		return true;
	}

	public void menuAction(UIMenu menu, UIItem cmd) {
		UIItem selLabel = this.subscribePanel.getSelectedItem();
		if (selLabel != null) {
			Object[] objects = (Object[]) this.subscriptions.get(selLabel);
			Contact c = (Contact) objects[0];
			int action = ((Integer) objects[1]).intValue();
			if (cmd == cmd_yes) {
				if (action == SubscribeScreen.ADD ) XMPPClient.getInstance()
						.getRoster().subscribeContact(c);
				else if (action == SubscribeScreen.DELETE) XMPPClient
						.getInstance().getRoster().unsubscribeContact(c);
				this.subscribePanel
						.removeItem((UIItem) selLabel.getContainer());
				this.subscriptions.remove(selLabel);
			} else if (cmd == cmd_no) {
				this.subscribePanel
						.removeItem((UIItem) selLabel.getContainer());
				this.subscriptions.remove(selLabel);
			}
			if (this.subscriptions.isEmpty())
				this.itemAction(this.close);
			else
				this.askRepaint();
		}
	}

	public void itemAction(UIItem cmd) {
		if (cmd == this.close) {
			// so that the user preferred resource is reset
			SubscribeScreen.releaseScreen(this);
			UICanvas.getInstance().close(this);
		} else if (cmd == acceptAll) {
			while (this.subscriptions.isEmpty() == false) {
				UILabel selLabel = (UILabel) this.subscriptions.keys()
						.nextElement();
				this.subscribePanel.setSelectedItem((UIItem) selLabel
						.getContainer());
				this.menuAction(null, cmd_yes);
				this.subscriptions.remove(selLabel);
			}
		} else if (cmd == acceptAlways) {
			this.itemAction(acceptAll);
			Config cfg = Config.getInstance();
			String acceptedGateways = cfg.getProperty(Config.ACCEPTED_GATEWAYS, "");
			acceptedGateways = acceptedGateways+"<"+this.fromContact.jid.trim();
			cfg.setProperty(Config.ACCEPTED_GATEWAYS, acceptedGateways);
			cfg.saveToStorage();
		}
	}

	private static SubscribeScreen userSubscriptionScreen = null;
	
	public synchronized static void releaseScreen(SubscribeScreen ss) {
		if (ss == SubscribeScreen.userSubscriptionScreen){
			userSubscriptionScreen = null;
		}
	}
	public synchronized static SubscribeScreen getUserSubscription() {
		if (userSubscriptionScreen==null)
			userSubscriptionScreen = new SubscribeScreen();
		return userSubscriptionScreen;
	}
}
