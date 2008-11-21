/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: CommandListScreen.java 846 2008-09-11 12:20:05Z luca $
*/

package it.yup.screens;

import lampiro.LampiroMidlet;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.xmpp.CommandExecutor;
import it.yup.xmpp.Contact;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

/**
 *
 */
public class CommandListScreen extends List implements CommandListener {

	private static ResourceManager rm = ResourceManager.getManager("common",
																	"en");

	private Command cmd_select = new Command(rm
			.getString(ResourceIDs.STR_EXECUTE), Command.BACK, 2);
	private Command cmd_cancel = new Command(rm
			.getString(ResourceIDs.STR_CANCEL), Command.BACK, 1);

	private Contact usr;

	public CommandListScreen(Contact _usr) {
		super(rm.getString(ResourceIDs.STR_CMDSCREEN_TITLE), Choice.IMPLICIT);
		usr = _usr;
		for (int i = 0; i < usr.cmdlist.length; i++) {
			String[] cmd = usr.cmdlist[i];
			append(cmd[1], null);
		}
		setCommandListener(this);
		setSelectCommand(cmd_select);
		addCommand(cmd_cancel);
	}

	public void commandAction(Command cmd, Displayable disp) {
		if (cmd == cmd_cancel) {
			LampiroMidlet.disp.setCurrent(RosterScreen.getInstance());
			return;
		}
		if (cmd == cmd_select) {
			int idx = getSelectedIndex();
			String[] selcmd = usr.cmdlist[idx];
			/* not the most beautiful way of programming, creating a floating object*/
			new CommandExecutor(usr, selcmd);
			LampiroMidlet.disp.setCurrent(RosterScreen.getInstance());
			return;
		}
	}
}
