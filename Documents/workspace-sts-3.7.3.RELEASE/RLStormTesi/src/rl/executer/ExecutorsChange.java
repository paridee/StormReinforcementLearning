package rl.executer;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import mainClasses.MainClass;
import rl.policies.EpsilonGreedyChooser;
import singletons.Settings;

//TODO nota!!! diamo per scontato che tutti gli operatori abbiano lo stesso
//grado di parallelismo massimo


public class ExecutorsChange implements ActionExecutor {

	ArrayList<String>	boltsName;
	int[]	executorLevel;
	int 	increaseValue;
	int		maxExecutorNumber;
	public final static Logger logger	=	LogManager.getLogger(ExecutorsChange.class);
	String	topologyName;
	int 	coresPerMachine	=	8;
	
	public ExecutorsChange(ArrayList<String>boltsName,int increaseValue,int maxExecutorNumber,String topologyName){
		super();
		executorLevel			=	new int[boltsName.size()];
		this.boltsName			=	boltsName;
		this.increaseValue		=	increaseValue;
		this.maxExecutorNumber	=	maxExecutorNumber;
		this.topologyName		=	topologyName;
		for(int i=0;i<this.executorLevel.length;i++){
			executorLevel[i]	=	1;
		}
		applyLevel();
	}
	
	@Override
	public double execute(int action) {
		int boltN	=	action/2;
		int actionF	=	action-(2*boltN);
		if(boltN<boltsName.size()){
			if(actionF==0){
				if(executorLevel[boltN]>1){
					int tempValue	=	executorLevel[boltN]-increaseValue;
					if(tempValue<1){
						executorLevel[boltN]	=	1;
					}
					else{
						executorLevel[boltN]	=	tempValue;
					}
				}
				//logger.debug("action choosen: "+action+" decrease level for bolt "+boltN+" to "+bolts.get(boltN).level);
				applyLevel();
			}
			else if(actionF==1){
				if(executorLevel[boltN]<this.maxExecutorNumber){
					int tempValue	=	executorLevel[boltN]+increaseValue;
					if(tempValue>this.maxExecutorNumber){
						executorLevel[boltN]	=	this.maxExecutorNumber;
					}
					else{
						executorLevel[boltN]	=	tempValue;
					}
				}
				//logger.debug("action choosen: "+action+" increase level for bolt "+boltN+" to "+bolts.get(boltN).level);
				applyLevel();
			}
		}
		else{
			//logger.debug("action choosen: "+action+" leave unchanged");
		}
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void applyLevel() {
		String	execFlags	=	"";
		int totalExecutors	=	0;
		for(int i=0;i<executorLevel.length;i++){
			totalExecutors	=	totalExecutors	+	executorLevel[i];
			execFlags		=	execFlags+" -e "+this.boltsName.get(i)+"="+this.executorLevel[i];
		}
		if(totalExecutors%coresPerMachine==0){
			totalExecutors	=	totalExecutors/coresPerMachine;
		}
		else{
			totalExecutors	=	(totalExecutors/coresPerMachine)+1;
		}
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
}
