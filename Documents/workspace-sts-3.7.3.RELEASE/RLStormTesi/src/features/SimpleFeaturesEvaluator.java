package features;

import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jetty.util.log.Log;

import linearGradientSarsa.LinearGradientDescendSarsaLambda;

public class SimpleFeaturesEvaluator implements FeaturesEvaluator {
	ArrayList<String> opName;
	int states;
	int featuresPerState;
	public final static Logger logger	=	LogManager.getLogger(SimpleFeaturesEvaluator.class);
	public SimpleFeaturesEvaluator(ArrayList<String> opName,int states,int featuresPerState) {
		super();
		this.opName				=	opName;
		this.states				=	states;
		this.featuresPerState	=	featuresPerState;
	}


	@Override
	public int[] getFeatures(int state, int action) throws Exception {
		// TODO Auto-generated method stub
		int[] features	=	new int[featuresPerState*states];
		//System.out.print("features number "+features.length);
		int operator	=	action/2;
		int actionV		=	action%2;
		if(operator==opName.size()&&actionV==0){
			features[(state*this.featuresPerState)+5]	=	1;
			//System.out.print(" Action do nothing\n");
		}
		else if(operator>opName.size()||(operator==opName.size()&&actionV>0)){
			//System.out.print(" not allowed\n");
		}
		else{
			if(actionV==0){
				String op	=	opName.get(operator);
				if(singletons.SystemStatus.isLeastLoaded(op)){
					features[(state*this.featuresPerState)+0]	=	1; 
				//	System.out.print(" least loaded\n");
				}
				else if(singletons.SystemStatus.isBottleneck(op)){
					features[(state*this.featuresPerState)+2]	=	1;
					//System.out.print(" bottleneck\n");
				}
				else{
					features[(state*this.featuresPerState)+4]	=	1;
				}
			}
			else{
				String op	=	opName.get(operator);
				if(singletons.SystemStatus.isBottleneck(op)){
					//logger.debug("operator "+op+" is bottleneck increase action "+action);
					features[(state*this.featuresPerState)+1]	=	1;
				}
				else{
					features[(state*this.featuresPerState)+3]	=	1;
				}
			}
			//System.out.print(" Action "+actionV+" on operator "+operator+"\n");
		}
		return features;
	}

	public static void main(String[] args) {
		ArrayList<String>op	=	new ArrayList<String>();
		op.add("op1");
		op.add("op2");
		SimpleFeaturesEvaluator eval	=	new SimpleFeaturesEvaluator(op,3,6);
		try {
			eval.getFeatures(0, 0);
			eval.getFeatures(0, 1);
			eval.getFeatures(0, 2);
			eval.getFeatures(0, 3);
			eval.getFeatures(0, 4);
			eval.getFeatures(0, 5);
			eval.getFeatures(0, 6);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
