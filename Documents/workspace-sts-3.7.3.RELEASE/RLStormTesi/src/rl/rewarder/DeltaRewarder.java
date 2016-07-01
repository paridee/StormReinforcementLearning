package rl.rewarder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeltaRewarder implements RewardCalculator {
	private static final Logger LOG = LoggerFactory.getLogger(DeltaRewarder.class);
	private double oldDistance;
	private int lowerBound;
	private int obj;
	private int upperBound;
	private int rewMax;
	private double costPerInstance;
	private int oldExNumber	=	1;

	public DeltaRewarder(int obj,int lowerBound, int upperBound, int rewMax,double costPerInstance) {
		super();
		this.lowerBound = lowerBound;
		this.obj		=	obj;
		this.upperBound = upperBound;
		this.rewMax = rewMax;
		this.oldDistance	=	obj;
		this.costPerInstance=	costPerInstance;
	}




	@Override
	public double giveReward() {
		double latency	=	singletons.SystemStatus.processLatency;
		if(latency<=upperBound){
			if(latency>=lowerBound){
				oldDistance	=	latency-obj;
				if(oldDistance<0){
					oldDistance	=	- oldDistance;
				}
				double reward		=	rewMax;
				reward				=	reward+(this.costPerInstance*(this.oldExNumber-singletons.SystemStatus.getOperatorsLevel()));
				this.oldExNumber	=	singletons.SystemStatus.getOperatorsLevel();
				return reward;
			}
		}
		double newDistance	=	latency-obj;
		if(newDistance<0){
			newDistance	=	-newDistance;
		}
		double delta		=	oldDistance-newDistance;
		delta				=	delta/1000;
		LOG.debug("delta distance "+(delta*1000)+" ms, instance delta penalty contribution "+(this.costPerInstance*(this.oldExNumber-singletons.SystemStatus.getOperatorsLevel()))+" delta contribution "+(delta/1000));
		//if more executor than past has to be a cost
		delta	=	delta+(this.costPerInstance*(this.oldExNumber-singletons.SystemStatus.getOperatorsLevel()));
		double notOptimalPenalty	=	(upperBound-obj)/1000;
		this.oldExNumber	=	singletons.SystemStatus.getOperatorsLevel();
		this.oldDistance	=	newDistance;
		return delta;
	}
	
	
}
