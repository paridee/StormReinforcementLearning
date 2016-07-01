package rl.executer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import expectedSarsa.IntervalManager;
import mainClasses.MainClass;
import rl.rewarder.DeltaRewarder;
import rl.rewarder.RewardCalculator;

public class BottleneckExecutor implements ActionExecutor {
	private static final Logger logger = LoggerFactory.getLogger(BottleneckExecutor.class);
	private int maxParallelism;
	private rl.rewarder.RewardCalculator rewarder;	
	private ArrayList<String> boltList;
	private int coresPerMachine;
	private IntervalManager intManager;
	
	
	public BottleneckExecutor(int maxParallelism, RewardCalculator rewarder, ArrayList<String> boltList,
			int coresPerMachine, IntervalManager intManager) {
		super();
		this.maxParallelism = maxParallelism;
		this.rewarder = rewarder;
		this.boltList = boltList;
		this.coresPerMachine = coresPerMachine;
		this.intManager = intManager;
	}

	//decrease least congested operator increase most congested operator
	@Override
	public double execute(int action) {
		if(action==0){
			String 	lessCongested	=	"";
			double  capacity		=	1;
			for(int i=0;i<boltList.size();i++){
				double bCapacity	=	singletons.SystemStatus.operatorCapacity.get(boltList.get(i));
				if(capacity>bCapacity){
					lessCongested	=	boltList.get(i);
					capacity		=	bCapacity;
				}
			}
			//TODO execute
			changeLevel(false,lessCongested);
		}
		else if(action==2){
			String 	mostCongested	=	"";
			double  capacity		=	0;
			for(int i=0;i<boltList.size();i++){
				double bCapacity	=	singletons.SystemStatus.operatorCapacity.get(boltList.get(i));
				if(capacity<bCapacity){
					mostCongested	=	boltList.get(i);
					capacity		=	bCapacity;
				}
			}
			changeLevel(true,mostCongested);
		}
		// TODO Auto-generated method stub
		try {
			Thread.sleep(this.intManager.getEvalInterval());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this.rewarder.giveReward();
	}

	private void changeLevel(boolean increase, String operatorName) {
		if(increase==true){
			int opLevel	=	singletons.SystemStatus.executors.get(operatorName);
			if(opLevel+1<this.maxParallelism){
				singletons.SystemStatus.executors.put(operatorName, opLevel+1);
			}
		}
		else{
			int opLevel	=	singletons.SystemStatus.executors.get(operatorName);
			if(opLevel-1>0){
				singletons.SystemStatus.executors.put(operatorName, opLevel-1);
			}
		}
		String	execFlags	=	"";
		int totalExecutors	=	0;
		for(int i=0;i<this.boltList.size();i++){
			MainClass.operatorsLevel[i].set(singletons.SystemStatus.executors.get(boltList.get(i)));
			totalExecutors	=	totalExecutors	+	singletons.SystemStatus.executors.get(boltList.get(i));
			execFlags		=	execFlags+" -e "+this.boltList.get(i)+"="+singletons.SystemStatus.executors.get(boltList.get(i));
		}
		if(totalExecutors%coresPerMachine==0){
			totalExecutors	=	totalExecutors/coresPerMachine;
		}
		else{
			totalExecutors	=	(totalExecutors/coresPerMachine)+1;
		}
		MainClass.PARALLELISM_VAL.set(totalExecutors);
		singletons.SystemStatus.workerNumber	=	totalExecutors;
		String command	=	singletons.Settings.stormPath+"storm rebalance "+singletons.Settings.topologyName+" -n "+totalExecutors+execFlags;
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

	private void populate(HashMap<Double, HashMap<String, Integer>> operatorCapacityAndLevel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double execute(int action, int state) {
		// TODO Auto-generated method stub
		return 0;
	}

}
