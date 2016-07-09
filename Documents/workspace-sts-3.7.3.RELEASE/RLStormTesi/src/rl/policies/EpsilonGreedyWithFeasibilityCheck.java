package rl.policies;

import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import rl.executer.ActionExecutor;

public class EpsilonGreedyWithFeasibilityCheck implements PolicyChooser {
	ActionExecutor exec;
	double epsilon;
	public final static Logger logger	=	LogManager.getLogger(EpsilonGreedyChooser.class);
		
	public EpsilonGreedyWithFeasibilityCheck(ActionExecutor exec, double epsilon) {
		super();
		this.exec = exec;
		this.epsilon = epsilon;
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
		int 	actions			=	qrow.length;
		double Q[]		=	new double[actions];
		double[] policy	=	new double[actions];
		int nAStates	=	0;
		for(int i=0;i<actions;i++){
			boolean feasible	=	this.exec.isFeasible(stateId,i);
			if(feasible==false){
				Q[i]	=	Double.NEGATIVE_INFINITY;
				nAStates++;
			}
			else{
				Q[i]	=	qrow[i];
			}
		}
		int 		bestaction	=	this.getBestAction(stateId, qrow);
		policy[bestaction]		=	1-epsilon;
		for(int i=0;i<qrow.length;i++){
			if(i!=bestaction){
				if(Q[i]>Double.NEGATIVE_INFINITY){
					policy[i]		=	epsilon/(qrow.length-1-nAStates);
				}
			}
		}
		return policy;
	}

	@Override
	public int actionForState(int currentState, double[] qrow) {
		double 	randomV			=	Math.random();
		double 	qActionChoosen	=	0;
		int 	actions			=	qrow.length;
		double Q[]	=	new double[actions];
		for(int i=0;i<actions;i++){
			boolean feasible	=	this.exec.isFeasible(currentState,i);
			if(feasible==false){
				Q[i]	=	Double.NEGATIVE_INFINITY;
			}
			else{
				Q[i]	=	qrow[i];
			}
		}
		//testing
		for(int i=0;i<actions;i++){
			System.out.println("Q["+currentState+"]["+i+"] "+Q[i]+"\t");
		}
		if(randomV>epsilon){
			//exploitation	
			
			//find best action
			int newAction	=	0;
			
			System.out.println("number of actions "+actions);
			
			double qAction	=	Q[0];
			for(int j=1;j<actions;j++){
				if(Q[j]>qAction){
					newAction	=	j;
					qAction		=	Q[j];
				}
			}
			int action			=	newAction;
			qActionChoosen	=	qAction;
			System.out.println("Exploiation:Current state: "+currentState+" action "+action);
			return action;
		}
		else{
			double tempQ	=	Double.NEGATIVE_INFINITY;
			int action		= 	-1;
			while(tempQ==Double.NEGATIVE_INFINITY){
				action			=	(int)((Math.random()*actions)%actions);
				tempQ	=	Q[action];
			}
			//testing
			System.out.println("Exploration:Current state: "+currentState+" action "+action);
			System.out.println("Q["+currentState+"]["+action+"] "+qActionChoosen+"\t");
			return action;
		}
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

}
