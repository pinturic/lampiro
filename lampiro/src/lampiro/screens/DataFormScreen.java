/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: DataFormScreen.java 1976 2010-02-12 16:59:23Z luca $
*/

/**
 * 
 */
package lampiro.screens;

import it.yup.ui.UIButton;
import it.yup.ui.UICanvas;
import it.yup.ui.UICheckbox;
import it.yup.ui.UICombobox;
import it.yup.ui.UIGauge;
import it.yup.ui.UIHLayout;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UILayout;
import it.yup.ui.UIMenu;
import it.yup.ui.UIPanel;
import it.yup.ui.UIScreen;
import it.yup.ui.UISeparator;
import it.yup.ui.UITextField;
import it.yup.ui.UIUtils;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.util.Utils;
import it.yup.xml.Element;
import it.yup.xmpp.Config;
import it.yup.xmpp.DataFormListener;
import it.yup.xmpp.IQResultListener;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.DataForm;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.DataForm.Field;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.ContentConnection;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextField;

import lampiro.screens.RosterScreen.WaitScreen;

import org.bouncycastle.util.encoders.Base64;

// #mdebug
//@
//@import it.yup.util.Logger;
//@
// #enddebug

/**
 * <p>
 * This class handles the data form input from the user, rendering a given Data
 * Form using the base controls offered by J2ME UI. The input result is then
 * sent to a DataFormListener that handles the form outcome.
 * </p>
 * 
 * <p>
 * DataForms are rendered as follows.
 * <ul>
 * <li><b>hidden</b>: are skipped.</li>
 * <li><b>boolean</b>: are rendered with a ChoiceGroup flagged with "MULTIPLE"
 * and with a single choice item that may be checked (true) or unchecked
 * (false).</li>
 * <li><b>list-multi and list-single</b>: they show a button that opens a
 * separate List, List is "EXCLUSIVE" (a single voice can be selected) or
 * "MULTIPLE" (more than one voice selected) resp. for list-single and
 * list-multi.</li>
 * <li><b>jid-single</b>, <b>jid-multi</b>, <b>text-single</b>,
 * <b>text-multi</b>, <b>text-private</b>, <b>fixed</b>: are shown as a
 * single TextField, *-multi are split on '\n' chars when sending data;
 * text-private are flagged as PASSWORD fields, jid-single are flagged as
 * EMAILADDRESS fields. fixed are uneditable.</li>
 * </ul>
 * 
 * All fields have a Label before if the DataForm field has one. Commands are
 * placed on the menu. Instructions, if present, make a command "instructions"
 * appear on the menu and that voice pops up an alert showing the instructions.
 * Field desc are ignored.
 * 
 * At the bottom of the forms the button for the available actions are placed.
 * Available actions are passed via the method setActions()
 * 
 * <i>TODO LIST:
 * <ul>
 * <li>list-single and list-multi should be changed: they should show a non
 * editable control that exposes the label and the current selected content plus
 * a button that spawns the pop-up screen for the selection</li>
 * <li>text-multi and jid-multi should open a separate TextBox item (note. on
 * SonyEricsson it seems that there's no difference between the two...).</li>
 * <li>jid-multi should be checked for emailaddress format (emailaddress is not
 * enforceable for multiline TextBoxes</li>
 * <li>check '\n' in fields that are not *-multi and pop up error.</li>
 * <li>add a voice "field description" on the menu (or place a button with (?))
 * to honour the "desc" for each field.</li>
 * <li>Heuristics: when a form has a single list-* item or the list-* item has
 * no more than 2 or 3 options, there shouldn't be a need for a pop up screen.
 * </ul>
 * </i>
 * 
 */
public class DataFormScreen extends UIScreen implements WaitScreen {

	private static ResourceManager rm = ResourceManager.getManager();

	/** The handled data form */
	private DataForm df;

	/** the listener to be notified of commands */
	private DataFormListener dfl;

	/** the available actions */
	private int actions;

	private UIButton cmd_submit;
	private UIButton cmd_cancel;
	private UILabel menu_cancel = new UILabel(rm.getString(
			ResourceIDs.STR_CLOSE).toUpperCase());
	private UIButton cmd_prev;
	private UIButton cmd_next;
	private UILabel cmd_delay = new UILabel(rm
			.getString(ResourceIDs.STR_FILLLATER));

	private UIMenu show_instruction;
	private UILabel show_instruction_label = new UILabel(rm.getString(
			ResourceIDs.STR_INSTRUCTIONS).toUpperCase());

