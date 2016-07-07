package rl.rewarder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DeltaRewarderSimplified implements RewardCalculator {
	private static final Logger logger = LoggerFactory.getLogger(DeltaRewarderSimplified.class);
	int distThreshold;
	int oldInstanceNumber;
	int upperBound;
	int lowerBound;
	double oldDistance;
	int obj;
	
	public DeltaRewarderSimplified(int distThreshold, int obj,int upperBound) {
		super();
		this.distThreshold = distThreshold;
		this.obj = obj;
		oldInstanceNumber	=	singletons.SystemStatus.getOperatorsLevel();
		this.oldDistance	=	singletons.SystemStatus.processLatency-obj;	
		this.upperBound		=	upperBound;
		lowerBound			=	obj-(upperBound-obj);
	}



	@Override
	public double giveReward() {
		double reward		=	0;
		double currentLatency	=	singletons.SystemStatus.processLatency;
		double currentDist	=	singletons.SystemStatus.processLatency-obj;
		if(currentDist<0){
			currentDist	=	-currentDist;
		}
		if(currentDist<this.distThreshold){
			reward	=	reward+2;
		}
		logger.debug("distance delta "+(oldDistance-currentDist)+" positive means decreased");
		if(oldDistance-currentDist>this.distThreshold){
			
			reward	=	reward+1;
			logger.debug("Distance shortened, reward +1");
		}
		else if(oldDistance-currentDist<-this.distThreshold){
			reward	=	reward-1;
			logger.debug("Distance increased, reward -1");
		}
		this.oldDistance		=	currentDist;
		int machineDelta		=	this.oldInstanceNumber-singletons.SystemStatus.getOperatorsLevel();
		this.oldInstanceNumber	= 	singletons.SystemStatus.getOperatorsLevel();
		if(machineDelta>0){
			logger.debug("Instances number decreased, reward +0.5");
			reward	=	reward+0.5;
		}
		else if(machineDelta<0){
			logger.debug("Instances number increased, reward -0.5");
			reward	=	reward-0.5;
		}
		else if(currentLatency<this.lowerBound){	//test to force to do something in underload
			reward	=	reward-0.25;
			logger.debug("Underloaded and number of instance not decreased");
		}
		if(/*(currentLatency<this.lowerBound)||*/(currentLatency>this.upperBound)){
			logger.debug("Destination state overloaded, reward -0.5 (latency "+currentLatency+")");
			reward	=	reward - 0.5;
		}
		return reward;
	}

}
