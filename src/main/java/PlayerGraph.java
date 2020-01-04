/**
 * 
 * @author Michael De Angelis
 * @matricola: 560049
 * @project Word Quizzle
 * @A.A 2019 - 2020 [UNIPI]
 *
 */

import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Graph representing WQ players and close friendships between them
public class PlayerGraph {
	// encapsulates a player object and determines his friends via a link represented by a linked list
	private class Node {
		private Player player;
		private LinkedList<Player> adjacencyList;
		
		// Constructor used to create a brand new Node in the graph
		public Node(Player player) {
			if(player == null)
				throw new NullPointerException(ErrorMacro.NULL_ARG);
			
			this.player = player;
			adjacencyList = new LinkedList<Player>();
		}
		
		// Constructor used to update an existing Node in the graph
		public Node(Player player, LinkedList<Player> adjacencyList) {
			if(player == null || adjacencyList == null)
				throw new NullPointerException(ErrorMacro.NULL_ARGS);
			
			this.player = player;
			this.adjacencyList = adjacencyList;
		}
		
		public void setAdjacencyList(LinkedList<Player> adjacencyList) {
			this.adjacencyList = adjacencyList;
		}
	}
	
	// Implementation of the graph 
	private ConcurrentHashMap<String, Node> graph;
	
	public PlayerGraph() {
		graph = new ConcurrentHashMap<String, Node>();
	}
	
	// Initialiaze the Graph, used in deserialization
	public void initializeGraph() {
		graph.forEach((k, n) -> {
			n.player.setStatus(Status.offline);
			n.player.setPlayerSocket(null);
			n.player.setUdpPort(-1);
			
			// Update the references
			LinkedList<Player> newAdjacencyList = new LinkedList<Player>();
			for(Player p : n.adjacencyList) {
				String tmpUsr = p.getUsr();
				newAdjacencyList.add(graph.get(tmpUsr).player);
			}
			n.setAdjacencyList(newAdjacencyList);
		});
	}
	
	/**
	 * Add a player to the graph; the method is thread safe becuase we use the
	 * putIfAbsent function to create the new node of the graph itself.
	 * @param username
	 * @param psw
	 * @return true  : if the new player has been included in the graph
	 * 		   false : if the username was already taken
	 * @throws NullPointerException     : if usr or psw are null
	 * @throws IllegalArgumentException : if usr or psw are empty
	 */
	public boolean addPlayer(String usr, String psw) {
		if(usr == null || psw == null)
			throw new NullPointerException(ErrorMacro.NULL_ARGS);
		if(usr.isEmpty() || psw.isEmpty())
			throw new IllegalArgumentException(ErrorMacro.EMPTY_ARGS);
		
		if(graph.putIfAbsent(usr, new Node(new Player(usr, psw))) != null)
			return false;
		
		return true;
	}
	
	/**
	 * Add the socket parameter to a client; the method is thread safe becuase we use the
	 * computeIfAbsent function to update the Node in graph.
	 * @param usr
	 * @param addr
	 * @throws NullPointerException	    : if usr is null
	 * @throws IllegalArgumentException : if usr is empty
	 * @throws NotExistingUsrException  : the specified username (usr) is not present in the graph
	 */
	public void setSocketAddress(String usr, SocketChannel sock) throws NotExistingUsrException {
		if(usr == null)
			throw new NullPointerException();
		if(usr.isEmpty())
			throw new IllegalArgumentException();
		
		if(graph.computeIfPresent(usr, (k, n) -> {
			n.player.setPlayerSocket(sock);
			return new Node(n.player, n.adjacencyList);
		}) == null)
			throw new NotExistingUsrException();
	}
	