	private UILabel show_desc_label = new UILabel(rm.getString(
			ResourceIDs.STR_DESC).toUpperCase());

	private UIMenu instruction_menu = null;
	private UIMenu desc_menu = new UIMenu("");
	private UILabel si_instructions = new UILabel("");

	/** the item array created to represent the form */
	private UIItem[] items;

	UIGauge progress_gauge = null;

	/*
	 * To construct the "Expand" support
	 */
	UIMenu zoomSubmenu;
	UILabel zoomLabel = new UILabel("EXPAND");

	private UIHLayout mainLayout = new UIHLayout(3);
	UIPanel mainPanel = new UIPanel(true, false);

	public class CidListener extends IQResultListener {

		private int mediaType;
		private UILabel label;
		private Field fld;

		public CidListener(Field fld, int mediaType, UILabel label) {
			this.mediaType = mediaType;
			this.label = label;
			this.fld = fld;
		}

		public void handleError(Element e) {
			// #mdebug
			//@			Logger.log("In retrieving cid");
			// #enddebug
		}

		public void handleResult(Element e) {
			Element data = e.getChildByName(null, XMPPClient.DATA);
			String text = data.getText();
			byte[] decodedData = Base64.decode(text);

			UICanvas.lock();
			try {
				showMedia(fld, decodedData, mediaType, this.label);
			} catch (Exception ex) {
				// #mdebug
				//@				Logger.log("In retrieving media2");
				//@				System.out.println(ex.getMessage());
				//@				ex.printStackTrace();
				// #enddebug
			} finally {
				UICanvas.unlock();
			}

		}

	}

	public DataFormScreen(DataForm df, DataFormListener dfl, int cmds) {
		setTitle(rm.getString(ResourceIDs.STR_FILL_FORM));

		UISeparator separator = new UISeparator(0);
		mainLayout.setGroup(false);
		mainLayout.insert(separator, 0, 3, UILayout.CONSTRAINT_PIXELS);
		mainLayout.insert(mainPanel, 1, 100, UILayout.CONSTRAINT_PERCENTUAL);
		mainLayout.insert(separator, 2, 3, UILayout.CONSTRAINT_PIXELS);
		this.append(mainLayout);

		this.df = df;
		this.dfl = dfl;

		if (df.title != null) {
			setTitle(df.title);
		}

		setMenu(new UIMenu(""));
		UIMenu menu = getMenu();
		menu.append(menu_cancel);
		//menu.append(cmd_delay);
		actions = DataFormListener.CMD_SUBMIT | DataFormListener.CMD_CANCEL;
		instruction_menu = UIUtils.easyMenu(rm
				.getString(ResourceIDs.STR_INSTRUCTIONS), 10, 20, this
				.getWidth() - 10, null);
		//		desc_menu.setAbsoluteX(10);
		//		desc_menu.setAbsoluteY(20);
		//		desc_menu.setWidth(this.getWidth() - 10);
		desc_menu.append(show_desc_label);
		show_instruction = UIUtils.easyMenu("", 10, 20, this.getWidth() - 10,
				show_instruction_label);
		// prepare zoomSubMenu
		zoomSubmenu = UIUtils.easyMenu("", 10, 10, this.getWidth() - 30,
				zoomLabel);
		zoomLabel.setAnchorPoint(Graphics.HCENTER);
		if (cmds > 0) {
			this.setActions(cmds);
		}
		this.createControls();
	}

	/**
	 * Sets the available command buttons. Actions should be one of the CMD_*
	 * flags defined in the DataFormListener interface.
	 * 
	 * @param cmds
	 */
	private void setActions(int _ac) {
		/* submit and cancel are always shown */
		actions = _ac /*| DataFormListener.CMD_SUBMIT*/
				| DataFormListener.CMD_CANCEL;
	}

