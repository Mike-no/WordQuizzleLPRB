/**
 * 
 * @author Michael De Angelis
 * @matricola: 560049
 * @project Word Quizzle
 * @A.A 2019 - 2020 [UNIPI]
 *
 */

import java.nio.channels.SocketChannel;

/** 
 * Class that represent a player of WQ, every registered player
 * will be placed in an undirected graph indicating the relationships 
 * between the users themselves.
 */
public class Player {
	// Username and password, the username is unique
	private String username;
	private String psw;
	
	private transient SocketChannel playerSocket = null;
	private transient int udpPort = -1;
	
	// Used to decide the operations to be performed based on its value
	private transient Status status = Status.offline;
	// Score of the player, used to compile the ranking of players
	private long score;
	
	/** Constructor
	 *  @param username
	 *  @param psw
	 *  @throws NullPointerException 	 : if username or psw are null
	 *  @throws IllegalArgumentException : if username or psw are empty
	 */
	public Player(String username, String psw) {
		if(username == null || psw == null)
			throw new NullPointerException(ErrorMacro.NULL_ARGS);
		if(username.isEmpty() || psw.isEmpty())
			throw new IllegalArgumentException();
		
		this.username = username;
		this.psw = psw;

		playerSocket = null;
		udpPort = -1;
		
		status = Status.offline;
		score = 0;
	}
	
	/** Constructor
	 *  @param username
	 *  @param psw
	 *  @param score
	 *  @throws NullPointerException : if username or psw are null
	 *  @throws IllegalArgumentException : if username or psw are empty
	 */
	public Player(String username, String psw, long score) {
		if(username == null || psw == null)
			throw new NullPointerException(ErrorMacro.NULL_ARGS);
		if(username.isEmpty() || psw.isEmpty())
			throw new IllegalArgumentException();
		
		this.username = username;
		this.psw = psw;
		
		playerSocket = null;
		udpPort = -1;
		
		status = Status.offline;
		this.score = score;
	}
	
	// Get the username of the player
	public String getUsr() {
		return username;
	}
	
	// Get the password of the player
	public String getPsw() {
		return psw;
	}
	
	// Get the player's socket
	public SocketChannel getPlayerSocket() {
		return playerSocket;
	}
	
	// Set the player's socket
	public void setPlayerSocket(SocketChannel sock) {
		playerSocket = sock;
	}
	
	// Get the player's udp port
	public int getUdpPort() {
		return udpPort;
	}
	
	// Set the player's udp port
	public void setUdpPort(int port) {
		udpPort = port;
	}
	
	// Get the status of the player
	public Status getStatus() {
		return status;
	}
	
	/** Simply sets the status of the player
	 *  @param newStatus : new state that the player will have to assume
	 */
	public void setStatus(Status newStatus) {
		status = newStatus;
	}
	
	/** A player's score cannot go below zero
	 *  @param points : points earned during the last challenge
	 */
	public void updateScore(int points) {
		score += points;
		if(score < 0)
			score = 0;
	}
	
	// Get the score of the player
	public long getScore() {
		return score;
	}
}