	/**
	 * Get the score of the specified Player; the method is thread safe due to the nature
	 * of the ConcurrentHashMap;
	 * @param usr
	 * @return
	 * @throws NullPointerException		: if usr is null
	 * @throws IllegalArgumentException : if usr is empty
	 * @throws NotExistingUsrException  : the specified username (usr) is not present in the graph
	 */
	public SocketChannel getSocketAddress(String usr) throws NotExistingUsrException {
		if(usr == null)
			throw new NullPointerException();
		if(usr.isEmpty())
			throw new IllegalArgumentException();
		
		Node tmp = graph.get(usr);
		if(tmp == null)
			throw new NotExistingUsrException();
		
		return tmp.player.getPlayerSocket();
	}
	
	
	/**
	 * Check if the end point passed as an argument is already logged in
	 * @param sock
	 * @return true  : if an user from that end point is already logged in
	 * 		   false : if there is no user logged in on that end point
	 */
	public boolean existsSocket(SocketChannel sock) {
		if(sock == null)
			throw new NullPointerException();
		
		@SuppressWarnings("rawtypes") // Safe cast
		Iterator itr = graph.entrySet().iterator();
		while(itr.hasNext()) {
			@SuppressWarnings("rawtypes") // Safe cast
			Map.Entry pair = (Map.Entry)itr.next();
			Node n = (Node)pair.getValue();
			if(n.player.getPlayerSocket() != null && n.player.getPlayerSocket().equals(sock))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Add the port parameter to a client; the method is thread safe becuase we use the
	 * computeIfAbsent function to update the Node in graph.
	 * @param usr
	 * @param port
	 * @throws NullPointerException		: if usr is null
	 * @throws IllegalArgumentException : if usr is empty
	 * @throws NotExistingUsrException  : the specified username (usr) is not present in the graph
	 */
	public void setUdpPort(String usr, int udpPort) throws NotExistingUsrException {
		if(usr == null)
			throw new NullPointerException();
		if(usr.isEmpty())
			throw new IllegalArgumentException();
		
		if(graph.computeIfPresent(usr, (k, n) -> {
			n.player.setUdpPort(udpPort);;
			return new Node(n.player, n.adjacencyList);
		}) == null)
			throw new NotExistingUsrException();
	}
	
	/**
	 * Get the score of the specified Player; the method is thread safe due to the nature
	 * of the ConcurrentHashMap;
	 * @param usr
	 * @return
	 * @throws NullPointerException		: if usr is null
	 * @throws IllegalArgumentException : if usr is empty
	 * @throws NotExistingUsrException  : the specified username (usr) is not present in the graph
	 */
	public int getUdpPort(String usr) throws NotExistingUsrException {
		if(usr == null)
			throw new NullPointerException();
		if(usr.isEmpty())
			throw new IllegalArgumentException();
		
		Node tmp = graph.get(usr);
		if(tmp == null)
			throw new NotExistingUsrException();
		
		return tmp.player.getUdpPort();
	}
	
	/**
	 * Make a user online; the method is thread safe due to the concurrentHashMap nature
	 * and the fact that a single thread will call the login function for each player.
	 * @param usr
	 * @param psw
	 * @return true  : the specified user has been marked as online
	 * 		   false : the specified user is already online
	 * @throws NullPointerException     : if usr or psw are null
	 * @throws IllegalArgumentException : if usr or psw are empty
	 * @throws NotExistingUserException : the specified username is not present in the graph
	 * @throws WrongPswException		: the specified password does not match the username
	 */
	public boolean login(String usr, String psw) throws NotExistingUsrException, WrongPswException {
		if(usr == null || psw == null)
			throw new NullPointerException(ErrorMacro.NULL_ARGS);
		if(usr.isEmpty() || psw.isEmpty())
			throw new IllegalArgumentException(ErrorMacro.EMPTY_ARGS);
		
		Node tmp = graph.get(usr);
		if(tmp == null)
			throw new NotExistingUsrException();
		
		if(!tmp.player.getPsw().equals(psw))
			throw new WrongPswException();
			
		if(tmp.player.getStatus() == Status.online)
			return false;
		
		// No need to check if the key is present because we know that from the previous get call
		graph.compute(usr, (k, n) -> {
			n.player.setStatus(Status.online);
			return new Node(n.player, n.adjacencyList);
			});
			
		return true;
	}
	
	/**
	 * Make a user offline; the method is thread safe due to the concurrentHashMap nature
	 * and the fact that a single thread will call the logout function for each player.
	 * @param usr
	 * @return true  : the specified user has been marked as offline
	 * 		   false : the specified user is already offline
	 * @throws NullPointerException     : if usr is null
	 * @throws IllegalArgumentException : if usr is empty
	 * @throws NotExistingUserException : the specified username is not present in the graph
	 */
	public boolean logout(String usr) throws NotExistingUsrException {
		if(usr == null)
			throw new NullPointerException(ErrorMacro.NULL_ARG);
		if(usr.isEmpty())
			throw new IllegalArgumentException(ErrorMacro.USR_EMPTY);
		
		Node tmp = graph.get(usr);
		if(tmp == null)
			throw new NotExistingUsrException();
		
		if(tmp.player.getStatus() == Status.offline)
			return false;
		
		// No need to check if the key is present because we know that from the previous get call
		graph.compute(usr, (k, n) -> {
			n.player.setStatus(Status.offline);
			return new Node(n.player, n.adjacencyList);
		});
		
		return true;
	}
	
	/**
	 * Return if a user is online or not.
	 * @param usr
	 * @return true  : the specified user is online
	 * 		   false : the specified user is offline
	 * @throws NullPointerException		: if usr is null
	 * @throws IllegalArgumentException : if usr is empty
	 * @throws NotExistingUsrException  : the specified username is not present in the graph
	 */
	public boolean isOnline(String usr) throws NotExistingUsrException {
		if(usr == null)
			throw new NullPointerException();
		if(usr.isEmpty())
			throw new IllegalArgumentException();
		
		Node tmp = graph.get(usr);
		if(tmp == null)
			throw new NotExistingUsrException();
		
		if(tmp.player.getStatus() == Status.online)
			return true;
		else
			return false;
	}
	
	/**
	 * Used in case of error in reading / writing on the user's socket and it is necessary 
	 * to put the user offline.
	 * @param addr
	 * @throws NullPointerException : if addr is null
	 */
	public void ifOnlineSetOffline(SocketChannel sock) {
		if(sock == null)
			throw new NullPointerException();
		
		graph.forEach((k, n) -> {
			if(n.player.getPlayerSocket() != null)
				if(n.player.getPlayerSocket().equals(sock) && 
						n.player.getStatus() == Status.online) {
					n.player.setStatus(Status.offline);
					n.player.setPlayerSocket(null);
					n.player.setUdpPort(-1);
				}
		});
	}
	
	/**
	 * Simply return the adjacency list (friends) of the specified usr; the method is thread safe due to 
	 * the concurrentHashMap nature and the fact that a single thread will call the getAdjacencyList 
	 * function for each player.
	 * @param usr
	 * @return AdjacencyList of the specified Player
	 * @throws NullPointerException     : if usr is null
	 * @throws IllegalArgumentException : if usr is empty
	 * @throws NotExistingUserException : the specified username is not present in the graph
	 */
	public LinkedList<Player> getAdjacencyList(String usr) throws NotExistingUsrException {
		if(usr == null)
			throw new NullPointerException(ErrorMacro.NULL_ARG);
		if(usr.isEmpty())
			throw new IllegalArgumentException(ErrorMacro.USR_EMPTY);
		
		Node tmp = graph.get(usr); 
		if(tmp == null)
			throw new NotExistingUsrException();
		
		return tmp.adjacencyList;
	}
	
	/**
	 * 
	 * @param usr
	 * @return
	 * @throws NullPointerException     : if usr is null
	 * @throws IllegalArgumentException : if usr is empty 
	 * @throws NotExistingUsrException  : the specified username (usr) is not present in the graph
	 */
	public LinkedList<Player> getRankingList(String usr) throws NotExistingUsrException {
		if(usr == null)
			throw new NullPointerException();
		if(usr.isEmpty())
			throw new IllegalArgumentException();
		
		Node tmp = graph.get(usr);
		if(tmp == null)
			throw new NotExistingUsrException();
		
		LinkedList<Player> retLst = new LinkedList<Player>(tmp.adjacencyList);
		retLst.add(tmp.player);
		
		return retLst;
	}
	
	/**
	 * Indicates whether two players are friends; the method is thread safe due to 
	 * the concurrentHashMap nature and the fact that a single thread will call the isFriendOf 
	 * function for each player.
	 * @param usr
	 * @param usrFriend
	 * @return true  : the specified users are friends
	 * 		   false : the specified users are not friends
	 * @throws NullPointerException     : if usr or usrFriend are null
	 * @throws IllegalArgumentException : if usr or usrFriend are empty
	 * @throws NotExistingUserException : the specified username (usr) is not present in the graph
	 */
	public boolean isFriendOf(String usr, String usrFriend) throws NotExistingUsrException {
		if(usrFriend == null)
			throw new NullPointerException(ErrorMacro.NULL_ARG);
		if(usrFriend.isEmpty())
			throw new IllegalArgumentException(ErrorMacro.USR_EMPTY);
		
		LinkedList<Player> tmp = getAdjacencyList(usr);
		for(Player p : tmp) {
			if(p.getUsr().equals(usrFriend))
				return true;
		}
		
		return false;	
	}
	
	/**
	 * Make a connection between two players by making them friends; the method is thread safe due to 
	 * the concurrentHashMap nature and the fact that a single thread will call the addLink function for 
	 * each player.
	 * @param usr
	 * @param usrFriend
	 * @return true  : the specified users are now friends
	 * 		   false : the specified users were already friends
	 * @throws NullPointerException     : if usr or usrFriend are null
	 * @throws IllegalArgumentException : if usr or usrFriend are empty
	 * @throws NotExistingUserException : the specified usernames (usr or usrFriend) are not present in the graph
	 */
	public boolean addLink(String usr, String usrFriend) throws NotExistingUsrException {
		if(isFriendOf(usr, usrFriend))
			return false;
		
		if(graph.get(usrFriend) == null)
			throw new NotExistingUsrException();
		
		// No need to check if the key is present because we know that from the previous call
		graph.compute(usr, (k, n) -> {
			n.adjacencyList.add(graph.get(usrFriend).player);
			return new Node(n.player, n.adjacencyList);
		});
		graph.compute(usrFriend, (k, n) -> {
			n.adjacencyList.add(graph.get(usr).player);
			return new Node(n.player, n.adjacencyList);
		});
		
		return true;
	}
	
	/**
	 * Update the score of the specified player; the method is thread safe because we
	 * use the method computeIfPresent to update the Node in graph.
	 * @param usr
	 * @param points
	 * @throws NotExistingUsrException 
	 * @throws NullPointerException     : if usr is null
	 * @throws IllegalArgumentException : if usr is empty
	 * @throws NotExistingUserException : the specified username (usr) is not present in the graph
	 */
	public void updateScore(String usr, int points) throws NotExistingUsrException {
		if(usr == null)
			throw new NullPointerException(ErrorMacro.NULL_ARG);
		if(usr.isEmpty())
			throw new IllegalArgumentException(ErrorMacro.USR_EMPTY);
		
		if(graph.computeIfPresent(usr, (k, n) -> {
			n.player.updateScore(points);
			return new Node(n.player, n.adjacencyList);
		}) == null)
			throw new NotExistingUsrException();
	}
	
	/**
	 * Get the score of the specified Player; the method is thread safe due to the nature
	 * of the ConcurrentHashMap;
	 * @param usr
	 * @return
	 * @throws NullPointerException     : if usr is null
	 * @throws IllegalArgumentException : if usr is empty
	 * @throws NotExistingUserException : the specified username (usr) is not present in the graph
	 */
	public long getScore(String usr) throws NotExistingUsrException {
		if(usr == null)
			throw new NullPointerException(ErrorMacro.NULL_ARG);
		if(usr.isEmpty())
			throw new IllegalArgumentException(ErrorMacro.USR_EMPTY);
		
		Node tmp = graph.get(usr);
		if(tmp == null)
			throw new NotExistingUsrException();
		
		return tmp.player.getScore();
	}
}