	/**
	 * Show the form, dynamically adding all the controls
	 */
	private void createControls() {

		// as always: many operations on the gui need a freeze since
		// i love the battery life
		this.setFreezed(true);

		this.mainPanel.removeAllItems();

		/* do I create this only once? */
		items = new UIItem[df.fields.size()];

		for (int i = 0; i < df.fields.size(); i++) {
			DataForm.Field fld = (DataForm.Field) df.fields.elementAt(i);

			if (fld.type == DataForm.FLT_HIDDEN) {
				continue;
			}

			if (fld.type == DataForm.FLT_BOOLEAN) {
				// XXX: check how to render this
				String fldName = (fld.label == null ? fld.varName : fld.label);
				UICheckbox cgrp = new UICheckbox(fldName);
				if ("1".equals(fld.dValue) || "true".equals(fld.dValue)) {
					cgrp.setChecked(true);
				} else {
					cgrp.setChecked(false);
				}
				mainPanel.addItem(cgrp);
				items[i] = cgrp;
				continue;
			}

			if (fld.type == DataForm.FLT_LISTMULTI
					|| fld.type == DataForm.FLT_LISTSINGLE) {
				String title = (fld.label == null ? fld.varName : fld.label);
				UICombobox cgrp = new UICombobox(title,
						(fld.type == DataForm.FLT_LISTMULTI));
				boolean[] flags = new boolean[fld.options.size()];
				for (int j = 0; j < fld.options.size(); j++) {
					String[] opt = (String[]) fld.options.elementAt(j);
					cgrp.append(opt[1]);
					if (fld.dValue != null && fld.dValue.indexOf(opt[0]) != -1) {
						flags[j] = true;
					} else {
						flags[j] = false;
					}
				}
				cgrp.setSelectedFlags(flags);
				mainPanel.addItem(cgrp);
				items[i] = cgrp;
				continue;
			}

			if (fld.type == DataForm.FLT_JIDSINGLE
					|| fld.type == DataForm.FLT_TXTPRIV
					|| fld.type == DataForm.FLT_TXTSINGLE
					|| fld.type == DataForm.FLT_JIDMULTI
					|| fld.type == DataForm.FLT_TXTMULTI
					|| fld.type == DataForm.FLT_FIXED) {
				String title = (fld.label == null ? ""/* fld.varName */
				: fld.label);
				int flags = TextField.ANY;
				if (fld.type == DataForm.FLT_TXTPRIV) {
					flags |= TextField.PASSWORD;
				}
				if (fld.type == DataForm.FLT_JIDSINGLE) {
					flags |= TextField.EMAILADDR;
				}
				if (fld.type == DataForm.FLT_FIXED) {
					flags |= TextField.UNEDITABLE;
				}
				// XXX: Which the maximum allowed length? We use 1k for the
				// moment
				UITextField tf = new UITextField(title, fld.dValue, 1024, flags);
				mainPanel.addItem(tf);
				if (fld.type == DataForm.FLT_TXTMULTI
						|| fld.type == DataForm.FLT_FIXED
						|| fld.type == DataForm.FLT_TXTSINGLE) {
					if (fld.type == DataForm.FLT_TXTMULTI) {
						tf.setMinLines(4);
					}
					tf.setWrappable(true);
				}
				items[i] = tf;

				// get the images for media types
				if (fld.media != null) {
					UILabel objLabel = new UILabel(UICanvas
							.getUIImage("/icons/warning.png"));
					objLabel.setAnchorPoint(Graphics.HCENTER);
					mainPanel.addItem(objLabel);
					this.retrieveMedia(fld, objLabel);
				}

				continue;
			}
		}

		if (df.instructions != null) {
			for (int i = 0; i < items.length; i++) {
				if (items[i] != null) {
					items[i].setSubmenu(show_instruction);
				}
			}
			si_instructions.setText(df.instructions);
			if (instruction_menu.getItems().contains(si_instructions) == false) instruction_menu
					.append(si_instructions);
			si_instructions
					.setWrappable(true, instruction_menu.getWidth() - 10);
		}

		// add the desc
		this.addDesc();

		/*
		 * Spacer sp = new Spacer(10, 5); sp.setLayout(Item.LAYOUT_NEWLINE_AFTER |
		 * Item.LAYOUT_NEWLINE_BEFORE); append(sp);
		 */

		/* Buttons: should be placed in-line */
		/* show actions. order is CANCEL, [PREV], [NEXT], SUBMIT */
		cmd_submit = new UIButton(rm.getString(ResourceIDs.STR_SUBMIT));
		cmd_cancel = new UIButton(rm.getString(ResourceIDs.STR_CANCEL));
		cmd_prev = new UIButton(rm.getString(ResourceIDs.STR_PREV));
		cmd_next = new UIButton(rm.getString(ResourceIDs.STR_NEXT));

		UIHLayout uhl1 = new UIHLayout(2);
		UIHLayout uhl2 = UIUtils.easyCenterLayout(cmd_cancel, 150);
		boolean addUhl1 = false;
		uhl1.setGroup(false);

		addUhl1 |= insertCommand(DataFormListener.CMD_NEXT, cmd_next, uhl1, 1);
		if (addUhl1 == false) {
			addUhl1 |= insertCommand(DataFormListener.CMD_SUBMIT, cmd_submit,
					uhl1, 1);
		}
		addUhl1 |= insertCommand(DataFormListener.CMD_PREV, cmd_prev, uhl1, 0);

		if (addUhl1) mainPanel.addItem(uhl1);
		mainPanel.addItem(uhl2);

		// it is nice to have the next item selected by default
		//		if (uhl1.contains(cmd_next)) uhl1.setSelectedItem(cmd_next);
		//		else if (uhl1.contains(cmd_submit)) uhl1.setSelectedItem(cmd_submit);
		//		else if (uhl1.contains(cmd_prev)) uhl1.setSelectedItem(cmd_prev);

		//this.setSelectedIndex(0);
		this.setFreezed(false);
		this.askRepaint();
	}

