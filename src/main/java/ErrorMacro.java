/**
 * 
 * @author Michael De Angelis
 * @matricola: 560049
 * @project Word Quizzle
 * @A.A 2019 - 2020 [UNIPI]
 *
 */

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicBoolean;

// Class used only to contain all the generic error messages, cannot be instantiated
public class ErrorMacro {	
	public static final String NULL_ARG   					= "null arg used";
	public static final String NULL_ARGS  					= "null arg(s) used";
	public static final String EMPTY_ARGS 					= "empty arg(s) used";
	public static final String USR_EMPTY  					= "empty arg used";
	
	public static final String IOEXCEPTION		 			= "An I/O error occurred; the server must be stopped";
	public static final String SOFT_IOEXCEPTION		 		= "An I/O error occurred";
	public static final String IOE_READ_RQST	   			= "An I/O error occurred while reading the player request";
	public static final String IOE_WR_RESP		   			= "An I/O error occurred while writing the response to the player";
	public static final String CLOSING_CHANNEL_IOE 			= "An I/O error occurred while closing the channel";
	public static final String IOEXCEPTION_MATCH			= "An I/O error occurred; the match must be canceled";
	public static final String CLOSED_CHANNEL_EXCEPTION		= "ClosedChannelException occurred while trying to register a socket to a selector";
	
	public static final String INTERRUPTED_EXCEPTION		= "An InterruptedException occurred while waiting for the matches completition";
	
	public static final String RMI_REMOTE_EXCEPTION			= "An error occurred while creating the sign up service; the server must be stopped";
	public static final String RMI_REMOTE_EXCEPTION_CLOSURE = "An orror occurred while closing the sign up service";
	
	public static final String NOT_BOUND_EXCEPTION			= "A NotBoundException occurred while closing the sign un service";
	
	public static final String DISCONNECTED					= " disconnected";
	
	public static final String MALFORMED_URL_EXCEPTION		= "A MalformedURLException occurred while creating URL object(s)";
	
	public static final String MATCH_TIME_OUT_REQ			= "The match request timed out";
	
	public static final String IOEXCEPTION_CLIENT_COMM		= "An I/O error occurred while communicating with the server";
	
	private ErrorMacro() {}
	
	public static void ioExceptionHandling(IOException ioe) {
		//ioe.printStackTrace();
		System.err.println(IOEXCEPTION);
		System.exit(-1);
	}
	
	public static void remoteExceptionHandling(RemoteException re) {
		//re.printStackTrace();
		System.err.println(RMI_REMOTE_EXCEPTION_CLOSURE);
	}
	
	public static void notBoundExceptionHandling(NotBoundException nbe) {
		//nbe.printStackTrace();
		System.err.println(NOT_BOUND_EXCEPTION);
	}
	
	public static void InterruptedExceptionHandling(InterruptedException ie) {
		//ie.printStackTrace();
		System.err.println(INTERRUPTED_EXCEPTION);
	}
	
	public static void softIoExceptionHandling(IOException ioe) {
		//ioe.printStackTrace();
		System.err.println(SOFT_IOEXCEPTION);
	}
	
	public static void rqstIoExceptionKeyHandling(IOException ioe, SelectionKey key) {
		//ioe.printStackTrace();
		key.cancel();
		try {
			key.channel().close();
		} catch (IOException e) {
			//e.printStackTrace();
			System.err.println(CLOSING_CHANNEL_IOE);
		}
		System.err.println(IOE_READ_RQST);
	}
	
	public static void respIoExceptionKeyHandling(IOException ioe, SelectionKey key) {
		//ioe.printStackTrace();
		key.cancel();
		try {
			key.channel().close();
		} catch (IOException e) {
			//e.printStackTrace();
			System.out.println(CLOSING_CHANNEL_IOE);
		}
		System.err.println(IOE_WR_RESP);
	}
	
	public static void malformedUrlExceptionHandling(MalformedURLException mue) {
		//mue.printStackTrace();
		System.err.println(MALFORMED_URL_EXCEPTION);
		System.exit(-1);
	}
	
	public static void closedChannelExceptionHandling(ClosedChannelException cce) {
		//cce.printStackTrace();
		System.err.println(CLOSED_CHANNEL_EXCEPTION);
	}

