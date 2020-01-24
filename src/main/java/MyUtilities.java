/**
 * 
 * @author Michael De Angelis
 * @matricola: 560049
 * @project Word Quizzle
 * @A.A 2019 - 2020 [UNIPI]
 *
 */

public class MyUtilities {
	public static final int SIGN_UP_PORT 			= 8080;
	public static final int TCP_CONTROL_PORT 		= 9999;
	public static final int BUFFER_SIZE				= 8192;
	public static final int BYTE_ARRAY_SIZE			= 512;
	public static final int MATCH_DURATION			= 2;
	public static final int MATCH_DURATION_MILLIS	= 120000;
	public static final int TIMEOUT					= 60000;
	public static final int TIMEOUT_UDP				= 45000;
	
	public static final String PORT_EXCEPTION		= "A \"Well Known Port(s)\" has been used";
	public static final String WORD_QUIZZLE			= "Word Quizzle";
	public static final String ENCODING				= "UTF-8";
	public static final String SERVER_SIGN_UP_NAME 	= "WQ_SIGN_UP";
	public static final String JSON_GRAPH_PATH 		= "./json/graph.json";
	public static final String SPLIT      	  		= ",";
	public static final String SPACE 				= " ";
	public static final String DDOT					= " : ";
	
	public static final String LOGIN 				= "LOGIN";
	public static final String LOGOUT 				= "LOGOUT";
	public static final String ADD_FRIEND 			= "ADD_FRIEND";
	public static final String FRIEND_LIST  		= "FRIEND_LIST";
	public static final String MATCH 				= "MATCH";
	public static final String SHOW_SCORE			= "SHOW_SCORE";
	public static final String SHOW_RANKING 		= "SHOW_RANKING";
	
	public static final String SUCCESS_CODE		 	= "200";
	public static final String CLIENT_ERROR_CODE 	= "400";
	public static final String UNAUTHORIZED_CODE	= "403";
	public static final String RETRY_WITH_CODE	 	= "449";
	public static final String SERVER_ERROR_CODE	= "500";
	public static final String NOT_IMPLEMENTED_CODE = "501";
	public static final String TRANSLATE			= "TRANSLATE";
	public static final String TRANSLATION			= "TRANSLATION";
		
	public static final String UNKNOWN_RQST   		= "BAD REQUEST, UNKNOWN METHOD REQUESTED";
	public static final String NULL_ARGS  	  		= "BAD REQUEST, NULL ARG(S)";
	public static final String EMPTY_ARGS 	  		= "BAD REQUEST, EMPTY ARG(S)";
	public static final String MISSING_ARGS   		= "BAD REQUEST, MISSING ARG(S)";
	
	public static final String OFFLINE_USR			= "BAD REQUEST, LOGIN REQUIRED!";
	public static final String NOT_EXISTING_USR 	= "BAD REQUEST, NOT EXISTING USER";
	public static final String WRONG_PSW			= "BAD REQUEST, WRONG PASSWORD FOR USER";
	
	public static final String SIGN_UP_REQUEST 		= "Sign up request received - usr,psw : ";
	
	public static final String LOGIN_REQUEST		= "Login request received from ";
	public static final String LOGOUT_FIRST			= "ALREADY LOGGED, LOGOUT FIRST";
	public static final String ONLINE 		  		= " ONLINE";
	public static final String ALREADY_ONLINE   	= "USER ALREADY ONLINE";
	
	public static final String LOGOUT_REQUEST 		= "Logout request received from ";
	public static final String OFFLINE 				= " OFFLINE";
	public static final String ALREADY_OFFLINE		= "USER NOT ONLINE";
	
	public static final String ADD_FRIEND_REQUEST 	= "Add friend request received from ";
	public static final String ADDED				= " ADDED TO FRIEND LIST";
	public static final String CANNOT_ADD_YOURSELF	= "YOU CAN'T ADD YOURSELF";
	public static final String ALREADY_FRIEND		= "USER ALREADY A FRIEND";
	
	public static final String FRIEND_LIST_REQUEST	= "Friend list request received from ";