	private void retrieveMedia(Field fld, UILabel label) {
		// get the img data
		int mediaType = Config.IMG_TYPE;
		String cidUri = null;
		String httpUri = null;

		for (int i = 0; i < fld.media.urisTypes.length; i++) {
			Object[] uriType = (Object[]) fld.media.urisTypes[i];
			String uri = (String) uriType[0];
			if (uri.indexOf("cid:") == 0) {
				cidUri = uri;
				mediaType = ((Integer) uriType[1]).intValue();
				break;
			} else if (uri.indexOf("http://") == 0
					|| uri.indexOf("https://") == 0) {
				httpUri = uri;
				mediaType = ((Integer) uriType[1]).intValue();
			}
		}

		if (cidUri != null) cidRetrieve(fld, cidUri, mediaType, label);
		else if (httpUri != null) httpRetrieve(fld, httpUri, mediaType, label);
	}

	private void cidRetrieve(Field fld, String cidUri, int mediaType,
			UILabel label) {
		Iq iq = new Iq(this.dfl.getFrom(), Iq.T_GET);
		Element data = new Element(XMPPClient.NS_BOB, XMPPClient.DATA);
		iq.addElement(data);
		String uri = Utils.replace(cidUri, "cid:", "");
		data.setAttribute("cid", uri);
		CidListener cid = new CidListener(fld, mediaType, label);
		XMPPClient.getInstance().sendIQ(iq, cid);
	}

	private void httpRetrieve(Field fld, String httpUri, int mediaType,
			UILabel label) {
		ContentConnection c = null;
		DataInputStream dis = null;
		byte[] data = null;
		try {
			c = (ContentConnection) Connector.open(httpUri);
			int len = (int) c.getLength();
			dis = c.openDataInputStream();
			if (len > 0) {
				data = new byte[len];
				dis.readFully(data);
			} else {
				int ch;
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				while ((ch = dis.read()) != -1) {
					bos.write(ch);
				}
				data = bos.toByteArray();
			}
		} catch (Exception e) {
			// #mdebug
			//@			Logger.log("In retrieving media2");
			//@			System.out.println(e.getMessage());
			//@			e.printStackTrace();
			// #enddebug
		} finally {
			try {
				if (dis != null) dis.close();
				if (c != null) c.close();
			} catch (Exception e) {
				// #mdebug
				//@				Logger.log("In retrieving media2");
				//@				System.out.println(e.getMessage());
				//@				e.printStackTrace();
				// #enddebug
			}
		}
		if (data != null) showMedia(fld, data, mediaType, label);
	}

	private void showMedia(Field fld, byte[] data, int mediaType, UILabel label) {
		Image img = null;
		if (mediaType == Config.IMG_TYPE) {
			img = Image.createImage(data, 0, data.length);
		} else if (mediaType == Config.AUDIO_TYPE) {
			img = UICanvas.getUIImage("/icons/mic.png");
		}

		// resize media
		int mWidth = fld.media.width;
		int mHeight = fld.media.height;

		// resize to fit screen
		int tempWidth = UICanvas.getInstance().getWidth() - 20;
		if (img.getWidth() >= tempWidth) {
			mHeight = (tempWidth * img.getHeight()) / img.getWidth();
			mWidth = tempWidth;
		}

		if (mWidth > 0 && mHeight > 0 && mWidth != img.getWidth()
				&& mHeight != img.getHeight()) {
			img = UIUtils.imageResize(img, mWidth, mHeight, false);
		}

		label.setImg(img);
		this.askRepaint();
	}

