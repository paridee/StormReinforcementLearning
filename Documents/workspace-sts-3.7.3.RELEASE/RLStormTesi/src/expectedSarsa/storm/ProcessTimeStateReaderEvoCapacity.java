package expectedSarsa.storm;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import expectedSarsa.StateReader;
import mainClasses.MainClass;
import singletons.SystemStatus;

public class ProcessTimeStateReaderEvoCapacity implements StateReader {

	
	int upperBound;
	StateTranslator translator;
	int maxParallelism;
	int correctLoadThreshold;
	private static final Logger LOG = LoggerFactory.getLogger(ProcessTimeStateReaderEvoCapacity.class);
	
	public ProcessTimeStateReaderEvoCapacity(int upperBound, StateTranslator translator,int maxParallelism,double correctLoadThreshold) {
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
				if(opLevel<this.correctLoadThreshold){
					underloaded	=	true;
				}
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
		return translator.getIntForState(feat);
	}


}
