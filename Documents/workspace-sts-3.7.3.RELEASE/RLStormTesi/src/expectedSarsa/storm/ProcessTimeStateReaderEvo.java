package expectedSarsa.storm;

import java.util.ArrayList;

import expectedSarsa.StateReader;
import mainClasses.MainClass;

public class ProcessTimeStateReaderEvo implements StateReader {
	int lowerBound;
	int upperBound;
	StateTranslator translator;
	int maxParallelism;
	
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
			feat[1]	=	(int)singletons.SystemStatus.completeUtilization;
			MainClass.SYST_UTIL.set(feat[1]);
		}while(feat[1]==-1.0);
		
		if(feat[1]>this.maxParallelism){
			feat[1]=this.maxParallelism;
		}
		System.out.println("TEST READER "+feat[1]);
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
