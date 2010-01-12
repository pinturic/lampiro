/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: CommandListScreen.java 1905 2009-11-11 14:56:07Z luca $
*/

package lampiro.screens;

import javax.microedition.lcdui.Gauge;

import lampiro.screens.RosterScreen.WaitScreen;

import it.yup.ui.UIButton;
import it.yup.ui.UICanvas;
import it.yup.ui.UIGauge;
import it.yup.ui.UIHLayout;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UIMenu;
import it.yup.ui.UIPanel;
import it.yup.ui.UIScreen;
import it.yup.ui.UIUtils;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.xmpp.CommandExecutor;
import it.yup.xmpp.Contact;

/**
 * XXX: maybe not necessary anymore with submenus
 */
public class CommandListScreen extends UIScreen implements WaitScreen {

	private static ResourceManager rm = ResourceManager.getManager();

	private UILabel cmd_select = new UILabel(rm.getString(
			ResourceIDs.STR_EXECUTE).toUpperCase());
	private UILabel cmd_cancel = new UILabel(rm
			.getString(ResourceIDs.STR_CLOSE).toUpperCase());

	private UIButton cmd_close = new UIButton(rm
			.getString(ResourceIDs.STR_CLOSE));

	private Contact usr;

	private UIPanel mainList = new UIPanel(true, false);

	/*
	 * The chosen resource for this command   
	 */
	private String chosenResource;

	UIGauge progress_gauge = null;

	public CommandListScreen(Contact _usr, String chosenResource) {
		setMenu(new UIMenu(""));
		UIMenu menu = getMenu();
		menu.append(cmd_select);
		menu.append(cmd_cancel);

		setTitle(rm.getString(ResourceIDs.STR_CMDSCREEN_TITLE));
		usr = _usr;
		this.chosenResource = chosenResource;
		int buttonSize = Math.min(Math.max(150, (UICanvas.getInstance()
				.getWidth() * 3) / 4), UICanvas.getInstance().getWidth());

		UILabel avCommands = new UILabel(rm
				.getString(ResourceIDs.STR_AVAILABLE_COMMANDS)
				+ " " + _usr.getPrintableName());
		avCommands.setWrappable(true, buttonSize);
		mainList.addItem(UIUtils.easyCenterLayout(avCommands, buttonSize));

		for (int i = 0; i < usr.cmdlist.length; i++) {
			String[] cmd = usr.cmdlist[i];
			UIButton ithCommLabel = new UIButton(cmd[1]);
			ithCommLabel.setStatus(cmd);
			ithCommLabel.setFocusable(true);
			ithCommLabel.setWrappable(true, buttonSize - 15);
			mainList
					.addItem(UIUtils.easyCenterLayout(ithCommLabel, buttonSize));
		}
		if (usr.cmdlist.length == 0) {
			UILabel ithCommLabel = new UILabel(rm
					.getString(ResourceIDs.STR_NO_COMMAND));
			ithCommLabel.setFocusable(false);
			mainList.addItem(ithCommLabel);
			menu.remove(cmd_select);
		} else {
			UIHLayout firstCommandLayout = (UIHLayout) this.mainList.getItems()
					.elementAt(1);
			firstCommandLayout.setSelectedItem(firstCommandLayout.getItem(1));
		}
		mainList.addItem(UIUtils.easyCenterLayout(cmd_close, buttonSize));

		mainList.setMaxHeight(-1);
		this.append(mainList);
		this.setSelectedItem(mainList);
	}

	public void menuAction(UIMenu menu, UIItem cmd) {
		if (cmd == cmd_cancel) {
			stopWaiting();
		} else if (cmd == cmd_select) {
			this.itemAction(mainList.getSelectedItem());
		}
	}

	public boolean keyPressed(int kc) {
		if (super.keyPressed(kc)) return true;

		return RosterScreen.makeRoll(kc, this);
	}

	public void itemAction(final UIItem item) {
		if (item == null) return;
		if (item == cmd_close) {
			menuAction(null, cmd_cancel);
			return;
		} else if (item instanceof UIButton) {
			this.getMenu().remove(cmd_select);
			cmd_cancel.setText(rm.getString(ResourceIDs.STR_CLOSE)
					.toUpperCase());
			mainList.removeAllItems();
			progress_gauge = new UIGauge(rm.getString(ResourceIDs.STR_WAIT),
					false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING);;
			mainList.addItem(progress_gauge);
			progress_gauge.start();
//			RosterScreen.getInstance().setWaitingDF(this);
//			this.askRepaint();
			CommandExecutor cmdEx = null;
			String[] selCmd = (String[]) item.getStatus();

			cmdEx = new CommandExecutor(selCmd, chosenResource,
					getReturnScreen());
			RosterScreen.getInstance()._handleTask(cmdEx);
			RosterScreen.getInstance().setWaitingDF(this);
			this.askRepaint();
			cmdEx.setupCommand();
		}
	}

	public void stopWaiting() {
		if (progress_gauge!=null)
			progress_gauge.cancel();
		UICanvas.getInstance().close(this);
	}
}
