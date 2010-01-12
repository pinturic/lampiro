/* Copyright (c) 2008 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: CommandExecutor.java 1913 2009-12-02 14:21:24Z luca $
*/

package it.yup.xmpp;

import java.util.Date;
import javax.microedition.lcdui.AlertType;

// #ifndef UI
//@import javax.microedition.lcdui.Display;
//@import javax.microedition.lcdui.Displayable;
//@
//#endif

import it.yup.xml.Element;
import it.yup.xmlstream.BasicXmlStream;
import it.yup.xmlstream.EventQuery;
import it.yup.xmlstream.PacketListener;
import it.yup.xmpp.XMPPClient.XmppListener;
import it.yup.xmpp.packets.DataForm;
import it.yup.xmpp.packets.Iq;
import it.yup.xmpp.packets.Message;

public class CommandExecutor implements PacketListener, DataFormListener, Task {

	private static final String STATUS_EXECUTING = "executing";
	private static final String STATUS_COMPLETED = "completed";
	private static final String STATUS_CANCELED = "canceled";

	private static final String PREV = "prev";
	private static final String NEXT = "next";
	private static final String CANCEL = "cancel";
	private static final String COMPLETE = "complete";
	private static final String EXECUTE = "execute";

	public boolean enableDisplay = false;
	public boolean enableNew = false;

	// private static ResourceManager rm = ResourceManager.getManager("common",
	// Config.lang);

	public static final int MSG_BROKEN_DF = 1101;

	/** the command information (node, label) */
	private String[] cmd;
	/** the sid associated with this command iteration */
	private String sid;
	/** the data form associated with this command */
	private DataForm df;
	/** the status of this command */
	private byte status;

	private Element current_element = null;

	private Object screen;

	private String note = null;

	private Date last_modify;

	private int step;

	private String chosenResource;
	private Object optionalArguments;


	public CommandExecutor(String[] cmd, String chosenResource,
			Object optionalArguments) {
		this.optionalArguments = optionalArguments;
		this.cmd = cmd;
		this.chosenResource = chosenResource;
		this.status = Task.CMD_EXECUTING;
		step = 1;
		last_modify = new Date();
		XMPPClient.getInstance().updateTask(this);
	}

	/**
	 * @param cmd
	 * @param chosenResource
	 */
	public void setupCommand() {
		Iq iq = new Iq(chosenResource, Iq.T_SET);
		Element cel = iq.addElement(XMPPClient.NS_COMMANDS, XMPPClient.COMMAND);
		cel.setAttribute("node", cmd[0]);
		cel.setAttribute(XMPPClient.ACTION, EXECUTE);

		sendPacket(iq);
	}

	private void sendPacket(Iq iq) {
		XMPPClient xmpp = XMPPClient.getInstance();
		EventQuery eq = new EventQuery(Iq.IQ, new String[] { "id" },
				new String[] { iq.getAttribute(Iq.ATT_ID) });

		BasicXmlStream.addOnetimeEventListener(eq, this);
		xmpp.sendPacket(iq);
	}

	public void packetReceived(Element el) {
		current_element = el;

		XMPPClient client = XMPPClient.getInstance();
		Element command = (Element) el.getChildByName(XMPPClient.NS_COMMANDS,
				XMPPClient.COMMAND);
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
			// unexpected status, discard the message, and notify?
			this.status = Task.CMD_ERROR;
			// XXX is this enough?
		}
		if (el.getAttribute(Iq.ATT_TYPE).equals(Iq.T_ERROR)) this.status = Task.CMD_ERROR;
		if (df == null && this.status != Task.CMD_ERROR) {
			if (this.status != Task.CMD_FINISHED) {
				this.status = Task.CMD_FORM_LESS;
			} else {
				this.status = Task.CMD_DESTROY;
			}
		}

		Element note_element = command.getChildByName(XMPPClient.NS_COMMANDS,
				"note");
		if (note_element != null) {
			this.note = note_element.getText();
		} else {
			this.note = null;
		}