	private boolean insertCommand(int cmd_code, UIButton item, UIHLayout uhl,
			int position) {
		UIButton itemToInsert;
		boolean retVal = false;

		if ((actions & cmd_code) != 0) {
			itemToInsert = item;
			retVal = true;
			if (df.instructions != null) {
				itemToInsert.setSubmenu(show_instruction);
			}
		} else {
			itemToInsert = item;
			retVal = false;
			itemToInsert.setFocusable(false);
			itemToInsert.setFg_color(0xCCCCCC);
			itemToInsert.setButtonColor(0xAAAAAA);
		}
		uhl.insert(itemToInsert, position, 50, UILayout.CONSTRAINT_PERCENTUAL);
		return retVal;
	}

	private void addDesc() {
		for (int i = 0; i < df.fields.size(); i++) {
			DataForm.Field fld = (DataForm.Field) df.fields.elementAt(i);
			if (fld.desc != null) {
				items[i].setSubmenu(desc_menu);
			}
		}
	}

	protected void paint(Graphics g, int w, int h) {
		super.paint(g, w, h);

		// longest textfield handling 
		//		UIItem panelItem = mainPanel.getSelectedItem();
		//		if (panelItem instanceof UITextField) {
		//			Graphics tg = getGraphics();
		//			int labelHeight = panelItem.getHeight(tg);
		//			int availableHeight = UICanvas.getInstance().getClipHeight()
		//					- this.headerLayout.getHeight(tg)
		//					- this.footer.getHeight(tg);
		//			UIMenu itemSubMenu = panelItem.getSubmenu();
		//			if (labelHeight > availableHeight
		//					&& (itemSubMenu == null || itemSubMenu != zoomSubmenu)) {
		//				panelItem.setSubmenu(zoomSubmenu);
		//				// always reset these values when asking a "repaint" within a "paint"
		//				UICanvas ci = UICanvas.getInstance();
		//				g.translate(-g.getTranslateX(), -g.getTranslateY());
		//				g.setClip(0, 0, ci.getWidth(), ci.getHeight() + 1);
		//				this.askRepaint();
		//			}
		//		}
	}

	/**
	 * Command handler
	 */
	public void menuAction(UIMenu menu, UIItem cmd) {
		int comm = -1;
		boolean setWaiting = false;
		boolean checkRequired = false;

		if (cmd == cmd_cancel || cmd == menu_cancel) {
			comm = DataFormListener.CMD_CANCEL;
		} else if (cmd == cmd_submit) {
			comm = DataFormListener.CMD_SUBMIT;
			setWaiting = true;
			checkRequired = true;
		} else if (cmd == cmd_next) {
			comm = DataFormListener.CMD_NEXT;
			setWaiting = true;
			checkRequired = true;
		} else if (cmd == cmd_prev) {
			comm = DataFormListener.CMD_PREV;
			setWaiting = true;
		} else if (cmd == cmd_delay) {
			comm = DataFormListener.CMD_DELAY;
			setWaiting = true;
		} else if (cmd == this.zoomLabel) {
			UITextField selLabel = (UITextField) this.getSelectedItem();
			selLabel.handleScreen();
		} else if (cmd == show_desc_label) {
			openDescription(this, df);
		} else if (cmd == show_instruction_label) {
			/* show/hide instructions */
			this.addPopup(this.instruction_menu);
			return;
		}

		if (comm == -1) {
			/* ???? */
			return;
		}

		int missingField = fillForm(checkRequired);
		if (missingField >= 0) {
			this.mainPanel.setSelectedIndex(missingField);
			UILabel label = new UILabel(rm
					.getString(ResourceIDs.STR_MISSING_FIELD));
			label.setWrappable(true, UICanvas.getInstance().getWidth() - 60);
			UIMenu missingMenu = UIUtils.easyMenu(rm
					.getString(ResourceIDs.STR_WARNING), 30, 30, UICanvas
					.getInstance().getWidth() - 60, label, rm
					.getString(ResourceIDs.STR_CANCEL), rm
					.getString(ResourceIDs.STR_SELECT));
			this.addPopup(missingMenu);
			return;
		}
		// if the dataform will have an answer, e.g. an IQ contained dataform
		// #ifndef BLUENDO_SECURE
		setWaiting &= dfl.execute(comm);
		// #endif
		RosterScreen.getInstance()._handleTask(dfl);
		// #ifdef UI
		if (setWaiting == true) {
			mainPanel.removeAllItems();
			progress_gauge = new UIGauge(rm.getString(ResourceIDs.STR_WAIT),
					false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING);
			mainPanel.addItem(progress_gauge);
			progress_gauge.start();
			RosterScreen.getInstance().setWaitingDF(this);
			this.askRepaint();
		} else {
			this.stopWaiting();
		}
		// #endif
	}

