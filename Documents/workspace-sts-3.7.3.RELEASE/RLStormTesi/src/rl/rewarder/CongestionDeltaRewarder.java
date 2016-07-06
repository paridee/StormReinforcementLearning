package rl.rewarder;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import monitors.NewStormMonitor;
import rl.executer.ActionExecutor;

public class CongestionDeltaRewarder implements RewardCalculator{
	private static final Logger LOG = LoggerFactory.getLogger(CongestionDeltaRewarder.class);
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
			System.out.println("Congestion level for "+bolts.get(i)+" "+singletons.SystemStatus.operatorCapacity.get(bolts.get(i)));
			double base =	-0.5;
			base = base+singletons.SystemStatus.operatorCapacity.get(bolts.get(i));
			reward	=	reward+base;
		}
		if(singletons.SystemStatus.processLatency>maxLatency){
			System.out.println("distance "+newDistance+" delta "+(newDistance-oldDistance)+" negative is better");
			if(newDistance>oldDistance){
				return -0.5;
			}
			else{
				return +0.5;
			}
		}
		oldDistance	=	newDistance;
		System.out.println("Reward "+reward);
		return reward;
	}


}
