/**
 * 
 * @author Michael De Angelis
 * @matricola: 560049
 * @project Word Quizzle
 * @A.A 2019 - 2020 [UNIPI]
 *
 */

/**
 *  Represent the states in which a player can be found.
 *  Because enums are automatically Serializable, there is no need 
 *  to explicitly add the "implements Serializable" clause following 
 *  the enum declaration.
 */
public enum Status {
	online,
	offline;
}
