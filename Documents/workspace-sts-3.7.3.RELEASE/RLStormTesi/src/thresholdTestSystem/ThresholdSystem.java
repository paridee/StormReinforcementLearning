package thresholdTestSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import io.prometheus.client.exporter.MetricsServlet;
import mainClasses.MainClass;
import mainClasses.NoLogging;
import monitors.NewStormMonitor;
import monitors.StormMonitor;
import singletons.PropertiesReader;
import singletons.Settings;
import singletons.SystemStatus;

public class ThresholdSystem implements Runnable {
	
	public static String	PROMETHEUS_URL		=	Settings.PROMETHEUS_URL;	//prometheus server url
	public static String	PROMETHEUS_PUSHG	=	Settings.PROMETHEUS_PUSHG;	//prometheus push gateway
	public static String	STORMUI_URL			=	Settings.STORMUI_URL;	//storm UI url
	public double min;
	public double max;
	public ConfigurationExecutor ex				=	new ConfigurationExecutor();
	
	public ThresholdSystem(double minUtil,double maxUtil){
		this.min	=	minUtil;
		this.max	=	maxUtil;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		ConfigurationExecutor	executor	=	new ConfigurationExecutor();
		StormMonitor 			sm			=	new StormMonitor(PROMETHEUS_URL,PROMETHEUS_PUSHG);
		Thread					sm_th		=	new Thread(sm);
		sm_th.start();
		NewStormMonitor 		rm			=	new NewStormMonitor(PROMETHEUS_URL,STORMUI_URL,10000);
		Thread					rm_th		=	new Thread(rm);
		rm_th.start();
		ArrayList<String> 		boltsName	=	new ArrayList<String>();
		while(boltsName.size()==0){
			boltsName	=	rm.getBoltsName();	
		}
		SystemStatus.bolts	= boltsName;
		launchWebServerForPrometheus();
		System.out.println("Threshold System Ready!\nBolts name:");
		HashMap<String,Integer> decisionMap	=	new HashMap<String,Integer>();
		for(int i=0;i<boltsName.size();i++){
			String boltName	=	boltsName.get(i);
			System.out.println(boltName+" replicas "+SystemStatus.executors.get(boltName)+" capacity "+SystemStatus.operatorCapacity.get(boltName));
			decisionMap.put(boltName, SystemStatus.executors.get(boltName));
		}
		System.out.println("Latenza E2E "+SystemStatus.processLatency);
		int sleepTime	=	Settings.decisionInterval;
		while(true){
			boolean continueEx			=	true;
			Iterator<String> names		=	decisionMap.keySet().iterator();
			String	leastLoadedName		=	"";
			double	leastLoadedCapacity	=	1;
			String  bottleneckName		=	"";
			double  bottleneckCapacity	=	0;
			while(names.hasNext()){
				String itName	=	names.next();
				try {
					//System.out.println("operator "+itName+" check");
					boolean isBottleneck	=	SystemStatus.isBottleneck(itName);
					int repLevel			=	decisionMap.get(itName);
					double capLevel			=	SystemStatus.operatorCapacity.get(itName);
					if(isBottleneck==true){
						System.out.println("Bottleneck operator "+itName+" replication "+repLevel+" utilization "+capLevel);
						bottleneckName		=	itName;
						bottleneckCapacity	=	capLevel;
					}
					if(repLevel>1){
						if(capLevel<leastLoadedCapacity){
							leastLoadedName		=	itName;
							leastLoadedCapacity	=	capLevel;
							System.out.println("new least loaded "+leastLoadedName+" with load "+leastLoadedCapacity);
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			try {
				if(bottleneckCapacity>max){
					System.out.println("trigger collo bottiglia "+bottleneckName+" da "+decisionMap.get(bottleneckName)+" repliche a ");
					int repLevel			=	decisionMap.get(bottleneckName);
					if(repLevel<Settings.maxParallelism){
						MainClass.STATE_READ.set(2);
						decisionMap.put(bottleneckName, decisionMap.get(bottleneckName)+1);
						SystemStatus.executors.put(bottleneckName, decisionMap.get(bottleneckName));
						System.out.println(" a "+decisionMap.get(bottleneckName));
						ex.executeConfiguration(decisionMap, Settings.topologyName);
					}
					else{
						System.out.println("max replication for bottleneck... PROBLEM!");
					}
				}
				else{
					if(leastLoadedCapacity<min){
						MainClass.STATE_READ.set(0);
						System.out.println("trigger meno carico "+leastLoadedName+" da "+decisionMap.get(leastLoadedName)+" repliche a ");
						decisionMap.put(leastLoadedName, decisionMap.get(leastLoadedName)-1);
						SystemStatus.executors.put(leastLoadedName, decisionMap.get(leastLoadedName));
						System.out.println(" a "+decisionMap.get(leastLoadedName));
						ex.executeConfiguration(decisionMap, Settings.topologyName);
					}
				}
				else{
					MainClass.STATE_READ.set(0);
				}
				MainClass.LATENCY_VAL.set(singletons.SystemStatus.processLatency);
				
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void launchWebServerForPrometheus(){
		  org.eclipse.jetty.util.log.Log.setLog(new NoLogging());
		  Server server = new Server(1234);
		  ServletContextHandler context = new ServletContextHandler();
		  context.setContextPath("/");
		  server.setHandler(context);
		  context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
		  try {
			  server.start();
		  } catch (Exception e1) {
			  e1.printStackTrace();
		  }
	}
}
