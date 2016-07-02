package singletons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class SystemStatus {
	public static double 	processLatency	=	-1;
	public static int	 	workerNumber	=	-1;
	public static HashMap<String, Integer>	executors		=	new HashMap<String,Integer>();
	public static HashMap<String, Double>	operatorCapacity=	new HashMap<String,Double>();
	public static ArrayList<String> bolts;
	public static void setExecutorLevel(String exName,int value){
		if(bolts.contains(exName)){
			executors.put(exName, value);	
		}
	}
	
	public static int getOperatorsLevel(){
		int temp	=	0;
		Iterator<String> it	=	executors.keySet().iterator();
		while(it.hasNext()){
			String name	=	it.next();
			if(bolts.contains(name)){
				temp	=	temp+executors.get(name);
			}
		}
		return temp;
	}
}
