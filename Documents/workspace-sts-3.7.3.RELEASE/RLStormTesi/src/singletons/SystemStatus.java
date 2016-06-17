package singletons;

import java.util.HashMap;

public class SystemStatus {
	public static double 	processLatency	=	-1;
	public static double 	workerNumber	=	-1;
	public static HashMap	executors		=	new HashMap<String,Integer>();
}
