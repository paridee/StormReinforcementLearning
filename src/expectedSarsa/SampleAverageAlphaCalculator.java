package expectedSarsa;

public class SampleAverageAlphaCalculator implements AlphaCalculator{

	int nStates	=	1;
	int counters[];
	
	public SampleAverageAlphaCalculator(int nStates){
		this.nStates	=	nStates;
		counters		=	new int[nStates];
		for(int i=0;i<nStates;i++){
			counters[i]	=	0;
		}
	}
	@Override
	public double getAlpha(int action) {
		counters[action]++;
		return (1/(1+counters[action]));
	}

}
