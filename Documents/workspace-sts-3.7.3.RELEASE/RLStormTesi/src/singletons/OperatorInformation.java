package singletons;

import java.util.Comparator;

public class OperatorInformation implements Comparable<OperatorInformation> {
	public String 	operatorName;
	public Double	congestionLevel	=	-1.0;
	public int 		level	=	-1;
	
	public static Comparator<OperatorInformation> ascendent	=	new Comparator<OperatorInformation>(){
        @Override
        public int compare(OperatorInformation op1, OperatorInformation op2)
        {

            return  op1.compareTo(op2);
        }
	};
	
	public static Comparator<OperatorInformation> discendent	=	new Comparator<OperatorInformation>(){
        @Override
        public int compare(OperatorInformation op1, OperatorInformation op2)
        {

            return  -op1.compareTo(op2);
        }
	};
	
	@Override
	public int compareTo(OperatorInformation o) {
		OperatorInformation operator	=	(OperatorInformation) o;
		if(operator.congestionLevel==this.congestionLevel){
			return 0;
		}
		else if(operator.congestionLevel>this.congestionLevel){
			return -1;
		}
		else{
			return 1;
		}
	}
	
}
