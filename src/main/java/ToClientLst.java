/**
 * 
 * @author Michael De Angelis
 * @matricola: 560049
 * @project Word Quizzle
 * @A.A 2019 - 2020 [UNIPI]
 *
 */

import java.lang.reflect.Type;
import java.util.LinkedList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

// Support class built for the sole purpose of not returning to the client a list of players containing their passwords
public class ToClientLst {
	public class PlayerFriends {
		private String username;
		private long score;
		
		/**
		 * Constructor
		 * @param username
		 * @param score
		 * @throws NullPointerException 	: if username is null
		 * @throws IllegalArgumentException : if username is empty
		 */
		public PlayerFriends(String username, long score) {
			if(username == null)
				throw new NullPointerException();
			if(username.isEmpty())
				throw new IllegalArgumentException();
			
			this.username = username;
			this.score = score;
		}
		
		// Get the username
		public String getUsr() {
			return username;
		}
		
		// Get the score of the user
		public long getScore() {
			return score;
		}
	}
	
	private Gson gson;
	
	public ToClientLst() {
		gson = new GsonBuilder().setPrettyPrinting().create();
	}
	
	/**
	 * Create a new player friends list without the passwords
	 * @param lst
	 * @return json object list
	 * @throws NullPointerException : if lst is null
	 */
	public String clientLst(LinkedList<Player> lst) {
		if(lst == null)
			throw new NullPointerException();
		
		LinkedList<PlayerFriends> retLst = new LinkedList<PlayerFriends>();
		for(Player p : lst)
			retLst.add(new PlayerFriends(p.getUsr(), p.getScore()));
		
		return gson.toJson(retLst);
	}
	
	/**
	 * Given a user list in json format returns the corresponding array list
	 * @param jsonLst
	 * @return list from json object
	 */
	public LinkedList<PlayerFriends> getClientLst(String jsonLst) {
		if(jsonLst == null)
			throw new NullPointerException();
		
		// Avoiding Type Erasure
		Type type = new TypeToken<LinkedList<PlayerFriends>>() {}.getType();
		@SuppressWarnings("unchecked") // Safe cast
		LinkedList<PlayerFriends> tmp = (LinkedList<PlayerFriends>)gson.fromJson(jsonLst, type);
		return tmp;
	}
}
