/**
 * 
 * @author Michael De Angelis
 * @matricola: 560049
 * @project Word Quizzle
 * @A.A 2019 - 2020 [UNIPI]
 *
 */

import java.rmi.Remote;
import java.rmi.RemoteException;

// Interface that will be implemented by the players and the server
public interface WqSignUp extends Remote {
	static final String SUCCESSFUL_SIGN_UP	= "REGISTRATION SUCCESSFUL";
	static final String EXISTING_USER		= "RETRY WITH ANOTHER USERNAME";
	
	/**
	 * Called by players who want to register; implemented by the WQ server
	 * @param usr
	 * @param psw
	 * @return String : represent a JSON response that will be received by the client.
	 * 					If one or more parameters are null or empty it returns a response 
	 * 					with code 400 and related error message;
	 * 					if the submitted username is already taken it returns a response 
	 * 					with code 449;
	 * 					if it successful it returns a response with code 200.
	 * @throws RemoteException
	 */
	public String signUp(String usr, String psw) throws RemoteException;
}
