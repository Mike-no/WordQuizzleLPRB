/**
 * 
 * @author Michael De Angelis
 * @matricola: 560049
 * @project Word Quizzle
 * @A.A 2019 - 2020 [UNIPI]
 *
 */

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

// Manages the match between two players
public class MatchHandler implements Runnable {
	private SocketChannel challengerSocket;
	private SocketChannel challengedSocket;
	private int oppoUdpPort;
	
	// Old selector used when exiting the game to allow users to send further commands
	private Selector oldSel;
	private Selector sel;
	
	private String usr;
	private String oppo;
	private DatagramSocket serverUdpSocket;
	private TranslationHandler translHandler;
	private JsonHandler jsonHandler;
	private PlayerGraph playerGraph;
	
	private Thread timeout;
	private boolean running = true;
	private int challengerIndex = 0;
	private int challengerScore = 0;
	
	private int challengedIndex = 0;
	private int challengedScore = 0;
	
	public MatchHandler(String usr, String oppo, SocketChannel challengerSocket, 
			SocketChannel challengedSocket, int oppoUdpPort, Selector oldSel, JsonHandler jsonHandler,
			PlayerGraph playerGraph) {
		if(usr == null || challengerSocket == null || challengedSocket == null || 
				oldSel == null || jsonHandler == null || playerGraph == null)
			throw new NullPointerException();
		if(usr.isEmpty() || oppoUdpPort < 0)
			throw new IllegalArgumentException();
		
		this.challengerSocket = challengerSocket;
		this.challengedSocket = challengedSocket;
		this.oppoUdpPort = oppoUdpPort;
		this.oldSel = oldSel;
		
		this.usr = usr;
		this.oppo = oppo;
		translHandler = new TranslationHandler();
		this.jsonHandler = jsonHandler;
		this.playerGraph = playerGraph;
		
		// Match timeout
		timeout = new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(MyUtilities.MATCH_DURATION_MILLIS);
				} catch (InterruptedException e) {
					return;
				}
				if(sel != null)
					sel.wakeup();
			}
		};
		
		// Used to send the challenge request
		try {
			serverUdpSocket = new DatagramSocket();
			serverUdpSocket.setSoTimeout(MyUtilities.TIMEOUT);
			sel = Selector.open();
		} catch (IOException ioe) {
			String rsp = jsonHandler.toJson(Collections.singletonMap(MyUtilities.SERVER_ERROR_CODE, MyUtilities.SETUP_MATCH));
			ErrorMacro.matchIoExceptionHandling(ioe, challengerSocket, oldSel, rsp, sel, serverUdpSocket);
		}
	}
	
	// Exit procedure; used before the BEGIN; only for challenger socket if its match request has been refused
	private void exitMatchProcedureREAD() {
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		ByteBuffer request = ByteBuffer.allocate(MyUtilities.BUFFER_SIZE);
		ByteBuffer[] ByteBuffers = { length, request };
		try {
			challengerSocket.register(oldSel, SelectionKey.OP_READ, ByteBuffers);
			oldSel.wakeup();
			try {
				if(sel != null)
					sel.close();
				if(serverUdpSocket != null)
					serverUdpSocket.close();
			} catch (IOException ioe) {
				ErrorMacro.softIoExceptionHandling(ioe);
			}
		} catch (ClosedChannelException cce) {
			ErrorMacro.closedChannelExceptionHandling(cce);
		}
	}
	
	
	private void exitMatchProcedure(SocketChannel sock) {
		String rsp = jsonHandler.toJson(Collections.singletonMap(MyUtilities.UNAUTHORIZED_CODE, MyUtilities.OPPO_LEFT));
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		length.putInt(rsp.length());
		length.flip();
		
		ByteBuffer rspB = ByteBuffer.wrap(rsp.getBytes());
		
		ByteBuffer[] byteBuffers = { length, rspB };
		try {
			sock.register(oldSel, SelectionKey.OP_WRITE, byteBuffers);
			oldSel.wakeup();
		} catch (ClosedChannelException cce) {
			ErrorMacro.closedChannelExceptionHandling(cce);
		}
	}
	
	// Send the ByteBuffers 
	private boolean send(SocketChannel sock, ByteBuffer length, ByteBuffer rsp) {
		try {
			//challengerSocket.configureBlocking(true);
			while(length.hasRemaining())
				sock.write(length);
			while(rsp.hasRemaining())
				sock.write(rsp);
		} catch (IOException ioe) {		// The client will never receive this message probably, mainly used to put the client back into the server main selector
			String srsp = jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.CHALLENGER_DISCONN));
			ErrorMacro.matchIoExceptionHandling(ioe, sock, oldSel, srsp, sel, serverUdpSocket);
			return false;
		}
		
		return true;
	}
	
	// It informs the client that it is waiting for the challenge to respond
	private boolean sendWaitForAccADV() {
		String waitForAccADV = jsonHandler.toJson(Collections.singletonMap(MyUtilities.SUCCESS_CODE, MyUtilities.MATCH_REQUEST_SEND));
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		length.putInt(waitForAccADV.length());
		length.flip();
		
		ByteBuffer rsp = ByteBuffer.wrap(waitForAccADV.getBytes());
		
		if(send(challengerSocket, length, rsp))
			return true;
		else 
			return false;
	}
	
	// Response received from the opponent: ACCEPTED or REFUSED
	private boolean sendChallengeResponse(String response) {
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		length.putInt(response.length());
		length.flip();
		
		ByteBuffer rsp = ByteBuffer.wrap(response.getBytes());
		
		if(send(challengerSocket, length, rsp))
			return true;
		else
			return false;
	}
	
	// After this both challenger and challenged will be ready for the match
	private boolean sendBegin() {
		String rsp = jsonHandler.toJson(Collections.singletonMap(MyUtilities.SUCCESS_CODE, MyUtilities.BEGIN_MATCH));
		
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		length.putInt(rsp.length());
		length.flip();
		
		ByteBuffer rspBuf = ByteBuffer.wrap(rsp.getBytes());
		
		boolean retvalF = send(challengerSocket, length, rspBuf);
		if(!retvalF)
			exitMatchProcedure(challengedSocket);
		
		length.rewind();
		rspBuf.rewind();
		
		boolean retvalS = send(challengedSocket, length, rspBuf);
		if(!retvalS)
			exitMatchProcedure(challengerSocket);
		
		return retvalF && retvalS;
	}
	
	// Set the sockets on the new selector so that the challenge can take place
	private void setWord(SocketChannel sock, String word) throws ClosedChannelException {
		String wordEncapsulated = jsonHandler.toJson(Collections.singletonMap(MyUtilities.TRANSLATE, word));
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		length.putInt(wordEncapsulated.length());
		length.flip();
		
		ByteBuffer tranWord = ByteBuffer.wrap(wordEncapsulated.getBytes());
		
		ByteBuffer[] byteBuffers = { length, tranWord };
		sock.register(sel, SelectionKey.OP_WRITE, byteBuffers);
	}
	
	// Simply send the word that must be translated; it works similarly to the main server send
	private boolean sendWord(SelectionKey key) {
		SocketChannel sock = (SocketChannel)key.channel();
		ByteBuffer[] byteBuffers = (ByteBuffer[])key.attachment();
		
		try {
			sock.write(byteBuffers[0]);
			if(!byteBuffers[0].hasRemaining()) {
				sock.write(byteBuffers[1]);
				if(!byteBuffers[1].hasRemaining()) {
					ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
					ByteBuffer request = ByteBuffer.allocate(MyUtilities.BUFFER_SIZE);
					ByteBuffer[] newByteBuffers = { length, request };
					sock.register(sel, SelectionKey.OP_READ, newByteBuffers);
				}
			}
		} catch (IOException ioe) {
			String rsp = jsonHandler.toJson(Collections.singletonMap(MyUtilities.SERVER_ERROR_CODE, MyUtilities.ERR_SENDING_WORD));
			ErrorMacro.matchIoExceptionHandling(ioe, sock, oldSel, rsp, sel, serverUdpSocket);
			
			if(sock.equals(challengerSocket))
				exitMatchProcedure(challengedSocket);
			else
				exitMatchProcedure(challengerSocket);
				
			return false;
		}
		
		return true;
	}
	
	// check if the translation sent by the client is correct
	private boolean correctTranslation(String translatedWord, LinkedList<String>[] words, int index) {
		if(translatedWord == null)
			return false;
		
		for(int i = 1; i < words[index].size(); i++)
			if(translatedWord.equals(words[index].get(i)))
				return true;
		
		return false;
	}
	
	// Read the translated word sent by the client and prepare the next word
	private boolean readTranslation(SelectionKey key, LinkedList<String>[] words) {
		SocketChannel sock = (SocketChannel)key.channel();
		ByteBuffer[] byteBuffers = (ByteBuffer[])key.attachment();
		
		try {
			long nread = sock.read(byteBuffers);
			if(nread < 0) {		// Disconnected
				String rsp = jsonHandler.toJson(Collections.singletonMap(MyUtilities.SERVER_ERROR_CODE, MyUtilities.ERR_RECEIVING_WORD));
				ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
				length.putInt(rsp.length());
				length.flip();
				
				ByteBuffer request = ByteBuffer.wrap(rsp.getBytes());
				
				ByteBuffer[] newByteBuffers = { length, request };
				
				// The client will never receive this message probably, mainly used to put the client back into the server main selector
				try {
					if(sel != null)
						sel.close();
					if(serverUdpSocket != null)
						serverUdpSocket.close();
				} catch (IOException ioe) {
					ErrorMacro.softIoExceptionHandling(ioe);
				}
				
				if(sock.equals(challengerSocket)) {
					try {
						challengerSocket.register(oldSel, SelectionKey.OP_WRITE, newByteBuffers);
						oldSel.wakeup();
					} catch (ClosedChannelException cce) {
						ErrorMacro.closedChannelExceptionHandling(cce);
					}
					exitMatchProcedure(challengedSocket);
				}
				else {
					try {
						challengedSocket.register(oldSel, SelectionKey.OP_WRITE, newByteBuffers);
						oldSel.wakeup();
					} catch (ClosedChannelException cce) {
						ErrorMacro.closedChannelExceptionHandling(cce);
					}
					exitMatchProcedure(challengerSocket);
				}
			
				return false;
			}
			
			if(!byteBuffers[0].hasRemaining()) {
				byteBuffers[0].flip();
				int length = byteBuffers[0].getInt();
				
				if(byteBuffers[1].position() == length) {
					byteBuffers[1].flip();
					Map<String, String> translationMap = 
							jsonHandler.fromJson(new String(byteBuffers[1].array()).trim());
					
					// It shouldn't be able to happen, but if the translation is null it returns false to the correctness check
					
					if(sock.equals(challengerSocket)) {
						if(correctTranslation(translationMap.get(MyUtilities.TRANSLATION), words, challengerIndex))
							challengerScore += 2;
						else
							challengerScore -= 1;
						challengerIndex ++;
						if(challengerIndex < 10)
							setWord(challengerSocket, words[challengerIndex].get(0));
						else
							challengerSocket.register(sel, 0);			// No left to do
					}
					else {
						if(correctTranslation(translationMap.get(MyUtilities.TRANSLATION), words, challengedIndex))
							challengedScore += 2;
						else
							challengedScore -= 1;
						challengedIndex++;
						if(challengedIndex < 10)
							setWord(challengedSocket, words[challengedIndex].get(0));
						else
							challengedSocket.register(sel, 0);			// No left to do
					}
				}
			}
		} catch (IOException ioe) {
			String rsp = jsonHandler.toJson(Collections.singletonMap(MyUtilities.SERVER_ERROR_CODE, MyUtilities.ERR_RECEIVING_WORD));
			ErrorMacro.matchIoExceptionHandling(ioe, sock, oldSel, rsp, sel, serverUdpSocket);
			
			if(sock.equals(challengerSocket))
				exitMatchProcedure(challengedSocket);
			else
				exitMatchProcedure(challengerSocket);
			
			return false;
		}
		
		return true;
	}
	
	/** When the timeout is received or both players have translated all the words, 
	*	set the latter to return to the main server selector.
	**/
	private void setControl(SocketChannel sock) {
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		ByteBuffer request = ByteBuffer.allocate(MyUtilities.BUFFER_SIZE);
		ByteBuffer[] ByteBuffers = { length, request };
		try {
			sock.register(oldSel, SelectionKey.OP_READ, ByteBuffers);
			oldSel.wakeup();
		} catch (ClosedChannelException cce) {
			ErrorMacro.closedChannelExceptionHandling(cce);
		}
	}
	
	// Simply send the result of the match at both players
	private void sendResult() {
		String res; 
		if(challengerScore > challengedScore)
			res = jsonHandler.toJson(Collections.singletonMap(MyUtilities.SUCCESS_CODE, usr + MyUtilities.DDOT + MyUtilities.WINNER +
					challengerScore + System.lineSeparator() + oppo + MyUtilities.DDOT + MyUtilities.LOSER + challengedScore));
		else if(challengerScore < challengedScore) {
			res = jsonHandler.toJson(Collections.singletonMap(MyUtilities.SUCCESS_CODE, oppo + MyUtilities.DDOT + MyUtilities.WINNER +
					challengedScore + System.lineSeparator() + usr + MyUtilities.DDOT + MyUtilities.LOSER + challengerScore));
		}
		else
			res = jsonHandler.toJson(Collections.singletonMap(MyUtilities.SUCCESS_CODE, MyUtilities.DRAW + challengerScore));
		
		/** Send the results of the challenge just ended without worrying about any errors 
		 * (following the send, the players will still be repositioned in the main selector of the server.
		**/
		
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		length.putInt(res.length());
		length.flip();
		
		ByteBuffer resBuf = ByteBuffer.wrap(res.getBytes());
		
		try {
			while(length.hasRemaining())
				challengerSocket.write(length);
			while(resBuf.hasRemaining())
				challengerSocket.write(resBuf);
		} catch (IOException ioe) {
			ErrorMacro.softIoExceptionHandling(ioe);
		} finally {
			length.rewind();
			resBuf.rewind();
			
			try {
			while(length.hasRemaining())
				challengedSocket.write(length);
			while(resBuf.hasRemaining())
				challengedSocket.write(resBuf);
			} catch (IOException ioee) {
				ErrorMacro.softIoExceptionHandling(ioee);
			}
		}
	}
	
	@Override
	public void run() {
		// Preparation of the challenge request
		String matchStr = jsonHandler.toJson(Collections.singletonMap(MyUtilities.MATCH, MyUtilities.MATCH_REQUEST_FROM + usr));
		DatagramPacket challengeRequest = new DatagramPacket(matchStr.getBytes(), matchStr.length(), InetAddress.getLoopbackAddress(), oppoUdpPort);
		
		// send the challenge request
		try {
			serverUdpSocket.send(challengeRequest);
			if(!sendWaitForAccADV())					// Match set up successful
				return;
			
			byte[] bufResponse = new byte[MyUtilities.BYTE_ARRAY_SIZE];
			DatagramPacket responseChallenge = new DatagramPacket(bufResponse, bufResponse.length);
			serverUdpSocket.receive(responseChallenge);
			
			String challengedResponse = new String(responseChallenge.getData()).trim();
			if(!sendChallengeResponse(challengedResponse)) {
				if(MyUtilities.MATCH_ACCEPTED.equals(jsonHandler.fromJson(challengedResponse).get(MyUtilities.MATCH)))
					exitMatchProcedure(challengedSocket);
				return;
			}
			
			if(!MyUtilities.MATCH_ACCEPTED.equals(jsonHandler.fromJson(challengedResponse).get(MyUtilities.MATCH))) {
				exitMatchProcedureREAD();
				return;
			}
			
		} catch (SocketTimeoutException ste) {
			String rsp = jsonHandler.toJson(Collections.singletonMap(MyUtilities.RETRY_WITH_CODE, MyUtilities.MATCH_NO_RESPONSE));
			ErrorMacro.matchSocketTimeoutExceptionReq(challengerSocket, oldSel, rsp, sel, serverUdpSocket);
			return;
		} catch (IOException ioe) {
			String rsp = jsonHandler.toJson(Collections.singletonMap(MyUtilities.SERVER_ERROR_CODE, MyUtilities.IOE_UDP));
			ErrorMacro.matchIoExceptionHandling(ioe, challengerSocket, oldSel, rsp, sel, serverUdpSocket);
			return;
		}

		// From this point on, in case of errors and / or exit, the sockets of both clients must be managed
		// Both expect the BEGIN
		
		LinkedList<String>[] words = null;
		
		try {
			challengedSocket.register(oldSel, 0);
			words = translHandler.getWords();
			
			for(int i = 0; i < words.length; i++)		// See the results
				System.out.println(words[i]);
				
		} catch (ClosedChannelException cce) {		// Handled as an IOException for convenience
			String rsp = jsonHandler.toJson(Collections.singletonMap(MyUtilities.SERVER_ERROR_CODE, MyUtilities.OPP_ERR));
			ErrorMacro.matchIoExceptionHandlingPAIR(cce, challengerSocket, challengedSocket, oldSel, rsp, sel, serverUdpSocket);
			return;
		} catch (IOException ioe) {
			String rsp = jsonHandler.toJson(Collections.singletonMap(MyUtilities.SERVER_ERROR_CODE, MyUtilities.TRANSLATE_SERVICE));
			ErrorMacro.matchIoExceptionHandlingPAIR(ioe, challengerSocket, challengedSocket, oldSel, rsp, sel, serverUdpSocket);
			return;
		}
		
		if(!sendBegin())
			return;

		// At this point the opponent accepted the challenge and they are both ready to start
		try {
			setWord(challengerSocket, words[challengerIndex].get(0));
			setWord(challengedSocket, words[challengedIndex].get(0));

			timeout.start();
			long timestamp = System.currentTimeMillis();
			while(running) {
				sel.select();
				
				if(System.currentTimeMillis() >= timestamp + MyUtilities.MATCH_DURATION_MILLIS)
					break;
				
				Set<SelectionKey> readyKeys = sel.selectedKeys();
				Iterator<SelectionKey> itr = readyKeys.iterator();
				
				while(itr.hasNext()) {
					SelectionKey key = itr.next();
					itr.remove();
					
					if(!key.isValid())
						continue;
					else if(key.isWritable()) {
						if(!sendWord(key))
							return;
					}
					else if(key.isReadable()) {
						if(!readTranslation(key, words))
							return;
					}
				}
				
				// If both have translated all the words, exit 
				if(challengerIndex == 10 && challengedIndex == 10) {
					timeout.interrupt();
					running = false;
				}
			}
		} catch (IOException ioe) {
			String rsp = jsonHandler.toJson(Collections.singletonMap(MyUtilities.SERVER_ERROR_CODE, MyUtilities.ERR_IO_SEL));
			ErrorMacro.matchIoExceptionHandlingPAIR(ioe, challengerSocket, challengedSocket, oldSel, rsp, sel, serverUdpSocket);
			return;
		} 
		
		if(challengerScore > challengedScore)
			challengerScore += 3;
		else if(challengerScore < challengedScore)
			challengedScore += 3;
		
		try {
			playerGraph.updateScore(usr, challengerScore);
			playerGraph.updateScore(oppo, challengedScore);
		} catch (NotExistingUsrException e) {
			// Should never happen
		}
		
		File graphPath = new File(MyUtilities.JSON_GRAPH_PATH);
		if(!graphPath.exists())
			try {
				graphPath.createNewFile();
			} catch (IOException e) {
				return;
			}
		jsonHandler.writeJSON(playerGraph, graphPath.toPath());
		
		sendResult();
		
		// Set the clients in the main selector
		setControl(challengerSocket);
		setControl(challengedSocket);
		try {
			if(sel != null)
				sel.close();
			if(serverUdpSocket != null)
				serverUdpSocket.close();
		} catch (IOException ioe) {
			ErrorMacro.softIoExceptionHandling(ioe);
		}
		
		return;
	}
}
