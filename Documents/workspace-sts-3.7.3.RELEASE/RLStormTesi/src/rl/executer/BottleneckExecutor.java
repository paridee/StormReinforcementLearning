package rl.executer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import expectedSarsa.IntervalManager;
import mainClasses.MainClass;
import rl.rewarder.DeltaRewarder;
import rl.rewarder.RewardCalculator;
import singletons.OperatorInformation;
import singletons.SystemStatus;

public class BottleneckExecutor implements ActionExecutor{
	public final static Logger logger	=	LogManager.getLogger(BottleneckExecutor.class);
	RewardCalculator rewarder;
	private IntervalManager intManager;
	private int maxOpLevel;
	private int coresPerMachine;
	private String topologyName;
	public BottleneckExecutor(RewardCalculator rewarder,IntervalManager intManager,int maxOpLevel){
		super();
		this.rewarder	=	rewarder;
		this.intManager	=	intManager;
		this.maxOpLevel	=	maxOpLevel;
	}
	
	
	@Override
	public double execute(int action) {
		// TODO Auto-generated method stub
		if(action==0){
			ArrayList<OperatorInformation> operators	=	SystemStatus.getOperatorInformationList();
			for(int i=0;i<operators.size();i++){
				System.out.println(operators.get(i).operatorName+" level "+operators.get(i).level+" congestion "+operators.get(i).congestionLevel);
			}
			operators	=	SystemStatus.removeOperatorAtLevel(operators, 1);
			System.out.println("\n\nremoving operator level 1");
			for(int i=0;i<operators.size();i++){
				System.out.println(operators.get(i).operatorName+" level "+operators.get(i).level+" congestion "+operators.get(i).congestionLevel);
			}
			Collections.sort(operators, OperatorInformation.ascendent);
			System.out.println("\n\nordered list");
			for(int i=0;i<operators.size();i++){
				System.out.println(operators.get(i).operatorName+" level "+operators.get(i).level+" congestion "+operators.get(i).congestionLevel);
			}
			if(operators.size()>0){
				String opName	=	operators.get(0).operatorName;
				singletons.SystemStatus.executors.put(opName, singletons.SystemStatus.executors.get(opName)-1);
				System.out.println("applicando livello -1 "+singletons.SystemStatus.executors.get(opName)+" ad esecutore "+opName);
			}
		}
		else if(action==1){
			System.out.println("Leaving level unchanged... action 1");
		}
		else if(action==2){
			ArrayList<OperatorInformation> operators	=	SystemStatus.getOperatorInformationList();
			for(int i=0;i<operators.size();i++){
				System.out.println(operators.get(i).operatorName+" level "+operators.get(i).level+" congestion "+operators.get(i).congestionLevel);
			}
			operators	=	SystemStatus.removeOperatorAtLevel(operators, this.maxOpLevel);
			System.out.println("\n\nremoving operator level "+this.maxOpLevel);
			for(int i=0;i<operators.size();i++){
				System.out.println(operators.get(i).operatorName+" level "+operators.get(i).level+" congestion "+operators.get(i).congestionLevel);
			}
			Collections.sort(operators, OperatorInformation.discendent);
			System.out.println("\n\nordered list");
			for(int i=0;i<operators.size();i++){
				System.out.println(operators.get(i).operatorName+" level "+operators.get(i).level+" congestion "+operators.get(i).congestionLevel);
			}
			if(operators.size()>0){
				String opName	=	operators.get(0).operatorName;
				singletons.SystemStatus.executors.put(opName, singletons.SystemStatus.executors.get(opName)+1);
				System.out.println("applicando livello +1 "+singletons.SystemStatus.executors.get(opName)+" ad esecutore "+opName);
			}
		}
		
		//TODO apply method
		try {
			Thread.sleep(this.intManager.getEvalInterval());
			System.out.println("applicazione simulata");
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		try {
			Thread.sleep(1);//this.intManager.getEvalInterval());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.debug(e.getMessage());
		}
		return this.rewarder.giveReward();
	}

	@Override
	public double execute(int action, int state) {
		return execute(action);
	}
	
	void apply(){
		String	execFlags	=	"";
		int totalExecutors	=	0;
		Iterator<String> exIterator	= singletons.SystemStatus.executors.keySet().iterator();	
		while(exIterator.hasNext()){
			String 	opName	=	exIterator.next();
			int 	opLevel	=	singletons.SystemStatus.executors.get(opName);
			MainClass.levelMap.get(opName).set(opLevel);
			totalExecutors	=	totalExecutors	+ opLevel;
			execFlags		=	execFlags+" -e "+opName+"="+opLevel;
		}
		if(totalExecutors%coresPerMachine==0){
			totalExecutors	=	totalExecutors/coresPerMachine;
		}
		else{
			totalExecutors	=	(totalExecutors/coresPerMachine)+1;
		}
		MainClass.PARALLELISM_VAL.set(totalExecutors);
		singletons.SystemStatus.workerNumber	=	totalExecutors;
		String command	=	singletons.Settings.stormPath+"storm rebalance "+this.topologyName+" -n "+totalExecutors+execFlags;
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


	@Override
	public boolean isFeasible(int currentState, int i) {
		// TODO Auto-generated method stub
		return false;
	}
}
