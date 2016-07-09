package rl.policies;

public interface PolicyChooser {
	double[]	policyForState(int stateId, double[][] q);
	int 		actionForState(int currentState, double[][] q);
	double[]	policyForState(int stateId, double[] qrow);
	int 		actionForState(int currentState, double[] qrow);
}
