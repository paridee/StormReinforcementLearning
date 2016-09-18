package features;

import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import rl.executer.ActionExecutor;
import state.StateTranslator;

public class SimpleFeaturesEvaluatorMultilevelExtended implements FeaturesEvaluator {
	ArrayList<String> opName;
	int states;
	int featuresPerState;
	int maxParallelism;
	StateTranslator translator;
	ActionExecutor  executor;
	int[] steps;
	int totalSize;;
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
		int[] arraySizes	=	new int[15];
		arraySizes[0]	=	featuresPerState*states;
		arraySizes[1]	=	(maxParallelism+1)*this.states*steps.length*this.featuresPerState;	
		arraySizes[2]	=	this.states*3*this.featuresPerState;
		arraySizes[3]	=	this.states*steps.length*this.featuresPerState;
		arraySizes[4]	=	3*this.states*steps.length*this.featuresPerState;	
		arraySizes[5]	=	this.states*(this.maxParallelism+1)*this.featuresPerState;
		arraySizes[6]	=	(maxParallelism+1)*(maxParallelism+1)*opName.size();
		arraySizes[7]	=	(this.states)*(this.opName.size())*(11)*(this.featuresPerState);
		arraySizes[8]	=	(this.states)*(this.opName.size())*(11)*(this.featuresPerState)*(this.steps.length);
		arraySizes[9]	=	(this.states)*(this.maxParallelism+1)*(this.opName.size())*(11)*(this.featuresPerState);
		arraySizes[10]	=	(this.states)*(this.maxParallelism+1)*(this.opName.size())*(11)*(this.featuresPerState)*(this.steps.length);
		arraySizes[11]	=	(states)*(this.opName.size())*(11)*(this.featuresPerState);
		arraySizes[12]	=	(states)*(2)*(this.featuresPerState);
		arraySizes[13]	=	(states)*(6)*(this.featuresPerState);
		arraySizes[14]	=	(states)*(6)*(this.featuresPerState)*(this.steps.length);
		totalSize	=	0;
		for(int i=0;i<arraySizes.length;i++){
			totalSize	=	totalSize	+	arraySizes[i];
		}
		//logger.setLevel(Level.OFF);
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
					System.out.print(" least loaded\n");
				}
				else if(singletons.SystemStatus.isBottleneck(op)){
					features[(state*this.featuresPerState)+2]	=	1;
					offset	=	2;
					logger.debug("Feature decrease bottleneck loaded in state load "+state);
					System.out.print(" bottleneck\n");
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
			System.out.print(" Action "+actionV+" on operator "+operator+"\n");
		}
		for(int i=0;i<features.length;i++){
			allFeatures[i]	=	features[i];
			if(allFeatures[cursor]>=1){
				//logger.debug("Feature number: "+cursor);
			}
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
					if(allFeatures[cursor]>=1){
						//logger.debug("Feature number: "+cursor);
					}
					cursor++;
				}
			}
		}
		

		int[][][] thirdBlock	= 	new int[this.states][steps.length][this.featuresPerState];
		if(offset<(this.featuresPerState)){
			thirdBlock[state][changeStep][offset]	=	1;
			logger.debug("Third Block Feature offset "+offset+" state "+state+" step "+changeStep);
		}
		for(int j=0;j<this.states;j++){
			for(int k=0;k<steps.length;k++){
				for(int l=0;l<this.featuresPerState;l++){
					allFeatures[cursor]	=	thirdBlock[j][k][l];
					if(allFeatures[cursor]>=1){
						//logger.debug("Feature number: "+cursor);
					}
					cursor++;
				}
				
			}
		}
		
		int[][][][] fourthBlock	= 	new int[3][this.states][steps.length][this.featuresPerState];
		if(offset<(this.featuresPerState)){
			fourthBlock[loadMacroLevel][state][changeStep][offset]	=	1;
			logger.debug("Fourth Block Feature offset "+offset+" for macrolevel "+loadMacroLevel+" state "+state+" step "+changeStep);
		}
		for(int i=0;i<3;i++){
			for(int j=0;j<this.states;j++){
				for(int k=0;k<steps.length;k++){
					for(int l=0;l<this.featuresPerState;l++){
						allFeatures[cursor]	=	fourthBlock[i][j][k][l];
						if(allFeatures[cursor]>=1){
							//logger.debug("Feature number: "+cursor);
						}
						cursor++;
					}
					
				}
			}
		}
		
		int[][][]	fifth	=	new int[this.states][this.maxParallelism+1][this.featuresPerState];	
		fifth[state][parallelismLevel][offset]	=	1;
		logger.debug("Fifth Block Feature offset "+offset+" state "+state+" parallelism "+parallelismLevel);
		for(int i=0;i<this.states;i++){
			for(int j=0;j<this.maxParallelism+1;j++){
				for(int k=0;k<this.featuresPerState;k++){
					allFeatures[cursor]	=	fifth[i][j][k];
					if(allFeatures[cursor]>=1){
						//logger.debug("Feature number: "+cursor);
					}
					cursor++;
				}
			}
		}
		
		int[][][][] sixthBlock	= 	new int[maxParallelism+1][this.states][steps.length][this.featuresPerState];
		if(offset<(this.featuresPerState)){
			sixthBlock[parallelismLevel][state][changeStep][offset]	=	1;
			logger.debug("Sixth Block Feature offset "+offset+" for level "+parallelismLevel+" state "+state+" step "+changeStep);
		}
		for(int i=0;i<maxParallelism+1;i++){
			for(int j=0;j<this.states;j++){
				for(int k=0;k<steps.length;k++){
					for(int l=0;l<this.featuresPerState;l++){
						allFeatures[cursor]	=	sixthBlock[i][j][k][l];
						if(allFeatures[cursor]>=1){
							//logger.debug("Feature number: "+cursor);
						}
						cursor++;
					}
					
				}
			}
		}
		
		int[][][] seventhBlock	=	new int[maxParallelism+1][opName.size()][maxParallelism+1];
		if(state<2){
			int[] preview		=	executor.newConfigurationPreview(action, stateRaw);
			for(int i=0;i<preview.length;i++){
				seventhBlock[parallelismLevel][i][preview[i]]=1;
				logger.debug("Seventh block feature for parallelism level "+parallelismLevel+" operator "+i+" operator level "+preview[i]);
			}
			for(int i=0;i<maxParallelism+1;i++){
				for(int j=0;j<this.opName.size();j++){
					for(int k=0;k<maxParallelism+1;k++){
						allFeatures[cursor]	=	seventhBlock[i][j][k];
						if(allFeatures[cursor]>=1){
							//logger.debug("Feature number: "+cursor);
						}
						cursor++;					
					}
				}
			}
		}
		int[][][][] eightthBlock	=	new int[this.states][this.opName.size()][11][this.featuresPerState];
		if(operator<opName.size()){//if is not do nothing	
			int opUtilLevel										=	feats[2+(opName.size())+operator];
			logger.debug("DEBUG block 8 "+state+" "+operator+" "+opUtilLevel+" "+offset);
			eightthBlock[state][operator][opUtilLevel][offset]	=	1;			
			logger.debug("Block feature 8 for state "+state+" operator "+operator+" operator util level "+opUtilLevel+" offset "+offset);
		}
		for(int i=0;i<this.states;i++){
			for(int j=0;j<this.opName.size();j++){
				for(int k=0;k<11;k++){
					for(int l=0;l<this.featuresPerState;l++){
						allFeatures[cursor]	=	eightthBlock[i][j][k][l];
						if(allFeatures[cursor]>=1){
							//logger.debug("Feature number: "+cursor);
						}
						cursor++;
					}
				}
			}
		}
		int[][][][][] ninethBlock	=	new int[this.states][this.opName.size()][11][this.featuresPerState][this.steps.length];
		if(operator<opName.size()){//if is not do nothing
			int opUtilLevel										=	feats[2+(opName.size())+operator];
			ninethBlock[state][operator][opUtilLevel][offset][changeStep]	=	1;
			logger.debug("Block feature 9 for state "+state+" operator "+operator+" operator util level "+opUtilLevel+" offset "+offset+" step "+changeStep);
		}
		for(int i=0;i<this.states;i++){
			for(int j=0;j<this.opName.size();j++){
				for(int k=0;k<11;k++){
					for(int l=0;l<this.featuresPerState;l++){
						for(int m=0;m<this.steps.length;m++){
							allFeatures[cursor]	=	ninethBlock[i][j][k][l][m];
							if(allFeatures[cursor]>=1){
								//logger.debug("Feature number: "+cursor);
							}
							cursor++;	
						}
					}
				}
			}
		}		
		
		int[][][][][] tenthBlock	=	new int[this.states][this.maxParallelism+1][this.opName.size()][11][this.featuresPerState];
		if(operator<opName.size()){
			int opUtilLevel										=	feats[2+(opName.size())+operator];
			tenthBlock[state][parallelismLevel][operator][opUtilLevel][offset]	=	1;
			logger.debug("Block feature 10 for state "+state+" level "+parallelismLevel+" operator "+operator+" operator util level "+opUtilLevel+" offset "+offset);
		}
		for(int i=0;i<this.states;i++){
			for(int j=0;j<this.maxParallelism+1;j++){
				for(int k=0;k<this.opName.size();k++){
					for(int l=0;l<11;l++){
						for(int m=0;m<this.featuresPerState;m++){
							allFeatures[cursor]	=	tenthBlock[i][j][k][l][m];
							if(allFeatures[cursor]>=1){
								//logger.debug("Feature number: "+cursor);
							}
							cursor++;	
						}
					}
				}
			}
		}		
		
		
		int[][][][][][] eleventhBlock	=	new int[this.states][this.maxParallelism+1][this.opName.size()][11][this.featuresPerState][this.steps.length];
		if(operator<opName.size()){
			int opUtilLevel										=	feats[2+(opName.size())+operator];
			eleventhBlock[state][parallelismLevel][operator][opUtilLevel][offset][changeStep]	=	1;
			logger.debug("Block feature 11 for state "+state+" level "+parallelismLevel+" operator "+operator+" operator util level "+opUtilLevel+" offset "+offset+" step "+changeStep);
		}

		for(int i=0;i<this.states;i++){
			for(int j=0;j<this.maxParallelism+1;j++){
				for(int k=0;k<this.opName.size();k++){
					for(int l=0;l<11;l++){
						for(int m=0;m<this.featuresPerState;m++){
							for(int n=0;n<this.steps.length;n++){
								allFeatures[cursor]	=	eleventhBlock[i][j][k][l][m][n];
								if(allFeatures[cursor]>=1){
									//logger.debug("Feature number: "+cursor);
								}
								cursor++;	
								
							}
						}
					}
				}
			}
		}	
		
		int[][][][] twelvethBlock			=	new int[states][this.opName.size()][11][this.featuresPerState];
		
		if(operator<opName.size()){
			int opUtilLevel										=	feats[2+(opName.size())+operator];
			logger.debug("Block feature 12 for state "+state+" operator "+operator+" operator util level "+opUtilLevel+" offset "+offset);
			twelvethBlock[state][operator][opUtilLevel][offset]	=	1;
		}
		
		for(int i=0;i<this.states;i++){
			for(int k=0;k<this.opName.size();k++){
				for(int l=0;l<11;l++){
					for(int m=0;m<this.featuresPerState;m++){
							allFeatures[cursor]	=	twelvethBlock[i][k][l][m];
							if(allFeatures[cursor]>=1){
								//logger.debug("Feature number: "+cursor);
							}
							cursor++;			
					}
				}
			}
		}	
		
		int[][][] thirteenthBlock	=	new int[states][2][this.featuresPerState];
		if(operator<opName.size()){//if is not do nothing
			int opUtilLevel	=	feats[2+(opName.size())+operator];
			int ind	=	0;
			if(opUtilLevel>5){
				ind	=	1;
			}
			thirteenthBlock[state][ind][offset]	=	1;
			logger.debug("Block feature 13 for state "+state+" high utilization "+ind+" offset "+offset);
		}
		for(int i=0;i<states;i++){
			for(int j=0;j<2;j++){
				for(int k=0;k<this.featuresPerState;k++){
					allFeatures[cursor]	=	thirteenthBlock[i][j][k];
					cursor++;
				}
			}
		}
		int[][][] fourteenthBlock = new int[states][6][this.featuresPerState];	
		if(operator<opName.size()){//if is not do nothing
			int opUtilLevel	=	feats[2+(opName.size())+operator];
			int ind	=	opUtilLevel/2;
			fourteenthBlock[state][ind][offset]	=	1;
			logger.debug("Block feature 14 for state "+state+" utilization/5 "+ind+" offset "+offset);
		}
		for(int i=0;i<states;i++){
			for(int j=0;j<6;j++){
				for(int k=0;k<this.featuresPerState;k++){
					allFeatures[cursor]	=	fourteenthBlock[i][j][k];
					cursor++;
				}
			}
		}
		int[][][][] fifteenthBlock = new int[states][6][this.featuresPerState][this.steps.length];	
		if(operator<opName.size()){//if is not do nothing
			int opUtilLevel	=	feats[2+(opName.size())+operator];
			int ind	=	opUtilLevel/2;
			fifteenthBlock[state][ind][offset][changeStep]	=	1;
			logger.debug("Block feature 15 for state "+state+" utilization/5 "+ind+" offset "+offset+" step "+changeStep);
		}
		for(int i=0;i<states;i++){
			for(int j=0;j<6;j++){
				for(int k=0;k<this.featuresPerState;k++){
					for(int l=0;l<steps.length;l++){
						allFeatures[cursor]	=	fifteenthBlock[i][j][k][l];
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



