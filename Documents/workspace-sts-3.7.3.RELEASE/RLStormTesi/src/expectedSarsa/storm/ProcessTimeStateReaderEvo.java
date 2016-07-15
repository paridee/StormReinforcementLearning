package expectedSarsa.storm;

import java.util.ArrayList;

import expectedSarsa.StateReader;

public class ProcessTimeStateReaderEvo implements StateReader {
	int lowerBound;
	int upperBound;
	StateTranslator translator;
	
	public ProcessTimeStateReaderEvo(int lowerBound, int upperBound, StateTranslator translator) {
		super();
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.translator = translator;
	}

	@Override
	public int getCurrentState() {
		// TODO Auto-generated method stub
		ArrayList<String> bolts	=	singletons.SystemStatus.bolts;
		Integer[] feat		=	new Integer[(2*(bolts.size()))+1];
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
		
		for(int i=0;i<bolts.size();i++){
			feat[1+i]	=	singletons.SystemStatus.executors.get(bolts.get(i));
		}
		for(int i=0;i<bolts.size();i++){
			int opLevel	=	(int)(singletons.SystemStatus.operatorCapacity.get(bolts.get(i))*10);
			if(opLevel>10){
				opLevel	=	9;
			}
			feat[1+bolts.size()+i]	=	opLevel;
		}
		return translator.getIntForState(feat);
	}

}
