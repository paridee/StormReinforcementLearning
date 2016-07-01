package mainClasses;

import java.util.ArrayList;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import expectedSarsa.ExpectedSarsa;
import expectedSarsa.FixedIntervalManager;
import expectedSarsa.storm.ProcessTimeStateReader;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.MetricsServlet;
import monitors.NewStormMonitor;
import monitors.StormMonitor;
import rl.alpha.StaticAlphaCalculator;
import rl.executer.ExecutorsChange;
import rl.executer.WorkerNumberExecutor;
import rl.policies.EpsilonGreedyChooser;
import rl.rewarder.DeltaRewarder;
import rl.rewarder.DeltaRewarderSimplified;
import rl.rewarder.ParabolicComplexResponseTimeRewarder;
import rl.rewarder.ParabolicProcessTimeRewardCalculator;
import rl.rewarder.RewardCalculator;
import singletons.Settings;

public class MainClass {
	public static int 	 		monitoringInterval	=	Settings.decisionInterval;							//storm monitoring interval (>=60000)
	public static final String	PROMETHEUS_URL		=	"http://160.80.97.147:9090";	//prometheus server url
	public static final String	PROMETHEUS_PUSHG	=	"http://160.80.97.147:9091";	//prometheus push gateway
	public static final	Gauge	REWARD_VAL			=	Gauge.build().name("bench_rewardVal").help("Reward received value").register();	//prometheus metric to be monitored on Graphana
	public static final	Gauge	LATENCY_VAL			=	Gauge.build().name("bench_latencyRead").help("Latency read by decisor").register();	//prometheus metric to be monitored on Graphana
	public static final	Gauge	PARALLELISM_VAL		=	Gauge.build().name("bench_parallelism").help("Parallelism level decided").register();	//prometheus metric to be monitored on Graphana
	public static final int 	STATES_NUM			=	3;		//states
	public static int			ACTIONS_NUM			=	4;		//actions
	public static Gauge.Child[][]	qMatrix;				//prometheus variables
	public static Gauge.Child[]		operatorsLevel;			//prometheus variables

	public static void main(String[] args) {	//arguments (opt): topology name
		BasicConfigurator.configure();			//default logging configuration
		if (args != null && args.length > 0) {
			Settings.topologyName	=	args[0];
		}
		NewStormMonitor 	rm		=	new NewStormMonitor(PROMETHEUS_URL,20000);
		Thread			rm_th	=	new Thread(rm);
		rm_th.start();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//TEST
		ArrayList<String>						boltsName	=	new ArrayList<String>();
		boltsName.add("firststage");
		boltsName.add("secondstage");

		
		//RewardCalculator					 	rewarder	=	new ParabolicComplexResponseTimeRewarder(3000,125,4500,ACTIONS_NUM);
		//RewardCalculator					 	rewarder	=	new DeltaRewarder(3000,4500,15,0.2);
		RewardCalculator					 	rewarder	=	new DeltaRewarderSimplified(300,3000);
		ProcessTimeStateReader					reader		=	new ProcessTimeStateReader(3000,0.5,1.5);
		FixedIntervalManager					intManager	=	new FixedIntervalManager(Settings.decisionInterval);
		//WorkerNumberExecutor					executor	=	new WorkerNumberExecutor(rewarder,intManager);
		EpsilonGreedyChooser					chooser		=	new EpsilonGreedyChooser(0.1);
		StaticAlphaCalculator					alpha		=	new StaticAlphaCalculator(0.8);
		//Thread sarsaThread									=	new Thread(sarsa);
		//sarsaThread.start();
		
		
		int 									actionsN	=	(boltsName.size()*2)+1;	
		ACTIONS_NUM											=	actionsN;
		initializePromVariables(boltsName);				//initializes variables for prometheus
		 
		int[] steps	=	new int[3];
		steps[0]	=	2;
		steps[1]	=	1;
		steps[2]	=	4;
		ExecutorsChange							executor	=	new ExecutorsChange(boltsName,steps, 32, singletons.Settings.topologyName,intManager,rewarder);
		ExpectedSarsa							sarsa		=	new	ExpectedSarsa(3,actionsN,1,chooser,executor,reader,alpha);
		Thread									sarsaTh		=	new Thread(sarsa);
		
		
		launchWebServerForPrometheus();			//launches a web server for prometheus monitoring
		
		sarsaTh.start();
		
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
	
	public static void initializePromVariables(ArrayList<String> boltsName){
		qMatrix			=	new Gauge.Child[STATES_NUM][ACTIONS_NUM];
		operatorsLevel	=	new Gauge.Child[boltsName.size()];
		String[] labels	=	new String[2];
		labels[0]		=	"row";
		labels[1]		=	"column";
		System.out.println("Q matrix dimension "+STATES_NUM+" "+ACTIONS_NUM);
		Gauge q			=	Gauge.build().name("QValue").help("Value of Q matrix in position").labelNames(labels).register();
		for(int i=0;i<STATES_NUM;i++){// metrics, 3 == number of states
			for(int j=0;j<ACTIONS_NUM;j++){
				String[] labelst	=	new String[2];
				labelst[0]		=	i+"";
				labelst[1]		=	j+"";
				qMatrix[i][j]	=	new Gauge.Child();
				q.setChild(qMatrix[i][j], labelst);
			}
		}
		labels	=	new String[1];
		labels[0]		=	"row";
		Gauge l			=	Gauge.build().name("OperatorsLevel").help("Operators").labelNames(labels).register();
		for(int i=0;i<boltsName.size();i++){
			operatorsLevel[i]	=	new Gauge.Child();
			String labelst		=	boltsName.get(i);
			l.setChild(operatorsLevel[i],labelst);
		}
	}
}
