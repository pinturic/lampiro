/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: MUCComposer.java 846 2008-09-11 12:20:05Z luca $
*/

package lampiro.screens;

import it.yup.ui.UICanvas;
import it.yup.xmpp.MUC;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.Message;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

public class MUCComposer extends SimpleComposerScreen {

	MUCComposer(MUC muc) {
		super(muc);
		preferredResource = muc.jid;
	}

	public void commandAction(Command cmd, Displayable d) {
		if (cmd == cmd_send) {
			String msgText = getString();
			Message msg = null;
			msg = new Message(preferredResource, "groupchat");
			msg.setBody(msgText);
			XMPPClient.getInstance().sendPacket(msg);
			UICanvas.display(null);
			UICanvas.getInstance().askRepaint(
												UICanvas.getInstance()
														.getCurrentScreen());
		} else {
			super.commandAction(cmd, d);
		}
	}
}
