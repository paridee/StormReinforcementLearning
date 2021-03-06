package rl.rewarder;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mainClasses.MainClass;
import state.StateReader;

public class DeltaNonNegativeRewarderRelativeStepsWithCapacity implements RewardCalculator {

	private static final Logger logger = LoggerFactory.getLogger(DeltaNonNegativeRewarderRelativeSteps.class);
	int distThreshold;
	int oldInstanceNumber;
	int upperBound;
	int lowerBound;
	double maxStep;
	double oldDistance;
	int obj;
	double loadCheck;
	boolean losing;
	double latencySensibility	=	0.3;
	StateReader stateReader;
	private double loadOKBonus;
	
	public DeltaNonNegativeRewarderRelativeStepsWithCapacity(int distThreshold, int obj,int upperBound,int maxStep,double loadCheck,double latencySensibility,StateReader reader,double loadOKBonus) {
		super();
		this.distThreshold 		= 	distThreshold;
		this.obj 				= 	obj;
		oldInstanceNumber		=	singletons.SystemStatus.getOperatorsLevel();
		this.oldDistance		=	singletons.SystemStatus.processLatency-obj;	
		this.upperBound			=	upperBound;
		lowerBound				=	obj-(upperBound-obj);
		this.maxStep			=	maxStep;
		this.loadCheck			=	loadCheck;
		this.losing				=	singletons.SystemStatus.losingTuples;
		this.latencySensibility	=	latencySensibility;
		this.stateReader		=	reader;
		this.loadOKBonus		=	loadOKBonus;
	}



	@Override
	public double giveReward() {
		double reward		=	0;
		boolean	losingNow	=	singletons.SystemStatus.losingTuples;
		if(losing==true){	//like tcp loss are sign of congestion
			if(losingNow==false){
				reward	=	reward+10;
			}
		}
		boolean beginToLose	=	false;
		if(losing==false){	//if i begin to lose pkts
			if(losingNow==true){
				beginToLose	=	true;
			}
		}
		losing	=	losingNow;
		double currentDist	=	singletons.SystemStatus.processLatency;//-obj;
		if(currentDist<0){
			currentDist	=	-currentDist;
		}
		if(currentDist<upperBound&&(beginToLose==false)){
			if(this.operatorLoadCheckOK()==true){
				reward	=	reward+loadOKBonus;
				logger.debug("All operators loaded and latency compliant, reward+30");
			}
		}
		double distDelta	=	oldDistance-currentDist;
		int machineDelta		=	this.oldInstanceNumber-singletons.SystemStatus.getOperatorsLevel();
		logger.debug("distance delta "+(distDelta)+"threshold "+this.distThreshold+" positive means decreased "+" machine delta "+machineDelta);
		if((distDelta>this.distThreshold)&&(beginToLose==false)){
			reward	=	reward+1;
			reward	=	reward + ((distDelta/this.distThreshold)*(this.latencySensibility));
			if(machineDelta<0){	//ho aumentato il numero di thread
				reward	=	reward+((machineDelta*(1/(maxStep*2))*reward));
			}
			logger.debug("Distance shortened, reward "+reward);
		}
		else if((oldDistance-currentDist>-this.distThreshold)&&(beginToLose==false)){	//se non mi sono allontanato oltre la soglia
			if(machineDelta>0){
				reward	=	reward+(machineDelta);
				logger.debug("Instances number decreased, reward "+reward);
			}
		}
		this.oldDistance		=	currentDist;
		this.oldInstanceNumber	= 	singletons.SystemStatus.getOperatorsLevel();
		
		//prometheus update
		int loss	=	0;
		if(losing==true){
			loss	=	1;
		}
		MainClass.TUPLE_LOSS.set(loss);
		//end prometheus update
		
		return reward;
	
	}
	
	
	boolean operatorLoadCheckOK(){	//if there are underloaded bolts return false
		ArrayList<String> boltsName	=	singletons.SystemStatus.bolts;
		for(int i=0;i<boltsName.size();i++){
			String boltName		=	boltsName.get(i);
			boolean underloaded	=	this.stateReader.isOperatorUnderloaded(boltName);
			if(underloaded==true){
				return false;
			}
		}
		return true;
	}
}
