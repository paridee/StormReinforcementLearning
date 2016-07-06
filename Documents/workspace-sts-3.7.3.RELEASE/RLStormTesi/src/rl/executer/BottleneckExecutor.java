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
	public BottleneckExecutor(RewardCalculator rewarder){
		super();
		this.rewarder	=	rewarder;
	}
	@Override
	public double execute(int action) {
		// TODO Auto-generated method stub
		if(action==0){
			ArrayList<OperatorInformation> operators	=	SystemStatus.getOperatorInformationList();
			operators	=	SystemStatus.removeOperatorAtLevel(operators, 1);
			Collections.sort(operators, OperatorInformation.ascendent);
			if(operators.size()>0){
				String opName	=	operators.get(0).operatorName;
				singletons.SystemStatus.executors.put(opName, singletons.SystemStatus.executors.get(opName)-1);
			}
		}
		else if(action==1){
			
		}
		else if(action==2){
			ArrayList<OperatorInformation> operators	=	SystemStatus.getOperatorInformationList();
			operators	=	SystemStatus.removeOperatorAtLevel(operators, this.maxOpLevel);
			Collections.sort(operators, OperatorInformation.discendent);
			if(operators.size()>0){
				String opName	=	operators.get(0).operatorName;
				singletons.SystemStatus.executors.put(opName, singletons.SystemStatus.executors.get(opName)+1);
			}
		}
		
		//TODO apply method
		
		try {
			Thread.sleep(this.intManager.getEvalInterval());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOG.debug(e.getMessage());
		}
		return this.rewarder.giveReward();
	}

	@Override
	public double execute(int action, int state) {
		// TODO Auto-generated method stub
		return 0;
	}
}