	/**
	 * 
	 */
	public static void openDescription(UIScreen dataScreen, DataForm df) {
		int index = 0;

		if (dataScreen instanceof DataFormScreen) index = ((DataFormScreen) dataScreen).mainPanel
				.getSelectedIndex();
		else if (dataScreen instanceof DataResultScreen) index = ((DataResultScreen) dataScreen).mainPanel
				.getSelectedIndex();

		String desc = ((DataForm.Field) df.fields.elementAt(index)).desc;
		UITextField descField = new UITextField("", desc, desc.length(),
				TextField.UNEDITABLE);
		descField.setWrappable(true);
		UIMenu descriptionMenu = UIUtils.easyMenu(rm
				.getString(ResourceIDs.STR_DESC), 10, 20,
				dataScreen.getWidth() - 20, descField);
		//descPanel.setMaxHeight(UICanvas.getInstance().getClipHeight() / 2);

		descriptionMenu.cancelMenuString = "";
		descriptionMenu.selectMenuString = rm.getString(ResourceIDs.STR_CLOSE)
				.toUpperCase();
		descriptionMenu.setSelectedIndex(1);
		dataScreen.addPopup(descriptionMenu);
		descField.expand();
	}

	public boolean keyPressed(int kc) {
		if (super.keyPressed(kc)) return true;

		return RosterScreen.makeRoll(kc, this);
	}

	/**
	 * Command handler for on-screen buttons
	 */
	public void itemAction(UIItem cmd) {
		menuAction(null, cmd);
	}

	/**
	 * Called when submit is pressed
	 * @param checkRequired 
	 * 	used to indicated that the check for required fields must be accomplished
	 * @return 
	 * 	if greater or equal to zero the indicates the index of the empty field
	 */
	private int fillForm(boolean checkRequired) {
		int missingField = -1;
		// XXX: here we could verify the required fields
		for (int i = 0; i < df.fields.size(); i++) {
			DataForm.Field fld = (DataForm.Field) df.fields.elementAt(i);
			if (fld.type == DataForm.FLT_HIDDEN) {
				continue;
			}

			// if need check and field is required and the field
			// is empty then the form is not completely filled
			if (checkRequired && fld.required) {
				if (items[i] instanceof UITextField) {
					UITextField tf = (UITextField) items[i];
					String text = tf.getText();
					if (text == null || text.length() == 0) missingField = i;
				} else if (items[i] instanceof UICombobox) {
					UICombobox cf = (UICombobox) items[i];
					int selIndex = cf.getSelectedIndex();
					if (selIndex < 0) missingField = i;
				}
			}

			if (fld.type == DataForm.FLT_BOOLEAN) {
				UICheckbox cgrp = (UICheckbox) items[i];
				fld.dValue = (cgrp.isChecked() ? "true" : "false");
				continue;
			}

			if (fld.type == DataForm.FLT_LISTMULTI
					|| fld.type == DataForm.FLT_LISTSINGLE) {
				UICombobox cmb = (UICombobox) items[i];
				boolean[] flags = cmb.getSelectedFlags();
				StringBuffer dtext = new StringBuffer();
				int scount = 0;
				for (int j = 0; j < flags.length; j++) {
					if (flags[j]) {
						scount++;
						String[] opt = (String[]) fld.options.elementAt(j);
						if (scount > 1) {
							dtext.append("\n");
						}
						dtext.append(opt[0]);
					}
				}
				if (scount == 0) {
					fld.dValue = "";
				} else {
					fld.dValue = dtext.toString();
				}
				continue;
			}

			if (fld.type == DataForm.FLT_JIDSINGLE
					|| fld.type == DataForm.FLT_TXTPRIV
					|| fld.type == DataForm.FLT_TXTSINGLE
					|| fld.type == DataForm.FLT_JIDMULTI
					|| fld.type == DataForm.FLT_TXTMULTI
					|| fld.type == DataForm.FLT_FIXED) {
				UITextField tf = (UITextField) items[i];
				fld.dValue = tf.getText();
				continue;
			}
		}
		return missingField;
	}

	public void stopWaiting() {
		if (progress_gauge != null) progress_gauge.cancel();
		UICanvas.getInstance().close(this);
	}
}
