package rl.rewarder;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeltaNonNegativeRewarderRelativeStepsWithCapacity implements RewardCalculator {

	private static final Logger logger = LoggerFactory.getLogger(DeltaNonNegativeRewarderRelativeSteps.class);
	int distThreshold;
	int oldInstanceNumber;
	int upperBound;
	int lowerBound;
	double maxStep;
	double oldDistance;
	int obj;
	
	public DeltaNonNegativeRewarderRelativeStepsWithCapacity(int distThreshold, int obj,int upperBound,int maxStep) {
		super();
		this.distThreshold = distThreshold;
		this.obj = obj;
		oldInstanceNumber	=	singletons.SystemStatus.getOperatorsLevel();
		this.oldDistance	=	singletons.SystemStatus.processLatency-obj;	
		this.upperBound		=	upperBound;
		lowerBound			=	obj-(upperBound-obj);
		this.maxStep		=	maxStep;
	}



	@Override
	public double giveReward() {
		double reward		=	0;
		double currentDist	=	singletons.SystemStatus.processLatency-obj;
		if(currentDist<0){
			currentDist	=	-currentDist;
		}
		if(currentDist<upperBound){
			if(this.operatorLoadCheckOK()==true){
				reward	=	reward+30;
				logger.debug("All operators loaded and latency compliant, reward+30");
			}
		}
		double distDelta	=	oldDistance-currentDist;
		int machineDelta		=	this.oldInstanceNumber-singletons.SystemStatus.getOperatorsLevel();
		logger.debug("distance delta "+(distDelta)+"threshold "+this.distThreshold+" positive means decreased "+" machine delta "+machineDelta);
		if(distDelta>this.distThreshold){
			reward	=	reward+1;
			reward	=	reward + (distDelta/1000);
			if(machineDelta<0){	//ho aumentato il numero di thread
				reward	=	reward+((machineDelta*(1/(maxStep)*2)*reward));
			}
			logger.debug("Distance shortened, reward "+reward);
		}
		else if(oldDistance-currentDist>-this.distThreshold){	//se non mi sono allontanato oltre la soglia
			if(machineDelta>0){
				reward	=	reward+(machineDelta);
				logger.debug("Instances number decreased, reward "+reward);
			}
		}
		this.oldDistance		=	currentDist;
		this.oldInstanceNumber	= 	singletons.SystemStatus.getOperatorsLevel();
		return reward;
	}
	
	boolean operatorLoadCheckOK(){	//if there are underloaded bolts return false
		ArrayList<String> boltsName	=	singletons.SystemStatus.bolts;
		for(int i=0;i<boltsName.size();i++){
			String boltName	=	boltsName.get(i);
			int repLevel	=	singletons.SystemStatus.executors.get(boltName);
			if(repLevel>1){
				double capacity	=	singletons.SystemStatus.operatorCapacity.get(boltName);
				if(capacity<0.7){
					return false;
				}
			}
		}
		return true;
	}
}
