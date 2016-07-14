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
import rl.policies.PolicyChooser;

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
	PolicyChooser chooser;
	String filename	=	"vectors.txt";
	
	public LinearGradientDescendSarsaLambda(PolicyChooser chooser,int featuresN, double epsilon, double yota, double lambda,
			expectedSarsa.StateReader reader, features.FeaturesEvaluator eval, rl.executer.ActionExecutor executor, rl.alpha.AlphaCalculator alphaCalculator,
			int actions,int initAction) {
		super();
		this.chooser			=	chooser;
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
		this.loadVector(filename);
		currentState	=	reader.getCurrentState();	//read state
		while(true){
			int[] features	=	null;
			System.out.println("features for state "+currentState+" action "+action);
			try {
				features		=	eval.getFeatures(currentState, action);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.println("feat length "+features.length);
			for(int i=0;i<features.length;i++){
				if(features[i]==1){
					eVector[i]	=	1;
				}
				System.out.print(features[i]+" ");
			}
			System.out.println("\n");
	
			double reward	=	-Double.MAX_VALUE;
			try {
				logger.debug("going to execute action "+action);
				reward	=	executor.execute(action,currentState);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
			mainClasses.MainClass.REWARD_VAL.set(reward);
			logger.debug("reward obtained "+reward);
			double delta	=	reward;
			for(int i=0;i<features.length;i++){
				if(features[i]==1){
					delta		=	delta	-	omega[i];	
				}
			}
			logger.debug("delta value "+delta);
			currentState	=	reader.getCurrentState();
			double qActionChoosen	=	0;
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
				boolean feasible	=	this.executor.isFeasible(currentState,i);
				if(feasible==false){
					Q[i]	=	Double.NEGATIVE_INFINITY;
				}
				logger.debug("Q["+currentState+"]["+i+"] = "+Q[i]);
			}
			action				=	chooser.actionForState(currentState, Q);
			qActionChoosen			=	Q[action];
			delta	=	delta	+	(yota*qActionChoosen);
			logger.debug("Action choosen "+action);
			for(int i=0;i<featuresN;i++){
				//System.out.println("updating values for feature "+i+" omega "+omega[i]);
				//System.out.println("alpha "+alphaCalculator.getAlpha(action)+" delta "+delta+" trace "+eVector[i]);
				omega[i]	=	omega[i]+(alphaCalculator.getAlpha(action)*delta*eVector[i]);
				//System.out.println("updated values for feature "+i+" omega "+omega[i]);
				eVector[i]	=	yota*lambda*eVector[i];
			}
			this.saveVectors(filename);
			System.out.println("Omega vector:");
			for(int i=0;i<featuresN;i++){
				System.out.print(omega[i]+" ");
			}
			System.out.println("\nTrace vector:");
			for(int i=0;i<featuresN;i++){
				System.out.print(eVector[i]+" ");
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
	

	private void loadVector(String filename){
		int lineCounter	=	0;
		ArrayList<Double[]> matrix	=	new ArrayList<Double[]>();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			logger.debug("Vector is not saved in FS, skipping loading");
			return;
		}
		try {
		    StringBuilder sb = new StringBuilder();
		    String line;
			try {
				line = br.readLine();
			} catch (IOException e) {
				logger.debug(e.getMessage());
				logger.debug("skipping Feature vector loading");
				return;
			}

		    if(line!=null){
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        double[] lineValues	=	new double[this.featuresN];
		        String[] values =	line.split(" ");
		        if(values.length!=this.featuresN){
		        	logger.debug("File not compatible");
		        	return;
		        }
		        logger.debug("size array "+values.length);
		        for(int i=0;i<values.length;i++){
		        	lineValues[i]	=	Double.parseDouble(values[i]);
		        }
		        this.eVector	=	lineValues;
		        try {
					line = br.readLine();
					
				} catch (IOException e) {
					logger.debug(e.getMessage());
					logger.debug("skipping trace vector loading");
					return;
				}
		    }
		    if(line!=null){
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        double[] lineValues	=	new double[this.featuresN];
		        String[] values =	line.split(" ");
		        if(values.length!=this.featuresN){
		        	logger.debug("File not compatible");
		        	return;
		        }
		        for(int i=0;i<values.length;i++){
		        	lineValues[i]	=	Double.parseDouble(values[i]);
		        }
		        this.omega	=	lineValues;
		        try {
					line = br.readLine();
					
				} catch (IOException e) {
					logger.debug(e.getMessage());
					logger.debug("skipping omega vector loading");
					return;
				}
		    }
		    
		} finally {
		    try {
				br.close();
			} catch (IOException e) {
				logger.debug(e.getMessage());
				logger.debug("error while closing stream");
				return;
			}
		}
	}
	
}
