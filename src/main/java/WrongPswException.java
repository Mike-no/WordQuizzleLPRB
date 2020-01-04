/**
 * 
 * @author Michael De Angelis
 * @matricola: 560049
 * @project Word Quizzle
 * @A.A 2019 - 2020 [UNIPI]
 *
 */

// exception thrown when the user specifies an incorrect password for a given username
public class WrongPswException extends Exception {
	private static final long serialVersionUID = 4651170607611658024L;

	public WrongPswException() {
		super();
	}
	
	public WrongPswException(String s) {
		super(s);
	}
}
