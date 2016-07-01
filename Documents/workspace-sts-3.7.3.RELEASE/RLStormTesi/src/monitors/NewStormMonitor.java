package monitors;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewStormMonitor implements Runnable {
	
	private String promUrl;
	public NewStormMonitor(String promUrl){
		//interval			=	intervalM;
		this.promUrl		=	promUrl;
	}
	
	
	private static final Logger LOG = LoggerFactory.getLogger(NewStormMonitor.class);
	boolean continueEx	=	true;
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(continueEx==true){
			try {
				//String query	=	"rate(node_cpu{job=\""+this.subj.promName+"\",mode=\"idle\",instance=\""+this.subj.promInstance+"\"}["+interval/1000+"s])";
				String query	=	"processLatency";
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
							LOG.debug(innerMet.getString("name")+" "+innerMet.getString("operatorName")+" "+value.getDouble(1));
						}
					}
				}
			}
			catch(Exception e){
				
			}
		}
	}

	public static void main(String[] args) {
		
	}
}
