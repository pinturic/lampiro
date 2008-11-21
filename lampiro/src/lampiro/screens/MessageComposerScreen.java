/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: MessageComposerScreen.java 846 2008-09-11 12:20:05Z luca $
*/

package lampiro.screens;

import it.yup.ui.UIButton;
import it.yup.ui.UICanvas;
import it.yup.ui.UICombobox;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UIMenu;
import it.yup.ui.UIScreen;
import it.yup.ui.UITextField;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.xmpp.Contact;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.Message;
import it.yup.xmpp.packets.Stanza;

import javax.microedition.lcdui.TextField;

public class MessageComposerScreen extends UIScreen {

	private static ResourceManager rm = ResourceManager.getManager("common", "en");
	private Contact user = null;
    private int mtype;
	private UICombobox cg_type = new UICombobox("Type", false);
	private UITextField tf_subject = new UITextField("Subject", "", 100, TextField.ANY);
	private UITextField tf_body = new UITextField("Message", "", 1000, TextField.ANY);
	private UIButton btn_send = new UIButton("send"); 
	
	private UILabel cmd_send = new UILabel(rm.getString(ResourceIDs.STR_SEND));
	private UILabel cmd_cancel = new UILabel(rm.getString(ResourceIDs.STR_CANCEL));
	
	public static final int MESSAGE = 0;
	public static final int CHAT = 1;
	
    /**
     * Builds a composer screen associated with the given Contact and for the 
     * given message type (message/chat).
     */
	public MessageComposerScreen(Contact user, int default_type) {
		setTitle(rm.getString(ResourceIDs.STR_MESSAGE_TO) + " " + user.getPrintableName());
		this.user = user;
		cg_type.append("message");
		cg_type.append("chat");
		cg_type.setSelectedIndex(default_type);
		append(cg_type);
		if(default_type == MessageComposerScreen.MESSAGE) {
			append(tf_subject);
		}
		append(tf_body);
		mtype = default_type;
        
		append(btn_send);

		setMenu(new UIMenu(""));
        UIMenu menu = getMenu();
		menu.append(cmd_send);
        menu.append(cmd_cancel);
	}

    /*
     * (non-Javadoc)
     * @see it.yup.ui.UIScreen#menuAction(it.yup.ui.UIMenu, it.yup.ui.UIItem)
     */
	public void menuAction(UIMenu menu, UIItem cmd) {
		if(cmd == cmd_send) {
			Message msg;
			if(cg_type.getSelectedIndex() == 0) {
				msg = new Message(user.jid, null);
				String subject = tf_subject.getText();
				if(subject != null && !"".equals(subject)) {
					msg.addElement(Stanza.NS_JABBER_CLIENT, Message.SUBJECT, null).content = subject;
				}
			} else {
				msg = new Message(user.jid, "chat");
			}
			
			String body = tf_body.getText();
			if(body == null) body = "";
			msg.setBody(body);
            XMPPClient.getInstance().sendPacket(msg);
            user.addMessageToHistory(msg);
            UICanvas.getInstance().close(this);
		} else if(cmd == cmd_cancel) {
            UICanvas.getInstance().close(this);
		}
	}

    /*
     * (non-Javadoc)
     * @see it.yup.ui.UIScreen#itemAction(it.yup.ui.UIItem)
     */
    public void itemAction(UIItem item) {
        if(item == btn_send) {
            menuAction(null, cmd_send);
        } else if(item == cg_type) {
            if(mtype == 1 && cg_type.getSelectedIndex() == 0) {
                insert(1, tf_subject);
            } else if(mtype == 0 && cg_type.getSelectedIndex() == 1) {
                remove(1);
            }
            mtype = cg_type.getSelectedIndex();
        }
    }
    
}
