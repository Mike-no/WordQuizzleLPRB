/**
 * 
 * @author Michael De Angelis
 * @matricola: 560049
 * @project Word Quizzle
 * @A.A 2019 - 2020 [UNIPI]
 *
 */

// exception thrown when the username specified doesn't exists in the Player Graph
public class NotExistingUsrException extends Exception {
	private static final long serialVersionUID = -106392241596575540L;

	public NotExistingUsrException() {
		super();
	}
	
	public NotExistingUsrException(String s) {
		super(s);
	}
}
