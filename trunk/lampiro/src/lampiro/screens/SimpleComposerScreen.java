/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: SimpleComposerScreen.java 931 2008-10-28 17:02:18Z luca $
*/

package lampiro.screens;

import it.yup.ui.UICanvas;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.xmpp.Contact;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.Message;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

public class SimpleComposerScreen extends TextBox implements CommandListener {

	private static ResourceManager rm = ResourceManager.getManager("common",
			"en");

	private Contact user;
	protected Command cmd_send = new Command(
			rm.getString(ResourceIDs.STR_SEND), Command.OK, 1);
	private Command cmd_exit = new Command(rm.getString(ResourceIDs.STR_EXIT),
			Command.CANCEL, 1);

	protected String preferredResource = null;

	public SimpleComposerScreen(Contact u) {
		super("", null, 2000, TextField.ANY);
		this.user = u;
		setTitle(rm.getString(ResourceIDs.STR_MESSAGE_TO) + " "
				+ user.getPrintableName());
		addCommand(cmd_send);
		addCommand(cmd_exit);
		setCommandListener(this);
		if (u.lastResource != null) preferredResource = u.jid + "/"
				+ u.lastResource;
		else
			preferredResource = u.getFullJid();
		//an offline contact may have no resources in that case
		// I use its jid and nothing else
		if (preferredResource == null) preferredResource = u.jid;

	}

	public void commandAction(Command cmd, Displayable d) {
		if (cmd == cmd_send) {
			// Send the message and add it to the conversation thread
			// XXX I must mantain the the thread ID, check the subject
			String msgText = getString();
			// Config cfg = Config.getInstance();
			// String myjid = cfg.user + "@" + cfg.server + "/" +
			// Config.YUP_RESOURCE_NAME;

			// override resource
			Message msg = null;
			msg = new Message(preferredResource, "chat");
			msg.setBody(msgText);
			XMPPClient.getInstance().sendPacket(msg);
			user.addMessageToHistory(msg);
			UICanvas.display(null);
			//UICanvas.getInstance().askRepaint(
			//		UICanvas.getInstance().getCurrentScreen());

		} else if (cmd == cmd_exit) {
			UICanvas.display(null);
		}
	}
}
