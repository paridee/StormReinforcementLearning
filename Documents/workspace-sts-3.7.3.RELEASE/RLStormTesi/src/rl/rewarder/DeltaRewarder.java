package rl.rewarder;

public class DeltaRewarder implements RewardCalculator {
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
				return rewMax;
			}
		}
		double newDistance	=	latency-obj;
		if(newDistance<0){
			newDistance	=	-newDistance;
		}
		double delta		=	oldDistance-newDistance;
		delta			=	delta/1000;
		if(delta>0){
			delta	=	-delta;
		}
		//if more executor than past has to be a cost
		delta	=	delta-(this.costPerInstance*(this.oldExNumber-singletons.SystemStatus.getOperatorsLevel()));
		this.oldExNumber	=	singletons.SystemStatus.getOperatorsLevel();
		return delta;
	}
	
	
}
