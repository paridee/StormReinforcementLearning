package expectedSarsa.storm;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import expectedSarsa.StateReader;
import mainClasses.MainClass;
import monitors.NewStormMonitor;

public class ProcessTimeStateReaderEvo implements StateReader {
	int lowerBound;
	int upperBound;
	StateTranslator translator;
	int maxParallelism;
	private static final Logger LOG = LoggerFactory.getLogger(ProcessTimeStateReaderEvo.class);
	
	public ProcessTimeStateReaderEvo(int lowerBound, int upperBound, StateTranslator translator,int maxParallelism) {
		super();
		this.lowerBound 	= 	lowerBound;
		this.upperBound 	= 	upperBound;
		this.translator 	= 	translator;
		this.maxParallelism	=	maxParallelism;
	}

	@Override
	public int getCurrentState() {
		// TODO Auto-generated method stub
		ArrayList<String> bolts	=	singletons.SystemStatus.bolts;
		Integer[] feat		=	new Integer[(2*(bolts.size()))+2];
		double latency	=	singletons.SystemStatus.processLatency;
		MainClass.LATENCY_VAL.set(latency);
		if(latency<lowerBound){
			feat[0]		=	0;
		}
		else if(latency<upperBound){
			feat[0]		=	1;
		}
		else if(latency>upperBound){
			feat[0]		=	2;
		}
		do{
			double orig	=	singletons.SystemStatus.completeUtilization;
			feat[1]		=	(int)orig;
			LOG.debug("reading level "+feat[1]+" original ");
		}while(feat[1]==-1.0);
		
		if(feat[1]>this.maxParallelism){
			feat[1]=this.maxParallelism;
		}
		MainClass.SYST_UTIL.set(feat[1]);
		System.out.println("Livello utilizzazione elaborato "+feat[1]);
		for(int i=0;i<bolts.size();i++){
			feat[2+i]	=	singletons.SystemStatus.executors.get(bolts.get(i));
		}
		for(int i=0;i<bolts.size();i++){
			int opLevel	=	(int)(singletons.SystemStatus.operatorCapacity.get(bolts.get(i))*10);
			if(opLevel>10){
				opLevel	=	9;
			}
			feat[2+bolts.size()+i]	=	opLevel;
		}
		return translator.getIntForState(feat);
	}

}
