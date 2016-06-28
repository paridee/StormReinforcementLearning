package rl.rewarder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Jama.Matrix;

public class ParabolicComplexResponseTimeRewarder implements RewardCalculator {
	/**
	 * This class gives a "parabolic" reward set by:
	 * -focus
	 * -left (X axis) intercept
	 * -right (X axis) intercept
	 * -maximum reward
	 * takes in account also: machine number switch and number of instances
	 */
	
	double a				=	0;
	double b				=	0;
	double c				=	0;
	double maxReward		=	100;
	double focus;
	double xLIntercept		=	0;	//intercetta asse X sx
	double xRIntercept		=	0;	//intercetta alle X dx
	int nInstances			=	0;
	int prevInstanceNumber	=	0;
	
	private static final Logger LOG = LoggerFactory.getLogger(ParabolicComplexResponseTimeRewarder.class);
	
	
	
	public ParabolicComplexResponseTimeRewarder(double targetProcessTime, double maxReward,
		double xRIntercept,int nInstances) { //XL intercept is fixed on order to have maxv there!!!
		super();
		this.focus = targetProcessTime;
		this.maxReward = maxReward;
		this.xLIntercept 	= 	targetProcessTime-(xRIntercept-targetProcessTime);
		this.xRIntercept 	= 	xRIntercept;
		this.nInstances		=	nInstances;
	}

	@Override
	public double giveReward() {
		if(a==0){
			if(a==b){
				if(a==c){
					LOG.info("Parameters not initialized, calculation");
					this.calculateParameters();
				}
			}
		}
		double lat			=	singletons.SystemStatus.processLatency;
		double latSquared	=	lat*lat;
		double reward		=	(a*latSquared)+(b*lat)+c;
		reward				=	reward	-	(maxReward/(nInstances*2))*singletons.SystemStatus.workerNumber;
		/*if(singletons.SystemStatus.workerNumber!=this.prevInstanceNumber){
			LOG.info("switch number detected");
			reward			=	reward-(maxReward/4);
			this.prevInstanceNumber	=	singletons.SystemStatus.workerNumber;
		}*/
		LOG.info("reward returned "+reward);
		return reward;
	}

	/**
	 * This methods calculates parabolic cohefficients
	 */
	public void calculateParameters(){
		double xLSquared	=	Math.pow(xLIntercept,2);
		double xRSquared	=	Math.pow(xRIntercept,2);
		double focusSquared	=	Math.pow(focus,2);
		//Ax=B
	    double[][] vals 	= 	{{xLSquared,xLIntercept,1.},{focusSquared,focus,1.},{xRSquared,xRIntercept,1.}};
	    Matrix 		A 		= 	new Matrix(vals);
	    double[][] valsB	=	{{0.},{maxReward},{0.}};	
	    Matrix 		b 		= 	new Matrix(valsB);
	    Matrix 		x 		= 	A.solve(b);
	    System.out.println(x.getRowDimension()+"X"+x.getColumnDimension());
	    for(int i=0;i<x.getRowDimension();i++){
	    	for(int j=0;j<x.getColumnDimension();j++){
	    		LOG.info(x.get(i, j)+"");
	    	  }
	    }
	    this.a	=	x.get(0, 0);
	    this.b	=	x.get(1, 0);
	    this.c	=	x.get(2, 0);
	}

}