		client.updateTask(this);
		XmppListener xmppListener = client.getXmppListener();
		if (xmppListener != null) {
			xmppListener.handleTask(this);
		}

	}

	public boolean execute(int cmd) {
		/*
		 * not checking if the cmd is in the allowed ones, as I have built the
		 * screen accordingly...
		 */
		boolean setWaiting = false;
		last_modify = new Date();
		switch (cmd) {
			case DataFormListener.CMD_CANCEL:
				status = Task.CMD_CANCELING;
				sendReply(CANCEL, null);
				break;
			case DataFormListener.CMD_PREV:
				step--;
				status = Task.CMD_EXECUTING;
				sendReply(PREV, null);
				setWaiting = true;
				break;
			case DataFormListener.CMD_NEXT:
				step++;
				status = Task.CMD_EXECUTING;
				if (df != null) {
					df.type = DataForm.TYPE_SUBMIT;
					sendReply(NEXT, df.getResultElement());
				} else
					sendReply(NEXT, null);
				setWaiting = true;
				break;
			case DataFormListener.CMD_SUBMIT:
				step++;
				status = Task.CMD_EXECUTING;
				if (df != null) {
					df.type = DataForm.TYPE_SUBMIT;
					sendReply(EXECUTE, df.getResultElement());
				} else
					sendReply(EXECUTE, null);
				setWaiting = true;
				break;
			case DataFormListener.CMD_DELAY:
				// do nothing, just display the next screen
				setWaiting = true;
				break;
			case DataFormListener.CMD_DESTROY:
				status = Task.CMD_DESTROY;
		}

		// update command status
		XMPPClient instance = XMPPClient.getInstance();
		instance.updateTask(this);
		return setWaiting;
	}

	void sendReply(String action, Element dfel) {
		Iq iq = new Iq(chosenResource, Iq.T_SET);
		Element cel = iq.addElement(XMPPClient.NS_COMMANDS, XMPPClient.COMMAND);
		cel.setAttribute("node", cmd[0]);
		if (sid != null) {
			cel.setAttribute("sessionid", sid);
		}
		if (action != null) {
			cel.setAttribute(XMPPClient.ACTION, action);
		}
		if (dfel != null) {
			cel.addElement(dfel);
		}


		sendPacket(iq);
	}

	// #ifdef UI 
	public void display() {
		// #endif
// #ifndef UI
		//@	public void display(Display disp, Displayable next_screen) {
		// #endif
		final XMPPClient client = XMPPClient.getInstance();
		switch (status) {
			case Task.CMD_INPUT:
				Element command = (Element) current_element.getChildByName(
						XMPPClient.NS_COMMANDS, XMPPClient.COMMAND);
				Element actions = command.getChildByName(
						XMPPClient.NS_COMMANDS, "actions");
				if (actions == null) {
					actions = new Element(XMPPClient.NS_COMMANDS, "actions");
					actions.addElement(null, "complete");
				}
				// add the available actions
				if (df.type == DataForm.TYPE_FORM) {
					int cmds = 0;
					Element[] children = actions.getChildren();
					if (children.length == 0) {
						cmds |= DataFormListener.CMD_SUBMIT;
					}
					for (int i = 0; i < children.length; i++) {
						Element iel = children[i];
						if (NEXT.equals(iel.name)) {
							cmds |= DataFormListener.CMD_NEXT;
						} else if (PREV.equals(iel.name)) {
							cmds |= DataFormListener.CMD_PREV;
						} else if (COMPLETE.equals(iel.name)) {
							cmds |= DataFormListener.CMD_SUBMIT;
						}
					}
					XmppListener xmppListener = XMPPClient.getInstance()
							.getXmppListener();
					if (xmppListener != null) {
						screen = xmppListener.handleDataForm(df,
								Task.CMD_INPUT, this, cmds, optionalArguments);
					}
					/*
					screen = new DataFormScreen(df, this);
					DataFormScreen dfs = (DataFormScreen) screen;
					dfs.setActions(cmds);
					*/
				}
				break;
			case Task.CMD_EXECUTING:
				client.showAlert(AlertType.INFO, Config.ALERT_COMMAND_INFO,
						Config.ALERT_WAIT_COMMAND, null);
				break;
			case Task.CMD_CANCELING:
				client.showAlert(AlertType.INFO, Config.ALERT_COMMAND_INFO,
						Config.ALERT_CANCELING_COMMAND, null);
				break;
			case Task.CMD_CANCELED:
				client.showAlert(AlertType.INFO, Config.ALERT_COMMAND_INFO,
						Config.ALERT_CANCELED_COMMAND, getLabel());
				break;
			case Task.CMD_FINISHED:
				if (df != null) {
					/*
					screen = new DataResultScreen(df, this);
					*/
					XmppListener xmppListener = XMPPClient.getInstance()
							.getXmppListener();
					if (xmppListener != null) {
						screen = xmppListener.handleDataForm(df,
								Task.CMD_FINISHED, this, -1, optionalArguments);
					}
				} else {
					// XXX handle note here, if present
					status = Task.CMD_DESTROY;
					client.updateTask(this);

					new Thread() {
						public void run() {
							XmppListener xmppListener = client
									.getXmppListener();
							if (xmppListener != null) {
								xmppListener.handleTask(CommandExecutor.this);
							}
							client.showAlert(AlertType.INFO,
									Config.ALERT_COMMAND_INFO,
									Config.ALERT_FINISHED_COMMAND, null);
						}
					}.start();
				}
				break;
			case Task.CMD_ERROR:
				String errorText = null;
				Element error = (Element) current_element.getChildByName(null,
						Message.ERROR);
				if (error != null) {
					String code = error.getAttribute(XMPPClient.CODE);
					if (code != null) errorText = (XMPPClient
							.getErrorString(code));
					Element text = error.getChildByName(null, XMPPClient.TEXT);
					if (text != null) {
						errorText += ". " + text.getText();
					}
				}
				client.showAlert(AlertType.ERROR, Config.ALERT_COMMAND_INFO,
						Config.ALERT_ERROR_COMMAND, errorText);
				break;
		}

		if (status != Task.CMD_ERROR) {
			if (screen != null) {
				XmppListener xmppListener = client.getXmppListener();
				xmppListener.showCommand(screen);
			}
			if (note != null) {
				client.showAlert(AlertType.INFO, Config.ALERT_NOTE,
						Config.ALERT_NOTE, note);
			}
		}
	}

	public String getLabel() {
		return "[" + last_modify.toString().substring(11, 16) + "][" + step
				+ "] " + cmd[1];
	}

	public byte getStatus() {
		return status;
	}

	public String getFrom() {
		return this.chosenResource;
	}

	/**
	 * @return the sid
	 */
	public String getSid() {
		return sid;
	}

	public void setEnableDisplay(boolean enableDisplay) {
		this.enableDisplay = enableDisplay;
	}

	public void setEnableNew(boolean enableNew) {
		this.enableNew = enableNew;
	}

	public boolean getEnableDisplay() {
		return this.enableDisplay;
	}

	public boolean getEnableNew() {
		return this.enableNew;
	}
}