	public static void matchSocketTimeoutExceptionReq(SocketChannel challengerSocket, Selector sel,
			String rsp, Selector newSel, DatagramSocket udp) {
		System.err.println(MATCH_TIME_OUT_REQ);
		
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		length.putInt(rsp.length());
		length.flip();
		
		ByteBuffer request = ByteBuffer.wrap(rsp.getBytes());
		
		ByteBuffer[] byteBuffers = { length, request };
		try {
			challengerSocket.register(sel, SelectionKey.OP_WRITE, byteBuffers);
			sel.wakeup();
			try {
				if(newSel != null)
					newSel.close();
				if(udp != null)
					udp.close();
			} catch (IOException ioe) {
				softIoExceptionHandling(ioe);
			}
		} catch (ClosedChannelException cce) {
			closedChannelExceptionHandling(cce);
		}
	}
	
	public static void matchIoExceptionHandling(IOException ioe,
			SocketChannel sock, Selector sel, String rsp, Selector newSel, DatagramSocket udp) {
		//ioe.printStackTrace();
		System.err.println(IOEXCEPTION_MATCH);
		
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		length.putInt(rsp.length());
		length.flip();
		
		ByteBuffer request = ByteBuffer.wrap(rsp.getBytes());
		
		ByteBuffer[] byteBuffers = { length, request };
		try {
			sock.register(sel, SelectionKey.OP_WRITE, byteBuffers);
			sel.wakeup();
			try {
				if(newSel != null)
					newSel.close();
				if(udp != null)
					udp.close();
			} catch (IOException ioee) {
				softIoExceptionHandling(ioee);
			}
		} catch (ClosedChannelException cce) {
			closedChannelExceptionHandling(cce);
		}
	}
	
	public static void matchIoExceptionHandlingPAIR(IOException ioe, 
			SocketChannel challengerSocket, SocketChannel challengedSocket, 
			Selector sel, String rsp, Selector newSel, DatagramSocket udp) {
		//ioe.printStackTrace();
		System.err.println(IOEXCEPTION_MATCH);
		
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		length.putInt(rsp.length());
		length.flip();
		
		ByteBuffer request = ByteBuffer.wrap(rsp.getBytes());
		
		ByteBuffer[] byteBuffers = { length, request };
		
		ByteBuffer slength = ByteBuffer.allocate(Integer.BYTES);
		slength.putInt(rsp.length());
		slength.flip();
		
		ByteBuffer srequest = ByteBuffer.wrap(rsp.getBytes());
		
		ByteBuffer[] sbyteBuffers = { slength, srequest };
		try {
			challengerSocket.register(sel, SelectionKey.OP_WRITE, byteBuffers);
			challengedSocket.register(sel, SelectionKey.OP_WRITE, sbyteBuffers);
			sel.wakeup();
			try {
				if(newSel != null)
					newSel.close();
				if(udp != null)
					udp.close();
			} catch (IOException ioee) {
				softIoExceptionHandling(ioee);
			}
		} catch (ClosedChannelException cce) {
			closedChannelExceptionHandling(cce);
		}
	}
	
	public static void clientIoExceptionHandlingUDP(IOException ioe, SocketChannel sock, 
			DatagramSocket udp, String usr, AtomicBoolean flag) {
		//ioe.printStackTrace();
		System.err.println(IOEXCEPTION_CLIENT_COMM);
		
		try {
			if(sock != null)
				sock.close();
			if(udp != null)
				udp.close();
			if(usr != null)
				usr = null;
			flag.set(false);
		} catch (IOException ioee) {
			softIoExceptionHandling(ioee);
		}
	}
	
	public static void clientIoExceptionHandling(IOException ioe, SocketChannel sock, 
			DatagramSocket udp, String usr, AtomicBoolean flag, AtomicBoolean flag2) {
		//ioe.printStackTrace();
		System.err.println(IOEXCEPTION_CLIENT_COMM);
		
		try {
			flag.set(true);
			if(sock != null)
				sock.close();
			if(udp != null)
				udp.close();
			if(usr != null)
				usr = null;
			flag2.set(false);
		} catch (IOException ioee) {
			softIoExceptionHandling(ioee);
		}
	}
}
