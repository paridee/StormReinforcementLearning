package expectedSarsa;

public class FixedIntervalManager implements IntervalManager {
	int interval;
	
	public FixedIntervalManager(int interval) {
		super();
		this.interval = interval;
	}

	public int getEvalInterval() {
		return interval;
	}

}
