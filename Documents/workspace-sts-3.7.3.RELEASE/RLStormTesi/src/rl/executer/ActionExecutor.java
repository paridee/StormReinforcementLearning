package rl.executer;

public interface ActionExecutor {
	/**
	 * Applies an action on RL system
	 * @param action action to be executed
	 * @return reward from the system
	 */
	double execute(int action);
}
