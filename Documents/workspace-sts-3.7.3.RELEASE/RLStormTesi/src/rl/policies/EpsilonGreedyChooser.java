package rl.policies;

import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class EpsilonGreedyChooser implements PolicyChooser {
	double	epsilon	=	0.2;
	public final static Logger logger	=	LogManager.getLogger(EpsilonGreedyChooser.class);
	
	public EpsilonGreedyChooser(double eps){
		epsilon	=	eps;
	}
	
	@Override
	public double[] policyForState(int stateId, double[][] q) {
		return policyForState(stateId,q[stateId]);
	}

	@Override
	public int actionForState(int currentState, double[][] q) {
		return actionForState(currentState,q[currentState]);
	}
	
	private int getBestAction(int currentState, double[] q){
		int 	tempaction	=	0;
		double 	tempvalue	=	q[0];
		for(int i=1;i<q.length;i++){
			if(q[i]>tempvalue){
				tempaction	=	i;
				tempvalue	=	q[i];
			}
		}
		ArrayList<Integer> valueActions	=	new ArrayList<Integer>();
		for(int i=0;i<q.length;i++){
			if(q[i]==tempvalue){
				valueActions.add(i);
			}
		}
		if(valueActions.size()>1){
			logger.debug("not a single action with value "+tempvalue);
			int index	=	((int)(Math.random()*valueActions.size()+10000))%valueActions.size();
			return valueActions.get(index);
		}
		return tempaction;
	}

	@Override
	public double[] policyForState(int stateId, double[] qrow) {
		double[] 	policy		=	new double[qrow.length];
		int 		bestaction	=	this.getBestAction(stateId, qrow);
		policy[bestaction]		=	1-epsilon;
		for(int i=0;i<qrow.length;i++){
			if(i!=bestaction){
				policy[i]		=	epsilon/(qrow.length-1);
			}
		}
		return policy;
	}

	@Override
	public int actionForState(int currentState, double[] qrow) {
		int bestAction	=	this.getBestAction(currentState, qrow);
		double rand		=	Math.random();		//TODO: change uniform random choosing in exploration
		if(rand<epsilon){
			logger.debug("Exploration phase "+rand);
			int tempAction	=	bestAction;
			while(tempAction==bestAction){
				tempAction	=	(int) (Math.random()*qrow.length);
			}
			return tempAction;
		}
		else{
			logger.debug("Exploitation phase "+rand);
			return bestAction;
		}
	}

}
