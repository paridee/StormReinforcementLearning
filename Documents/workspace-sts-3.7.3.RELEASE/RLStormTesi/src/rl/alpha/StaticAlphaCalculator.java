package rl.alpha;

public class StaticAlphaCalculator implements AlphaCalculator {
	double alpha;
	
	public StaticAlphaCalculator(double alpha){
		this.alpha	=	alpha;
	}
	
	@Override
	public double getAlpha(int action) {
		return alpha;
	}

}
