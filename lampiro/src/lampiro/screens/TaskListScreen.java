/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: TaskListScreen.java 2002 2010-03-06 19:02:12Z luca $
*/

package lampiro.screens;

import it.yup.ui.UICanvas;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UIMenu;
import it.yup.ui.UIScreen;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.xmpp.Task;

/**
 * XXX: maybe not necessary anymore with screen switch
 */
public class TaskListScreen extends UIScreen {

	private static ResourceManager rm = ResourceManager.getManager();

	private UILabel cmd_cancel = new UILabel(rm.getString(
			ResourceIDs.STR_CANCEL).toUpperCase());

	private Task tasks[];

	public TaskListScreen(Task tasks[]) {
		setTitle(rm.getString(ResourceIDs.STR_TASKHISTORY_TITLE));
		this.tasks = new Task[tasks.length];
		for (int i = 0; i < tasks.length; i++) {
			UILabel ul = new UILabel(tasks[i].getLabel());
			ul.setFocusable(true);
			append(ul);
			this.tasks[i] = tasks[i];
		}

		setMenu(new UIMenu(""));
		UIMenu menu = getMenu();
		menu.append(cmd_cancel);
	}

	public void menuAction(UIMenu menu, UIItem cmd) {
		if (cmd == cmd_cancel) {
			UICanvas.getInstance().close(this);
		}
	}

	public void itemAction(UIItem item) {
		String tname = null;
		if (!(item instanceof UILabel)) { return; }
		tname = ((UILabel) item).getText();
		for (int i = 0; i < tasks.length; i++) {
			if (tasks[i].getLabel().equals(tname)) {
				// #ifdef UI
				tasks[i].display();
				// #endif
			}
		}
	}
}
