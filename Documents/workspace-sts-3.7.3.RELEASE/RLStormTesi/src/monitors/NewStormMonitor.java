package monitors;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.BasicConfigurator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mainClasses.MainClass;

public class NewStormMonitor implements Runnable {
	
	private String promUrl;
	private int pollingInt;
	public NewStormMonitor(String promUrl,int pollingInt){
		//interval			=	intervalM;
		this.promUrl		=	promUrl;
		this.pollingInt		=	pollingInt;
	}
	
	
	private static final Logger LOG = LoggerFactory.getLogger(NewStormMonitor.class);
	boolean continueEx	=	true;
	@Override
	public void run() {
		// TODO Auto-generated method stub
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
								singletons.SystemStatus.processLatency	=	value.getDouble(1);
								MainClass.LATENCY_VAL.set(singletons.SystemStatus.processLatency);
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

	public static void main(String[] args) {
		BasicConfigurator.configure();			//default logging configuration
		NewStormMonitor mon	=	new NewStormMonitor("http://160.80.97.147:9090",15000);
		Thread aTh	=	new Thread(mon);
		aTh.start();
	}
}
