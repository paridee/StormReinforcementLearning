package expectedSarsa.storm;
import java.util.ArrayList;
import java.util.HashMap;

import redis.clients.jedis.Jedis;

public class StateTranslator {
	private class EntryManager{
		int batchSize;
		String[] entries;
		int pos	=	0;
		int database	=	-1;
		Jedis jedis;
		EntryManager(int batchSize,Jedis jedis,int database){
			super();
			this.jedis		=	jedis;
			this.batchSize	=	batchSize;
			this.database	=	database;
			entries			=	new String[batchSize*2];
		}
		void insertValue(String key,String value){
			entries[2*pos]		=	key;
			entries[(2*pos)+1]	=	value;
			pos++;
			if(pos==batchSize){
				//System.out.println(entries+" "+jedis);
				jedis.select(database);
				jedis.mset(entries);
				pos	=	0;
				entries	=	new String[batchSize*2];
			}
		}
		void flush(){
			jedis.select(database);
			String[] temp	=	new String[pos*2];
			for(int i=0;i<pos*2;i++){
				temp[i]	=	entries[i];
				if(i%2==0){
					//System.out.println("key "+temp[i]);
				}
				else{
					//System.out.println("value "+temp[i]);
				}
				
				if(entries[i]==null){
					System.out.println("passando valore null a indice "+i);
				}
			}
			if(pos>0){
				jedis.mset(temp);
			}
			pos	=	0;
			entries	=	new String[batchSize*2];
		}
	}
	
	int operators;
	int stateLevels;
	int maxParallelism;
	int currentPos	=	0;
	Jedis jedis;
	EntryManager jedisManager;
	EntryManager jedisManagerRev;
	
	//HashMap<String,Integer> 	dict	=	new HashMap<String,Integer>();
	//HashMap<Integer,Integer[]> revDict	=	new HashMap<Integer,Integer[]>();
	int stateN	=	0;
	
	
	public StateTranslator(int operators, int stateLevels, int maxParallelism,Jedis redis) {
	super();
	this.jedis		=	redis;
	jedisManager	=	new EntryManager(50000,jedis,0);
	jedisManagerRev	=	new EntryManager(50000,jedis,1);
	this.operators = operators;
	this.stateLevels = stateLevels;
	this.maxParallelism = maxParallelism;
	/*
	for(int i=0;i<stateLevels;i++){
		Integer[] story	=	new Integer[1];
		story[0]	=	i;
		elementsRecursive(story,operators,maxParallelism,false);
	}
	jedisManager.flush();
	jedisManagerRev.flush();*/
}
	public int[] getValuesFromState(String state){
		int[] result	=	new int[2+(2*operators)];
		String stateN	=	state.substring(0, (this.stateLevels+"").length());
		//System.out.println("Parsing "+stateN);
		result[0]		=	Integer.parseInt(stateN);
		String rest		=	state.substring((this.stateLevels+"").length());
		String temp		=	rest.substring(0,(this.maxParallelism+"").length());
		//System.out.println("Parsing load "+temp);
		result[1]		=	Integer.parseInt(temp);
		if(result[1]>this.maxParallelism){
			result[1]	=	this.maxParallelism;
		}
		rest	=	rest.substring((this.maxParallelism+"").length());
		for(int i=0;i<operators;i++){
			temp	=	rest.substring(0,(this.maxParallelism+"").length());
			//System.out.println("Parsing "+temp);
			result[i+2]	=	Integer.parseInt(temp);
			rest	=	rest.substring((this.maxParallelism+"").length());
		}
		for(int i=0;i<operators;i++){
			temp	=	rest.substring(0,(10+"").length());
			//System.out.println("Parsing "+temp);
			result[i+operators+2]	=	Integer.parseInt(temp);
			rest	=	rest.substring((10+"").length());
		}
		return result;
	}
	
	public String getStringForInt(int maxValue,int value){
		String mV	=	maxValue+"";
		String valueS=	value+"";
		while(mV.length()>valueS.length()){
			valueS	=	0+valueS;
		}
		if(valueS.length()>mV.length()){
			return "error";
		}
		return valueS;
	}
	
	public String getStringForState(Integer[] stateArray){
		String stateString	=	getStringForInt(stateLevels,stateArray[0]);
		stateString			=	stateString+getStringForInt(maxParallelism,stateArray[1]);
		for(int i=0;i<operators;i++){
			stateString	=	stateString+getStringForInt(maxParallelism,stateArray[i+2]);
		}
		for(int i=0;i<operators;i++){
			stateString	=	stateString+getStringForInt(10,stateArray[operators+i+2]);
		}
		return stateString;
	}
	
	public void elementsRecursive(Integer prev[],int level,int maxValue,boolean stop){
		//System.out.println(level);
		for(int i=0;i<maxValue;i++){
			Integer[] story;
			if(prev!=null){
				story	=	new Integer[prev.length+1];
			}
			else{
				story	=	new Integer[1];
			}
			for(int j=0;j<prev.length;j++){
				story[j]	=	prev[j];
			}
			story[prev.length]=i;
			if(level>1){
				elementsRecursive(story,level-1,maxValue,stop);
			}
			else if(level==1&&stop==false){
				elementsRecursive(story,operators,10,true);
			}
			else if(level==1&&stop==true){
				String state	=	getStringForState(story);
				//jedisRew.set(stateN+"", state);
				//jedis.set(state, stateN+"");
				//System.out.println(state+" "+stateN);
				jedisManager.insertValue(state, stateN+"");
				jedisManagerRev.insertValue(stateN+"", state);
				stateN++;
				if(stateN%100000==0){
					System.out.println("+100000 "+stateN);
				}
			}
		}
	}
	
	public int getIntForState(Integer[] state){
		System.out.println("TEST");
		for(int i=0;i<state.length;i++){
			System.out.print(state[i]+" ");
		}
		String key	=	this.getStringForState(state);
		jedis.select(0);
		String inDb	=	jedis.get(key);
		if(inDb==null){
			int position	=	currentPos++;
			jedis.set(key,position+"");
			jedis.select(1);
			jedis.set(position+"", key);
			return position;
		}
		return Integer.parseInt(inDb);
	}
/*
	public Integer[] getFeat(int state){
		return revDict.get(state);
	}
	*/
	public static void main(String[] args) {
	      Jedis jedis = new Jedis("127.0.0.1",6379);
	      jedis.flushAll();
	      jedis.select(0);
	      Jedis jedis2 = new Jedis("127.0.0.1",6379);
	      jedis2.select(1);
		
		// TODO Auto-generated method stub
		System.out.println("test");

	      System.out.println("Connection to server sucessfully");
	      //check whether server is running or not
	      System.out.println("Server is running: "+jedis.ping());	
	      System.out.println("vado...");
		StateTranslator tr	=	new StateTranslator(2,2,4,jedis);
		System.out.println(tr.stateN);
		int testStep	=	tr.stateN/100;
		for(int i=0;i<100;i++){
			String str	=	jedis2.get((i*testStep)+"");
			jedis.select(0);
			String str2	=	jedis.get(str);
			int[] arrayFeat	=	tr.getValuesFromState(str);
			String x		=	"";
			for(int j=0;j<arrayFeat.length;j++){
				x	=	x+arrayFeat[j]+" ";
			}
			System.out.println("test "+i+" valore "+str+" "+str2+" "+x);
		}
		
		String test		=	"09";
		int testparse	=	Integer.parseInt(test);
		System.out.println("test parsing "+testparse);
	}
	public String getStringFromId(int key) {
		jedis.select(1);
		return jedis.get(key+"");
	}
}
