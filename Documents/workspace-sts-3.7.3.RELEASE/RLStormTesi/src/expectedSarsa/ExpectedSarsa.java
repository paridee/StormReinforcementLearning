package expectedSarsa;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rl.alpha.AlphaCalculator;
import rl.executer.ActionExecutor;
import rl.policies.PolicyChooser;

public class ExpectedSarsa implements Runnable{
	int states				=	1;
	int actions				=	1;
	//int	evalInterval		=	1000;
	int currentState		=	0;
	double yotaParameter	=	0.2;
	String filename			=	"QMatrix.txt";
	double[] V;
	double[][] Q;
	PolicyChooser 	policy;
	ActionExecutor 	executor;
	StateReader		stateReader;
	AlphaCalculator	alphaCalculator;
	private static final Logger logger = LoggerFactory.getLogger(ExpectedSarsa.class);
	
	public ExpectedSarsa(int states, int actions,int initialState,PolicyChooser chooser,ActionExecutor actionExecutor,StateReader stateReader,AlphaCalculator alphaCalculator){
		V	=	new double[states];
		Q	=	new double[states][actions];
		this.states				=	states;
		this.actions			=	actions;
		this.policy				=	chooser;
		this.executor			=	actionExecutor;
		this.stateReader		=	stateReader;
		this.alphaCalculator	=	alphaCalculator;
		currentState	=	0;
		for(int i=0;i<states;i++){
			V[i]	=	0;
			for(int j=0;j<actions;j++){
				Q[i][j]	=	0;
			}
		}
	}

	private void saveQMatrix(String filename){
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
		for(int i=0;i<states;i++){
			for(int j=0;j<actions;j++){
				writer.print(Q[i][j]+" ");
			}
			writer.print("\n");
		}
		logger.debug("saved Q Matrix");
		writer.close();
	}
	private void loadQMatrix(String filename){
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
	}
	
	@Override
	public void run() {
		logger.debug("Start Expected Sarsa Algorithm");
		currentState		=	this.stateReader.getCurrentState();
		logger.debug("State initialization "+currentState);
		this.loadQMatrix(filename);
		while(true){
			currentState		=	this.stateReader.getCurrentState();
			int action			=	this.policy.actionForState(currentState,Q);
			logger.debug("Action chosen "+action);	//test
			double reward		=	this.executor.execute(action,currentState);
			logger.debug("Reward obtained "+reward);	//test
			mainClasses.MainClass.REWARD_VAL.set(reward);
			int oldState		=	this.currentState;
			int newState		=	this.stateReader.getCurrentState();
			this.currentState	=	newState;
			double[] policy		=	this.policy.policyForState(newState,Q);
			
			
			logger.debug("Policy for state "+newState+":");	//test
			for(int i=0;i<policy.length;i++){
				logger.debug("Act:"+i+":"+policy[i]);
			}
			logger.debug("");								//endtest
			
			double temp			=	0;
			for(int i=0;i<actions;i++){
				temp			=	temp	+	(policy[i]*Q[newState][i]);
			}
			V[newState]			=	temp;
			Q[oldState][action]	=	Q[oldState][action]+
									this.alphaCalculator.getAlpha(action)*(
											reward+(this.yotaParameter*V[newState])-Q[oldState][action]);
			
			logger.info("Updated Q["+oldState+"]["+action+"]");
			logger.info("Q matrix:"+states+" "+actions);
			for(int i=0;i<states;i++){
				for(int j=0;j<actions;j++){
					//logger.debug("printing "+i+" "+j);
					double qij					=	Q[i][j];
					DecimalFormat numberFormat = new DecimalFormat("0.00");
					System.out.print(numberFormat.format(qij)+"\t");
					mainClasses.MainClass.qMatrix[i][j].set(qij);
				}
				System.out.print("\n");
			}
			this.saveQMatrix(this.filename);
			/*
			try {
				Thread.sleep(this.evalInterval);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		}
		
	}
	
	
}
