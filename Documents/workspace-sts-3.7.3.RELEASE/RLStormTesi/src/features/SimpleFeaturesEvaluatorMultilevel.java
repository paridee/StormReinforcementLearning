package features;

import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import rl.executer.ActionExecutor;
import state.StateTranslator;

public class SimpleFeaturesEvaluatorMultilevel implements FeaturesEvaluator {
	ArrayList<String> opName;
	int states;
	int featuresPerState;
	int maxParallelism;
	StateTranslator translator;
	ActionExecutor  executor;
	int[] steps;
	int totalSize;
	public final static Logger logger	=	LogManager.getLogger(SimpleFeaturesEvaluatorMultilevel.class);
	
	
	
	public SimpleFeaturesEvaluatorMultilevel(ArrayList<String> opName,int[] steps, int states, int featuresPerState,
			int maxParallelism, StateTranslator translator, ActionExecutor executor) {
		super();
		this.opName = opName;
		this.steps	= steps;
		this.states = states;
		this.featuresPerState = featuresPerState;
		this.maxParallelism = maxParallelism;
		this.translator = translator;
		this.executor = executor;
		totalSize	=	(featuresPerState*states)+((maxParallelism+1)*this.states*steps.length*this.featuresPerState)+((maxParallelism+1)*(maxParallelism+1)*opName.size());
		
	}



	@Override
	public int[] getFeatures(int stateRaw, int action) throws Exception {
		// TODO Auto-generated method stub
		int[] feats	=	translator.getValuesFromState(translator.getStringFromId(stateRaw));
		int parallelismLevel	=	feats[1];
		int state				=	feats[0];
		int	cursor	=	0;
		int[] allFeatures	=	new int[totalSize];
		int[] features	=	new int[featuresPerState*states];
		//System.out.print("features number "+features.length);
		int operator	=	action/(2*steps.length); //counts steps as increase + decrease
		int actionV		=	action%(2*steps.length);
		int changeStep	=	actionV%steps.length;
		int offset		=	Integer.MAX_VALUE;
		if(operator==opName.size()&&actionV==0){
			features[(state*this.featuresPerState)+5]	=	1;
			offset	=	this.featuresPerState-1;
			logger.debug("Feature leave unchanged loaded in state load "+state);
		}
		else if(operator>opName.size()||(operator==opName.size()&&actionV>0)){
			//System.out.print(" not allowed\n");
		}
		else{
			if(actionV<steps.length){
				String op	=	opName.get(operator);
				if(singletons.SystemStatus.isLeastLoadedWithMultipleExecutors(op)){
					features[(state*this.featuresPerState)+0]	=	1; 
					offset	=	0;
					logger.debug("Feature decrease least loaded in state load "+state);
				//	System.out.print(" least loaded\n");
				}
				else if(singletons.SystemStatus.isBottleneck(op)){
					features[(state*this.featuresPerState)+2]	=	1;
					offset	=	2;
					logger.debug("Feature decrease bottleneck loaded in state load "+state);
					//System.out.print(" bottleneck\n");
				}
				else{
					features[(state*this.featuresPerState)+4]	=	1;
					logger.debug("Feature decrease intermediate loaded in state load "+state);
					offset	=	4;
				}
			}
			else if(actionV<2*steps.length){
				String op	=	opName.get(operator);
				if(singletons.SystemStatus.isBottleneck(op)){
					logger.debug("operator "+op+" is bottleneck increase action "+action);
					features[(state*this.featuresPerState)+1]	=	1;
					logger.debug("Feature increase bottleneck loaded in state load "+state);
					offset	=	1;
				}
				else{
					features[(state*this.featuresPerState)+3]	=	1;
					logger.debug("Feature increase another loaded in state load "+state);
					offset	=	3;
				}
			}
			//System.out.print(" Action "+actionV+" on operator "+operator+"\n");
		}
		for(int i=0;i<features.length;i++){
			allFeatures[i]	=	features[i];
			cursor++;
		}

		int[][][][] secondBlock	= 	new int[maxParallelism+1][this.states][steps.length][this.featuresPerState];
		if(offset<(this.featuresPerState)){
			secondBlock[parallelismLevel][state][changeStep][offset]	=	1;
			logger.debug("Second Block Feature offset "+offset+" for level "+parallelismLevel+" state "+state+" step "+changeStep);
		}
		for(int i=0;i<maxParallelism+1;i++){
			for(int j=0;j<this.states;j++){
				for(int k=0;k<steps.length;k++){
					for(int l=0;l<this.featuresPerState;l++){
						allFeatures[cursor]	=	secondBlock[i][j][k][l];
						cursor++;
					}
					
				}
			}
		}
		
		int[][][] thirdBlock	=	new int[maxParallelism+1][opName.size()][maxParallelism+1];
		if(state<2){
			int[] preview		=	executor.newConfigurationPreview(action, stateRaw);
			for(int i=0;i<preview.length;i++){
				thirdBlock[parallelismLevel][i][preview[i]]=1;
				logger.debug("Third block feature for parallelism level "+parallelismLevel+" operator "+i+" operator level "+preview[i]);
			}
			for(int i=0;i<maxParallelism+1;i++){
				for(int j=0;j<this.opName.size();j++){
					for(int k=0;k<maxParallelism+1;k++){
						allFeatures[cursor]	=	thirdBlock[i][j][k];
						cursor++;					
					}
				}
			}
		}
		return allFeatures;
	}



	@Override
	public int getFeaturesN() {
		return this.totalSize;
	}

}
