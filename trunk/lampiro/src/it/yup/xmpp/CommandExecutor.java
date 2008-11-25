/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: CommandExecutor.java 846 2008-09-11 12:20:05Z luca $
*/

package it.yup.xmpp;

import java.util.Date;
import javax.microedition.lcdui.AlertType;

// #ifdef UI 
import it.yup.ui.UICanvas;
import it.yup.ui.UIScreen;
import lampiro.screens.DataFormScreen;
import lampiro.screens.DataResultScreen;
import lampiro.screens.RosterScreen;
import lampiro.screens.DataFormScreen.DataFormListener;

// #endif
// #ifndef UI
//@import lampiro.LampiroMidlet;
//@import javax.microedition.lcdui.Display;
//@import javax.microedition.lcdui.Displayable;
//@import javax.microedition.lcdui.Screen;
//@
//@import it.yup.screens.DataFormScreen;
//@import it.yup.screens.DataResultScreen;
//@import it.yup.screens.DataFormScreen.DataFormListener;
//@
// #endif

import it.yup.xmlstream.Element;
import it.yup.xmlstream.EventQuery;
import it.yup.xmlstream.PacketListener;
import it.yup.xmpp.packets.DataForm;
import it.yup.xmpp.packets.Iq;

public class CommandExecutor implements PacketListener, DataFormListener, Task {

	private static final String STATUS_EXECUTING = "executing";
	private static final String STATUS_COMPLETED = "completed";
	private static final String STATUS_CANCELED = "canceled";

	// private static ResourceManager rm = ResourceManager.getManager("common",
	// "en");

	public static final int MSG_BROKEN_DF = 1101;
	/** the user associated with this command */
	private Contact usr;
	/** the command information (node, label) */
	private String[] cmd;
	/** the sid associated with this command iteration */
	private String sid;
	/** the data form associated with this command */
	private DataForm df;
	/** the status of this command */
	private byte status;

	private Element current_element = null;

	// #ifdef UI 
	/** the screen assocated (if shown) to this command */
	private UIScreen screen;
	// #endif
// #ifndef UI
//@		private Displayable next_screen;
	// #endif

	private String note = null;

	private Date last_modify;

	private int step;

	/** Create the command and send it to the remote node */
	public CommandExecutor(Contact usr, String[] cmd) {
		this.usr = usr;
		this.cmd = cmd;
		this.status = Task.CMD_EXECUTING;

		step = 1;
		last_modify = new Date();

		Iq iq = new Iq(usr.getFullJid(), Iq.T_SET);
		Element cel = iq.addElement(XMPPClient.NS_COMMANDS, "command",
				XMPPClient.NS_COMMANDS);
		cel.setAttribute("node", cmd[0]);
		cel.setAttribute("action", "execute");
		XMPPClient xmpp = XMPPClient.getInstance();
		EventQuery eq = new EventQuery(Iq.IQ, new String[] { "id" },
				new String[] { iq.getAttribute(Iq.ATT_ID) });

		xmpp.registerOneTimeListener(eq, this);
		xmpp.sendPacket(iq);

		XMPPClient.getInstance().updateTask(this);

	}

	public void packetReceived(Element el) {
		current_element = el;

		XMPPClient client = XMPPClient.getInstance();
		Element command = (Element) el.getChildByName(XMPPClient.NS_COMMANDS,
				"command");
		if (command == null) {
			/* ? possible ? */
			return;
		}

		/* every time this is copied, not a problem, SHOULD stay the same */
		sid = command.getAttribute("sessionid");

		/* Parse the dataform if present */
		Element form = command.getChildByName(DataForm.NAMESPACE, DataForm.X);
		if (form != null) {
			df = new DataForm(form);
		} else {
			df = null;
		}

		/*
		 * Some implementations seem to send the result of a completed form
		 * using a DataForm of type "form" instead of "result", so let's check
		 * the "status" and not the type of the form
		 */

		String el_status = command.getAttribute("status");
		if (STATUS_CANCELED.equals(el_status)) {
			/*
			 * the server has canceled the command. could this happen? yes, as
			 * aswer of a cancel
			 */
			this.status = Task.CMD_CANCELED;
		} else if (STATUS_COMPLETED.equals(el_status)) {
			this.status = Task.CMD_FINISHED; // the command is finished
		} else if (STATUS_EXECUTING.equals(el_status)) {
			this.status = Task.CMD_INPUT;
		} else {
			// unexpexted status, discard the message, and notify?
			this.status = Task.CMD_ERROR;
			// XXX is this enough?
		}

		Element note_element = command.getChildByName(
				"http://jabber.org/protocol/commands", "note");
		if (note_element != null) {
			this.note = note_element.content;
		} else {
			this.note = null;
		}

		client.updateTask(this);
	}

