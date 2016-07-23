package features;

import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import expectedSarsa.storm.StateTranslator;
import rl.executer.ActionExecutor;

public class SimpleFeaturesEvaluatorMultilevelExtended implements FeaturesEvaluator {
	ArrayList<String> opName;
	int states;
	int featuresPerState;
	int maxParallelism;
	StateTranslator translator;
	ActionExecutor  executor;
	int[] steps;
	int totalSize;
	public final static Logger logger	=	LogManager.getLogger(SimpleFeaturesEvaluatorMultilevelExtended.class);
	
	
	
	public SimpleFeaturesEvaluatorMultilevelExtended(ArrayList<String> opName,int[] steps, int states, int featuresPerState,
			int maxParallelism, StateTranslator translator, ActionExecutor executor) {
		super();
		this.opName = opName;
		this.steps	= steps;
		this.states = states;
		this.featuresPerState = featuresPerState;
		this.maxParallelism = maxParallelism;
		this.translator = translator;
		this.executor = executor;
		totalSize	=	(featuresPerState*states)+((maxParallelism+1)*this.states*steps.length*this.featuresPerState)+(this.states*3*this.featuresPerState)+(3*this.states*steps.length*this.featuresPerState)+(this.states*(this.maxParallelism+1)*this.featuresPerState)+((maxParallelism+1)*(maxParallelism+1)*opName.size());
		
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
				if(singletons.SystemStatus.isLeastLoaded(op)){
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
		
		int loadMacroLevel		=	parallelismLevel/(this.maxParallelism/3);
		if(loadMacroLevel==3){ // if @ maximum level fallback to 3rd /3
			loadMacroLevel	=	2;
		}
		
		int[][][]	secondBlock	=	new int[this.states][3][this.featuresPerState];	
		secondBlock[state][loadMacroLevel][offset]	=	1;
		logger.debug("Second Block Feature offset "+offset+" macro load level "+loadMacroLevel);
		for(int i=0;i<this.states;i++){
			for(int j=0;j<3;j++){
				for(int k=0;k<this.featuresPerState;k++){
					allFeatures[cursor]	=	secondBlock[i][j][k];
					cursor++;
				}
			}
		}
		
		int[][][][] thirdBlock	= 	new int[3][this.states][steps.length][this.featuresPerState];
		if(offset<(this.featuresPerState)){
			thirdBlock[loadMacroLevel][state][changeStep][offset]	=	1;
			logger.debug("Third Block Feature offset "+offset+" for macrolevel "+loadMacroLevel+" state "+state+" step "+changeStep);
		}
		for(int i=0;i<3;i++){
			for(int j=0;j<this.states;j++){
				for(int k=0;k<steps.length;k++){
					for(int l=0;l<this.featuresPerState;l++){
						allFeatures[cursor]	=	thirdBlock[i][j][k][l];
						cursor++;
					}
					
				}
			}
		}
		
		int[][][]	fourthBlock	=	new int[this.states][this.maxParallelism+1][this.featuresPerState];	
		fourthBlock[state][parallelismLevel][offset]	=	1;
		logger.debug("Fourth Block Feature offset "+offset+" state "+state+" parallelism "+parallelismLevel);
		for(int i=0;i<this.states;i++){
			for(int j=0;j<this.maxParallelism+1;j++){
				for(int k=0;k<this.featuresPerState;k++){
					allFeatures[cursor]	=	fourthBlock[i][j][k];
					cursor++;
				}
			}
		}
		
		int[][][][] fifthBlock	= 	new int[maxParallelism+1][this.states][steps.length][this.featuresPerState];
		if(offset<(this.featuresPerState)){
			fifthBlock[parallelismLevel][state][changeStep][offset]	=	1;
			logger.debug("Fifth Block Feature offset "+offset+" for level "+parallelismLevel+" state "+state+" step "+changeStep);
		}
		for(int i=0;i<maxParallelism+1;i++){
			for(int j=0;j<this.states;j++){
				for(int k=0;k<steps.length;k++){
					for(int l=0;l<this.featuresPerState;l++){
						allFeatures[cursor]	=	fifthBlock[i][j][k][l];
						cursor++;
					}
					
				}
			}
		}
		
		int[][][] sixthBlock	=	new int[maxParallelism+1][opName.size()][maxParallelism+1];
		if(state<2){
			int[] preview		=	executor.newConfigurationPreview(action, stateRaw);
			for(int i=0;i<preview.length;i++){
				sixthBlock[parallelismLevel][i][preview[i]]=1;
				logger.debug("Sixth block feature for parallelism level "+parallelismLevel+" operator "+i+" operator level "+preview[i]);
			}
			for(int i=0;i<maxParallelism+1;i++){
				for(int j=0;j<this.opName.size();j++){
					for(int k=0;k<maxParallelism+1;k++){
						allFeatures[cursor]	=	sixthBlock[i][j][k];
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



