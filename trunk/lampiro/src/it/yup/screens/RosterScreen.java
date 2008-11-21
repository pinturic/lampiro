/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: RosterScreen.java 846 2008-09-11 12:20:05Z luca $
*/

package it.yup.screens;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import lampiro.LampiroMidlet;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;

// #ifdef SCREENSAVER
//@import it.yup.util.Utils;
//@import java.util.TimerTask;
// #endif

import it.yup.xmlstream.Element;
import it.yup.xmpp.Contact;
import it.yup.xmpp.Task;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.IQResultListener;
import it.yup.xmpp.packets.Iq;

public class RosterScreen extends Canvas implements CommandListener {

	private static ResourceManager rm = ResourceManager.getManager("common",
																	"en");

	private Command cmd_send = new Command(rm
			.getString(ResourceIDs.STR_SEND_MESSAGE), Command.EXIT, 1);
	private Command cmd_chat = new Command(rm.getString(ResourceIDs.STR_CHAT),
			Command.ITEM, 2);
	// XXX info delayed 
	// private Command cmd_info  = new Command(rm.getString(ResourceIDs.STR_EDIT_CONTACT), Command.SCREEN, 3);
	private Command cmd_state = new Command(rm
			.getString(ResourceIDs.STR_CHANGE_STATUS), Command.SCREEN, 4);
	private Command cmd_addc = new Command(rm
			.getString(ResourceIDs.STR_ADD_CONTACT), Command.SCREEN, 5);
	private Command cmd_delc = new Command(rm
			.getString(ResourceIDs.STR_DELETE_CONTACT), Command.ITEM, 6);
	// XXX update delayed
	//    private Command cmd_reload = new Command(rm.getString(ResourceIDs.STR_RELOAD_CONTACT), Command.SCREEN, 6);
	private Command cmd_exit = new Command(rm.getString(ResourceIDs.STR_EXIT),
			Command.SCREEN, 99);
	// #debug    
	//@	    private Command cmd_debug = new Command(rm.getString(ResourceIDs.STR_DEBUG), Command.SCREEN, 100);
	private Command cmd_about = new Command(
			rm.getString(ResourceIDs.STR_ABOUT), Command.SCREEN, 98);
	private Command cmd_querycmd = new Command(rm
			.getString(ResourceIDs.STR_QUERYCMD), Command.ITEM, 10);
	private Command cmd_listcmd = new Command(rm
			.getString(ResourceIDs.STR_LISTCMD), Command.ITEM, 11);
	private Command cmd_options = new Command(rm
			.getString(ResourceIDs.STR_OPTIONS_SETUP), Command.SCREEN, 12);
	private Command cmd_tasks = new Command(rm
			.getString(ResourceIDs.STR_PENDINGTASK), Command.ITEM, 1);

	private Vector active_contacts = new Vector();
	private Vector shown_contacts = null;

	private Image img_msg;
	private Image img_cmd;
	private Image img_task;
	private Image img_arrow;
	private Image img_traffic;

	private int paint_calls = 0;

	// #ifdef TIMING    
	//@    private long paint_time = 0;
	// #endif

	/** fount used for the conversation */
	public static Font f_u;

	// drawing constants
	//    /** true when some conversation has pending messages */
	//    private boolean unread_messages = ;

	/** the line from which the roster is drawn */
	private int start_line;

	/** the highlighted line */
	private int selected_line;

	/** the line that will be highligther after a repaint */
	private int new_selected_line;

	private Contact selected_contact = null;

	/** roster line height */
	private int line_height = 0;

	private int display_lines = -1;

	private int width = 0;
	private int height = 0;

	/** when true repaint only the selection */
	private boolean repaint_selection = false;
	private boolean display_contact_info = false;

	/** enabled scroll commands */
	private byte scroll_enable;

	private static final byte SCROLL_DOWN = 1;
	private static final byte SCROLL_UP = 2;

	/** singleton */
	private static RosterScreen _instance;

	private char[][] itu_keys = { { ' ', '0' }, { '1' }, { 'a', 'b', 'c', '�', '2' }, { 'd', 'e', 'f', '�', '�', '3' }, { 'g', 'h', 'i', '�', '4' }, { 'j', 'k', 'l', '5' }, { 'm', 'n', 'o', '�', '6' }, { 'p', 'q', 'r', 's', '7' }, { 't', 'u', 'v', '�', '8' }, { 'w', 'x', 'y', 'z', '9' } };
	private String sel_pattern = "";
	private long sel_last_ts = 0;
	private int sel_last_key = -1;
	private int sel_key_offset = 0;

