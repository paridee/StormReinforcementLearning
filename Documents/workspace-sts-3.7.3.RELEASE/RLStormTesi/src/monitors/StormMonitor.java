package monitors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mainClasses.MainClass;
import singletons.SystemStatus;

public class StormMonitor implements Runnable{
	private static final Logger LOG = LoggerFactory.getLogger(StormMonitor.class);
	boolean continueEx	=	true;
	//int interval		=	10000;
	String promUrl		=	"";
	private String pushGatUrl;
	HashMap<String,Object>  latest	=	new HashMap<String,Object>();
	HashMap<String,Integer> count	=	new HashMap<String,Integer>();
	double topologyLatency	=	0;
	
	public StormMonitor(String promUrl,String pushGatUrl){
		//interval			=	intervalM;
		this.promUrl		=	promUrl;
		this.pushGatUrl		=	pushGatUrl;
	}
	
	//double getTopologyLatency(){
	//	return this.topologyLatency;
	//}
	
	
	
	@Override
	public void run() {
		while(continueEx == true){
			try {
				//String query	=	"rate(node_cpu{job=\""+this.subj.promName+"\",mode=\"idle\",instance=\""+this.subj.promInstance+"\"}["+interval/1000+"s])";
				String query	=	"_storm___complete_latency_default";
				//System.out.println("Query "+query);
				String urlString=promUrl+"/api/v1/query?query="+query;
				//System.out.println(urlString);
				URL oracle = new URL(urlString);
				BufferedReader in;
				in = new BufferedReader(new InputStreamReader(oracle.openStream()));
				String inputLine;
				String outl=null;
				while ((inputLine = in.readLine()) != null){
					//LOG.info("Metric query result "+inputLine);
					outl	=	inputLine;
					if(outl!=null){
						JSONObject jObj		=	new JSONObject(outl);
						jObj				=	jObj.getJSONObject("data");
						JSONArray results	=	jObj.getJSONArray("result");
						for(int i=0;i<results.length();i++){
							JSONObject 	result	=	results.getJSONObject(i);
							JSONArray	value	=	result.getJSONArray("value");
							JSONObject  innerMet=	result.getJSONObject("metric");
							//LOG.info("STORM metric "+innerMet.toString()+" value "+value);
							boolean discard	=	false;
							if(innerMet.has("exported_instance")&&innerMet.has("job")){
								if(innerMet.has("__name__")){
									String metricName	=	innerMet.getString("__name__");
									double processTimeValue	=	value.getDouble(1);
									String myKey			=	metricName+""+innerMet.getString("exported_instance");
									if(this.latest.containsKey(myKey)){										
										if(this.latest.get(myKey).equals(processTimeValue)){
											discard		=	true;
											int myCount	=	1;
											if(this.count.containsKey(myKey)){
												myCount	=	this.count.get(myKey)+1;
											}
											if(myCount%12==0){
												//LOG.info("Duplicate value found after 60 seconds, going to delete "+this.latest.get(metricName+""+innerMet.getString("exported_instance"))+" "+processTimeValue);
												
												URL url = null;
												try {
												    url = new URL(pushGatUrl+"/metrics/job/"+innerMet.getString("exported_job")+"/instance/"+innerMet.getString("exported_instance"));
												    LOG.info("Going to DELETE @ "+pushGatUrl+"/metrics/job/"+innerMet.getString("exported_job")+"/instance/"+innerMet.getString("exported_instance"));
												} catch (MalformedURLException exception) {
												    exception.printStackTrace();
												}
												HttpURLConnection httpURLConnection = null;
												try {
												    httpURLConnection = (HttpURLConnection) url.openConnection();
												    httpURLConnection.setRequestProperty("Content-Type",
												                "application/x-www-form-urlencoded");
												    httpURLConnection.setRequestMethod("DELETE");
												    //LOG.info("Response code "+httpURLConnection.getResponseCode());
												} catch (IOException exception) {
												    exception.printStackTrace();
												} finally {         
												    if (httpURLConnection != null) {
												        httpURLConnection.disconnect();
												    }
												}
											}
											this.count.put(myKey, myCount);
										}
									}
									if(discard==false&&processTimeValue>0){
										latest.put(metricName+""+innerMet.getString("exported_instance"), processTimeValue);
										this.topologyLatency		=	processTimeValue;
										
										//TODO check again (storm UI updates each 10m!!!)
										SystemStatus.processLatency	=	processTimeValue;
										//LOG.info("Updated latency to "+topologyLatency);
									}
								}
							}
						}
					}
				}
				
				query	=	"_storm___fail_count_default";
				//System.out.println("Query "+query);
				urlString=promUrl+"/api/v1/query?query="+query;
				//System.out.println(urlString);
				oracle = new URL(urlString);
				in = new BufferedReader(new InputStreamReader(oracle.openStream()));
				outl=null;
				long triggerMillis	=	0;
				while ((inputLine = in.readLine()) != null){
					//LOG.info("Metric query result "+inputLine);
					outl	=	inputLine;
					if(outl!=null){
						JSONObject jObj		=	new JSONObject(outl);
						jObj				=	jObj.getJSONObject("data");
						JSONArray results	=	jObj.getJSONArray("result");
						for(int i=0;i<results.length();i++){
							JSONObject 	result	=	results.getJSONObject(i);
							JSONArray	value	=	result.getJSONArray("value");
							JSONObject  innerMet=	result.getJSONObject("metric");
							//LOG.info("STORM metric "+innerMet.toString()+" value "+value);
							boolean discard	=	false;
							if(innerMet.has("exported_instance")&&innerMet.has("job")){
								if(innerMet.has("__name__")){
									String metricName	=	innerMet.getString("__name__");
									double failed		=	value.getDouble(1);
									String myKey			=	metricName+""+innerMet.getString("exported_instance");
									if(this.latest.containsKey(myKey)){										
										if(this.latest.get(myKey).equals(failed)){
											discard	=	true;
											int myCount	=	1;
											if(this.count.containsKey(myKey)){
												myCount	=	this.count.get(myKey)+1;
											}
											if(myCount%12==0){
												//LOG.info("Duplicate value found after 60 seconds, going to delete "+this.latest.get(metricName+""+innerMet.getString("exported_instance"))+" "+processTimeValue);
												
												URL url = null;
												try {
												    url = new URL(pushGatUrl+"/metrics/job/"+innerMet.getString("exported_job")+"/instance/"+innerMet.getString("exported_instance"));
												    LOG.info("Going to DELETE @ "+pushGatUrl+"/metrics/job/"+innerMet.getString("exported_job")+"/instance/"+innerMet.getString("exported_instance"));
												} catch (MalformedURLException exception) {
												    exception.printStackTrace();
												}
												HttpURLConnection httpURLConnection = null;
												try {
												    httpURLConnection = (HttpURLConnection) url.openConnection();
												    httpURLConnection.setRequestProperty("Content-Type",
												                "application/x-www-form-urlencoded");
												    httpURLConnection.setRequestMethod("DELETE");
												    //LOG.info("Response code "+httpURLConnection.getResponseCode());
												} catch (IOException exception) {
												    exception.printStackTrace();
												} finally {         
												    if (httpURLConnection != null) {
												        httpURLConnection.disconnect();
												    }
												}
											}
											this.count.put(myKey, myCount);
										}
									}
									if(discard==false&&failed>0){
										latest.put(metricName+""+innerMet.getString("exported_instance"), failed);
										triggerMillis		=	System.currentTimeMillis();
										SystemStatus.losingTuples	=	true;
										LOG.info("triggering on losing tuples");
									}
								}
							}
						}
					}
				}
				if(System.currentTimeMillis()-triggerMillis>60000){
					if(SystemStatus.losingTuples==true){
						SystemStatus.losingTuples	=	false;
						LOG.info("triggering off losing tuples");
					}
				}
				Thread.sleep(10000);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}

}
