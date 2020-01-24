/**
 * 
 * @author Michael De Angelis
 * @matricola: 560049
 * @project Word Quizzle
 * @A.A 2019 - 2020 [UNIPI]
 *
 */

import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class WqServer {
	private File jsonGraphPath;
	private JsonHandler jsonHandler;
	private ToClientLst clientLst;
	private PlayerGraph playerGraph;
	
	private ServerSocketChannel serverSocket = null;
	private Selector sel = null;
	private SelectionKey serverKey = null;
	
	private RmiSignUpService rmiSignUpServ;
	private Registry rmiReg;
	
	private boolean rmiServiceOnline = false;
	private boolean running = true;
	
	private int ntcpConnections;
	
	private ThreadPoolExecutor executor;
	
	// Implementation of the RMI service for the players sign up
	private class RmiSignUpService extends RemoteServer implements WqSignUp {
		private static final long serialVersionUID = 8828534552177727445L;

		@Override
		public String signUp(String usr, String psw) throws RemoteException {
			if(usr == null || psw == null)
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.NULL_ARGS));
			if(usr.isEmpty() || psw.isEmpty() || usr.equals(MyUtilities.SPACE) || psw.equals(MyUtilities.SPACE))
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.EMPTY_ARGS));
			
			System.out.println(MyUtilities.SIGN_UP_REQUEST + usr + MyUtilities.SPLIT + psw);
			
			// If the new player is added, save the new information regarding the sign up on the json
			if(playerGraph.addPlayer(usr, psw)) {
				createIfNotExists();
				jsonHandler.writeJSON(playerGraph, jsonGraphPath.toPath());
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.SUCCESS_CODE, SUCCESSFUL_SIGN_UP));
			}
			else
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.RETRY_WITH_CODE, EXISTING_USER));
		}
	}
	
	public WqServer() {
		jsonGraphPath = new File(MyUtilities.JSON_GRAPH_PATH);
		jsonHandler = new JsonHandler();
		clientLst = new ToClientLst();
		ntcpConnections = 0;
		
		executor = (ThreadPoolExecutor)Executors.newCachedThreadPool();
		
		// Loads, if exists, a previous state of the graph (persistent data) 
		try {
			if(jsonGraphPath.exists())
				if(jsonGraphPath.length() != 0) {
					playerGraph = jsonHandler.readJSON(jsonGraphPath.toPath());
					playerGraph.initializeGraph();
				}
				else
					playerGraph = new PlayerGraph();
			else {
				jsonGraphPath.createNewFile();
				playerGraph = new PlayerGraph();
			}			
			
			// Initialize selector and serverSocket [ NIO ]
			sel = Selector.open();
			serverSocket = ServerSocketChannel.open();
			serverSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), MyUtilities.TCP_CONTROL_PORT));
			serverSocket.configureBlocking(false);
			serverKey = serverSocket.register(sel, SelectionKey.OP_ACCEPT);
		} catch(IOException ioe) {
			try {
				tryClose();	// Try to close selector and serverSocket, error(s) occurred
			} catch (IOException ioee) {
				ErrorMacro.softIoExceptionHandling(ioee);
			}
			ErrorMacro.ioExceptionHandling(ioe);
		}		
		
		setShutDownHook();		// Graceful shutdown 
		
		runRMIService();		// Start the RMI sign up service
	}
	
	// Before returning the graph to the json file, make sure that it exists
	private void createIfNotExists() {
		if(!jsonGraphPath.exists())
			try {
				jsonGraphPath.createNewFile();
			} catch (IOException ioe) {
				ErrorMacro.ioExceptionHandling(ioe);
			}
	}
	
	// Try to close sel, serverSocket and the other clients socket; called before shut down
	private void tryClose() throws IOException {
		if(serverSocket != null) {
			serverSocket.close();
			if(serverKey != null) {
				serverKey.cancel();
			
				for(SelectionKey key : sel.keys()) {
					key.channel().close();
					key.cancel();
				}
			}
			sel.close();
		}
		else if(sel != null)
			sel.close();
	}
	
	// Graceful server shut down
	private void setShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
		    public void run() {
		    	// Stops the server main loop
		        running = false;
		        
		        // Shuts down the RMI sign up Server
		        try {
			        if(rmiServiceOnline) {
			        	rmiReg.unbind(MyUtilities.SERVER_SIGN_UP_NAME);
			        	UnicastRemoteObject.unexportObject(rmiSignUpServ, true);
			        }
		        } catch(RemoteException re) {
		        	ErrorMacro.remoteExceptionHandling(re);
		        } catch (NotBoundException nbe) {
		        	ErrorMacro.notBoundExceptionHandling(nbe);
				} finally {					// Wait for thread termination
					executor.shutdown();
					try {
						executor.awaitTermination(MyUtilities.MATCH_DURATION, TimeUnit.MINUTES);
					} catch (InterruptedException ie) {
						ErrorMacro.InterruptedExceptionHandling(ie);
					}
					finally {
						try {
							tryClose();		// Try to close socket(s) and selector	
						} catch(IOException ioe) {
							ErrorMacro.softIoExceptionHandling(ioe);
						}
					}
		        }
		    }
		}));
	}
	
	// Performs the rmi service allowing players to sign up.
	private void runRMIService() {
		// Export the object RmiSignUpService and bind it to a registry so it will be used to accept the sign up requests
		try {
			rmiSignUpServ = new RmiSignUpService();
			WqSignUp stub = (WqSignUp)UnicastRemoteObject.exportObject(rmiSignUpServ, 0);
			
			rmiReg = LocateRegistry.createRegistry(MyUtilities.SIGN_UP_PORT);
			Registry reg = LocateRegistry.getRegistry(MyUtilities.SIGN_UP_PORT);
			
			reg.rebind(MyUtilities.SERVER_SIGN_UP_NAME, stub);
			
			rmiServiceOnline = true;
		} catch(RemoteException re) {
			// RemoteException occurred in the initial phase, exit
			re.printStackTrace();
			System.err.println(ErrorMacro.RMI_REMOTE_EXCEPTION);
			System.exit(-2);
		}
	}
	
	// Server main loop, accepts TCP connection requests and and executes player requests
	public void runWqService() {
		while(running) {
			try {
				sel.select();
			} catch (IOException ioe) {
				ErrorMacro.ioExceptionHandling(ioe);
			}
			
			Set<SelectionKey> readyKeys = sel.selectedKeys();
			Iterator<SelectionKey> itr = readyKeys.iterator();
			
			while(itr.hasNext()) {
				SelectionKey key = itr.next();
				itr.remove();
				
				if(!key.isValid())
					continue;
				else if(key.isAcceptable()) {
					acceptPlayerConn();				// Accepts a new connection
				}
				else if(key.isReadable()) {
					handlePlayerRequest(key);		// Read player's request
				}
				else if(key.isWritable()) {
					serverResponse(key);			// Write response to player
				}
			}
		}
	}
	
	// Accepts new tcp connection request and print some player's information
	private void acceptPlayerConn() {
		try {
			SocketChannel dataSocket = serverSocket.accept();
			dataSocket.configureBlocking(false);
			ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
			ByteBuffer request = ByteBuffer.allocate(MyUtilities.BUFFER_SIZE);
			ByteBuffer[] byteBuffers = { length, request };
			// Registers the client to the selector with the ByteBuffer vector attached
			dataSocket.register(sel, SelectionKey.OP_READ, byteBuffers);
			
			// Print some useful information
			System.out.println("New Player connected : " + dataSocket.getRemoteAddress());
			
			ntcpConnections++;
			System.out.println("Current number of connections : " + ntcpConnections);
		} catch (IOException ioe) {
			ErrorMacro.ioExceptionHandling(ioe);
		}
	}
	
	/**
	 * Check if the request received respects the correct format
	 * @param rqstValue
	 * @return null   : correct request
	 * 		   String : uncorrect request
	 */
	private String checkStringCorrectness(String rqstValue) {
		if(rqstValue == null)			// It shouldn't be possible for it to happen
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.NULL_ARGS));
		if(rqstValue.isEmpty() || rqstValue.equals(MyUtilities.SPACE) || rqstValue.startsWith(MyUtilities.SPLIT) 
				|| rqstValue.contains(MyUtilities.SPLIT + MyUtilities.SPLIT))
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.EMPTY_ARGS));
	
		return null;
	}
	
	/**
	 * Manages login requests received from clients.
	 * @param rqstValue
	 * @return response to send to the client regarding the result of the request 
	 */
	private String handleLogin(String rqstValue, SocketChannel sock) {
		String retval = checkStringCorrectness(rqstValue);
		if(retval != null)
			return retval;
		
		String[] loginPar = rqstValue.split(MyUtilities.SPLIT);
		if(loginPar.length != 3)		// There must be two fields: usr, psw, udpPort
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.MISSING_ARGS));
		
		if(playerGraph.existsSocket(sock))
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.LOGOUT_FIRST));
		
		try {	
			if(playerGraph.login(loginPar[0], loginPar[1])) {
				playerGraph.setSocketAddress(loginPar[0], sock);
				playerGraph.setUdpPort(loginPar[0], Integer.parseInt(loginPar[2]));
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.SUCCESS_CODE, loginPar[0] + MyUtilities.ONLINE));
			}
			else
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.UNAUTHORIZED_CODE, MyUtilities.ALREADY_ONLINE));
		} catch (NotExistingUsrException e) {
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.NOT_EXISTING_USR));
		} catch (WrongPswException e) {
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.WRONG_PSW));
		}	
	}
	
	/**
	 * Manages logout requests received from clients.
	 * @param rqstValue
	 * @return response to send to the client regarding the result of the request 
	 */
	private String handleLogout(String rqstValue) {
		String retval = checkStringCorrectness(rqstValue);
		if(retval != null)
			return retval;
		
		try {
			if(playerGraph.logout(rqstValue)) {
				playerGraph.setSocketAddress(rqstValue, null);
				playerGraph.setUdpPort(rqstValue, -1);
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.SUCCESS_CODE, rqstValue + MyUtilities.OFFLINE)); 
			}
			else
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.UNAUTHORIZED_CODE, MyUtilities.ALREADY_OFFLINE)); 
		} catch (NotExistingUsrException e) {
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.NOT_EXISTING_USR)); 
		}
	}
	
	/**
	 * Manages add friend request received from clients.
	 * @param rqstValue
	 * @return response to send to the client regarding the result of the request 
	 */
	private String handleAddFriend(String rqstValue) {
		String retval = checkStringCorrectness(rqstValue);
		if(retval != null)
			return retval;
		
		String[] addFriendPar = rqstValue.split(MyUtilities.SPLIT);
		if(addFriendPar.length != 2)		// There must be two fields: usr, usrFriend
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.MISSING_ARGS));
		
		if(addFriendPar[0].equals(addFriendPar[1]))
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.CANNOT_ADD_YOURSELF));
		
		try {
			if(!playerGraph.isOnline(addFriendPar[0]))
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.OFFLINE_USR));

			if(playerGraph.addLink(addFriendPar[0], addFriendPar[1])) {
				createIfNotExists();
				jsonHandler.writeJSON(playerGraph, jsonGraphPath.toPath());
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.SUCCESS_CODE, MyUtilities.ADDED));
			}
			else
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.ALREADY_FRIEND));
		} catch (NotExistingUsrException e) {
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.NOT_EXISTING_USR));
		}
	}
	
	/**
	 * Manages friend list request received from clients.
	 * @param rqstValue
	 * @return response to send to the client regarding the result of the request 
	 */
	private String handleFriendList(String rqstValue) {
		String retval = checkStringCorrectness(rqstValue);
		if(retval != null)
			return retval;
		
		try {
			if(!playerGraph.isOnline(rqstValue))
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.OFFLINE_USR));
			
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.SUCCESS_CODE, clientLst.clientLst(playerGraph.getAdjacencyList(rqstValue))));
		} catch (NotExistingUsrException e) {
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.NOT_EXISTING_USR));
		}
	}
	
	/**
	 * 
	 * @param rqstValue
	 * @return response to send to the client regarding the result of the request 
	 */
	private String handleMatch(String rqstValue, SelectionKey key) {
		String retval = checkStringCorrectness(rqstValue);
		if(retval != null)
			return retval;
		
		String[] matchPair = rqstValue.split(MyUtilities.SPLIT);
		if(matchPair.length != 2)		// There must be two fields: usr, usrFriend
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.MISSING_ARGS));
	
		if(matchPair[0].equals(matchPair[1]))
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.CANNOT_MATCH_YSELF));
		
		try {
			if(!playerGraph.isOnline(matchPair[0]))
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.OFFLINE_USR));
			
			if(!playerGraph.isOnline(matchPair[1]))
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.RETRY_WITH_CODE, MyUtilities.NOT_ONLINE));
			
			if(!playerGraph.isFriendOf(matchPair[0], matchPair[1]))
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.NOT_FRIEND));
		
			key.interestOps(0);
			SocketChannel challengedSocket = playerGraph.getSocketAddress(matchPair[1]);	// If online it has an associated socket != null
			executor.execute(new MatchHandler(matchPair[0], matchPair[1], (SocketChannel)key.channel(), 
					challengedSocket, playerGraph.getUdpPort(matchPair[1]), sel, jsonHandler, playerGraph));
		
			return null;
		} catch (NotExistingUsrException e) {
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.NOT_EXISTING_USR));
		}
	}
	
	/**
	 * Manages show score request received from clients.
	 * @param rqstValue
	 * @return response to send to the client regarding the result of the request 
	 */
	private String handleShowScore(String rqstValue) {
		String retval = checkStringCorrectness(rqstValue);
		if(retval != null)
			return retval;
		
		try {
			long usrScore = playerGraph.getScore(rqstValue);
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.SUCCESS_CODE, MyUtilities.USER_SCORE + String.valueOf(usrScore)));
		} catch (NotExistingUsrException e) {
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.NOT_EXISTING_USR));
		}
	}
	
	/**
	 * Manages show ranking request received from clients.
	 * @param rqstValue
	 * @return response to send to the client regarding the result of the request 
	 */
	public String handleShowRanking(String rqstValue) {
		String retval = checkStringCorrectness(rqstValue);
		if(retval != null)
			return retval;
		
		try {
			if(!playerGraph.isOnline(rqstValue))
				return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.OFFLINE_USR));
			
			LinkedList<Player> tmpLst = playerGraph.getRankingList(rqstValue);
			Collections.sort(tmpLst, new Comparator<Player>() {
				public int compare(Player p1, Player p2) {
					if(p1.getScore() < p2.getScore())
						return 1;
					else if(p1.getScore() > p2.getScore())
						return -1;
					else 
						return 0;
				}
			});
			
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.SUCCESS_CODE, clientLst.clientLst(tmpLst)));
		} catch (NotExistingUsrException e) {
			return jsonHandler.toJson(Collections.singletonMap(MyUtilities.CLIENT_ERROR_CODE, MyUtilities.NOT_EXISTING_USR));
		}
	}
	
	/**
	 * Attach the response ByteBuffers to the client and record it for writing in the selector
	 * @param dataSocket
	 * @param response
	 * @throws ClosedChannelException
	 */
	private void setResponse(SocketChannel dataSocket, String response) throws ClosedChannelException {
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		length.putInt(response.length());
		length.flip();	// Prepare for read
		
		ByteBuffer rsp = ByteBuffer.wrap(response.getBytes());
		
		ByteBuffer[] byteBuffers = { length, rsp };
		dataSocket.register(sel, SelectionKey.OP_WRITE, byteBuffers);
	}
	
	/**
	 * Reads and executes client requests; then builds an appropriate response that will be sent to the latter
	 * @param key
	 */
	private void handlePlayerRequest(SelectionKey key) {
		SocketChannel dataSocket = (SocketChannel)key.channel();
		ByteBuffer[] byteBuffers = (ByteBuffer[])key.attachment();
		SocketAddress addr = null;
		
		try {
			// Used in case of error reading on the user's socket and it is necessary to put the user offline
			addr = dataSocket.getRemoteAddress();
			
			long nread = dataSocket.read(byteBuffers);
			if(nread < 0) {
				System.out.println(dataSocket.getRemoteAddress() + ErrorMacro.DISCONNECTED);
				playerGraph.ifOnlineSetOffline(dataSocket);
				key.cancel();
				key.channel().close();
				ntcpConnections--;
				return;
			}
			
			if(!byteBuffers[0].hasRemaining()) {
				byteBuffers[0].flip();
				int length = byteBuffers[0].getInt();			// Length of the request
				
				if(byteBuffers[1].position() == length) {		// Check if the request has been read completely
					byteBuffers[1].flip();
					Map<String, String> request = 
							jsonHandler.fromJson(new String(byteBuffers[1].array()).trim());
					
					if(request.containsKey(MyUtilities.LOGIN)) {
						System.out.println(MyUtilities.LOGIN_REQUEST + addr);
						String response = handleLogin(request.get(MyUtilities.LOGIN), dataSocket);
						setResponse(dataSocket, response);
					}
					else if(request.containsKey(MyUtilities.LOGOUT)) {
						System.out.println(MyUtilities.LOGOUT_REQUEST + addr);
						String response = handleLogout(request.get(MyUtilities.LOGOUT));
						setResponse(dataSocket, response);
					}
					else if(request.containsKey(MyUtilities.ADD_FRIEND)) {
						System.out.println(MyUtilities.ADD_FRIEND_REQUEST + addr);
						String response = handleAddFriend(request.get(MyUtilities.ADD_FRIEND));
						setResponse(dataSocket, response);
					}
					else if(request.containsKey(MyUtilities.FRIEND_LIST)) {
						System.out.println(MyUtilities.FRIEND_LIST_REQUEST + addr);
						String response = handleFriendList(request.get(MyUtilities.FRIEND_LIST)); 
						setResponse(dataSocket, response);
					}
					else if(request.containsKey(MyUtilities.MATCH)) {
						System.out.println(MyUtilities.MATCH_REQUEST + addr);
						String response = handleMatch(request.get(MyUtilities.MATCH), key);
						if(response != null)
							setResponse(dataSocket, response);
					}
					else if(request.containsKey(MyUtilities.SHOW_SCORE)) {
						System.out.println(MyUtilities.SHOW_SCORE_REQUEST + addr);
						String response = handleShowScore(request.get(MyUtilities.SHOW_SCORE));
						setResponse(dataSocket, response);
					}
					else if(request.containsKey(MyUtilities.SHOW_RANKING)) {
						System.out.println(MyUtilities.SHOW_RANKING_REQUEST + addr);
						String response = handleShowRanking(request.get(MyUtilities.SHOW_RANKING));
						setResponse(dataSocket, response);
					}
					/** If client logic works well this event should not happen; in case it happens, 
					 * it is stopped without returning errors of any kind to the client
					 */
					else if(request.containsKey(MyUtilities.TRANSLATION)) {
						ByteBuffer lengthT = ByteBuffer.allocate(Integer.BYTES);
						ByteBuffer requestT = ByteBuffer.allocate(MyUtilities.BUFFER_SIZE);
						ByteBuffer[] newByteBuffers = { lengthT, requestT };
						dataSocket.register(sel, SelectionKey.OP_READ, newByteBuffers);
					}
					else {
						String response = 
								jsonHandler.toJson(Collections.singletonMap(MyUtilities.NOT_IMPLEMENTED_CODE, MyUtilities.UNKNOWN_RQST));
						setResponse(dataSocket, response);
					}
				}
			}
		} catch (IOException ioe) {
			ntcpConnections--;
			if(dataSocket != null)
				playerGraph.ifOnlineSetOffline(dataSocket);
			ErrorMacro.rqstIoExceptionKeyHandling(ioe, key);
		}
	}
	
	/**
	 * Sends the responses generated by the various handlers to the client and attach a new ByteBuffers 
	 * to the client and record it for reading a new request in the selector
	 * @param key
	 */
	private void serverResponse(SelectionKey key) {
		SocketChannel dataSocket = (SocketChannel)key.channel();
		ByteBuffer[] byteBuffers = (ByteBuffer[])key.attachment();
		
		try {
			dataSocket.write(byteBuffers[0]);
			if(!byteBuffers[0].hasRemaining()) {		// If you have finished sending the size of the actual answer
				dataSocket.write(byteBuffers[1]);
				if(!byteBuffers[1].hasRemaining()) {	// If you have finished sending the reply
					byteBuffers[1].rewind();
					String isLogout = new String(byteBuffers[1].array()).trim();
					if(isLogout.contains(MyUtilities.OFFLINE)) {
						dataSocket.close();
						key.cancel();
						ntcpConnections--;
					}
					else {
						ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
						ByteBuffer request = ByteBuffer.allocate(MyUtilities.BUFFER_SIZE);
						ByteBuffer[] newByteBuffers = { length, request };
						dataSocket.register(sel, SelectionKey.OP_READ, newByteBuffers);
					}
				}
			}
		} catch (IOException ioe) {
			ntcpConnections--;
			if(dataSocket != null)
				playerGraph.ifOnlineSetOffline(dataSocket);
			ErrorMacro.respIoExceptionKeyHandling(ioe, key);
		}
	}
	
	// Run the Server
	public static void main(String[] args) {
		WqServer wqServer = new WqServer();
		
		System.out.println("RMI Sign up service online on port " + MyUtilities.SIGN_UP_PORT);
		System.out.println("WQ Server online on port " + MyUtilities.TCP_CONTROL_PORT + System.lineSeparator());
		
		wqServer.runWqService();
	}
}