	// #ifdef SCREENSAVER
	//@    private long last_key_time;    
	//@    private static long SCREENSAVER_DELAY = 10000; 
	//@    private TimerTask screensaver_starter = null;
	// #endif

	/* Style constants (should be moved to Config) */

	private int bg_color = 0xCBDBE3;
	private int fg_color = 0x000000;
	private int line_color = 0xCCCCCC;
	private int scroll_color = 0x444444;
	private int header_bg = 0x5590AD;
	private int header_fg = 0xDDE7EC;

	// #ifdef SCREENSAVER
	//@    class ScreenSaverStarter extends TimerTask {
	//@
	//@		public void run() {
	//@			if(isShown() && (System.currentTimeMillis()- last_key_time)>SCREENSAVER_DELAY) {
	//@				LampiroMidlet.disp.setCurrent(new ScreenSaver(RosterScreen.this));
	//@			}
	//@		}
	//@    } 
	// #endif

	private RosterScreen() {
		addCommand(cmd_addc);
		addCommand(cmd_exit);
		addCommand(cmd_state);
		//        addCommand(cmd_reload);
		// #debug        
		//@		        addCommand(cmd_debug);
		addCommand(cmd_about);
		addCommand(cmd_options);
		setCommandListener(this);
		f_u = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN,
							Font.SIZE_SMALL);

		shown_contacts = active_contacts;
		start_line = 0;
		selected_line = new_selected_line = -1;
		scroll_enable = 0;

		try {
			img_msg = Image.createImage("/icons/message.png");
		} catch (IOException e) {
			img_msg = Image.createImage(16, 16);
		}

		try {
			img_cmd = Image.createImage("/icons/gear.png");
		} catch (IOException e) {
			img_cmd = Image.createImage(16, 16);
		}

		try {
			img_task = Image.createImage("/icons/task.png");
		} catch (IOException e) {
			img_task = Image.createImage(16, 16);
		}

		try {
			img_arrow = Image.createImage("/icons/rightarrow.png");
		} catch (IOException e) {
			img_arrow = Image.createImage(16, 16);
		}

		try {
			img_traffic = Image.createImage("/icons/traffic.png");
		} catch (IOException e) {
			img_traffic = Image.createImage(16, 16);
		}

