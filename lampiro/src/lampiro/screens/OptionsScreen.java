/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: OptionsScreen.java 909 2008-10-14 21:14:03Z luca $
*/

package lampiro.screens;

import it.yup.ui.UICanvas;
import it.yup.ui.UICombobox;
import it.yup.ui.UIGauge;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UIMenu;
import it.yup.ui.UIScreen;
import it.yup.ui.UITextField;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.util.Utils;
import it.yup.xmpp.Config;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.TextField;

import lampiro.LampiroMidlet;

public class OptionsScreen extends UIScreen {

	private static ResourceManager rm = ResourceManager.getManager("common",
			"en");

	private UILabel cmd_save = new UILabel(rm.getString(ResourceIDs.STR_SAVE));
	private UILabel cmd_cancel = new UILabel(rm
			.getString(ResourceIDs.STR_CANCEL));
	private UILabel cmd_reset = new UILabel(rm
			.getString(ResourceIDs.STR_RESET_OPTIONS));

	private UICombobox ch_vibrate;
	private UICombobox ch_tone;
	public UICombobox color;
	public UICombobox font_size;

	private UITextField tf_keepalive;
	private UITextField history_size;
	private UIGauge g_volume;

	public OptionsScreen() {
		setTitle(rm.getString(ResourceIDs.STR_OPTIONS_SETUP));
		boolean flags[] = new boolean[2];
		Config cfg = Config.getInstance();
		String selected_status = cfg.getProperty(
				Config.VIBRATION_AND_TONE_SETTINGS, "1");

		// prepare the form
		// tone and vibration settings
		append(new UILabel(rm.getString(ResourceIDs.STR_WHEN_NEW_MESSAGES)));
		ch_vibrate = new UICombobox(rm.getString(ResourceIDs.STR_VIBRATE_IF),
				true);
		ch_vibrate.append(rm.getString(ResourceIDs.STR_HIDDEN));
		ch_vibrate.append(rm.getString(ResourceIDs.STR_SHOW));
		flags = Utils.str2flags(selected_status, 0, 2);
		ch_vibrate.setSelectedFlags(flags);
		append(ch_vibrate);

		ch_tone = new UICombobox(rm.getString(ResourceIDs.STR_PLAY_TONE_IF),
				true);
		ch_tone.append(rm.getString(ResourceIDs.STR_HIDDEN));
		ch_tone.append(rm.getString(ResourceIDs.STR_SHOW));
		flags = Utils.str2flags(selected_status, 2, 2);
		ch_tone.setSelectedFlags(flags);
		append(ch_tone);
		// XXX: append(new Spacer(100, 1));

		color = new UICombobox(rm.getString(ResourceIDs.STR_COLOR), false);
		color.setAnchorPoint(Graphics.RIGHT);
		color.append(rm.getString(ResourceIDs.STR_COLOR_BLUE));
		color.append(rm.getString(ResourceIDs.STR_COLOR_GREEN));
		color.append(rm.getString(ResourceIDs.STR_COLOR_RED));
		append(color);
		String colorString = Config.getInstance()
				.getProperty(Config.COLOR, "0");
		int colorInt = Integer.parseInt(colorString);
		color.setSelectedIndex(colorInt);

		String font_sizeString = rm.getString(ResourceIDs.STR_FONT_SIZE);
		font_size = new UICombobox(font_sizeString, false);
		font_size.setAnchorPoint(Graphics.RIGHT);
		font_size.append(rm.getString(ResourceIDs.STR_FONT_SMALL));
		font_size.append(rm.getString(ResourceIDs.STR_FONT_MEDIUM));
		font_size.append(rm.getString(ResourceIDs.STR_FONT_BIG));
		append(font_size);
		font_sizeString = Config.getInstance().getProperty(Config.FONT_SIZE,
				"1");
		font_size.setSelectedIndex(font_sizeString.toCharArray()[0] - '0');
		// XXX: append(new Spacer(100, 1));

		// volume
		int volume = Integer
				.parseInt(cfg.getProperty(Config.TONE_VOLUME, "50")) / 10;
		g_volume = new UIGauge(rm.getString(ResourceIDs.STR_VOLUME), true, 10,
				volume);
		append(g_volume);
		// XXX: append(new Spacer(100, 1));

		// keepalive
		long ka = Long.parseLong(cfg.getProperty(Config.KEEP_ALIVE)) / 1000;

		tf_keepalive = new UITextField(rm.getString(ResourceIDs.STR_KEEPALIVE),
				String.valueOf(ka), 5, TextField.NUMERIC);
		append(tf_keepalive);

		short hs = Short.parseShort(cfg.getProperty(Config.HISTORY_SIZE, "30"));
		history_size = new UITextField(rm
				.getString(ResourceIDs.STR_HISTORY_SIZE), String.valueOf(hs),
				5, TextField.NUMERIC);
		append(history_size);

		// add the commands
		setMenu(new UIMenu(""));
		UIMenu menu = getMenu();
		menu.append(cmd_save);
		menu.append(cmd_reset);
		menu.append(cmd_cancel);
	}

