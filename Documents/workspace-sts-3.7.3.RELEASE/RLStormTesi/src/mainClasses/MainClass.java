package mainClasses;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import expectedSarsa.EpsilonGreedyChooser;
import expectedSarsa.ExpectedSarsa;
import expectedSarsa.FixedIntervalManager;
import expectedSarsa.StaticAlphaCalculator;
import expectedSarsa.storm.ParabolicProcessTimeRewardCalculator;
import expectedSarsa.storm.ProcessTimeStateReader;
import expectedSarsa.storm.WorkerNumberExecutor;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.MetricsServlet;
import monitors.StormMonitor;
import singletons.Settings;

public class MainClass {
	public static int 	 		monitoringInterval	=	Settings.decisionInterval;							//storm monitoring interval (>=60000)
	public static final String	PROMETHEUS_URL		=	"http://160.80.97.147:9090";	//prometheus server url
	public static final String	PROMETHEUS_PUSHG	=	"http://160.80.97.147:9091";	//prometheus push gateway
	public static final	Gauge	REWARD_VAL			=	Gauge.build().name("bench_rewardVal").help("Reward received value").register();	//prometheus metric to be monitored on Graphana
	public static final int 	STATES_NUM			=	3;		//states
	public static final int		ACTIONS_NUM			=	4;		//actions
	public static Gauge.Child[][]	qMatrix;				//prometheus variables
	
	public static void main(String[] args) {	//arguments (opt): topology name
		BasicConfigurator.configure();			//default logging configuration
		launchWebServerForPrometheus();			//launches a web server for prometheus monitoring
		initializePromVariables();				//initializes variables for prometheus
		if (args != null && args.length > 0) {
			Settings.topologyName	=	args[0];
		}
		StormMonitor 	rm		=	new StormMonitor(60000,PROMETHEUS_URL,PROMETHEUS_PUSHG);
		Thread			rm_th	=	new Thread(rm);
		rm_th.start();
		ParabolicProcessTimeRewardCalculator 	rewarder	=	new ParabolicProcessTimeRewardCalculator(850,100,200,1700);
		ProcessTimeStateReader					reader		=	new ProcessTimeStateReader(850,0.2,2);
		FixedIntervalManager					intManager	=	new FixedIntervalManager(Settings.decisionInterval);
		WorkerNumberExecutor					executor	=	new WorkerNumberExecutor(rewarder,intManager);
		EpsilonGreedyChooser					chooser		=	new EpsilonGreedyChooser(0.1);
		StaticAlphaCalculator					alpha		=	new StaticAlphaCalculator(0.1);
		ExpectedSarsa							sarsa		=	new	ExpectedSarsa(3,4,1,chooser,executor,reader,alpha);
		Thread sarsaThread									=	new Thread(sarsa);
		sarsaThread.start();
	}
	
	public static void launchWebServerForPrometheus(){
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
	
	public static void initializePromVariables(){
		qMatrix			=	new Gauge.Child[STATES_NUM][ACTIONS_NUM];
		String[] labels	=	new String[2];
		labels[0]		=	"row";
		labels[1]		=	"column";
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
	}
}
