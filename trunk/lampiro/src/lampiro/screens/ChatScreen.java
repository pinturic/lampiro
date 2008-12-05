/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: ChatScreen.java 1017 2008-11-28 21:57:46Z luca $
*/

package lampiro.screens;

import it.yup.ui.UICanvas;
import it.yup.ui.UIConfig;
import it.yup.ui.UIEmoLabel;
import it.yup.ui.UIHLayout;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UILayout;
import it.yup.ui.UIMenu;
import it.yup.ui.UIPanel;
import it.yup.ui.UIScreen;
import it.yup.ui.UISeparator;
import it.yup.ui.UITextField;
import it.yup.ui.UITextPanel;
import it.yup.util.Logger;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;
import it.yup.util.Utils;
import it.yup.xmlstream.Element;
import it.yup.xmlstream.EventQuery;
import it.yup.xmlstream.EventQueryRegistration;
import it.yup.xmlstream.PacketListener;
import it.yup.xmpp.Config;
import it.yup.xmpp.Contact;
import it.yup.xmpp.MUC;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Presence;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

import lampiro.LampiroMidlet;

public class ChatScreen extends UIScreen implements PacketListener,
		CommandListener {

	class UICutLabel extends UIEmoLabel {

		private String completeText = "";

		public UICutLabel(String newText, String completeText) {
			super(newText);
			this.completeText = completeText;
			// TODO Auto-generated constructor stub
		}

		protected void paint(Graphics g, int w, int h) {
			super.paint(g, w, h);
			g.setColor(0x555555);
			Font currentFont = this.getFont();
			if (currentFont == null) currentFont = UIConfig.font_body;
			String moreString = " " + rm.getString(ResourceIDs.STR_MORE) + " ";
			int moreWidth = currentFont.stringWidth(moreString);
			g.fillRect(w - moreWidth - 2, h - currentFont.getHeight() - 2,
					moreWidth + 1, currentFont.getHeight() + 1);
			g.setColor(0xFFFFFF);
			g
					.drawString(moreString, w - moreWidth - 1, h
							- currentFont.getHeight() - 1, Graphics.TOP
							| Graphics.LEFT);
		}

	}

	static ResourceManager rm = ResourceManager.getManager("common", "en");

	/*
	 * The area in which the screen can paint (hence excluding
	 * headers footers and title screen)
	 */
	protected int printableHeight = -1;

	/*
	 * The total area screen. Since it may vary during execution 
	 * its value is saved here
	 */
	protected int screenHeight = -1;
	/*
	 * The header used to show stats and advice;
	 */
	protected UIHLayout header;

	/*
	 * The user associated to this UIscreen
	 * 
	 */
	protected Contact user;
	UILabel cmd_exit;
	private UILabel cmd_write;
	private UILabel cmd_clear;

	/*
	 * The menu used by UIlabels in chatscreen to close the screen
	 */
	UIMenu closeMenu;
	/*
	 * The UIlabel used by UIlabels in chatscreen to close the screen
	 */
	private UILabel closeLabel;

	// #mdebug
//@		 private UILabel cmd_debug = new UILabel("Debug");
	// #enddebug

	private Hashtable cmd_urls = new Hashtable();

	// XXX add a global handler for icons
	protected static Image img_msg;

	// the panel containing headers and labels
	protected UIPanel chatPanel;

	// private Image buffer;

	// /** number of currently displayed entries */
	// private int displayed_entries;

	// private boolean _entries_scrolled = false;

	static int scroll_color = 0x444444;

	private EventQueryRegistration reg = null;

	/** wrapped conversation cache */
	static Hashtable conversations = new Hashtable();
	private Vector current_conversation = null;
	private Config cfg;
	UIMenu zoomSubmenu = new UIMenu("");
	UILabel zoomLabel = new UILabel("EXPAND");

	public String resource;

	static {
		try {
			img_msg = Image.createImage("/icons/message.png");
		} catch (IOException e) {
			img_msg = Image.createImage(16, 16);
		}
	}

	public ChatScreen(Contact u) {
		super();

		// lots of insertion and deletion ...
		//this.setFreezed(true);

		cfg = Config.getInstance();
		// prepare closeMenu
		closeMenu = new UIMenu("");
		closeLabel = new UILabel(rm.getString(ResourceIDs.STR_CLOSE)
				.toUpperCase());
		closeMenu.append(closeLabel);

		user = u;
		// buffer = null;

		cmd_exit = new UILabel(rm.getString(ResourceIDs.STR_CLOSE));
		cmd_write = new UILabel(rm.getString(ResourceIDs.STR_WRITE));
		cmd_clear = new UILabel(rm.getString(ResourceIDs.STR_CLEAR_HIST));

		setMenu(new UIMenu(""));
		UIMenu menu = getMenu();
		// #debug
//@				 menu.append(cmd_debug);
		menu.append(cmd_exit);
		menu.append(cmd_write);
		menu.append(cmd_clear);

		setTitle(rm.getString(ResourceIDs.STR_CHAT_WITH) + " "
				+ user.getPrintableName());

		current_conversation = (Vector) conversations.get(user.jid);
		if (current_conversation == null) {
			current_conversation = new Vector();
			conversations.put(user.jid, current_conversation);
		}
		/*
		 * XXX: hack, create an item and select it, the item won't relinquish
		 * focus
		 */

		XMPPClient client = XMPPClient.getInstance();
		chatPanel = new UIPanel();
		chatPanel.setMaxHeight(-1);
		chatPanel.setFocusable(true);
		// the panel has a contestual menu that let close the the screen
		// as well as the uilabels used to print the chat lines
		chatPanel.setSubmenu(this.closeMenu);
		Image img = client.getPresenceIcon(user.getAvailability());
		header = new UIHLayout(2);
		header.setGroup(false);
		String status = null;
		Presence[] allPresences = user.getAllPresences();
		// could even be null if the user is offline now
		if (allPresences != null) {
			status = allPresences[0].getStatus();
		}
		if (status == null || status.length() == 0) {
			status = user.getPrintableName();
		}
		header.insert(new UILabel(status), 0, 50,
				UILayout.CONSTRAINT_PERCENTUAL);
		UILabel headerImg = new UILabel(img);
		header.insert(headerImg, 1, img.getWidth() + 2,
				UILayout.CONSTRAINT_PIXELS);
		header.setFocusable(false);
		this.append(header);
		UISeparator sep = new UISeparator(2);
		sep.setBg_color(0xCCCCCC);
		this.append(sep);
		this.append(chatPanel);
		this.setSelectedIndex(2);

		// to compute the printableHeight the currently visualizd screen must be 
		// used and not this one since it may be invisible
		getPrintableHeight(UICanvas.getInstance().getCurrentScreen()
				.getGraphics(), this.height);

		for (int j = 0; j < current_conversation.size(); j++) {
			ConversationEntry entry = (ConversationEntry) current_conversation
					.elementAt(j);
			this.updateLabel(entry);
		}
		if (chatPanel.getItems().size() > 0) {
			// remember the separator
			chatPanel.setSelectedIndex(chatPanel.getItems().size() - 2);
			chatPanel.setDirty(true);
			this.askRepaint();
		}

		// prepare zoomSubMenu
		zoomSubmenu.append(this.zoomLabel);
		zoomLabel.setAnchorPoint(Graphics.HCENTER);
		zoomSubmenu.setAbsoluteX(10);
		zoomSubmenu.setAbsoluteY(10);
		zoomSubmenu.setWidth(this.getWidth() - 30);

		// listen for all incoming messages with bodies
		EventQuery q = new EventQuery("message", null, null);
		q.child = new EventQuery("body", null, null);
		if (reg == null) {
			reg = XMPPClient.getInstance().registerListener(q, this);
		}

		// so to reset the status in the roster
		// it must be locked because it interferes 
		// with the items positioning in the roster screen!!!
		// hence all these operations must be done alltogether
		try {
			UICanvas.lock();
			updateConversation();
			RosterScreen roster = RosterScreen.getInstance();
			roster.reorganizeContact(user, Contact.CH_MESSAGE_READ);
		} finally {
			UICanvas.unlock();
		}

		this.askRepaint();

	}

	protected void paint(Graphics g, int w, int h) {
		this.updateConversation();
		RosterScreen.getInstance().reorganizeContact(user,
				Contact.CH_MESSAGE_READ);
		super.paint(g, w, h);
	}

	protected void getPrintableHeight(Graphics g, int h) {
		int maxHeight = h - 10;
		maxHeight -= this.header.getHeight(g);
		maxHeight -= this.headerLayout.getHeight(g);
		maxHeight -= this.footer.getHeight(g);
		this.printableHeight = maxHeight;
		this.screenHeight = h;
	}

	/**
	 * 
	 * @param screen_width
	 * @return true if new messages have been added
	 */
	protected boolean updateConversation() {
		Vector messages = user.getMessageHistory();
		user.unread_msg = false;
		if (messages == null) { return false; }
		for (int i = 0; i < messages.size(); i++) {
			String msg[] = (String[]) messages.elementAt(i);
			checkUrls(msg[1]);
			ConversationEntry entry = wrapMessage(msg);
			updateLabel(entry);
			current_conversation.addElement(entry);
		}
		user.resetMessageHistory();
		// must be done here after repaint to be sure all the panel
		// has been updated
		this.chatPanel.setSelectedIndex(this.chatPanel.getItems().size() - 2);
		return true;
	}

	/**
	 * @param entry
	 */
	private void updateLabel(ConversationEntry entry) {
		String s = (String) entry.text;
		s = getLabelHeader(entry) + s;
		UIEmoLabel uel = new UIEmoLabel(s);
		uel.setWrappable(true, this.width - 10);
		uel.setFocusable(true);

		int newBgColor = -1;
		if (entry.type == ConversationEntry.ENTRY_TO) {
			uel.setAnchorPoint(Graphics.RIGHT);
		} else {
			uel.setAnchorPoint(Graphics.LEFT);
			newBgColor = 0;
			int[] rgb = new int[] { UIConfig.bg_color, UIConfig.bg_color,
					UIConfig.bg_color };
			for (int i = 0; i < 3; i++) {
				rgb[i] &= (0xFF0000 >> (i * 8));
				rgb[i] *= 9;
				rgb[i] /= 10;
				rgb[i] &= (0xFF0000 >> (i * 8));
				newBgColor += rgb[i];
			}
			uel.setBg_color(newBgColor);
		}
		this.chatPanel.addItem(uel);
		uel.setSubmenu(this.closeMenu);
		this.checkSize(uel);
		UISeparator sep = new UISeparator(1);
		sep.setBg_color(0xCCCCCC);
		this.chatPanel.addItem(sep);

		// empty oldMessages
		if (cfg != null) {
			int hs = 2 * Short.parseShort(cfg.getProperty(Config.HISTORY_SIZE,
					"30"));
			while (this.chatPanel.getItems().size() > hs) {
				this.chatPanel.removeItemAt(0);
				this.chatPanel.removeItemAt(0);
			}
		}
	}

	/*
	 * Checks if the current label must be cut in order to fit the screen
	 * 
	 * @param uel the current label that is inserted now
	 */
	private void checkSize(UIEmoLabel uel) {
		//UIScreen currentScreen = UICanvas.getInstance().getCurrentScreen();
		//if (currentScreen == null) return;

		// first try to use the the Chatscreen
		// otherwise the currentScreen just in case
		// i am not shown (composing or other screen has focus)
		UIScreen currentScreen = this;
		Graphics tempGraphics = currentScreen.getGraphics();
		if (tempGraphics == null) {
			currentScreen = UICanvas.getInstance().getCurrentScreen();
			if (currentScreen == null) return;
			tempGraphics = currentScreen.getGraphics();
		}

		Font oldFont = tempGraphics.getFont();
		tempGraphics.setFont(UIConfig.font_body);
		int labelHeight = uel.getHeight(tempGraphics);
		//int panelHeight = this.chatPanel.getHeight(tempGraphics);
		UIMenu itemSubMenu = uel.getSubmenu();
		if (labelHeight > this.printableHeight
				&& (itemSubMenu == null || itemSubMenu != zoomSubmenu)) {
			String oldText = uel.getText();
			Vector oldTextLines = uel.getTextLines();
			int lineHeight = labelHeight / oldTextLines.size();
			oldTextLines.setSize(printableHeight / lineHeight - 1);
			String newText = "";
			Enumeration en = oldTextLines.elements();
			while (en.hasMoreElements()) {
				newText += (en.nextElement() + "\n");
			}
			newText = newText.substring(0, newText.length() - 1);
			UICutLabel uicl = new UICutLabel(newText, oldText);
			uicl.setWrappable(true, this.width - 10);
			uicl.setSubmenu(zoomSubmenu);
			uicl.setTextLines(null);
			int index = this.chatPanel.getItems().indexOf(uel);
			this.chatPanel.insertItemAt(uicl, index);
			this.chatPanel.removeItem(uel);
		}
		uel.setTextLines(null);
		tempGraphics.setFont(oldFont);
	}

	String getLabelHeader(ConversationEntry entry) {
		String retString = "";
		if (entry.arriveTime.length() > 0) {
			retString = "[" + entry.arriveTime + "] ";
		}
		return retString;
	}

	public void showNotify() {
		// reset the status img
		Image img = XMPPClient.getInstance().getPresenceIcon(
				user.getAvailability());
		if (img != ((UILabel) this.header.getItem(1)).getImg()) {
			((UILabel) this.header.getItem(1)).setImg(img);
			((UILabel) this.header.getItem(1)).setDirty(true);
			this.askRepaint();
		}
	}

	private void checkUrls(String text) {
		// parse the urls and add to the command menu
		Enumeration en = Utils.find_urls(text).elements();
		while (en.hasMoreElements()) {
			String url = (String) en.nextElement();
			if (!cmd_urls.containsKey(url)) {
				UILabel cmd = new UILabel(url);
				cmd_urls.put(url, cmd);
				UIMenu menu = getMenu();
				menu.append(cmd);
			}
		}
	}

	/**
	 * Wrap a message so that it fits the windows
	 * 
	 * @param
	 * @param screen_width
	 * 
	 * @return
	 */
	ConversationEntry wrapMessage(String text[]) {

		// #ifdef TIMING
		// @ long t1 = System.currentTimeMillis();
		// #endif

		byte type = user.jid.equals(text[0]) ? ConversationEntry.ENTRY_TO
				: ConversationEntry.ENTRY_FROM;

		// #ifdef TIMING
		// @ System.out.println("wrap conv: " + (System.currentTimeMillis() -
		// @ // t1));
		// #endif

		ConversationEntry convEntry = new ConversationEntry(text[1], type);
		if (text[3] != null) convEntry.arriveTime = text[3];
		return convEntry;
	}

	public void menuAction(UIMenu menu, UIItem cmd) {
		if (cmd == cmd_exit || cmd == this.closeLabel) {
			// so that the user preferred resource is reset
			user.lastResource = null;
			Hashtable chatScreenList = RosterScreen.getChatScreenList();
			chatScreenList.remove(this.user);
			// reset the status in the roster
			RosterScreen roster = RosterScreen.getInstance();
			roster.updateContact(user, Contact.CH_MESSAGE_READ);
			if (reg != null) {
				reg.remove();
				reg = null;
			}
			UICanvas.getInstance().close(this);
		} else if (cmd == cmd_write) {
			SimpleComposerScreen cs = null;
			if (user instanceof MUC == true) cs = new MUCComposer((MUC) user);
			else
				cs = new SimpleComposerScreen(user);
			UICanvas.display(cs);
		} else if (cmd == cmd_clear) {
			current_conversation.removeAllElements();
			Enumeration en = cmd_urls.elements();
			UIMenu mn = getMenu();
			while (en.hasMoreElements()) {
				mn.remove((UILabel) en.nextElement());
			}
			this.setFreezed(true);
			this.chatPanel.removeAllItems();
			this.setFreezed(false);
			cmd_urls.clear();
			this.setDirty(true);
			// #mdebug
//@						 } else if (cmd == cmd_debug) {
//@						 Logger.log(
//@						
//@						 "h:" + UICanvas.getInstance().getHeight() + "w:"
//@						 + UICanvas.getInstance().getWidth() + "ch:");
//@						 Logger.log(this.getGraphics().getClipHeight() + "cw:"
//@						 + this.getGraphics().getClipWidth() + "ph:"
//@						 + this.chatPanel.getHeight(getGraphics()));
//@						 //
//@						 DebugScreen debugScreen = new DebugScreen();
//@						 UICanvas.getInstance().open(debugScreen, true);
			// #enddebug
		} else if (cmd == this.zoomLabel) {
			UICutLabel selLabel = (UICutLabel) this.chatPanel.getSelectedItem();
			String selText = selLabel.completeText;
			/*TextBox tb = new TextBox("Expanded", selText, selText.length(),
					TextField.ANY);
			/** ok command for the TextBox */
			/*
			Command cmd_ok = new Command("OK", Command.CANCEL, 1);
			tb.addCommand(cmd_ok);
			tb.setCommandListener(this);
			UICanvas.display(tb);*/

			UIMenu zoomedMenu = new UIMenu("Expanded");
			UITextField expField = new UITextField("", selText, selText
					.length(), TextField.UNEDITABLE);
			expField.setWrappable(true);
			zoomedMenu.append(expField);
			zoomedMenu.setAbsoluteY(20);
			zoomedMenu.setAbsoluteX(10);
			zoomedMenu.setWidth(this.getWidth() - 20);
			zoomedMenu.cancelMenuString = "";
			zoomedMenu.selectMenuString = rm.getString(ResourceIDs.STR_CLOSE)
					.toUpperCase();
			zoomedMenu.setSelectedIndex(1);
			this.addPopup(zoomedMenu);
			expField.expand();
		} else {
			if (this.cmd_urls.contains(cmd)) {
				String url = ((UILabel) cmd).getText();

				try {
					LampiroMidlet._lampiro.platformRequest(url);
				} catch (ConnectionNotFoundException e) {
					UICanvas.showAlert(AlertType.ERROR, "URL Error",
							"Can't open URL:" + e.getMessage());
				}
			}
		}
	}

	boolean isPrintable(int key) {
		int keyNum = -1;
		switch (key) {
			case Canvas.KEY_NUM0:
			case Canvas.KEY_NUM1:
			case Canvas.KEY_NUM2:
			case Canvas.KEY_NUM3:
			case Canvas.KEY_NUM4:
			case Canvas.KEY_NUM5:
			case Canvas.KEY_NUM6:
			case Canvas.KEY_NUM7:
			case Canvas.KEY_NUM8:
			case Canvas.KEY_NUM9:
				keyNum = key;
		}
		if (keyNum == -1
				&& UICanvas.getInstance().getGameAction(key) != Canvas.FIRE) { return false; }
		return true;
	}

	/**
	 * Handle key events
	 * 
	 * @param kc
	 *            the pressed key
	 */
	public boolean keyPressed(int kc) {
		if (this.popupList.size() == 0
				&& this.getMenu().isOpenedState() == false) {
			int ga = UICanvas.getInstance().getGameAction(kc);

			boolean ip = isPrintable(kc);

			if (ip || ga == Canvas.FIRE) {
				if (this.getSelectedItem() == this.chatPanel) {
					SimpleComposerScreen cs = null;
					if (user instanceof MUC == true) cs = new MUCComposer(
							(MUC) user);
					else
						cs = new SimpleComposerScreen(user);
					UICanvas.display(cs);
					return true;
				}
			}

			switch (ga) {
				case Canvas.RIGHT: {
					RosterScreen roster = RosterScreen.getInstance();
					roster.updateContact(user, Contact.CH_MESSAGE_READ);
					RosterScreen.showNextScreen(this);
					return true;
				}
				case Canvas.LEFT: {
					RosterScreen roster = RosterScreen.getInstance();
					roster.updateContact(user, Contact.CH_MESSAGE_READ);
					RosterScreen.showPreviousScreen(this);
					return true;
				}
			}
		}
		return super.keyPressed(kc);
	}

	public void packetReceived(Element e) {
		// avoid useless repaint when computing conversation
		this.setFreezed(true);
		// check if it is a msg for myself so the img_msg is not shown
		// and avoid carotti/fast bot problem
		String fullJid = user.getFullJid();
		// fullJid could be null for offline contact
		// so let's use in that case the userhost and nothing more
		if (fullJid == null) fullJid = user.jid;
		String userHost = Contact.userhost(fullJid);
		boolean myPacket = Contact.userhost(e.getAttribute(Iq.ATT_FROM))
				.equals(userHost);
		if (myPacket && user.getHistoryLength() > 0
				&& this == UICanvas.getInstance().getCurrentScreen()) {
			boolean updated = false;
			try {
				UICanvas.lock();
				updated = updateConversation();
				RosterScreen.getInstance().reorganizeContact(user,
						Contact.CH_MESSAGE_READ);
			} finally {
				UICanvas.unlock();
			}

			if (updated == false) {
				this.setFreezed(false);
				return;
			}
		} else if (myPacket == false) {
			((UILabel) this.header.getItem(1)).setImg(img_msg);
			/*((UILabel) this.header.getItem(1)).setDirty(true);
			this.askRepaint();*/
		}
		this.setFreezed(false);
		askRepaint();

	}

	/**
	 * Entry for a conversation
	 */
	static class ConversationEntry {
		/** message from */
		public static final byte ENTRY_FROM = 0;
		/** message to */
		public static final byte ENTRY_TO = 1;

		/** previous message wrap XXX ? */
		public static final byte ENTRY_ERROR = 2;

		/** message type in / on */
		public byte type;

		/** the message itself */
		public String text;

		/** first line of the entry that is displayed */
		public int entry_offset = 0;
		public String from = "";
		public String arriveTime = "";

		public ConversationEntry(String text, byte type) {
			this.type = type;
			this.text = text;
		}
	}

	public void commandAction(Command cmd, Displayable disp) {
		UICanvas.display(null);
		this.dirty = true;
		this.askRepaint();

	}
}
