package lampiro.screens;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.TextField;

import it.yup.ui.UIButton;
import it.yup.ui.UICanvas;
import it.yup.ui.UIHLayout;
import it.yup.ui.UIItem;
import it.yup.ui.UILabel;
import it.yup.ui.UILayout;
import it.yup.ui.UIMenu;
import it.yup.ui.UIPanel;
import it.yup.ui.UIScreen;
import it.yup.ui.UITextField;
import it.yup.xmlstream.Element;
import it.yup.xmlstream.EventQuery;
import it.yup.xmlstream.PacketListener;
import it.yup.xmpp.Contact;
import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.IQResultListener;
import it.yup.xmpp.packets.Iq;
import it.yup.ui.UITextField;
import it.yup.util.ResourceIDs;
import it.yup.util.ResourceManager;

public class GatewayRegisterScreen extends UIScreen {

	private class GatewayRegistrationHandler extends IQResultListener {

		UIMenu resPopup = new UIMenu(rm.getString(ResourceIDs.STR_REGISTER));

		public GatewayRegistrationHandler() {
			resPopup.append(textLabel);
			String regText = rm.getString(ResourceIDs.STR_REG_GATEWAYS);
			String firstLetter = (regText.charAt(0) + "").toUpperCase();
			regText = firstLetter + regText.substring(1, regText.length());
			textLabel.setText(regText);
			textLabel.setFocusable(true);
			resPopup.setSelectedIndex(1);
			resPopup.setAbsoluteX(10);
			resPopup.setWidth(UICanvas.getInstance().getWidth() - 20);
			textLabel.setWrappable(true, resPopup.getWidth() - 5);
			Graphics cg = GatewayRegisterScreen.this.getGraphics();
			int offset = (cg.getClipHeight() - resPopup.getHeight(cg)) / 2;
			resPopup.setAbsoluteY(offset);

		}

		public void handleError(Element e) {
			textLabel.setText(rm.getString(ResourceIDs.STR_REG_ERROR));
			regSuccessfull = false;
			GatewayRegisterScreen.this.addPopup(resPopup);
		}

		public void handleResult(Element e) {
			Element error = e.getChildByName(null, "error");
			regSuccessfull = true;
			if (error != null) {
				textLabel.setText(rm.getString(ResourceIDs.STR_REG_ERROR));
				regSuccessfull = false;
			}
			GatewayRegisterScreen.this.addPopup(resPopup);
		}
	}

	static ResourceManager rm = ResourceManager.getManager("common", "en");

	private Element iq = null;

	private UIPanel mainPanel;

	private UITextField usernameLabel = null;

	private UITextField passwordLabel = null;

	private UIButton submit = new UIButton(rm.getString(ResourceIDs.STR_SUBMIT));
	private UIButton cancel = new UIButton(rm.getString(ResourceIDs.STR_CANCEL));

	private UIHLayout logLayout = new UIHLayout(4);

	UILabel textLabel = new UILabel("");

	private boolean regSuccessfull = false;

	public GatewayRegisterScreen(Element iq) {
		this.iq = iq;
		Element q = iq.getChildByName(RosterScreen.IQ_REGISTER, Iq.QUERY);
		mainPanel = new UIPanel();
		mainPanel.setMaxHeight(-1);
		this.append(mainPanel);
		Element instructions = q.getChildByName(null, "instructions");
		if (instructions != null) {
			String instructionText = instructions.content;
			if (instructionText == null) instructionText = "";
			UITextField instructionsLabel = new UITextField(rm
					.getString(ResourceIDs.STR_INSTRUCTIONS), instructionText,
					instructionText.length(), TextField.UNEDITABLE);
			mainPanel.addItem(instructionsLabel);
			instructionsLabel.setWrappable(true);
		}
		Element username = q.getChildByName(null, "username");
		if (username != null) {
			String usernameText = username.content;
			if (usernameText == null) usernameText = "";
			usernameLabel = new UITextField(rm
					.getString(ResourceIDs.STR_USERNAME), usernameText, 50,
					TextField.ANY);
			mainPanel.addItem(usernameLabel);
		}
		passwordLabel = new UITextField(rm.getString(ResourceIDs.STR_PASSWORD),
				"", 50, TextField.PASSWORD);
		mainPanel.addItem(passwordLabel);

		UILabel dummyLabel = new UILabel("");
		logLayout.setGroup(false);
		logLayout.insert(dummyLabel, 0, 50, UILayout.CONSTRAINT_PERCENTUAL);
		logLayout.insert(dummyLabel, 3, 50, UILayout.CONSTRAINT_PERCENTUAL);
		logLayout.insert(submit, 1, 75, UILayout.CONSTRAINT_PIXELS);
		logLayout.insert(cancel, 2, 75, UILayout.CONSTRAINT_PIXELS);
		submit.setAnchorPoint(Graphics.HCENTER);
		mainPanel.addItem(logLayout);
		this.titleLabel.setText(rm.getString(ResourceIDs.STR_REGISTER));
	}

	public void menuAction(UIMenu menu, UIItem c) {
		if (regSuccessfull) UICanvas.getInstance().close(this);
	}

	public void itemAction(UIItem item) {

		if (item == this.submit) {
			String from = iq.getAttribute(Iq.ATT_FROM);
			Iq iq = new Iq(from, Iq.T_SET);
			Element query = iq.addElement(RosterScreen.IQ_REGISTER, Iq.QUERY,
					RosterScreen.IQ_REGISTER);
			Element usr = query.addElement(RosterScreen.IQ_REGISTER,
					"username", RosterScreen.IQ_REGISTER);
			usr.content = this.usernameLabel.getText();
			Element pwd = query.addElement(RosterScreen.IQ_REGISTER,
					"password", RosterScreen.IQ_REGISTER);
			pwd.content = this.passwordLabel.getText();

			XMPPClient.getInstance().sendIQ(iq,
					new GatewayRegistrationHandler());

		} else if (item == this.cancel) {
			UICanvas.getInstance().close(this);
		}
	}

	public void packetReceived(Element e) {
		// TODO Auto-generated method stub

	}

}
