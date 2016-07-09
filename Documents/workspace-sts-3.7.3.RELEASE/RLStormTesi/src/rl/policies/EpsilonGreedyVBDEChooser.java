package rl.policies;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

//TODO not working... check

public class EpsilonGreedyVBDEChooser implements PolicyChooser{
	private double[] 	epsilont	=	null;
	private double		temperature;
	private double[][]	prevq		=	null;	//previous q matrix
	private int			preva		=	-1;		//previous action
	private int			prevs		=	-1;		//previous state
	private double[][]	q;
	private double 		delta		=	-1;
	double e			=	2.71828;
	public final static Logger logger	=	LogManager.getLogger(EpsilonGreedyVBDEChooser.class);
	
	public EpsilonGreedyVBDEChooser(double temperature){	//in the article delta is set to 1/#actions page 4 http://www.tokic.com/www/tokicm/publikationen/papers/AdaptiveEpsilonGreedyExploration.pdf
		super();
		this.temperature	=	temperature;
	}
	
	public EpsilonGreedyVBDEChooser(double temperature,double delta){
		super();
		this.temperature	=	temperature;
		this.delta			=	delta;
	}
	
	double function(int s,int a,double temperature){
		double temp	=	q[s][a]-prevq[s][a];
		if(temp>0){
			temp	=	-temp;
		}
		temp	=	temp/this.temperature;
		double num	=	1-(Math.pow(e, temp));
		double den	=	1+(Math.pow(e, temp));
		
		//test
		logger.debug("Function value state "+s+" action "+a+" temperature "+temperature+" num "+num+" den "+den+" exp value "+temp);
		//endtest
		
		return (num/den);
	}
	
	private int getBestAction(int currentState, double[][] q){
		int 	tempaction	=	0;
		double 	tempvalue	=	q[currentState][0];
		for(int i=1;i<q[currentState].length;i++){
			if(q[currentState][i]>tempvalue){
				tempaction	=	i;
				tempvalue	=	q[currentState][i];
			}
		}
		return tempaction;
	}
	
	@Override
	public double[] policyForState(int stateId, double[][] q) {
		double[] 	policy		=	new double[q[stateId].length];
		int 		bestaction	=	this.getBestAction(stateId, q);
		policy[bestaction]		=	1-epsilont[stateId];
		for(int i=0;i<q[stateId].length;i++){
			if(i!=bestaction){
				policy[i]		=	epsilont[stateId]/(q[stateId].length-1);
			}
		}
		return policy;
	}

	@Override
	public int actionForState(int currentState, double[][] q) {
		//initialization block
		if(this.prevq==null){
			prevq		=	new double[q.length][q[0].length];
			epsilont	=	new double[q.length];
			for(int i=0;i<q.length;i++){
				this.epsilont[i]	=	0.1;
			}
		}
		if(this.delta==-1){
			delta	=	(double)1/(q[0].length);	//default value: 1/#actions
		}
		//end of initialization block
		
		this.q						=	q;
		double[] policy				=	this.policyForState(currentState, q);
		double[] cumulative			=	new double[policy.length];
		cumulative[0]				=	policy[0];
		cumulative[policy.length-1]	=	1;
		for(int i=1;i<policy.length-1;i++){
			cumulative[i]	=	cumulative[i-1]+policy[i];
		}
		
		logger.debug("Cumulative dist");
		for(int i=0;i<policy.length;i++){
			logger.debug("X["+i+"]="+cumulative[i]);
		}
		
		double 	rand		=	Math.random();
		int		action		=	-1;
		logger.debug("Random value "+rand);
		for(int i=0;i<policy.length;i++){
			if(rand<=cumulative[i]){
				action	=	i;
				logger.debug("action "+i+" passed the test");
				break;
			}
		}
		if(action==-1){
			action	=	policy.length-1;
		}
		this.preva	=	action;
		this.prevs	=	currentState;
		this.updateEpsilons();
		return action;
	}

	private void updateEpsilons() {
		logger.debug("Updating epsilons ");
		logger.debug("Updating state "+this.prevs+" action "+this.preva+" from "+this.epsilont[this.prevs]);
		if(this.preva>-1){
			this.epsilont[this.prevs]	=	(delta*this.function(this.prevs, this.preva, this.temperature))+((1-this.delta)*epsilont[this.prevs]);
		}
		logger.debug("to "+this.epsilont[this.prevs]);
		logger.debug("Epsilon matrix ");
		for(int i=0;i<epsilont.length;i++){
			logger.debug(epsilont[i]);
		}
	}

	@Override
	public double[] policyForState(int stateId, double[] qrow) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int actionForState(int currentState, double[] qrow) {
		// TODO Auto-generated method stub
		return 0;
	}

}
