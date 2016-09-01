package rl.executer;

/**
 * This interface defines the methods to be implemented by action executors
 * @author Paride Casulli
 *
 */

public interface ActionExecutor {
	/**
	 * Applies an action on RL system
	 * @param action action to be executed
	 * @return action to be executed
	 */
	double execute(int action);

	/**
	 * Applies an action on RL system
	 * @param action action to be executed
	 * @param state environment state
	 * @return action to be executed
	 */
	double execute(int action, int state);

	/**
	 * Checks if an action is feasible in a given state
	 * @param currentState state of the system
	 * @param i action to be made
	 * @return feasibility
	 */
	boolean isFeasible(int currentState, int i);
	
	/**
	 * Preview of the system configuration if an action will be applied
	 * @param action action to be executed
	 * @param state state of the system
	 * @return feasible or not
	 */
	int[] newConfigurationPreview(int action,int state);
}