	public void itemAction(UIItem item) {
		if (item == this.color) {
			int colorIndex = this.color.getSelectedIndex();
			// #ifdef UI
			LampiroMidlet.changeColor(colorIndex);
			// #endif
			Config cfg = Config.getInstance();
			cfg.setProperty(Config.COLOR, this.color.getSelectedIndex() + "");
			cfg.saveToStorage();
			this.setDirty(true);
			this.askRepaint();
		}
		if (item == this.font_size) {
			int fontIndex = this.font_size.getSelectedIndex();
			// #ifdef UI
			LampiroMidlet.changeFont(fontIndex);
			// #endif 
			Config cfg = Config.getInstance();
			cfg.setProperty(Config.FONT_SIZE, this.font_size.getSelectedIndex()
					+ "");
			cfg.saveToStorage();
			this.setDirty(true);
			this.askRepaint();
		}
	}

	public void menuAction(UIMenu menu, UIItem cmd) {
		if (cmd_save == cmd) {
			saveOptions();
		} else if (cmd_reset == cmd) {
			Config cfg = Config.getInstance();
			cfg.resetStorage(false);
			// this *CAN* be set now instead of waiting reload
			UICanvas.getInstance().close(this);
			OptionsScreen os = new OptionsScreen();
			UICanvas.getInstance().open(os, true);
			os.itemAction(os.color);
			os.itemAction(os.font_size);
			os.saveOptions();
		} else if (cmd_cancel == cmd) {
			UICanvas.getInstance().close(this);
		}

	}

	/**
	 * 
	 */
	public void saveOptions() {
		Config cfg = Config.getInstance();
		boolean flags[] = new boolean[4];
		flags[0] = ch_vibrate.isSelected(0);
		flags[1] = ch_vibrate.isSelected(1);
		flags[2] = ch_tone.isSelected(0);
		flags[3] = ch_tone.isSelected(1);
		cfg.setProperty(Config.VIBRATION_AND_TONE_SETTINGS, Utils.flags2str(
				flags, 0));
		cfg.setProperty(Config.TONE_VOLUME, String
				.valueOf(g_volume.getValue() * 10));
		cfg.setProperty(Config.KEEP_ALIVE, String.valueOf(Integer
				.parseInt(tf_keepalive.getText()) * 1000));
		cfg.setProperty(Config.HISTORY_SIZE, String.valueOf(Integer
				.parseInt(history_size.getText())));
		cfg.saveToStorage();

		UICanvas.getInstance().close(this);
		UICanvas.showAlert(AlertType.WARNING, rm
				.getString(ResourceIDs.STR_WARNING), rm
				.getString(ResourceIDs.STR_SETTINGS_EFFECT));
	}

}
