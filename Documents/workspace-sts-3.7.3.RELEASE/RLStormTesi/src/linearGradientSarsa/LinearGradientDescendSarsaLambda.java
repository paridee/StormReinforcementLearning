package linearGradientSarsa;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import rl.executer.ExecutorsChange;

public class LinearGradientDescendSarsaLambda implements Runnable {
	public final static Logger logger	=	LogManager.getLogger(LinearGradientDescendSarsaLambda.class);
	int simulatedStep	=	0;
	long nextStepTime	=	0;
	int 	featuresN		=	12;
	double 	epsilon			=	0.1;
	double  yota			=	0.9;
	double  lambda			=	0.5;
	expectedSarsa.StateReader 		reader;
	features.FeaturesEvaluator 	eval;
	rl.executer.ActionExecutor		executor;
	rl.alpha.AlphaCalculator		alphaCalculator;
	int actions				=	4;
	int initAction;
	double[] 	eVector			=	new double[featuresN];
	double[] 	omega			=	new double[featuresN];
	int 		currentState;
	int 		action;
	
	public LinearGradientDescendSarsaLambda(int featuresN, double epsilon, double yota, double lambda,
			expectedSarsa.StateReader reader, features.FeaturesEvaluator eval, rl.executer.ActionExecutor executor, rl.alpha.AlphaCalculator alphaCalculator,
			int actions,int initAction) {
		super();
		this.featuresN 			= 	featuresN;
		this.eVector			=	new double[featuresN];
		this.omega				=	new double[featuresN];
		this.epsilon 			=	epsilon;
		this.yota 				= 	yota;
		this.lambda 			= 	lambda;
		this.reader 			= 	reader;
		this.eval 				= 	eval;
		this.executor 			= 	executor;
		this.alphaCalculator 	= 	alphaCalculator;
		this.actions 			= 	actions;
		this.initAction			=	initAction;
		action					=	this.initAction;
	}
/*
	public SimulatedLinearGradientDescendSarsaLambda(SimulatedStateReader reader, SimulatedFeaturesEvaluator eval, SimulatedActionExecutor executor,
			SimulatedAlphaCalculator alphaCalculator,RewardCalculator rewardCalculator,int initAction,SimulationScheduler sched,SimulatedEvalIntervalManager intManager) {
		super();
		this.reader 			= 	reader;
		this.eval 				= 	eval;
		this.executor 			= 	executor;
		this.alphaCalculator 	= 	alphaCalculator;
		this.initAction			=	initAction;
		this.rewardCalculator	=	rewardCalculator;
		this.sched				=	sched;
		this.intManager			=	intManager;
		action			=	this.initAction;
	}*/


	@Override
	public void run() {
		while(true){
			int[] features	=	null;
			currentState	=	reader.getCurrentState();	//read state
			System.out.println("features for state "+currentState+" "+action);
			try {
				features		=	eval.getFeatures(currentState, action);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.println("feat length "+features.length+" "+features[features.length-1]);
			for(int i=0;i<features.length;i++){
				if(features[i]==1){
					eVector[i]	=	1;
				}
				System.out.print(features[i]+"\t");
			}
			System.out.println("\n");
	
			double reward	=	-Double.MAX_VALUE;
			try {
				reward	=	executor.execute(action,currentState);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
			currentState	=	reader.getCurrentState();
			logger.debug("reward obtained "+reward);
			double delta	=	reward;
			for(int i=0;i<features.length;i++){
				if(features[i]==1){
					delta		=	delta	+	omega[i];	
				}
			}
			logger.debug("delta value "+reward);
			double randomV			=	Math.random();
			double qActionChoosen	=	0;
			if(randomV>epsilon){
				//exploitation
				double Q[]	=	new double[actions];
				for(int i=0;i<actions;i++){
					try {
						features	=	eval.getFeatures(currentState, i);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					for(int j=0;j<features.length;j++){
						Q[i]	=	Q[i]	+	(omega[j]*features[j]);
					}
				}
				
				//testing
				for(int i=0;i<actions;i++){
					System.out.println("Q["+currentState+"]["+i+"] "+Q[i]+"\t");
				}
				
				
				//find best action
				int newAction	=	0;
				
				System.out.println("number of actions "+actions);
				
				double qAction	=	Q[0];
				for(int j=1;j<actions;j++){
					if(Q[j]>qAction){
						newAction	=	j;
						qAction		=	Q[j];
					}
				}
				action			=	newAction;
				qActionChoosen	=	qAction;
				System.out.println("Exploiation:Current state: "+currentState+" action "+action);
				//best action found
			}
			else{
				//exploration
				action			=	(int)((Math.random()*actions)%actions);
				try {
					features		=	eval.getFeatures(currentState, action);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				double tempQ	=	0;
				for(int j=0;j<features.length;j++){
					tempQ	=	tempQ	+	(omega[j]*features[j]);
				}
				qActionChoosen	=	tempQ;
				
				//testing
				System.out.println("Exploration:Current state: "+currentState+" action "+action);
				System.out.println("Q["+currentState+"]["+action+"] "+qActionChoosen+"\t");
				
			}
			delta	=	delta	+	(yota*qActionChoosen);
			for(int i=0;i<featuresN;i++){
				omega[i]	=	omega[i]+(alphaCalculator.getAlpha(action)*delta*eVector[i]);
				eVector[i]	=	yota*lambda*eVector[i];
			}
			
			System.out.println("Omega vector:");
			for(int i=0;i<featuresN;i++){
				System.out.print(omega[i]+"\t");
			}
			System.out.println("\nTrace vector:");
			for(int i=0;i<featuresN;i++){
				System.out.print(eVector[i]+"\t");
			}		
		}
	}
	
	
	private void saveVectors(String filename){
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(filename, "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int j=0;j<this.featuresN;j++){
			writer.print(eVector[j]+" ");
		}
		writer.print("\n");
		for(int j=0;j<this.featuresN;j++){
			writer.print(omega[j]+" ");
		}
		logger.debug("saved Vector");
		writer.close();
	}
	
	/*
	private void loadVector(String filename){
		int lineCounter	=	0;
		ArrayList<Double[]> matrix	=	new ArrayList<Double[]>();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			logger.debug("Q matrix is not saved in FS, skipping loading");
			return;
		}
		try {
		    StringBuilder sb = new StringBuilder();
		    String line;
			try {
				line = br.readLine();
			} catch (IOException e) {
				logger.debug(e.getMessage());
				logger.debug("skipping Q matrix loading");
				return;
			}

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        Double[] lineValues	=	new Double[actions];
		        String[] values =	line.split(" ");
		        if(values.length!=actions){
		        	logger.debug("File not compatible");
		        	return;
		        }
		        for(int i=0;i<values.length;i++){
		        	lineValues[i]	=	Double.parseDouble(values[i]);
		        }
		        matrix.add(lineValues);
		        lineCounter++;
		        try {
					line = br.readLine();
					
				} catch (IOException e) {
					logger.debug(e.getMessage());
					logger.debug("skipping Q matrix loading");
					return;
				}
		    }
		    if(lineCounter!=states){
				logger.debug("wrong line number");
				return;
		    }
		    for(int i=0;i<states;i++){
		    	Double[] row	=	matrix.get(i);
		    	for(int j=0;j<actions;j++){
		    		this.Q[i][j]=row[j];
		    	}
		    }
		    String everything = sb.toString();
		    logger.debug("matrix read\n "+everything);
		} finally {
		    try {
				br.close();
			} catch (IOException e) {
				logger.debug(e.getMessage());
				logger.debug("error while closing stream");
				return;
			}
		}
	}*/
	
}
