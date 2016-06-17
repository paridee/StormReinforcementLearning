package expectedSarsa;

public interface PolicyChooser {
	double[]	policyForState(int stateId, double[][] q);
	int 		actionForState(int currentState, double[][] q);
}
