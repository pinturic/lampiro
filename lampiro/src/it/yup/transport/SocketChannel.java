/* Copyright (c) 2008-2009-2010 Bluendo S.r.L.
 * See about.html for details about license.
 *
 * $Id: SocketChannel.java 2002 2010-03-06 19:02:12Z luca $
*/

package it.yup.transport;

// #debug
//@import it.yup.util.Logger;
import it.yup.util.UTFReader;

import it.yup.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TimerTask;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;
import javax.microedition.io.StreamConnection;

//#ifdef TLS
//@
//@import org.bouncycastle.crypto.tls.AlwaysValidVerifyer;
//@import org.bouncycastle.crypto.tls.TlsProtocolHandler;
//@
//#endif

// #ifdef COMPRESSION
//@import com.jcraft.jzlib.JZlib;
//@import com.jcraft.jzlib.ZInputStream;
//@import com.jcraft.jzlib.ZOutputStream;
//@
// #endif

public class SocketChannel extends BaseChannel {

	/** String identifying the transport type */
	public static final String TRANSPORT_TYPE = "DIRECT_SOCKET";

	/**  Keepalive interval for XML streams */
	// Please note that if we are using WiFi this KEEP_ALIVE must be very short
	// Many cellphone WiFi implementations in fact hangs after 20-30s of inactivity,
	// and they cannot receive packets any more
	public long KEEP_ALIVE = 300 * 1000;

	protected String connectionUrl;
	protected TransportListener listener;
	protected StreamConnection connection;

	protected boolean exiting = false;


	// #ifdef COMPRESSION	
//@	private boolean compressed = false;
	// #endif

	private TimerTask ka_task = null;

	public SocketChannel(String connectionUrl,
			TransportListener transportListener) {
		this.connectionUrl = connectionUrl;
		this.listener = transportListener;
		this.transportType = TRANSPORT_TYPE;
		inputStream = null;
		outputStream = null;
	}

	public void open() {

		exiting = false;

		// create the opener and start the connection
		Runnable starter = new Runnable() {
			public void run() {

				inputStream = null;
				outputStream = null;

				try {
					// #debug					
//@					Logger.log("Connecting to " + connectionUrl);
					connection = (SocketConnection) Connector
							.open(connectionUrl);
					// #debug					
//@					Logger.log("Connected ");
					setupStreams(connection.openInputStream(), connection
							.openOutputStream());

					// start the sender after each new connection
					sender = new Sender(SocketChannel.this);
					sender.start();

					listener.connectionEstablished(SocketChannel.this);
				} catch (IOException e) {
					// #debug					
//@					Logger.log("Connection failed: " + e.getMessage());
					listener.connectionFailed(SocketChannel.this);
				} catch (Exception e) {
					// #debug		    		
//@					Logger.log("Unexpected exception: " + e.getMessage());
					listener.connectionFailed(SocketChannel.this);
					//		    		YUPMidlet.yup.reportException("Unexpected Exception on Channel start.", e, null);
				}
			}
		};
		new Thread(starter).start();
	}

	public void close() {
		if (pollAlive() == false) return;
		exiting = true;
		try {
			inputStream.close();
			outputStream.close();
			connection.close();
		} catch (IOException e) {
			// #mdebug 
//@			System.out.println("In closing strean");
//@			e.printStackTrace();
			// #enddebug
		} catch (Exception e) {
			// #mdebug 
//@			System.out.println("In closing strean");
//@			e.printStackTrace();
			// #enddebug
		}
	}

	public boolean isOpen() {
		return inputStream != null;
	}

	public InputStream getInputStream() {
		return this.inputStream;
	}

	public OutputStream getOutputStream() {
		return this.outputStream;
	}

	public void sendContent(byte[] packetToSend) {
		synchronized (packets) {
			packets.addElement(packetToSend);
			packets.notify();
		}

		if (ka_task != null) {
			ka_task.cancel();
		}

		ka_task = new TimerTask() {
			public void run() {
				/* utf-8 space */
				sendContent(new byte[] { 0x20 });
			}

		};

		Utils.tasks.schedule(ka_task, KEEP_ALIVE);

	}

	protected boolean pollAlive() {
		return !exiting;
	}

	public UTFReader getReader() {
		return new UTFReader(inputStream);
	}

	//#ifdef COMPRESSION
//@	public void startCompression() {
//@		synchronized (packets) {
//@			compressed = true;
//@			inputStream = new ZInputStream(inputStream);
//@			outputStream = new ZOutputStream(outputStream,
//@					JZlib.Z_DEFAULT_COMPRESSION);
//@			((ZOutputStream) outputStream).setFlushMode(JZlib.Z_PARTIAL_FLUSH);
//@			//sender.exiting = true;
//@			//packets.notify();
//@		}
//@	}
//@
	//	#endif

	// #ifdef TLS
//@	public void startTLS() throws IOException {
//@		synchronized (packets) {
//@			TlsProtocolHandler handler = new TlsProtocolHandler(inputStream,
//@					outputStream);
//@			handler.connect(new AlwaysValidVerifyer());
//@			outputStream = handler.getOutputStream();
//@			inputStream = handler.getInputStream();
//@		}
//@	}
	//  #endif

}
