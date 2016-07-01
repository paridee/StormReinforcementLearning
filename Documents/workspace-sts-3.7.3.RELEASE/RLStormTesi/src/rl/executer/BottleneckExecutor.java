package rl.executer;

import java.util.HashMap;

public class BottleneckExecutor implements ActionExecutor {
	private int maxParallelism;
	
	
	//decrease least congested operator increase most congested operator
	@Override
	public double execute(int action) {
		HashMap<Double,HashMap<String,Integer>> operatorCapacityAndLevel	=	new HashMap<Double, HashMap<String, Integer>>();
		if(action==0){
			operatorCapacityAndLevel.clear();
			populate(operatorCapacityAndLevel);
			boolean stop	=	false;
			while(stop==false){
				
				//double leastLoaded	=	
			}
		}
		// TODO Auto-generated method stub
		return 0;
	}

	private void populate(HashMap<Double, HashMap<String, Integer>> operatorCapacityAndLevel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double execute(int action, int state) {
		// TODO Auto-generated method stub
		return 0;
	}

}
