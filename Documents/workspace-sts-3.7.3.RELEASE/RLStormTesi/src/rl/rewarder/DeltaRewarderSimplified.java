package rl.rewarder;

public class DeltaRewarderSimplified implements RewardCalculator {
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
		double currentDist	=	singletons.SystemStatus.processLatency-obj;
		if(oldDistance-currentDist>this.distThreshold){
			reward	=	reward+1;
		}
		else if(oldDistance-currentDist<-this.distThreshold){
			reward	=	reward-1;
		}
		this.oldDistance		=	currentDist;
		int machineDelta		=	this.oldInstanceNumber-singletons.SystemStatus.getOperatorsLevel();
		this.oldInstanceNumber	= 	singletons.SystemStatus.getOperatorsLevel();
		if(machineDelta>0){
			reward	=	reward+0.5;
		}
		else if(machineDelta<0){
			reward	=	reward-0.5;
		}
		double currentLatency	=	singletons.SystemStatus.processLatency;
		if((currentLatency<this.lowerBound)||(currentLatency>this.upperBound)){
			reward	=	reward - 0.5;
		}
		return reward;
	}

}
