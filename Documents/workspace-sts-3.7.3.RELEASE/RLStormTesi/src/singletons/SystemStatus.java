package singletons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class SystemStatus {
	public static double 	processLatency	=	-1;
	public static int	 	workerNumber	=	-1;
	public static HashMap<String, Integer>	executors		=	new HashMap<String,Integer>();
	public static HashMap<String, Double>	operatorCapacity=	new HashMap<String,Double>();
	public static ArrayList<String> bolts	=	new ArrayList<String>();
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
			if(bolts.contains(name)&&executors.containsKey(name)){
				temp	=	temp+executors.get(name);
			}
		}
		return temp;
	}
	
	public static  ArrayList<OperatorInformation> getOperatorInformationList(){
		 ArrayList<OperatorInformation> opList	=	new ArrayList<OperatorInformation>();
		for(int i=0;i<bolts.size();i++){
			OperatorInformation opInfo	=	new OperatorInformation();
			String 	boltName	=	bolts.get(i);
			opInfo.operatorName	=	boltName;
			if(executors.containsKey(boltName)){
				opInfo.level	=	executors.get(boltName);
			}
			if(operatorCapacity.containsKey(boltName)){
				opInfo.congestionLevel	=	operatorCapacity.get(boltName);
			}
			opList.add(opInfo);
		}
		return opList;
	}
	
	public static ArrayList<OperatorInformation> removeOperatorAtLevel(ArrayList<OperatorInformation> info,int level){
		ArrayList<OperatorInformation> newList	=	new ArrayList<OperatorInformation>();
		for(int i=0;i<info.size();i++){
			if(info.get(i).level!= level){
				newList.add(info.get(i));
			}
		}
		return newList;
	}
}