	public void execute(int cmd) {
		/*
		 * not checking if the cmd is in the allowed ones, as I have built the
		 * screen accordingly...
		 */
		last_modify = new Date();
		switch (cmd) {
			case DataFormListener.CMD_CANCEL:
				status = Task.CMD_CANCELING;
				sendReply("cancel", null);
				break;
			case DataFormListener.CMD_PREV:
				step--;
				status = Task.CMD_EXECUTING;
				sendReply("prev", null);
				break;
			case DataFormListener.CMD_NEXT:
				step++;
				status = Task.CMD_EXECUTING;
				df.type = DataForm.TYPE_SUBMIT;
				sendReply("next", df.getResultElement());
				break;
			case DataFormListener.CMD_SUBMIT:
				step++;
				status = Task.CMD_EXECUTING;
				df.type = DataForm.TYPE_SUBMIT;
				sendReply("execute", df.getResultElement());
				break;
			case DataFormListener.CMD_DELAY:
				// do nothing, just display the next screen
				break;
			case DataFormListener.CMD_DESTROY:
				status = Task.CMD_DESTROY;
		}

		// update commad status
		XMPPClient.getInstance().updateTask(this);
		// #ifdef UI 
		if (screen != null) {
			/* close the screen */
			UICanvas.getInstance().close(screen);
			screen = null;
		} else {
			UICanvas.getInstance().show(RosterScreen.getInstance());
		}
		// #endif
// #ifndef UI
//@				LampiroMidlet.disp.setCurrent(next_screen);
		// #endif

	}

	private void sendReply(String action, Element dfel) {
		Iq iq = new Iq(usr.getFullJid(), Iq.T_SET);
		Element cel = iq.addElement("http://jabber.org/protocol/commands",
				"command", "http://jabber.org/protocol/commands");
		cel.setAttribute("node", cmd[0]);
		if (sid != null) {
			cel.setAttribute("sessionid", sid);
		}
		if (action != null) {
			cel.setAttribute("action", action);
		}
		if (dfel != null) {
			cel.children.addElement(dfel);
		}
		XMPPClient xmpp = XMPPClient.getInstance();
		EventQuery eq = new EventQuery(Iq.IQ, new String[] { "id" },
				new String[] { iq.getAttribute(Iq.ATT_ID) });

		xmpp.registerOneTimeListener(eq, this);
		xmpp.sendPacket(iq);
	}

	// #ifdef UI 
	public void display() {
		UIScreen screen = null;
		// #endif
// #ifndef UI
//@			public void display(Display disp, Displayable next_screen) {
//@				this.next_screen = next_screen;
//@				Screen screen = null;
		// #endif

		XMPPClient client = XMPPClient.getInstance();

		client = XMPPClient.getInstance();
		switch (status) {
			case Task.CMD_INPUT:
				screen = new DataFormScreen(df, this);
				DataFormScreen dfs = (DataFormScreen) screen;
				Element command = (Element) current_element.getChildByName(
						XMPPClient.NS_COMMANDS, "command");
				Element actions = command.getChildByName(
						XMPPClient.NS_COMMANDS, "actions");
				// add the available actions
				if (actions != null && df.type == DataForm.TYPE_FORM) {
					int cmds = 0;
					for (int i = 0; i < actions.children.size(); i++) {
						Element iel = (Element) actions.children.elementAt(i);
						if ("next".equals(iel.name)) {
							cmds |= DataFormListener.CMD_NEXT;
						} else if ("prev".equals(iel.name)) {
							cmds |= DataFormListener.CMD_PREV;
						} else if ("complete".equals(iel.name)) {
							cmds |= DataFormListener.CMD_SUBMIT;
						}
					}
					dfs.setActions(cmds);
				}
				break;
			case Task.CMD_EXECUTING:
				client.showAlert(AlertType.INFO, "Command Info",
						"Data submitted: awaiting a response from the server",
						null);
				break;
			case Task.CMD_CANCELING:
				client
						.showAlert(
								AlertType.INFO,
								"Command Info",
								"Command canceled: awaiting a response from the server",
								null);
				break;
			case Task.CMD_CANCELED:
				client.showAlert(AlertType.INFO, "Task canceled", "The task "
						+ getLabel() + " has been succesfully canceled", null);
				break;
			case Task.CMD_FINISHED:
				if (df != null) {
					screen = new DataResultScreen(df, this);
				} else {
					// XXX handle note here, if present
					client.showAlert(AlertType.INFO, "Task finished",
							"Task finished", null);
					status = Task.CMD_DESTROY;
					client.updateTask(this);
				}
				break;
			case Task.CMD_ERROR:
				client.showAlert(AlertType.INFO, "Task error",
						"An error occurred while executing the task", null);
				break;
		}

		// #ifdef UI 
		if (note != null) {
			client.showAlert(AlertType.INFO, "Note", note, null);
		}
		if (screen != null) {
			UICanvas.getInstance().open(screen, true);
		}
		// #endif
// #ifndef UI
//@				if (screen != null) {
//@					if (note != null) {
//@						client.showAlert(AlertType.INFO, "Note", note, screen);
//@		
//@					} else {
//@						LampiroMidlet.disp.setCurrent(screen);
//@					}
//@				}
		// #endif 

	}

	public String getLabel() {
		return "[" + last_modify.toString().substring(11, 16) + "][" + step
				+ "] " + cmd[1];
	}

	public byte getStatus() {
		return status;
	}

	public String getFrom() {
		return usr.jid;
	}

}
