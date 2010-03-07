/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: SimpleWaitScreen.java 1770 2009-09-16 20:40:01Z luca $
*/
package lampiro.screens;

import it.yup.ui.UICanvas;
import it.yup.ui.UIGauge;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UIMenu;
import it.yup.ui.UIPanel;
import it.yup.ui.UIScreen;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;

import javax.microedition.lcdui.Gauge;

class SimpleWaitScreen extends UIScreen implements RosterScreen.WaitScreen {

	private static ResourceManager rm = ResourceManager.getManager();

	private UILabel cmd_cancel = new UILabel(rm
			.getString(ResourceIDs.STR_CLOSE).toUpperCase());

	private UIPanel mainList = new UIPanel(true, false);

	UIGauge progress_gauge = new UIGauge(rm.getString(ResourceIDs.STR_WAIT),
			false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING);

	public SimpleWaitScreen(String waitTitle) {
		this.setMenu(new UIMenu(""));
		UIMenu menu = this.getMenu();
		menu.append(cmd_cancel);
		this.setTitle(waitTitle);
		this.append(mainList);
		mainList.addItem(progress_gauge);
		progress_gauge.start();
	}

	public void menuAction(UIMenu menu, UIItem cmd) {
		if (cmd == cmd_cancel) {
			stopWaiting();
			UICanvas.getInstance().close(this);
		}
	}

	public void stopWaiting() {
		progress_gauge.cancel();
		UICanvas.getInstance().close(this);
	}
}