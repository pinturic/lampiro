/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: MediaRetrieveListener.java 1858 2009-10-16 22:42:29Z luca $
*/

package lampiro.screens;

import javax.microedition.lcdui.Image;

import it.yup.ui.UICanvas;
import it.yup.ui.UILabel;
import it.yup.ui.UIScreen;
import it.yup.ui.UIUtils;
import it.yup.xmpp.Config;
import it.yup.xmpp.packets.Media;

//#mdebug
//@
//@import it.yup.util.Logger;
//@
// #enddebug

public class MediaRetrieveListener {

	private UILabel objLabel;
	private UIScreen scr;

	public MediaRetrieveListener(UIScreen scr, UILabel objLabel) {
		this.objLabel = objLabel;
		this.scr = scr;
	}

	/**
	 * @param media the media file taht should be retrieved
	 * @param mediaType the type of the file (i.e., Config.IMG_TYPE) 
	 * @param data the data to be represented
	 * @param synch data could be retrieve synchronously (http) or asynchronously (iq)
	 */
	public void showMedia(Media media, int mediaType, byte[] data, boolean synch) {
		Image img = null;
		if (mediaType == Config.IMG_TYPE) {
			img = Image.createImage(data, 0, data.length);
		} else if (mediaType == Config.AUDIO_TYPE) {
			img = UICanvas.getUIImage("/icons/mic.png");
		}

		// resize media
		int mWidth = media.width;
		int mHeight = media.height;

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

		this.objLabel.setImg(img);
		if (synch) {
			this.scr.askRepaint();
		} else {
			try {
				UICanvas.lock();
				this.scr.askRepaint();
			} catch (Exception e) {
				// #mdebug
//@				Logger.log("Error in mediaretrieveListener" + e.getClass());
				// #enddebug
			} finally {
				UICanvas.unlock();
			}
		}
	}

}
