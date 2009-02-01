/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: AddContactScreen.java 1159 2009-02-01 10:04:07Z fabio $
*/

package lampiro.screens;

import it.yup.ui.UICanvas;
import it.yup.ui.UICombobox;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UIMenu;
import it.yup.ui.UIScreen;
import it.yup.ui.UITextField;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.util.Utils;
import it.yup.xmlstream.Element;
import it.yup.xmpp.Contact;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.IQResultListener;
import it.yup.xmpp.packets.Iq;

import java.util.Vector;

import javax.microedition.lcdui.TextField;

/**
 * XXX: there is a warning on itemStateChanged; we implement here
 * ItemStateListener but the method clashes with a method with same signature
 * defined in Displayable. The Displayable method is package-protected so it is
 * not a problem, but it's a confusing warning... Displayable doesn't implement
 * ItemStateListener... nice job, Sun!
 */
public class AddContactScreen extends UIScreen {

	private static ResourceManager rm = ResourceManager.getManager("common",
																	"en");
	/*
	 * the found gateways
	 */
	private Vector gateways = new Vector();

	private UITextField t_name;
	private UITextField t_jid;
	private UITextField t_group;
	private UITextField t_error;
	private UICombobox t_type;

	private UILabel cmd_save = new UILabel(rm.getString(ResourceIDs.STR_SAVE));
	private UILabel cmd_exit = new UILabel(rm.getString(ResourceIDs.STR_EXIT));

	public AddContactScreen() {
		this.setFreezed(true);
		setTitle(rm.getString(ResourceIDs.STR_ADD_CONTACT));

		t_jid = new UITextField(rm.getString(ResourceIDs.STR_ADDRESS), null,
				64, TextField.EMAILADDR);
		t_name = new UITextField(rm.getString(ResourceIDs.STR_NICKNAME), null,
				64, TextField.NON_PREDICTIVE);
		t_group = new UITextField(rm.getString(ResourceIDs.STR_GROUP), null,
				64, TextField.ANY);
		// create but don't append error
		t_error = new UITextField(rm.getString(ResourceIDs.STR_ERROR), null,
				64, TextField.UNEDITABLE);

		t_type = new UICombobox(rm.getString(ResourceIDs.STR_CONTACT_TYPE),
				false);

		t_type.append("Jabber");
		t_type.setSelectedIndex(0);

		append(t_jid);
		append(t_name);
		append(t_group);
		append(t_type);

		/*
		 * XXX: useless? // I add a list of groups only if there are groups
		 * Vector v = XMPPClient.getInstance().getRoster().groups; for(int i =
		 * 1; i < v.size(); i++) { Group g = (Group) v.elementAt(i);
		 * ch_grps.append(g.name, null); } if(ch_grps.size() > 0) {
		 * append(ch_grps); }
		 */
		setMenu(UIMenu.easyMenu("", -1, -1, -1, cmd_save));
		getMenu().append(cmd_exit);
		this.setFreezed(false);
		this.askRepaint();
		getGateways();
	}

	private void getGateways() {
		// to get the gateways
		IQResultListener dih = new IQResultListener() {
			public void handleError(Element e) {
			}

			public void handleResult(Element e) {
				Element q = e.getChildByName(XMPPClient.NS_IQ_DISCO_ITEMS,
												Iq.QUERY);
				if (q != null) {
					Element items[] = q
							.getChildrenByName(XMPPClient.NS_IQ_DISCO_ITEMS,
												"item");
					for (int i = 0; i < items.length; i++) {
						String ithJid = items[i].getAttribute("jid");
						IQResultListener dih = new IQResultListener() {
							public void handleError(Element e) {
								// TODO Auto-generated method stub

							}

							public void handleResult(Element e) {
								Element q = e
										.getChildByName(
														XMPPClient.NS_IQ_DISCO_INFO,
														Iq.QUERY);
								if (q != null) {
									String name = "";
									String category = "";
									String from = e.getAttribute("from");
									Element identity = q
											.getChildByName(
															XMPPClient.NS_IQ_DISCO_INFO,
															"identity");
									name = identity.getAttribute("name");
									category = identity
											.getAttribute("category");
									if (category.toLowerCase()
											.compareTo("gateway") == 0) {
										t_type.append(name);
										AddContactScreen.this.gateways
												.addElement(from);
									}
								}
							}
						};
						Iq iq = new Iq(ithJid, Iq.T_GET);
						iq.addElement(XMPPClient.NS_IQ_DISCO_INFO, Iq.QUERY);
						XMPPClient.getInstance().sendIQ(iq, dih);
					}
				}
			}
		};
		String myJid = XMPPClient.getInstance().my_jid;
		String domain = Contact.domain(myJid);
		Iq iq = new Iq(domain, Iq.T_GET);
		iq.addElement(XMPPClient.NS_IQ_DISCO_ITEMS, Iq.QUERY);
		XMPPClient.getInstance().sendIQ(iq, dih);
	}

	public void menuAction(UIMenu menu, UIItem cmd) {
		if (cmd == cmd_save) {
			if (this.t_type.getSelectedIndex() == 0) {
				String jid = t_jid.getText();
				String name = t_name.getText();
				String group = t_group.getText();
				registerContact(jid, name, group);
			} else {
				IQResultListener gjh = new IQResultListener() {
					public void handleError(Element e) {
					}

					public void handleResult(Element e) {
						Element query = e
								.getChildByName(XMPPClient.JABBER_IQ_GATEWAY,
												Iq.QUERY);
						Element q = query
								.getChildByName(XMPPClient.JABBER_IQ_GATEWAY,
												"jid");
						String jid = q.content;
						String name = t_name.getText();
						String group = t_group.getText();
						AddContactScreen.this.registerContact(jid, name, group);

					}
				};
				String to = (String) this.gateways.elementAt(this.t_type
						.getSelectedIndex() - 1);
				Iq iq = new Iq(to, Iq.T_SET);
				Element query = iq.addElement(XMPPClient.JABBER_IQ_GATEWAY,
												Iq.QUERY);
				Element prompt = query
						.addElement(XMPPClient.JABBER_IQ_GATEWAY, Iq.PROMPT);
				prompt.content = this.t_jid.getText();
				XMPPClient.getInstance().sendIQ(iq, gjh);
			}
			UICanvas.getInstance().show(RosterScreen.getInstance());
			UICanvas.getInstance().close(this);
		} else if (cmd == cmd_exit) {
			UICanvas.getInstance().close(this);
		}
	}

	/**
	 * 
	 */
	private void registerContact(String jid, String name, String group) {
		Contact c;
		// XXX also check if the contact is not already present in the
		// roster
		if (jid == null || !(Utils.is_jid(jid))) {
			t_error.setText("bad jid");
			append(t_error);
			return;
		}

		String groups[] = null;
		if (group != null && group.length() > 0) {
			groups = new String[] { group };
		}
		c = new Contact(jid, name, null, groups);

		XMPPClient.getInstance().getRoster().subscribeContact(c, false);
	}
}
