package rl.executer;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import expectedSarsa.IntervalManager;
import mainClasses.MainClass;
import rl.rewarder.RewardCalculator;

public class ExecutorsChangeMultipleSteps implements ActionExecutor {

	ArrayList<String>	boltsName;
	int[]	executorLevel;
	int[]	steps;
	int		maxExecutorNumber;
	public final static Logger logger	=	LogManager.getLogger(ExecutorsChangeMultipleSteps.class);
	String	topologyName;
	int 	coresPerMachine	=	8;
	private IntervalManager intManager;
	RewardCalculator rewCalculator;

	public ExecutorsChangeMultipleSteps(ArrayList<String>boltsName,int 	coresPerMachine,int[] steps,int maxExecutorNumber,String topologyName, IntervalManager intManager,RewardCalculator rewCalculator){
		super();
		executorLevel			=	new int[boltsName.size()];
		this.boltsName			=	boltsName;
		this.steps				=	steps;
		this.maxExecutorNumber	=	maxExecutorNumber;
		this.coresPerMachine	=	coresPerMachine;
		this.topologyName		=	topologyName;
		this.intManager			=	intManager;
		this.rewCalculator		=	rewCalculator;
		for(int i=0;i<this.boltsName.size();i++){
			if(singletons.SystemStatus.executors.containsKey(boltsName.get(i))){
				executorLevel[i]	=	singletons.SystemStatus.executors.get(boltsName.get(i));
			}
			else{
				executorLevel[i]	=	1;
			}
		}
	}
	
	@Override
	public double execute(int action) {
		int boltN	=	action/6;
		int actionF	=	action-(6*boltN);
		if(boltN<boltsName.size()){
			if(actionF<3){
				if(executorLevel[boltN]>1){
					int tempValue	=	executorLevel[boltN]-steps[actionF];
					if(tempValue<1){
						executorLevel[boltN]	=	1;
					}
					else{
						executorLevel[boltN]	=	tempValue;
					}
				}
				logger.debug("action choosen: "+action+" decrease level for bolt "+boltN+" to "+executorLevel[boltN]);
				applyLevel();
			}
			else if(actionF<6){
				if(executorLevel[boltN]<this.maxExecutorNumber){
					int tempValue	=	executorLevel[boltN]+steps[actionF-3];
					if(tempValue>this.maxExecutorNumber){
						executorLevel[boltN]	=	this.maxExecutorNumber;
					}
					else{
						executorLevel[boltN]	=	tempValue;
					}
				}
				logger.debug("action choosen: "+action+" increase level for bolt "+boltN+" to "+executorLevel[boltN]);
				applyLevel();
			}
		}
		else{
			logger.debug("action choosen: "+action+" leave unchanged");
			applyLevel();//REBALANCE SVUOTA CODE!!! MESSO PER TEST!!! (DEVO POTER CONFRONTARE)
		}
		// TODO Auto-generated method stub
		try {
			Thread.sleep(this.intManager.getEvalInterval());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.debug(e.getMessage());
		}
		return this.rewCalculator.giveReward();
	}

	@Override
	public double execute(int action, int state) {
		logger.debug("Executor doesn't need state knowledge");
		return execute(action);
	}
	
	public int getExecutorsNumber(){
		int temp	=	0;
		for(int i=0;i<this.executorLevel.length;i++){
			temp	=	temp+this.executorLevel[i];
		}
		return temp;
	}

	@Override
	public boolean isFeasible(int currentState, int action) {
		int boltN		=	action/6;
		int actionF		=	action%6;
		if(boltN>=boltsName.size()){
			return true;
		}
		String boltName	=	this.boltsName.get(boltN);
		if(boltN<=this.boltsName.size()){
			if((actionF==0)||(actionF==1)||(actionF==2)){
				if(singletons.SystemStatus.executors.get(boltName)<=1){
					return false;
				}
			}
			else if((actionF>2)&&(actionF<6)){
				if(singletons.SystemStatus.executors.get(boltName)>=this.maxExecutorNumber){
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public int[] newConfigurationPreview(int action, int state) {
		int boltN	=	action/6;
		int actionF	=	action-(6*boltN);
		int[] preview	=	new int[executorLevel.length];
		if(boltN<boltsName.size()){
			if(actionF<3){
				if(executorLevel[boltN]>1){
					int tempValue	=	executorLevel[boltN]-steps[actionF];
					if(tempValue<1){
						preview[boltN]	=	1;
					}
					else{
						preview[boltN]	=	tempValue;
					}
				}
				//logger.debug("action choosen: "+action+" decrease level for bolt "+boltN+" to "+bolts.get(boltN).level);
			}
			else if(actionF<6){
				if(executorLevel[boltN]<this.maxExecutorNumber){
					int tempValue	=	executorLevel[boltN]+steps[actionF-3];
					if(tempValue>this.maxExecutorNumber){
						preview[boltN]	=	this.maxExecutorNumber;
					}
					else{
						preview[boltN]	=	tempValue;
					}
				}
	
			}
			for(int i=0;i<executorLevel.length;i++){
				if(i!=boltN){
					preview[i]	=	executorLevel[i];
				}
			}
		}
		else{
			for(int i=0;i<executorLevel.length;i++){
				preview[i]	=	executorLevel[i];
			}
		}
		return preview;
	}

	public void applyLevel() {
		String	execFlags	=	"";
		int totalExecutors	=	0;
		for(int i=0;i<executorLevel.length;i++){
			MainClass.operatorsLevel[i].set(executorLevel[i]);
			totalExecutors	=	totalExecutors	+	executorLevel[i];
			execFlags		=	execFlags+" -e "+this.boltsName.get(i)+"="+this.executorLevel[i];
			singletons.SystemStatus.setExecutorLevel(this.boltsName.get(i), this.executorLevel[i]);
			//logger.debug("Executor level increase "+executorLevel[i]+" total "+totalExecutors);
		}
		if(totalExecutors%coresPerMachine==0){
			totalExecutors	=	totalExecutors/coresPerMachine;
		}
		else{
			totalExecutors	=	(totalExecutors/coresPerMachine)+1;
		}
		singletons.SystemStatus.rebalanceTime	=	System.currentTimeMillis();
		MainClass.PARALLELISM_VAL.set(totalExecutors);
		singletons.SystemStatus.workerNumber	=	totalExecutors;
		String command	=	singletons.Settings.stormPath+"storm rebalance "+this.topologyName+" -w 0 -n "+totalExecutors+execFlags;
		logger.debug("sending command "+command);
		Runtime rt 		= 	Runtime.getRuntime();
		try {
			Process pr 		= 	rt.exec(command);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.debug(e.getMessage());
		}
	}

}
