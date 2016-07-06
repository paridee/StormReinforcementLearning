package rl.executer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import expectedSarsa.IntervalManager;
import mainClasses.MainClass;
import rl.rewarder.DeltaRewarder;
import rl.rewarder.RewardCalculator;
import singletons.OperatorInformation;
import singletons.SystemStatus;

public class BottleneckExecutor implements ActionExecutor{
	RewardCalculator rewarder;
	private IntervalManager intManager;
	private int maxOpLevel;
	private static final Logger LOG = 	LoggerFactory.getLogger(BottleneckExecutor.class);
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
			Thread.sleep(2000);
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
			LOG.debug(e.getMessage());
		}
		return this.rewarder.giveReward();
	}

	@Override
	public double execute(int action, int state) {
		return execute(action);
	}
}