		if (img_msg.getHeight() > f_u.getHeight()) {
			line_height = img_msg.getHeight() + 2;
		} else {
			line_height = f_u.getHeight() + 2;
		}
	}

	public static RosterScreen getInstance() {
		if (_instance == null) {
			_instance = new RosterScreen();
		}
		return _instance;
	}

	protected void sizeChanged(int w, int h) {
		this.width = w;
		this.height = h;
		display_lines = (h - line_height) / line_height;
		repaint();
	}

	// #ifdef SCREENSAVER    
	//@    protected void showNotify() {
	//@    	last_key_time = System.currentTimeMillis();
	//@    	if(screensaver_starter == null) {
	//@    		screensaver_starter = new ScreenSaverStarter();
	//@    		Utils.tasks.scheduleAtFixedRate(screensaver_starter, SCREENSAVER_DELAY, SCREENSAVER_DELAY);
	//@    	}
	//@	}
	//@    
	//@    protected void hideNotify() {
	//@    	if(screensaver_starter != null) {
	//@    		screensaver_starter.cancel();
	//@    		screensaver_starter = null;
	//@    	}
	//@    }
	// #endif

	/**
	 * Repaint the component
	 * @param g the used graphic context
	 */
	protected synchronized void paint(Graphics g) {

		// #ifdef TIMING
		//@    	long t1 = System.currentTimeMillis();
		// #endif

		// #ifdef SEND_DEBUG1
		//@    	boolean flag = repaint_selection;
		//@    	long t1 = System.currentTimeMillis();
		// #endif

		paint_calls++;

		if (display_lines < 0) {
			sizeChanged(g.getClipWidth(), g.getClipHeight());
		}

		if (repaint_selection) {
			paintHeader(g);
			paintSelection(g);
			repaint_selection = false;
		} else {
			// external event, redraw all
			g.setColor(bg_color);
			g.fillRect(0, 0, width, height);
			g.setFont(f_u);
			paintRoster(g);
			paintHeader(g);
		}

		selected_line = new_selected_line;

		toggleMenus();

		// #ifdef TIMING 
		//@    	paint_time += (System.currentTimeMillis() - t1);    	
		//@    	System.out.println("Roster: " + (System.currentTimeMillis() - t1));
		// #endif

		// #ifdef SEND_DEBUG1
		//@        XMPPClient.getInstance();
		//@    	XMPPClient.getInstance().sendDebug("repaint: " + flag +  " " + (System.currentTimeMillis() - t1));
		// #endif

	}

	//    private void paintContactInfo(Graphics g) {
	//    	g.setColor(bg_color);
	//    	Contact c = selected_contact;
	//    }

	/**
	 * Paint only the selection icon 
	 * @param g
	 */
	private void paintSelection(Graphics g) {
		g.setColor(bg_color);
		int hpos;
		if (selected_line != -1) {
			Contact c;
			if (selected_line != new_selected_line) {
				c = (Contact) shown_contacts.elementAt(selected_line);
			} else {
				c = selected_contact;
			}
			Image img = XMPPClient.getInstance()
					.getPresenceIcon(c.getAvailability());
			hpos = (selected_line - start_line + 1) * line_height;
			g.fillRect(0, hpos + 1, 16, 16);
			g.drawImage(img, 0, hpos + 1, Graphics.TOP | Graphics.LEFT);
		}
		hpos = (new_selected_line - start_line + 1) * line_height;
		g.fillRect(0, hpos + 1, 16, 16);
		g.drawImage(img_arrow, 0, hpos + 1, Graphics.TOP | Graphics.LEFT);
	}

	private void paintRoster(Graphics g) {
		XMPPClient xmpp = XMPPClient.getInstance();

		setTitle(rm.getString(ResourceIDs.STR_ROSTER_TITLE) + "("
				+ active_contacts.size() + ")");

		int hpos = line_height;

		g.setColor(fg_color);

		for (int i = start_line; (i < start_line + display_lines)
				&& (i < shown_contacts.size()); i++) {

			Contact u = (Contact) shown_contacts.elementAt(i);
			String uname = u.getPrintableName();

			Image pimg = xmpp.getPresenceIcon(u.getAvailability());
			Image cimg = null;
			if (u.cmdlist != null) {
				cimg = img_cmd;
			}
			if (u.pending_tasks) {
				cimg = img_task;
			} else if (u.unread_msg) {
				cimg = img_msg;
			} else if (u.cmdlist != null) {
				cimg = img_cmd;
			}

			if (new_selected_line == i) {
				pimg = img_arrow;
			}

			g.setColor(line_color);
			g.drawLine(0, hpos, width, hpos);
			g.drawImage(pimg, 0, hpos + 1, Graphics.TOP | Graphics.LEFT);
			int offset = 17;
			if (cimg != null) {
				g.drawImage(cimg, offset, hpos + 1, Graphics.TOP
						| Graphics.LEFT);
				offset = 34;
			}
			g.setColor(fg_color);
			g.drawString(uname, offset, hpos + 1, Graphics.TOP | Graphics.LEFT);

			hpos += line_height;
		}

		// there is something above
		scroll_enable = 0;
		if (start_line > 0) {
			// g.setColor(0x222222);
			// g.drawRect(width-13, 2, 13, 13);
			g.setColor(scroll_color);
			g.fillTriangle(width - 13, 13 + line_height, width - 2,
							13 + line_height, width - 8, 4 + line_height);
			scroll_enable |= SCROLL_UP;
		}
		if (start_line + display_lines < shown_contacts.size()) {
			// g.setColor(0x00222222);
			// g.drawRect(width-13, height-15, 13, 13);
			g.setColor(scroll_color);
			g.fillTriangle(width - 13, height - 14, width - 2, height - 14,
							width - 8, height - 4);
			scroll_enable |= SCROLL_DOWN;
		}

		//setTitle("" + line_height + ", " + hpos + ", " + display_lines + ", " + shown_contacts.size() + ", " + test);
	}

	private void toggleMenus() {
		// add or remove commands only if there is a selected user
		Contact c = selected_contact;
		removeCommand(cmd_querycmd);
		removeCommand(cmd_listcmd);
		removeCommand(cmd_tasks);
		if (c == null) {
			removeCommand(cmd_delc);
			removeCommand(cmd_send);
			// removeCommand(cmd_info);
			removeCommand(cmd_chat);
		} else {
			addCommand(cmd_delc);
			addCommand(cmd_send);
			// addCommand(cmd_info);
			addCommand(cmd_chat);
			/* requested command list, has commands */
			if (c.cmdlist != null) {
				addCommand(cmd_listcmd);
			} else {
				addCommand(cmd_querycmd);
			}
			if (c.pending_tasks) {
				addCommand(cmd_tasks);
			}
		}
	}

	private void paintHeader(Graphics g) {

		int w = g.getClipWidth();

		g.setColor(header_bg);
		g.fillRect(0, 0, width, line_height);
		g.setColor(header_fg);
		XMPPClient client = XMPPClient.getInstance();

		int bytes[] = XMPPClient.getTraffic();

		// #ifndef TIMING
		g.drawImage(img_traffic, 0, 0, Graphics.TOP | Graphics.LEFT);

		if (sel_pattern.length() > 0) {
			g.drawString("sel: " + sel_pattern, 17, 0, Graphics.TOP
					| Graphics.LEFT);
		} else {
			g.drawString(bytes[0] + "/" + bytes[1], 17, 0, Graphics.TOP
					| Graphics.LEFT);
		}

		Image pimg = client.getPresenceIcon(client.getMyContact()
				.getAvailability());
		g.drawImage(pimg, w, 0, Graphics.TOP | Graphics.RIGHT);
		// contacts with unread messages are always at the top
		if (active_contacts.size() > 0
				&& ((Contact) active_contacts.elementAt(0)).unread_msg) {
			g.drawImage(img_msg, w - 16, 0, Graphics.TOP | Graphics.RIGHT);
		}

		// #endif

		// #ifdef TIMING    	
		//@    	Runtime rt = Runtime.getRuntime().getRuntime();
		//@    	g.drawString("" + paint_time + "/" + paint_calls + "-" + rt.freeMemory() + "/" + rt.totalMemory(), g.getClipWidth() / 2, 0, Graphics.TOP | Graphics.HCENTER);
		// #endif

	}

	private void filter_contacts(boolean reuse) {
		if (!reuse) {
			shown_contacts = active_contacts;
		}
		Vector new_contacts = new Vector();
		Enumeration en = shown_contacts.elements();
		while (en.hasMoreElements()) {
			Contact c = (Contact) en.nextElement();

			if (c.getPrintableName().toLowerCase().indexOf(sel_pattern) == 0) {
				new_contacts.addElement(c);
			}
		}
		shown_contacts = new_contacts;
	}

	/**
	 * Handle key events
	 * @param kc the pressed key
	 */
	protected synchronized void keyPressed(int kc) {

		// #ifdef SCREENSAVER
		//@        last_key_time = System.currentTimeMillis();
		// #endif

		//        switch(kc) {
		//        	// case Canvas.UP:
		//            case Canvas.KEY_NUM2: {
		//            	if( (scroll_enable & SCROLL_UP) == 0 ) {
		//            		// no scrolling
		//            		return;
		//            	}
		//            	if(start_line>0) {
		//            		start_line -= display_lines/2;
		//            		if(start_line<0) start_line = 0;
		//            		repaint();
		//            	}
		//                return;
		//            }
		//            case Canvas.KEY_NUM5: {
		//            	// XXX is this useful?
		//                selected_line = display_lines -1;
		//                // removeCommand(cmd_info);
		//                removeCommand(cmd_send);
		//                removeCommand(cmd_chat);
		//                return;
		//            }
		//            // case Canvas.DOWN:
		//            case Canvas.KEY_NUM8: {
		//                if( (scroll_enable & SCROLL_DOWN) == 0) {
		//                	return;
		//                }
		//                int contact_size = shown_contacts.size();
		//                if(start_line + display_lines <= contact_size) {
		//                	start_line += display_lines/2;
		//                	if(start_line + display_lines > contact_size){
		//                		start_line = contact_size - display_lines;
		//                	}
		//                	repaint();
		//                }
		//                return;
		//            }
		//        }

		switch (kc) {
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
			int key_num = kc - Canvas.KEY_NUM0;
			long t = System.currentTimeMillis();
			if ((key_num != sel_last_key) || t - sel_last_ts > 2000) {
				// new key
				sel_key_offset = 0;
				sel_last_key = key_num;
				sel_pattern = sel_pattern + itu_keys[key_num][sel_key_offset];
				filter_contacts(true);
			} else {
				// shifted key
				sel_key_offset += 1;
				if (sel_key_offset >= itu_keys[key_num].length) sel_key_offset = 0;
				sel_pattern = sel_pattern
						.substring(0, sel_pattern.length() - 1)
						+ itu_keys[key_num][sel_key_offset];
				filter_contacts(false);
			}
			sel_last_ts = t;
			if (shown_contacts.size() > 0) {
				selected_line = new_selected_line = start_line = 0;
				selected_contact = (Contact) shown_contacts.elementAt(0);
			} else {
				selected_line = new_selected_line = -1;
				start_line = 0;
				selected_contact = null;
			}
			repaint();
			return;
		case Canvas.KEY_STAR:
			// rest the selection
			sel_pattern = "";
			shown_contacts = active_contacts;
			for (int i = 0; i < shown_contacts.size(); i++) {
				Contact c = (Contact) shown_contacts.elementAt(i);
				if (c.equals(selected_contact)) {
					selected_line = new_selected_line = start_line = i;
					break;
				}
			}

			repaint();
			return;
		case Canvas.KEY_POUND:
			LampiroMidlet.disp.setCurrent(new ContactInfoScreen(
					selected_contact));
			return;
		}

		int ga = getGameAction(kc);
		switch (ga) {
		case Canvas.DOWN: {
			int contact_size = shown_contacts.size();
			if (contact_size == 0) {
				break;
			}

			if (selected_line == -1) {
				selected_line = new_selected_line = start_line;
				selected_contact = (Contact) shown_contacts
						.elementAt(start_line);
				repaint();
				break;
			}

			if (selected_line < contact_size - 1) {
				new_selected_line++;
				selected_contact = (Contact) shown_contacts
						.elementAt(new_selected_line);
				repaint_selection = true;
				if (new_selected_line - start_line >= display_lines) {
					start_line += display_lines / 2;
					if (start_line + display_lines >= contact_size) {
						start_line = contact_size - display_lines;
					}
					repaint_selection = false;
				}
				repaint();
			}
			break;
		}
		case Canvas.UP: {
			int contact_size = shown_contacts.size();
			if (contact_size == 0) {
				break;
			}

			if (selected_line == -1) {
				selected_line = new_selected_line = start_line;
				selected_contact = (Contact) shown_contacts
						.elementAt(start_line);
				repaint();
				break;
			}
			if (selected_line > 0) {
				new_selected_line--;
				selected_contact = (Contact) shown_contacts
						.elementAt(new_selected_line);
				repaint_selection = true;
				if (new_selected_line < start_line) {
					start_line -= display_lines / 2;
					if (start_line < 0) {
						start_line = 0;
					}
					repaint_selection = false; // also scrolled, reset repaint selection
				}
				repaint();
			}
			break;
		}
		case Canvas.RIGHT: {
			Contact c = selected_contact;
			if (c != null) {
				chatWithSelected(true);
			}
			break;
		}
		case Canvas.FIRE: {
			Contact c = selected_contact;
			if (c != null) {
				chatWithSelected(false);
			}
			break;
		}
		case Canvas.LEFT: {
			if (sel_pattern.length() > 0) {
				sel_pattern = sel_pattern
						.substring(0, sel_pattern.length() - 1);
				if (sel_pattern.length() > 0) {
					filter_contacts(false);
				} else {
					shown_contacts = active_contacts;
				}
			} else {
				// go to the top
				start_line = selected_line = new_selected_line = 0;
				selected_contact = (Contact) shown_contacts.elementAt(0);
			}
			repaint();
			break;
		}
		default: {
			break;
		}
		}

	}

	/**
	 * Handle a command
	 * @param c the selected command
	 * @param d the object on which the command has been ivoked
	 */
	public void commandAction(Command c, Displayable d) {
		LampiroMidlet yup = LampiroMidlet._lampiro;
		Display disp = LampiroMidlet.disp;
		if (c == cmd_exit) {
			yup.exit();
		} else if (c == cmd_delc) {
			DeleteContactAlert a = new DeleteContactAlert(selected_contact);
			disp.setCurrent(a);
		} else if (c == cmd_addc) {
			AddContactScreen acs = new AddContactScreen();
			disp.setCurrent(acs);
			//        } else if(c == cmd_info) {
			//            Contact user = getSelectedContact();
			//            AddContactScreen acs = new AddContactScreen(user);
			//            disp.setCurrent(acs);
		} else if (c == cmd_send) {
			Contact user = selected_contact;
			MessageComposerScreen ms = new MessageComposerScreen(user, this,
					MessageComposerScreen.MESSAGE);
			disp.setCurrent(ms);
		} else if (c == cmd_chat) {
			chatWithSelected(false);
			//        } else if (c == cmd_reload) {
			//        	Roster.getInstance().updateRoster();
		} else if (c == cmd_state) {
			StatusScreen ssc = new StatusScreen();
			disp.setCurrent(ssc);
			// #mdebug        	
			//@			        } else if(c == cmd_debug) {
			//@			        	DebugScreen debugScreen = new DebugScreen();
			//@			        	debugScreen.setReturnScreen(this);
			//@			        	disp.setCurrent(debugScreen);
			// #enddebug        	
		} else if (c == cmd_querycmd) {
			Contact usr = selected_contact;
			usr.cmdlist = null;
			Iq iq = new Iq(usr.getFullJid(), Iq.T_GET);
			Element query = iq
					.addElement("http://jabber.org/protocol/disco#items",
								Iq.QUERY,
								"http://jabber.org/protocol/disco#info");
			query.setAttribute("node", "http://jabber.org/protocol/commands");
			AdHocCommandsHandlder handler = new AdHocCommandsHandlder();
			XMPPClient.getInstance().sendIQ(iq, handler);
		} else if (c == cmd_listcmd) {
			CommandListScreen cmdscr = new CommandListScreen(selected_contact);
			disp.setCurrent(cmdscr);
		} else if (c == cmd_about) {
			AboutScreen as = new AboutScreen();
			LampiroMidlet.disp.setCurrent(as);
		} else if (c == cmd_options) {
			OptionsScreen os = new OptionsScreen();
			LampiroMidlet.disp.setCurrent(os);
		} else if (c == cmd_tasks) {
			Contact usr = selected_contact;
			Task tasks[] = usr.getTasks();
			if (tasks.length == 1) {
				// #ifndef UI 
								tasks[0].display(disp, this);
				// #endif
			} else if (tasks.length > 1) {
				TaskListScreen taskListScreen = new TaskListScreen(tasks);
				disp.setCurrent(taskListScreen);
			}
		}
	}

	//    private Contact getSelectedContact() {
	//        if(selected_line < 0 || selected_line >= shown_contacts.size()) {
	//            return null;
	//        }
	//    	return (Contact) shown_contacts.elementAt(selected_line);
	//    }

	private void chatWithSelected(boolean force_chat) {
		Display disp = LampiroMidlet.disp;
		Contact user = selected_contact;
		ChatScreen ms = new ChatScreen(user);
		if (user.unread_msg || force_chat) {
			disp.setCurrent(ms);
		} else {
			SimpleComposerScreen cs = new SimpleComposerScreen(ms, user);
			LampiroMidlet.disp.setCurrent(cs);
		}
		user.unread_msg = false;
	}

	private class AdHocCommandsHandlder extends IQResultListener {

		public void handleError(Element e) {
			// simply ingnore -> XXX we could add an alert 			
		}

		public void handleResult(Element e) {
			XMPPClient.getInstance().handleClientCommands(e, true);
		}
	}

	/**
	 * Update the (global) status of a contact and repaint the roster accordingly to the new situation 
	 * @param c
	 */
	public synchronized void updateContact(Contact c, int reason) {

		// #ifdef TIMING 		
		//@		long t1 = System.currentTimeMillis();
		// #endif

		boolean removed = active_contacts.removeElement(c);

		if (removed || c.isVisible()) {

			// reinsert if it is visible
			int i = 0;
			if (c.isVisible()) {
				while (i < active_contacts.size()
						&& c.compareTo((Contact) active_contacts.elementAt(i)) < 0)
					i++;
				active_contacts.insertElementAt(c, i);
			}

			if (reason == Contact.CH_MESSAGE_NEW
					|| reason == Contact.CH_TASK_NEW) {
				// set the correct selection to the just updated task
				shown_contacts = active_contacts;
				sel_pattern = "";
				start_line = new_selected_line = selected_line = i;
			} else {
				// preserve the current selection and keep it visible
				if (selected_contact != null) {
					int index = shown_contacts.indexOf(selected_contact);
					if (index != -1) {
						selected_line = new_selected_line = index;
					} else {
						selected_line = new_selected_line = 0;
					}

					// make sure the selection is visible
					if (selected_line < start_line
							|| start_line + display_lines <= selected_line) {
						start_line = selected_line;
					}
				}
			}
			repaint();
		}

		// #ifdef TIMING
		//@		System.out.println("New sort time: " + (System.currentTimeMillis() - t1));
		// #endif 		
	}

	public synchronized void removeContact(Contact c) {
		active_contacts.removeElement(c);
		// XXX we could repaint only if this contact is really displayed 
		repaint();
	}

	public synchronized void removeAllContacts() {
		active_contacts.removeAllElements();
		repaint();
	}

}
