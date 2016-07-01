package singletons;

import java.util.HashMap;
import java.util.Iterator;

public class SystemStatus {
	public static double 	processLatency	=	-1;
	public static int	 	workerNumber	=	-1;
	public static HashMap<String, Integer>	executors		=	new HashMap<String,Integer>();
	public static HashMap<String, Double>	operatorCapacity=	new HashMap<String,Double>();
	public static void setExecutorLevel(String exName,int value){
		executors.put(exName, value);
	}
	
	public static int getOperatorsLevel(){
		int temp	=	0;
		Iterator<String> it	=	executors.keySet().iterator();
		while(it.hasNext()){
			temp	=	temp+executors.get(it.next());
		}
		return temp;
	}
}
