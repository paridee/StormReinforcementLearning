package monitors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.BasicConfigurator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mainClasses.MainClass;
import singletons.SystemStatus;

public class NewStormMonitor implements Runnable {
	long latestLatencyRead=0;
	double latestLatencyValueRead=0;
	private String promUrl;
	private String stormUIUrl;
	private int pollingInt;
	private HashMap<String,Double> executeLatencyBolt	=	new HashMap();
	private HashMap<String,Double> executedBolt			=	new HashMap();
	int emitted			=	-1;
	double latency		=	-1.0;
	int window			=	-1;
	int acked			=	-1;
	long rebalanceTime	=	0L;
	long totalEmitted	=	-1;
	int emittedInPollingInterval;
	String idT	=	null;
	public NewStormMonitor(String promUrl,String stormUIUrl,int pollingInt){
		//interval			=	intervalM;
		this.stormUIUrl		=	stormUIUrl;
		this.promUrl		=	promUrl;
		this.pollingInt		=	pollingInt;
		this.latestLatencyRead	=	System.currentTimeMillis();
		this.latestLatencyValueRead	=	singletons.SystemStatus.processLatency;
	}
	
	
	private static final Logger LOG = LoggerFactory.getLogger(NewStormMonitor.class);
	boolean continueEx	=	true;
	@Override
	public void run() {
		// TODO Auto-generated method stub
		//LOG.debug("TH started");
		while(continueEx==true){
			try {
				//String query	=	"rate(node_cpu{job=\""+this.subj.promName+"\",mode=\"idle\",instance=\""+this.subj.promInstance+"\"}["+interval/1000+"s])";
				String query	=	"completeLatency";
				//System.out.println("Query "+query);
		
				String urlString=promUrl+"/api/v1/query?query="+query;
				//LOG.debug("fetching metrics "+urlString);
				//System.out.println(urlString);
				URL oracle = new URL(urlString);
				HttpURLConnection con = (HttpURLConnection) oracle.openConnection();
				con.setRequestMethod("GET");
				int responseCode = con.getResponseCode();
				//LOG.debug("\nSending 'GET' request to URL : " + urlString+" response code "+responseCode);
				BufferedReader in;
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
	
				String inputLine;
				String outl=null;
				while ((inputLine = in.readLine()) != null){
					//LOG.info("Metric query result "+inputLine);
					outl	=	inputLine;
					if(outl!=null){
						//LOG.debug("fetched metrics JSON: "+outl);
						JSONObject jObj		=	new JSONObject(outl);
						jObj				=	jObj.getJSONObject("data");
						JSONArray results	=	jObj.getJSONArray("result");
						for(int i=0;i<results.length();i++){
							JSONObject 	result	=	results.getJSONObject(i);
							JSONArray	value	=	result.getJSONArray("value");
							JSONObject  innerMet=	result.getJSONObject("metric");
							if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
								latency	=	value.getDouble(1);
								
								//emergency latency read
								long now			=	System.currentTimeMillis();
								double latValInMon	=	singletons.SystemStatus.processLatency;
								if(now-this.latestLatencyRead>120000){
									if(this.latestLatencyValueRead==latValInMon){
										//LOG.debug("updated latency in emergency mode");
										singletons.SystemStatus.processLatency	=	latency;
									}
									this.latestLatencyValueRead	=	singletons.SystemStatus.processLatency;
									this.latestLatencyRead		=	now;
								}
								//TODO removed because storm UI averages on latest 10m
								//singletons.SystemStatus.processLatency	=	value.getDouble(1);
								//MainClass.LATENCY_VAL.set(singletons.SystemStatus.processLatency);
								//LOG.debug("set system latency to "+singletons.SystemStatus.processLatency);
								
								
							}
						}
					}
				}
				
				query	=	"capacity";
				//System.out.println("Query "+query);
		
				urlString=promUrl+"/api/v1/query?query="+query;
				//LOG.debug("fetching metrics "+urlString);
				//System.out.println(urlString);
				oracle = new URL(urlString);
				con = (HttpURLConnection) oracle.openConnection();
				con.setRequestMethod("GET");
				responseCode = con.getResponseCode();
				//LOG.debug("\nSending 'GET' request to URL : " + urlString+" response code "+responseCode);
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				outl=null;
				while ((inputLine = in.readLine()) != null){
					//LOG.info("Metric query result "+inputLine);
					outl	=	inputLine;
					if(outl!=null){
						//LOG.debug("fetched metrics JSON: "+outl);
						JSONObject jObj		=	new JSONObject(outl);
						jObj				=	jObj.getJSONObject("data");
						JSONArray results	=	jObj.getJSONArray("result");
						for(int i=0;i<results.length();i++){
							JSONObject 	result	=	results.getJSONObject(i);
							JSONArray	value	=	result.getJSONArray("value");
							JSONObject  innerMet=	result.getJSONObject("metric");
							if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
								//singletons.SystemStatus.processLatency	=	value.getDouble(1);
								//LOG.debug("set system latency to "+singletons.SystemStatus.processLatency);
								if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
									//LOG.debug("capacity metric "+innerMet.getString("operatorName")+" "+value.getDouble(1));
									singletons.SystemStatus.operatorCapacity.put(innerMet.getString("operatorName"), value.getDouble(1));
								}
							}
						}
					}
				}
				
