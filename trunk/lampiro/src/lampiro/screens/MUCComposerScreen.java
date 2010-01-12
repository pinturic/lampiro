/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: MUCComposerScreen.java 1836 2009-10-12 10:20:06Z luca $
*/

package lampiro.screens;

import it.yup.ui.UICanvas;
import it.yup.xmpp.Contact;
import it.yup.xmpp.MUC;
import it.yup.xmpp.packets.Message;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

public class MUCComposerScreen extends SimpleComposerScreen {

	MUCComposerScreen(ChatScreen parentScreen, MUC muc) {
		super(parentScreen, muc, null);
		preferredResource = muc.jid;
	}

	public void commandAction(Command cmd, Displayable d) {
		if (cmd == cmd_send) {
			sendMessage(Contact.userhost(preferredResource), Message.GROUPCHAT);
			// the screen must be changed after the message is added to the screen!
			try {
				UICanvas.lock();
				UICanvas.display(null);
			} finally {
				UICanvas.unlock();
			}
			this.charCounter.cancel();

			//			UICanvas.getInstance().askRepaint(
			//					UICanvas.getInstance().getCurrentScreen());
		} else {
			super.commandAction(cmd, d);
		}
	}
}
