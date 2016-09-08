package state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mainClasses.MainClass;
import monitors.StormMonitor;

public class ProcessTimeStateReader implements StateReader {
	/**
	 * @author Paride Casulli
	 * Class managing the system state from process latency in Storm,
	 * another thread fills data in singletons, status are 0: underloaded
	 * 1: correctly loaded 2: overloaded with a threshold approach
	 */
	private double targetTime;
	private double underLoadThreshold	=	0.25;
	private double overLoadThreshold	=	4;
	private static final Logger LOG = LoggerFactory.getLogger(ProcessTimeStateReader.class);
	
	/**
	 * Constructor for this reader
	 * @param targetTime target processing time of the topology
	 * @param underLoadThreshold underload factor for processing time ex 0.25 for a quarter of target time
	 * @param overLoadThreshold overload factor for processing time ex 5
	 */
	public ProcessTimeStateReader(double targetTime, double underLoadThreshold, double overLoadThreshold) {
		super();
		this.targetTime = targetTime;
		this.underLoadThreshold = underLoadThreshold;
		this.overLoadThreshold = overLoadThreshold;
	}

	/**
	 * Return current status
	 * @return status 0 1 2 underloaded ok overloaded
	 */
	@Override
	public int getCurrentState() {
		double currentLatency	=	singletons.SystemStatus.processLatency;
		while(currentLatency<=0){
			//if status is unknown return normal load
			LOG.warn("unknown system status, waiting data (1s sleep)");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		MainClass.LATENCY_VAL.set(currentLatency);
		if(currentLatency>0){
			if(currentLatency<this.underLoadThreshold*targetTime){
				LOG.info("System underloaded latency "+currentLatency+" ms");
				return	0;
			}
			else if(currentLatency<this.overLoadThreshold*targetTime){
				LOG.info("System correctly loaded latency "+currentLatency+" ms");
				return 1;
			}
			else{
				LOG.info("System over latency "+currentLatency+" ms");
				return 2;
			}
		}
		

		return 1;
	}

	@Override
	public boolean isOperatorUnderloaded(String opName) {
		// TODO Auto-generated method stub
		return false;
	}


}
