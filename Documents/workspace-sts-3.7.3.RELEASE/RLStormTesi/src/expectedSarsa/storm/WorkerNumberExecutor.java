package expectedSarsa.storm;

import java.io.IOException;

import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import expectedSarsa.ActionExecutor;
import expectedSarsa.IntervalManager;
import expectedSarsa.RewardCalculator;
import mainClasses.MainClass;
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
				int	sleepInt	=	intManager.getEvalInterval();
				LOG.info("Evaulation time "+sleepInt+", sleep");
				Thread.sleep(sleepInt);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		// TODO Auto-generated method stub
		return rewarder.giveReward();
	}

}
