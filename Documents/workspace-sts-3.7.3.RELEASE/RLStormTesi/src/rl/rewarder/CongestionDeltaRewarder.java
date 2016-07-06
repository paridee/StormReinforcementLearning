package rl.rewarder;

import java.util.ArrayList;

import rl.executer.ActionExecutor;

public class CongestionDeltaRewarder implements RewardCalculator{
	private double oldDistance=Double.MAX_VALUE;
	ArrayList<String> bolts;
	double maxLatency;
	double obj;
	
	public CongestionDeltaRewarder(ArrayList<String> bolts,double maxLatency,double obj){
		super();
		this.bolts		=	bolts;
		this.maxLatency	=	maxLatency;
	}

	@Override
	public double giveReward() {
		double reward	=	0;
		double newDistance	=	singletons.SystemStatus.processLatency-obj;
		if(newDistance<0){
			newDistance	=	-newDistance;
		}
		for(int i=0;i<bolts.size();i++){
			double base =	-0.5;
			base = base+singletons.SystemStatus.operatorCapacity.get(bolts.get(i));
			reward	=	reward+base;
		}
		if(singletons.SystemStatus.processLatency>maxLatency){
			if(newDistance>oldDistance){
				return -0.5;
			}
			else{
				return +0.5;
			}
		}
		oldDistance	=	newDistance;
		return reward;
	}


}
