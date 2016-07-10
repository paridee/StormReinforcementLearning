package rl.executer;

import java.io.IOException;

import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import expectedSarsa.IntervalManager;
import mainClasses.MainClass;
import rl.rewarder.RewardCalculator;
import singletons.Settings;

public class WorkerNumberExecutor implements ActionExecutor {
	private static final Logger LOG = 	LoggerFactory.getLogger(WorkerNumberExecutor.class);
	private RewardCalculator 	rewarder;
	private IntervalManager		intManager;
	
	public WorkerNumberExecutor(RewardCalculator rewarder,IntervalManager intManager) {
		super();
		this.rewarder 	= 	rewarder;
		this.intManager	=	intManager;
	}

	@Override
	public double execute(int action) {
		int nWorker	=	action+1;
		if(nWorker!=singletons.SystemStatus.workerNumber){
			Runtime rt = Runtime.getRuntime();
			try {
				String command	=	singletons.Settings.stormPath+"storm rebalance "+Settings.topologyName+" -n "+nWorker;
				LOG.info("Sending command to rebalance "+command);
				singletons.SystemStatus.workerNumber	=	nWorker;
				MainClass.PARALLELISM_VAL.set(nWorker);
				Process pr 		= rt.exec(command);
			} catch (IOException e) {
				e.printStackTrace();
				LOG.debug(e.getMessage());
			}		
		}
		int	sleepInt	=	intManager.getEvalInterval();
		LOG.info("Evaulation time "+sleepInt+", sleep");
		try {
			Thread.sleep(sleepInt);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOG.debug(e.getMessage());
		}
		// TODO Auto-generated method stub
		return rewarder.giveReward();
	}

	@Override
	public double execute(int action, int state) {
		return execute(action);
	}

	@Override
	public boolean isFeasible(int currentState, int i) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int[] newConfigurationPreview(int action, int state) {
		// TODO Auto-generated method stub
		return null;
	}

}