	public static final String MATCH_REQUEST		= "Match request received from ";
	public static final String CANNOT_MATCH_YSELF	= "YOU CANNOT CHALLENGE YOURSELF";
	public static final String NOT_FRIEND			= "THE USER SPECIFIED IS NOT YOUR FRIEND";
	public static final String NOT_ONLINE			= "THE USER SPECIFIED IS NOT ONLINE";
	public static final String MATCH_REQUEST_FROM	= "CHALLENGED BY ";
	public static final String MATCH_REQUEST_SEND	= "MATCH REQUEST SEND";
	public static final String MATCH_NO_RESPONSE 	= "NO ANSWER FROM THE CHALLENGED USER";
	public static final String SETUP_MATCH			= "CANNOT SET UP THE MATCH";
	public static final String IOE_UDP				= "ERROR(S) OCCURRED WHILE THE AGREEMENT OF THE MATCH";
	public static final String CHALLENGER_DISCONN	= "ERROR(S) OCCURRED WHILE SET UP THE AGREEMENT OF THE MATCH";
	public static final String MATCH_ACCEPTED		= "ACCEPTED";
	public static final String MATCH_REFUSED		= "REFUSED";
	public static final String TRANSLATE_SERVICE	= "TRANSLATE SERVICE UNREACHABLE";
	public static final String BEGIN_MATCH			= "BEGIN";
	public static final String OPP_ERR				= "CANNOT REACH THE OPPONENT";
	public static final String ERR_IO_SEL			= "ERROR(S) OCCURRED IN THE MAIN SERVER PROCESS";
	public static final String OPPO_LEFT			= "OPPO LEFT";
	public static final String ERR_SENDING_WORD		= "ERROR(S) OCCURRED WHILE SENDING A WORD FOR TRANSLATION";
	public static final String ERR_RECEIVING_WORD 	= "ERROR(S) OCCURRED WHILE RECEIVING A TRANSLATED WORD";
	public static final String WINNER				= "You win with a score of : ";
	public static final String LOSER				= "You lose with a score of : ";
	public static final String DRAW					= "You concluded with the same score of : ";
	
	public static final String SHOW_SCORE_REQUEST	= "Show score request received from ";
	public static final String USER_SCORE			= "user score : ";

	public static final String SHOW_RANKING_REQUEST = "Show ranking request received from ";
			
	public static final String CLIENT_UDP_LIS_ERR	= "It's look like an error occurred, the client must be restarted";
	public static final String ERR_ENTERING_PARS	= "Error entering parameters : ";
	
	public static final String USR_DIFFERENT_PSW	= "Passwords do not match";
	public static final String URS_TAKEN			= "The username is taken : ";
	public static final String SUCCESSFUL_REG		= "Registration was successful : ";
	public static final String CONNECTION_FAIL		= "Well, this is embarassing; It appears that the server is not active";
	public static final String CONNECTION_CLOSED	= "The connection has been closed";
	
	public static final String LOGIN_ERR_SEND_REQ	= "An error occurred in the login process, try again later";
	public static final String CLIENT_ALREADY_ON	= "The specified user is already online";
	public static final String CLIENT_ONLINE		= "Login successful : ";
	
	public static final String LOGOUT_ERR_SEND_REQ	= "An error occurred in the logout process, the client will be closed forcibly";
	public static final String CLIENT_ALREADY_OFF	= "The specified user is already offline";
	public static final String CLIENT_OFFLINE 		= "Logout successful : ";
	
	public static final String ADD_FRIEND_ERR_REQ	= "An error occurred adding a new friend, the client will be closed forcibly";
	
	public static final String FRIEND_LIST_ERR_REQ	= "An error occurred taking the friend list, the client will be closed forcibly";
	
	public static final String DO_YOU_ACCEPT		= "! Do you accept?";
	public static final String WHAT_DU_WANT_TO_DO	= "What do you want to do?";
	public static final String USER_SELECTED		= "User : ";
	public static final String MATCH_LIST			= "Match";
	public static final String SCORE_LIST			= "Score";
	public static final String CANCEL				= "Cancel";
	public static final String MATCH_ERR_SEND_REQ	= "An error occurred sending the match request, the client will be closed forcibly";
	public static final String WAITING_FOR_MATCH_RP = "Match request sent, waiting for response...";
	public static final String MATCHMAKING			= "Matchmaking";
	public static final String MATCH_RQST_TIMEOUT	= "The match request timed out";
	public static final String CLIENT_MATCH_REFUSED	= "Match refused!";
	public static final String CLIENT_MATCH_ACCPTD	= "Match accepted";
	public static final String TRANSLATE_FOLLOWING	= "Translate the following 10 words in maximum 2 minutes : \n";
	public static final String SOMETHING_WRONG_CL	= "An error occurred, the client must be closed";
	public static final String COMM_ERROR			= "A comunication error occurred";
	public static final String MATCH_ERR			= "An error occurred during the match, the client will be closed forcibly";
	public static final String BEGIN_NOT_RECEIVED	= "The match cannot start, you will return to the home";
	public static final String CHALLENGE_NUM		= "Challenge ";
	public static final String OF 					= "/10 : ";
	public static final String GET_TEXT_RES			= " -> ";
	
	public static final String SHOW_SCORE_ERR_REQ	= "An error occurred showing the user score, the client will be closed forcibly";
	
	public static final String RANKING_ERR_REQ		= "An error occurred showing the ranking, the client will be closed forcibly";
	
	private MyUtilities() {}
}
