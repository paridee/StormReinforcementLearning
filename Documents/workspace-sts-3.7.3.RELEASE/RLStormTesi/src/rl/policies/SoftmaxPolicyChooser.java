package rl.policies;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class SoftmaxPolicyChooser implements PolicyChooser {
	
	double e			=	2.71828;
	double temperature	=	1;
	public final static Logger logger	=	LogManager.getLogger(SoftmaxPolicyChooser.class);
	
	public SoftmaxPolicyChooser(double temp){
		super();
		this.temperature	=	temp;
	}
	
	@Override
	public double[] policyForState(int stateId, double[][] q) {
		return policyForState(stateId,q[stateId]);
	}

	@Override
	public int actionForState(int currentState, double[][] q) {
		return actionForState(currentState,q[currentState]);
	}

	@Override
	public double[] policyForState(int stateId, double[] qrow) {
		double den		=	0;
		double[] result	=	new double[qrow.length];
		for(int i=0;i<qrow.length;i++){
			den	=	den+(Math.pow(e, (qrow[i]/temperature)));
		}
		for(int i=0;i<qrow.length;i++){
			result[i]	=	Math.pow(e, (qrow[i]/temperature))/den;
			//logger.debug("p("+i+")="+result[i]);
		}
		return result;
	}

	@Override
	public int actionForState(int currentState, double[] qrow) {
		double[] policy				=	this.policyForState(currentState, qrow);
		double[] cumulative			=	new double[policy.length];
		cumulative[0]				=	policy[0];
		cumulative[policy.length-1]	=	1;
		for(int i=1;i<policy.length-1;i++){
			cumulative[i]	=	cumulative[i-1]+policy[i];
		}
		
		for(int i=0;i<policy.length;i++){
			logger.debug("X("+i+")="+cumulative[i]);
		}
		
		
		double rand	=	Math.random();
		logger.debug("Random value "+rand);
		for(int i=0;i<policy.length;i++){
			if(rand<=cumulative[i]){
				return i;
			}
		}
		return policy.length-1;
	}

}
