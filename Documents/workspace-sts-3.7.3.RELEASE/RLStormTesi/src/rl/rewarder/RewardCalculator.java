package rl.rewarder;
/**
 * This interface defines the methods for a rewarder
 * @author paride
 *
 */
public interface RewardCalculator {
	/**
	 * Give a reward
	 * @return reward
	 */
	double giveReward();
}
