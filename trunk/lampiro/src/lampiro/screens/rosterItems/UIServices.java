package lampiro.screens.rosterItems;

import lampiro.screens.RosterScreen;
import it.yup.ui.UIAccordion;
import it.yup.ui.UIMenu;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.xmpp.Contact;

public class UIServices extends UIContactGroup {

	private static ResourceManager rm = ResourceManager.getManager();

	protected UIServices(UIAccordion accordion) {
		super(rm.getString(ResourceIDs.STR_SERVICES), accordion, UIGroup.END);

		this.virtualGroup = true;
		updateColors();
	}

	public static UIContactGroup getGroup(UIAccordion accordion, boolean allocate) {
		return (UIContactGroup) UIGroup.getGroup(rm
				.getString(ResourceIDs.STR_SERVICES), accordion, allocate);
	}

	protected boolean showUIContact(Contact c) {
		// services are always to be shown
		return true;
	}

	public UIMenu openGroupMenu() {
		RosterScreen rs = RosterScreen.getInstance();
		rs.setFreezed(true);
		UIMenu superMenu = super.openGroupMenu();
		superMenu.remove(UIContactGroup.groupMessage);
		superMenu.remove(UIContactGroup.chgGroupName);
		rs.setFreezed(false);
		askRepaint();
		return superMenu;
	}
}
