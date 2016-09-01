package rl.policies;

/**
 * This interface contains the methods to be implemented in order to have a policy in a tabular system
 * @author Paride Casulli
 *
 */
public interface PolicyChooser {
	/**
	 * Obtain a policy for a state
	 * @param stateId state id
	 * @param q complete q matrix
	 * @return policy array
	 */
	double[]	policyForState(int stateId, double[][] q);
	/**
	 * Choose an action for a state
	 * @param currentState current state
	 * @param q q matrix
	 * @return action chosen
	 */
	int 		actionForState(int currentState, double[][] q);
	/**
	 * Obtain a policy for a state
	 * @param stateId state id
	 * @param qrow row of the Q matrix
	 * @return action chosen
	 */
	double[]	policyForState(int stateId, double[] qrow);
	/**
	 * Choose an action for a state
	 * @param currentState current state
	 * @param qrow row of Q matrix
	 * @return action chosen
	 */
	int 		actionForState(int currentState, double[] qrow);
}