				query	=	"executed";
				//System.out.println("Query "+query);
		
				urlString=promUrl+"/api/v1/query?query="+query;
				//LOG.debug("fetching metrics "+urlString);
				//System.out.println(urlString);
				oracle = new URL(urlString);
				con = (HttpURLConnection) oracle.openConnection();
				con.setRequestMethod("GET");
				responseCode = con.getResponseCode();
				//LOG.debug("\nSending 'GET' request to URL : " + urlString+" response code "+responseCode);
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				outl=null;
				while ((inputLine = in.readLine()) != null){
					//LOG.info("Metric query result "+inputLine);
					outl	=	inputLine;
					if(outl!=null){
						//LOG.debug("fetched metrics JSON: "+outl);
						JSONObject jObj		=	new JSONObject(outl);
						jObj				=	jObj.getJSONObject("data");
						JSONArray results	=	jObj.getJSONArray("result");
						for(int i=0;i<results.length();i++){
							JSONObject 	result	=	results.getJSONObject(i);
							JSONArray	value	=	result.getJSONArray("value");
							JSONObject  innerMet=	result.getJSONObject("metric");
							if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
								//singletons.SystemStatus.processLatency	=	value.getDouble(1);
								//LOG.debug("set system latency to "+singletons.SystemStatus.processLatency);
								if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
									//LOG.debug("executed metric "+innerMet.getString("operatorName")+" "+value.getDouble(1));
									this.executedBolt.put(innerMet.getString("operatorName"), value.getDouble(1));
								}
							}
						}
					}
				}
				query	=	"executeLatency";
				//System.out.println("Query "+query);
		
				urlString=promUrl+"/api/v1/query?query="+query;
				//LOG.debug("fetching metrics "+urlString);
				//System.out.println(urlString);
				oracle = new URL(urlString);
				con = (HttpURLConnection) oracle.openConnection();
				con.setRequestMethod("GET");
				responseCode = con.getResponseCode();
				//LOG.debug("\nSending 'GET' request to URL : " + urlString+" response code "+responseCode);
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				outl=null;
				while ((inputLine = in.readLine()) != null){
					//LOG.info("Metric query result "+inputLine);
					outl	=	inputLine;
					if(outl!=null){
						//LOG.debug("fetched metrics JSON: "+outl);
						JSONObject jObj		=	new JSONObject(outl);
						jObj				=	jObj.getJSONObject("data");
						JSONArray results	=	jObj.getJSONArray("result");
						for(int i=0;i<results.length();i++){
							JSONObject 	result	=	results.getJSONObject(i);
							JSONArray	value	=	result.getJSONArray("value");
							JSONObject  innerMet=	result.getJSONObject("metric");
							//LOG.debug("test "+innerMet.toString());
							if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
								//singletons.SystemStatus.processLatency	=	value.getDouble(1);
								//LOG.debug("set system latency to "+singletons.SystemStatus.processLatency);
								if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
									//LOG.debug("execute latency metric "+innerMet.getString("operatorName")+" "+value.getDouble(1));
									this.executeLatencyBolt.put(innerMet.getString("operatorName"), value.getDouble(1));
								}
							}
						}
					}
				}
				query	=	"capacity";
				//System.out.println("Query "+query);
		
				urlString=promUrl+"/api/v1/query?query="+query;
				//LOG.debug("fetching metrics "+urlString);
				//System.out.println(urlString);
				oracle = new URL(urlString);
				con = (HttpURLConnection) oracle.openConnection();
				con.setRequestMethod("GET");
				responseCode = con.getResponseCode();
				//LOG.debug("\nSending 'GET' request to URL : " + urlString+" response code "+responseCode);
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				outl=null;
				while ((inputLine = in.readLine()) != null){
					//LOG.info("Metric query result "+inputLine);
					outl	=	inputLine;
					if(outl!=null){
						//LOG.debug("fetched metrics JSON: "+outl);
						JSONObject jObj		=	new JSONObject(outl);
						jObj				=	jObj.getJSONObject("data");
						JSONArray results	=	jObj.getJSONArray("result");
						for(int i=0;i<results.length();i++){
							JSONObject 	result	=	results.getJSONObject(i);
							JSONArray	value	=	result.getJSONArray("value");
							JSONObject  innerMet=	result.getJSONObject("metric");
							if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
								//singletons.SystemStatus.processLatency	=	value.getDouble(1);
								//LOG.debug("set system latency to "+singletons.SystemStatus.processLatency);
								if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
									//LOG.debug("capacity metric "+innerMet.getString("operatorName")+" "+value.getDouble(1));
									singletons.SystemStatus.operatorCapacity.put(innerMet.getString("operatorName"), value.getDouble(1));
								}
							}
						}
					}
				}		
				
				query	=	"executors";
				//System.out.println("Query "+query);
		
				urlString=promUrl+"/api/v1/query?query="+query;
				//LOG.debug("fetching metrics "+urlString);
				//System.out.println(urlString);
				oracle = new URL(urlString);
				con = (HttpURLConnection) oracle.openConnection();
				con.setRequestMethod("GET");
				responseCode = con.getResponseCode();
				//LOG.debug("\nSending 'GET' request to URL : " + urlString+" response code "+responseCode);
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				outl=null;
				while ((inputLine = in.readLine()) != null){
					//LOG.info("Metric query result "+inputLine);
					outl	=	inputLine;
					if(outl!=null){
						//LOG.debug("fetched metrics JSON: "+outl);
						JSONObject jObj		=	new JSONObject(outl);
						jObj				=	jObj.getJSONObject("data");
						JSONArray results	=	jObj.getJSONArray("result");
						for(int i=0;i<results.length();i++){
							JSONObject 	result	=	results.getJSONObject(i);
							JSONArray	value	=	result.getJSONArray("value");
							JSONObject  innerMet=	result.getJSONObject("metric");
							if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
								//singletons.SystemStatus.processLatency	=	value.getDouble(1);
								//LOG.debug("set system latency to "+singletons.SystemStatus.processLatency);
								if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
									//LOG.debug("capacity metric "+innerMet.getString("operatorName")+" "+value.getDouble(1));
									singletons.SystemStatus.executors.put(innerMet.getString("operatorName"), value.getInt(1));
									//LOG.debug("operator "+innerMet.getString("operatorName")+" level read "+value.getInt(1));
								}
							}
						}
					}
				}
				
				query	=	"emitted";
				//System.out.println("Query "+query);
		
				urlString=promUrl+"/api/v1/query?query="+query;
				//LOG.debug("fetching metrics "+urlString);
				//System.out.println(urlString);
				oracle = new URL(urlString);
				con = (HttpURLConnection) oracle.openConnection();
				con.setRequestMethod("GET");
				responseCode = con.getResponseCode();
				//LOG.debug("\nSending 'GET' request to URL : " + urlString+" response code "+responseCode);
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				outl=null;
				while ((inputLine = in.readLine()) != null){
					//LOG.info("Metric query result "+inputLine);
					outl	=	inputLine;
					if(outl!=null){
						//LOG.debug("fetched metrics JSON: "+outl);
						JSONObject jObj		=	new JSONObject(outl);
						jObj				=	jObj.getJSONObject("data");
						JSONArray results	=	jObj.getJSONArray("result");
						for(int i=0;i<results.length();i++){
							JSONObject 	result	=	results.getJSONObject(i);
							JSONArray	value	=	result.getJSONArray("value");
							JSONObject  innerMet=	result.getJSONObject("metric");
							if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
								//singletons.SystemStatus.processLatency	=	value.getDouble(1);
								//LOG.debug("set system latency to "+singletons.SystemStatus.processLatency);
								if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
									//LOG.debug("emitted metric "+innerMet.getString("operatorName")+" "+value.getDouble(1));
									int oldEmitted	=	emitted;
									emitted	=	(int)value.getDouble(1);
									if(this.totalEmitted==-1){
										this.totalEmitted	=	0;
										
									}
									if(emitted==0||(emitted<oldEmitted)){
										rebalanceTime	=	System.currentTimeMillis();
									}
									else{
										if(oldEmitted!=-1){
											double increase	=	emitted-oldEmitted;
											//LOG.debug("increasing emitted "+increase);
											if(increase>0){
												MainClass.BENCH_EMITTED.inc(increase);
											}
										}
									}
								}
							}
						}
					}
				}
				
				int seconds	=	this.pollingInt/1000;
				query	=	"delta(bench_emitted[2m])";
				//System.out.println("Query "+query);
				urlString=promUrl+"/api/v1/query?query="+query;
				//LOG.debug("fetching metrics "+urlString);
				//System.out.println(urlString);
				oracle = new URL(urlString);
				con = (HttpURLConnection) oracle.openConnection();
				con.setRequestMethod("GET");
				responseCode = con.getResponseCode();
				//LOG.debug("\nSending 'GET' request to URL : " + urlString+" response code "+responseCode);
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				outl=null;
				while ((inputLine = in.readLine()) != null){
					//LOG.info("Metric query result "+inputLine);
					outl	=	inputLine;
					if(outl!=null){
						//LOG.debug("fetched metrics JSON: "+outl);
						JSONObject jObj		=	new JSONObject(outl);
						jObj				=	jObj.getJSONObject("data");
						JSONArray results	=	jObj.getJSONArray("result");
						for(int i=0;i<results.length();i++){
							JSONObject 	result	=	results.getJSONObject(i);
							//LOG.debug("emint res "+result);
							JSONArray	value	=	result.getJSONArray("value");
							JSONObject  innerMet=	result.getJSONObject("metric");
							//if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
								//singletons.SystemStatus.processLatency	=	value.getDouble(1);
								//LOG.debug("set system latency to "+singletons.SystemStatus.processLatency);
								//if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
									//LOG.debug("capacity metric "+innerMet.getString("operatorName")+" "+value.getDouble(1));
									//singletons.SystemStatus.operatorCapacity.put(innerMet.getString("operatorName"), value.getDouble(1));
									emittedInPollingInterval	=	(int) value.getDouble(1);
									//System.out.println("emitted in polling interval "+emittedInPollingInterval);
								//}
							//}
						}
					}
				}
				
				query	=	"acked";
				//System.out.println("Query "+query);
		
				urlString=promUrl+"/api/v1/query?query="+query;
				//LOG.debug("fetching metrics "+urlString);
				//System.out.println(urlString);
				oracle = new URL(urlString);
				con = (HttpURLConnection) oracle.openConnection();
				con.setRequestMethod("GET");
				responseCode = con.getResponseCode();
				//LOG.debug("\nSending 'GET' request to URL : " + urlString+" response code "+responseCode);
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				outl=null;
				while ((inputLine = in.readLine()) != null){
					//LOG.info("Metric query result "+inputLine);
					outl	=	inputLine;
					if(outl!=null){
						//LOG.debug("fetched metrics JSON: "+outl);
						JSONObject jObj		=	new JSONObject(outl);
						jObj				=	jObj.getJSONObject("data");
						JSONArray results	=	jObj.getJSONArray("result");
						for(int i=0;i<results.length();i++){
							JSONObject 	result	=	results.getJSONObject(i);
							JSONArray	value	=	result.getJSONArray("value");
							JSONObject  innerMet=	result.getJSONObject("metric");
							if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
								//singletons.SystemStatus.processLatency	=	value.getDouble(1);
								//LOG.debug("set system latency to "+singletons.SystemStatus.processLatency);
								if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
									//LOG.debug("emitted metric "+innerMet.getString("operatorName")+" "+value.getDouble(1));
									acked	=	(int)value.getDouble(1);
								}
							}
						}
					}
				}
				
				query	=	"window";
				//System.out.println("Query "+query);
		
				urlString=promUrl+"/api/v1/query?query="+query;
				//LOG.debug("fetching metrics "+urlString);
				//System.out.println(urlString);
				oracle = new URL(urlString);
				con = (HttpURLConnection) oracle.openConnection();
				con.setRequestMethod("GET");
				responseCode = con.getResponseCode();
				//LOG.debug("\nSending 'GET' request to URL : " + urlString+" response code "+responseCode);
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				outl=null;
				while ((inputLine = in.readLine()) != null){
					//LOG.info("Metric query result "+inputLine);
					outl	=	inputLine;
					if(outl!=null){
						//LOG.debug("fetched metrics JSON: "+outl);
						JSONObject jObj		=	new JSONObject(outl);
						jObj				=	jObj.getJSONObject("data");
						JSONArray results	=	jObj.getJSONArray("result");
						for(int i=0;i<results.length();i++){
							JSONObject 	result	=	results.getJSONObject(i);
							JSONArray	value	=	result.getJSONArray("value");
							JSONObject  innerMet=	result.getJSONObject("metric");
							if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
								//singletons.SystemStatus.processLatency	=	value.getDouble(1);
								//LOG.debug("set system latency to "+singletons.SystemStatus.processLatency);
								if((innerMet.getString("name").equals(singletons.Settings.topologyName))){
									//LOG.debug("window metric "+" "+value.getDouble(1));
									window	=	(int)value.getDouble(1)*1000;
								}
							}
						}
					}
				}
				double readInterval	=	System.currentTimeMillis()-rebalanceTime;
				if(rebalanceTime==0){
					readInterval	=	Double.MAX_VALUE;
				}
				double totalServTime=	this.getServiceTime();
				double utilLevel	=	((double)emitted/window)*totalServTime;
				double utilLevel2	=	((double)emittedInPollingInterval/120000)*totalServTime;
				//MainClass.SYST_UTIL.set(utilLevel);
				//LOG.debug("Load levels: "+utilLevel+" fine: "+utilLevel2);
				//this.LOG.debug("Calculated utilization emitted: "+emitted+" interval: "+readInterval+" latency: "+latency+" VALUE: "+utilLevel);
				double temp	=	singletons.SystemStatus.completeUtilization;
				temp	=	(0.9*temp)+(0.1*utilLevel2);
				MainClass.SYST_UTIL_FINE.set(temp);
				singletons.SystemStatus.completeUtilization	=	temp;
				LOG.debug("Load levels: "+utilLevel+" fine: "+utilLevel2+" smoothed "+temp);
				//this.LOG.debug("Calculated Processor Equivalent number "+utilLevel+" emitted "+emitted+" read interval "+readInterval+" total service time "+totalServTime);
				
				
			}
			catch(Exception e){
				e.printStackTrace();
			}
			try {
				Thread.sleep(this.pollingInt);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public double getServiceTime(){
		double st	=	0;
		for(int i=0;i<singletons.SystemStatus.bolts.size();i++){
			String boltName	=	singletons.SystemStatus.bolts.get(i);	
			double rate	=	this.executedBolt.get(boltName);
			rate		=	rate/acked;
			double stb	=	this.executeLatencyBolt.get(boltName);
			st	=	st	+	(rate*stb);
			//LOG.debug("st temp value "+st);
		}
		return st;
	}
	

	public ArrayList<String> getBoltsName() {
		ArrayList<String> names	=	new ArrayList<String>();
		String urlString=this.stormUIUrl+"/api/v1/topology/summary";
		//LOG.debug("fetching metrics "+urlString);
		//System.out.println(urlString);
		URL oracle;
		try {
			oracle = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) oracle.openConnection();
		con.setRequestMethod("GET");
		int responseCode = con.getResponseCode();
		//LOG.debug("\nSending 'GET' request to URL : " + urlString+" response code "+responseCode);
		BufferedReader in;
		in = new BufferedReader(new InputStreamReader(con.getInputStream()));

		String inputLine;
		String outl=null;
		while ((inputLine = in.readLine()) != null){
			//LOG.info("Metric query result "+inputLine);
			outl	=	inputLine;
			if(outl!=null){
				//LOG.debug("fetched metrics JSON: "+outl);
				JSONObject jObj		=	new JSONObject(outl);
				//LOG.debug("has topologies? "+jObj.has("topologies"));
				if(jObj.has("topologies")){
					JSONArray topologies	=	jObj.getJSONArray("topologies");
					for(int i=0;i<topologies.length();i++){
						JSONObject topology	=	topologies.getJSONObject(i);
						if(topology.has("name")){
							String topName	=	topology.getString("name");
							//LOG.debug("TOP NAME "+topName);
							if(topName.equals(singletons.Settings.topologyName)){
								if(topology.has("encodedId")){
									idT	=	topology.getString("encodedId");
								}
							}
						}
					}
				}
			}
		}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(idT!=null){
			urlString=this.stormUIUrl+"/api/v1/topology/"+idT;
			//LOG.debug("fetching metrics "+urlString);
			//System.out.println(urlString);
			try {
				oracle = new URL(urlString);
			HttpURLConnection con = (HttpURLConnection) oracle.openConnection();
			con.setRequestMethod("GET");
			int responseCode = con.getResponseCode();
			//LOG.debug("\nSending 'GET' request to URL : " + urlString+" response code "+responseCode);
			BufferedReader in;
			in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			String outl=null;
			while ((inputLine = in.readLine()) != null){
				outl	=	inputLine;
				if(outl!=null){
					//LOG.debug("fetched metrics JSON: "+outl);
					JSONObject jObj		=	new JSONObject(outl);
					//LOG.debug("has topologies? "+jObj.has("topologies"));
					if(jObj.has("bolts")){
						JSONArray bolts	=	jObj.getJSONArray("bolts");
						for(int i=0;i<bolts.length();i++){
							JSONObject bolt	=	bolts.getJSONObject(i);
							if(bolt.has("encodedBoltId")){
								String boltName	=	bolt.getString("encodedBoltId");
								names.add(boltName);
								//LOG.debug("BOLT NAME "+boltName);
							}
						}
					}
				}
			}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return names;
	}
	
	public static void main(String[] args) {
		BasicConfigurator.configure();			//default logging configuration
		MainClass.launchWebServerForPrometheus();
		ArrayList<String> boltsName	=	new ArrayList<String>();
		boltsName.add("firststage");
		boltsName.add("secondstage");
		boltsName.add("thirdstage");
		singletons.SystemStatus.bolts	=	boltsName;
		NewStormMonitor mon	=	new NewStormMonitor("http://160.80.97.147:9090","http://160.80.97.147:8090",15000);
		mon.getBoltsName();
		Thread aTh	=	new Thread(mon);
		//LOG.debug("starting test TH");
		aTh.start();
	}
}
