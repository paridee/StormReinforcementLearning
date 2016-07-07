package state;

import java.util.ArrayList;

public class StateTranslator {
	private ArrayList<String> operators;
	private int maxParallel;
	private int loadLevels;
	private ArrayList<Object> translation=new ArrayList<Object>();
	public StateTranslator(ArrayList<String> operators, int maxParallel, int loadLevels) {
		super();
		this.operators 		= operators;
		this.maxParallel 	= maxParallel;
		this.loadLevels 	= loadLevels;
	}
	
	int getState(int load,ArrayList<Integer> conf,ArrayList<Double>opCapacity) throws Exception{
		if((opCapacity.size()!=operators.size())||(conf.size()!=operators.size())){
			throw new Exception("Wrong parameters for getting state");
		}
		String retString	=	"";
		for(int i=0;i<operators.size();i++){
			double temp	=	opCapacity.get(i);
			if(temp>1){
				temp	=	0.99999999999999;
			}
			retString		=	retString+obtainString((int)(temp*10),9);
		}
		for(int i=0;i<operators.size();i++){
			retString		=	obtainString(conf.get(i),operators.size())+retString;
		}
		retString			=	obtainString(load+1,loadLevels)+retString;
		return Integer.parseInt(retString);
	}
	
	private static String obtainString(int value,int maxValue){
		String maxValueString	=	maxValue+"";
		String valueString		=	value+"";
		while(valueString.length()<maxValueString.length()){
			valueString	=	0+valueString;
		}
		return valueString;
	}
	
	public static void main(String[] args) {
		System.out.println(obtainString(3,1000));
	}
}
