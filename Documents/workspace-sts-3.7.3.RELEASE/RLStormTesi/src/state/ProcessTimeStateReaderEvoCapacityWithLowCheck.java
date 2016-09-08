package state;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mainClasses.MainClass;
import singletons.SystemStatus;

public class ProcessTimeStateReaderEvoCapacityWithLowCheck implements StateReader {

	
	int upperBound;
	StateTranslator translator;
	int maxParallelism;
	int correctLoadThreshold;
	private static final Logger LOG = LoggerFactory.getLogger(ProcessTimeStateReaderEvoCapacityWithLowCheck.class);
	
	public ProcessTimeStateReaderEvoCapacityWithLowCheck(int upperBound, StateTranslator translator,int maxParallelism,double correctLoadThreshold) {
		super();
		this.upperBound 			= 	upperBound;
		this.translator 			= 	translator;
		this.maxParallelism			=	maxParallelism;
		this.correctLoadThreshold	=	(int) (correctLoadThreshold*10);
	}

	@Override
	public int getCurrentState() {
		// TODO Auto-generated method stub
		boolean underloaded	=	false;
		ArrayList<String> bolts	=	singletons.SystemStatus.bolts;
		Integer[] feat		=	new Integer[(2*(bolts.size()))+2];
		double latency	=	singletons.SystemStatus.processLatency;
		MainClass.LATENCY_VAL.set(latency);
		do{
			double orig	=	singletons.SystemStatus.completeUtilization;
			feat[1]		=	(int)orig;
			//LOG.debug("reading level "+feat[1]+" original ");
		}while(feat[1]<=-1.0);
		
		if(feat[1]>this.maxParallelism){
			feat[1]=this.maxParallelism;
		}
		MainClass.SYST_UTIL.set(feat[1]);
		//System.out.println("Livello utilizzazione elaborato "+feat[1]);
		for(int i=0;i<bolts.size();i++){
			feat[2+i]	=	singletons.SystemStatus.executors.get(bolts.get(i));
		}
		for(int i=0;i<bolts.size();i++){
			int opLevel	=	(int)(singletons.SystemStatus.operatorCapacity.get(bolts.get(i))*10);
			if(opLevel>10){
				opLevel	=	10;
			}
			feat[2+bolts.size()+i]	=	opLevel;
			if(singletons.SystemStatus.executors.get(bolts.get(i))>1){
				underloaded	=	this.isOperatorUnderloaded(bolts.get(i));
			}
		}
		if(latency>upperBound){
			feat[0]		=	2;
		}
		else if(SystemStatus.losingTuples==true){
			LOG.debug("losing tuples triggered");
			feat[0]		=	2;
		}
		else if(latency<=upperBound){
			if(underloaded==false){
				feat[0]		=	1;	
			}
			else{
				feat[0]		=	0;
			}
		}
		LOG.debug("State wrap up result state "+feat[0]+" underloaded "+underloaded+" latency "+latency+" upperbound "+upperBound);
		MainClass.STATE_READ.set(feat[0]);
		return translator.getIntForState(feat);
	}

	@Override
	public boolean isOperatorUnderloaded(String opName) {
		double opLevel		=	singletons.SystemStatus.operatorCapacity.get(opName);
		int    repLevel		=	singletons.SystemStatus.executors.get(opName);
		double opLowCheck	=	(double)1-((double)1/(repLevel));
		if(repLevel>1){
			if(opLevel<this.correctLoadThreshold){
				if(opLowCheck==0){
					LOG.debug("operator "+opName+" load level "+opLevel);
					return true;
				}
				if(opLevel<opLowCheck){
					LOG.debug("operator "+opName+" load level "+opLevel);
					return true;	
				}
			}
		}
		return false;
	}


}
