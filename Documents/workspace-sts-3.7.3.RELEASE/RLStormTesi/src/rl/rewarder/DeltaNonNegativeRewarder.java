package rl.rewarder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeltaNonNegativeRewarder implements RewardCalculator {


	private static final Logger logger = LoggerFactory.getLogger(DeltaNonNegativeRewarder.class);
	int distThreshold;
	int oldInstanceNumber;
	int upperBound;
	int lowerBound;
	double oldDistance;
	int obj;
	
	public DeltaNonNegativeRewarder(int distThreshold, int obj,int upperBound) {
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
		if(currentDist<0){
			currentDist	=	-currentDist;
		}
		if(currentDist<this.distThreshold){
			reward	=	reward+2;
		}
		logger.debug("distance delta "+(oldDistance-currentDist)+" positive means decreased");
		int machineDelta		=	this.oldInstanceNumber-singletons.SystemStatus.getOperatorsLevel();
		if(oldDistance-currentDist>this.distThreshold){
			reward	=	reward+1;
			logger.debug("Distance shortened, reward +1");
		}
		else if(oldDistance-currentDist>-this.distThreshold){	//se non mi sono allontanato oltre la soglia
			if(machineDelta>0){
				logger.debug("Instances number decreased, reward +0.5");
				reward	=	reward+0.5;
			}
		}
		this.oldDistance		=	currentDist;
		this.oldInstanceNumber	= 	singletons.SystemStatus.getOperatorsLevel();
		return reward;
	}
	
}
