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

import mainClasses.MainClass;
import rl.policies.PolicyChooser;

public class LinearGrandientDescendExpectedSarsa implements Runnable {

	public final static Logger logger	=	LogManager.getLogger(LinearGrandientDescendExpectedSarsa.class);
	int simulatedStep	=	0;
	long nextStepTime	=	0;
	int 	featuresN		=	12;
	double 	epsilon			=	0.1;
	double  yota			=	0.9;
	double  lambda			=	0.5;
	state.StateReader 		reader;
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
	
	public LinearGrandientDescendExpectedSarsa(PolicyChooser chooser,int featuresN, double epsilon, double yota, double lambda,
			state.StateReader reader, features.FeaturesEvaluator eval, rl.executer.ActionExecutor executor, rl.alpha.AlphaCalculator alphaCalculator,
			int actions,int initAction) {
		super();
		this.chooser			=	chooser;
		this.featuresN 			= 	featuresN;
		this.eVector			=	new double[featuresN];
		this.omega				=	new double[featuresN];
		/*
		for(int i=0;i<18;i++){
			omega[i]			=	3;
			System.out.println("Omega "+i+" "+omega[i]);
		}*/
		//TODO test initialization
		omega[0]	=	1;
		omega[11]	=	1;
		omega[13]	=	1;		
		//END TEST
		
		
		
		
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
		this.logger.debug("load finished");
		currentState	=	reader.getCurrentState();	//read state
		this.logger.debug("initial state read... beginning");
		while(true){
			int[] features	=	null;
			System.out.println("features for state "+currentState+" action "+action);
			try {
				features		=	eval.getFeatures(currentState, action);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			for(int i=0;i<features.length;i++){
				if(features[i]==1){
					eVector[i]	=	1;
				}
				//System.out.print(features[i]+" ");
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
			double delta	=	reward;
			double estValue	=	0;
			for(int i=0;i<features.length;i++){
				if(features[i]==1){
					delta		=	delta	-	omega[i];	
					estValue	=	estValue+	omega[i];
				}
			}
			currentState	=	reader.getCurrentState();			
			double Q[]	=	this.getUpdatedQMatrix();			
			double[] policyR	=	this.chooser.policyForState(currentState,Q);
			double tempP			=	0;
			for(int i=0;i<actions;i++){
				if(Q[i]>Double.NEGATIVE_INFINITY){
					tempP			=	tempP	+	(policyR[i]*Q[i]);
				}
			}
			double V			=	tempP;
			delta	=	delta	+	(yota*V);		
			MainClass.GRADIENT_DELTA.set(delta);
			for(int i=0;i<featuresN;i++){
				//System.out.println("updating values for feature "+i+" omega "+omega[i]);
				//System.out.println("alpha "+alphaCalculator.getAlpha(action)+" delta "+delta+" trace "+eVector[i]);
				double temp			=	omega[i];
				double featDelta	=	(alphaCalculator.getAlpha(action)*delta*eVector[i]);
				omega[i]	=	omega[i]+featDelta;
				if(omega[i]!=0){
					logger.debug("Updating omega before: "+temp+" after "+omega[i]+" feature delta "+featDelta);
				}
				//System.out.println("updated values for feature "+i+" omega "+omega[i]);
				eVector[i]	=	yota*lambda*eVector[i];
			}				
			logger.debug("updated matrix");
			Q	=	this.getUpdatedQMatrix();
			//TODO ONLY FOR DEBUG
			for(int i=0;i<Q.length;i++){
				logger.debug("Q["+currentState+"]["+i+"] = "+Q[i]);
			}	
			//TODO END DEBUG
			action				=	chooser.actionForState(currentState, Q);
			logger.debug("Action choosen "+action);
			this.saveVectors(filename);
			logger.debug("previous action reward obtained "+reward);
			logger.debug("previous action estimated value "+delta);
			logger.debug("previous action delta value "+delta+" yota "+this.yota);
			
			System.out.println("Omega vector:");
			for(int i=0;i<featuresN;i++){
				//System.out.print(omega[i]+" ");
			}
			System.out.println("\nTrace vector:");
			for(int i=0;i<featuresN;i++){
				//System.out.print(eVector[i]+" ");
			}		
		}
	}
	
	private double[] getUpdatedQMatrix(){
		double Q[]	=	new double[actions];
		int[] features=null;
		for(int i=0;i<actions;i++){
			try {
				features	=	eval.getFeatures(currentState, i);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for(int j=0;j<features.length;j++){
				Q[i]	=	Q[i]	+	(omega[j]*features[j]);
				if(features[j]>=1){
					logger.debug("feature "+j+" omega "+omega[j]);
				}
				if(Q[i]==Double.NaN){
					logger.debug("NaN value omega "+omega[j]+" features "+features[j]);
				}
			}
			boolean feasible	=	this.executor.isFeasible(currentState,i);
			if(feasible==false){
				Q[i]	=	Double.NEGATIVE_INFINITY;
			}
			logger.debug("Q["+i+"] = "+Q[i]);
		}
		return Q;
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
