package it.yup.xml;

import it.yup.xmpp.XMPPClient;
import it.yup.xmpp.packets.Stanza;

import java.util.Hashtable;

public class SystemConfig {

	public static Hashtable parse(Element el) {
		return parseEl(el, XMPPClient.SECTION, false);
	}

	private static Hashtable parseEl(Element el, String itemName,
			boolean isSection) {

		Element[] items = el.getChildrenByName(XMPPClient.NS_BLUENDO_CFG,
				itemName);
		Hashtable section = new Hashtable();
		for (int i = 0; items != null && items.length > 0 && i < items.length; i++) {
			Element element = items[i];
			Object objectParsed = null;
			if (isSection) objectParsed = parseItem(element);
			else
				objectParsed = parseEl(element, "i", true);
			section.put(element.getAttribute("name"), objectParsed);
		}
		return section;

	}

	private static Object parseItem(Element itemEl) {
		String type = itemEl.getAttribute(Stanza.ATT_TYPE);

		if (type.equals("str")) return itemEl.getText();
		else if (type.equals("int")) return new Integer(Integer.parseInt(itemEl
				.getText()));
		else if (type.equals("map")) {
			Element[] children = itemEl.getChildrenByName(
					XMPPClient.NS_BLUENDO_CFG, "i");
			Hashtable map = new Hashtable(children.length);
			for (int i = 0; i < children.length; i++) {
				Element element = children[i];
				map.put(element.getAttribute("name"), parseItem(element));
			}
			return map;
		} else if (type.equals("array")) {
			Element[] children = itemEl.getChildrenByName(
					XMPPClient.NS_BLUENDO_CFG, "i");
			Object[] array = new Object[children.length];
			for (int i = 0; i < children.length; i++)
				array[i] = parseItem(children[i]);
			return array;
		}

		return null;
	}

}
