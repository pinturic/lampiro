/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: CommandListScreen.java 913 2008-10-21 10:47:46Z luca $
*/

package lampiro.screens;

import javax.microedition.lcdui.Canvas;

import it.yup.ui.UICanvas;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UIMenu;
import it.yup.ui.UIPanel;
import it.yup.ui.UIScreen;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.xmpp.CommandExecutor;
import it.yup.xmpp.Contact;

/**
 * XXX: maybe not necessary anymore with submenus
 */
public class CommandListScreen extends UIScreen {

	private static ResourceManager rm = ResourceManager.getManager("common",
			"en");

	private UILabel cmd_select = new UILabel(rm
			.getString(ResourceIDs.STR_EXECUTE));
	private UILabel cmd_cancel = new UILabel(rm
			.getString(ResourceIDs.STR_CANCEL));

	private Contact usr;

	private UIPanel mainPanel = new UIPanel();

	public CommandListScreen(Contact _usr) {
		setTitle(rm.getString(ResourceIDs.STR_CMDSCREEN_TITLE));
		usr = _usr;
		for (int i = 0; i < usr.cmdlist.length; i++) {
			String[] cmd = usr.cmdlist[i];
			UILabel ithCommLabel = new UILabel(cmd[1]);
			ithCommLabel.setFocusable(true);
			append(ithCommLabel);
		}
		if (usr.cmdlist.length == 0) {
			UILabel ithCommLabel = new UILabel(rm
					.getString(ResourceIDs.STR_NO_COMMAND));
			ithCommLabel.setFocusable(true);
			append(ithCommLabel);
		}
		mainPanel.setMaxHeight(-1);
		this.append(mainPanel);
		setMenu(new UIMenu(""));
		UIMenu menu = getMenu();
		menu.append(cmd_select);
		menu.append(cmd_cancel);
	}

	public void menuAction(UIMenu menu, UIItem cmd) {
		if (cmd == cmd_cancel) {
			UICanvas.getInstance().close(this);
		} else if (cmd == cmd_select) {
			String idx = ((UILabel) getSelectedItem()).getText();
			for (int i = 0; i < usr.cmdlist.length; i++) {
				String[] selcmd = usr.cmdlist[i];
				if (idx.equals(selcmd[1])) {
					/*
					 * not the most beautiful way of programming, creating a
					 * floating object
					 */
					new CommandExecutor(usr, selcmd);
				}
			}
			UICanvas.getInstance().close(this);
		}
	}

	public boolean keyPressed(int kc) {
		if (super.keyPressed(kc)) return true;

		if (this.popupList.size() == 0
				&& this.getMenu().isOpenedState() == false) {
			int ga = UICanvas.getInstance().getGameAction(kc);

			switch (ga) {
				case Canvas.RIGHT: {
					RosterScreen.showNextScreen(this);
					return true;
				}
				case Canvas.LEFT: {
					RosterScreen.showPreviousScreen(this);
					return true;
				}
			}
		}
		return false;
	}

	public void itemAction(UIItem item) {
		String idx = ((UILabel) item).getText();
		for (int i = 0; i < usr.cmdlist.length; i++) {
			String[] selcmd = usr.cmdlist[i];
			if (idx.equals(selcmd[1])) {
				/*
				 * not the most beautiful way of programming, creating a
				 * floating object
				 */
				new CommandExecutor(usr, selcmd);
			}
		}
		UICanvas.getInstance().close(this);
	}
}
