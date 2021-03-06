package mainClasses;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import expectedSarsa.ExpectedSarsa;
import expectedSarsa.FixedIntervalManager;
import features.FeaturesEvaluator;
import features.SimpleFeaturesEvaluator;
import features.SimpleFeaturesEvaluatorMultilevel;
import features.SimpleFeaturesEvaluatorMultilevelExtended;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.MetricsServlet;
import linearGradientSarsa.LinearGradientDescendSarsaLambda;
import linearGradientSarsa.LinearGrandientDescendExpectedSarsa;
import monitors.NewStormMonitor;
import monitors.StormMonitor;
import redis.clients.jedis.Jedis;
import rl.alpha.StaticAlphaCalculator;
import rl.executer.ActionExecutor;
import rl.executer.BottleneckExecutor;
import rl.executer.ExecutorsChange;
import rl.executer.ExecutorsChangeMultipleSteps;
import rl.executer.WorkerNumberExecutor;
import rl.policies.EpsilonGreedyChooser;
import rl.policies.EpsilonGreedyWithFeasibilityCheck;
import rl.rewarder.CongestionDeltaRewarder;
import rl.rewarder.DeltaNonNegativeRewarder;
import rl.rewarder.DeltaNonNegativeRewarderRelativeSteps;
import rl.rewarder.DeltaNonNegativeRewarderRelativeStepsWithCapacity;
import rl.rewarder.DeltaRewarder;
import rl.rewarder.DeltaRewarderSimplified;
import rl.rewarder.ParabolicComplexResponseTimeRewarder;
import rl.rewarder.ParabolicProcessTimeRewardCalculator;
import rl.rewarder.RewardCalculator;
import singletons.Settings;
import singletons.SystemStatus;
import state.ProcessTimeStateReader;
import state.ProcessTimeStateReaderEvo;
import state.ProcessTimeStateReaderEvoCapacity;
import state.ProcessTimeStateReaderEvoCapacityWithLowCheck;
import state.StateReader;
import state.StateTranslator;
import thresholdTestSystem.ThresholdSystem;
import thresholdTestSystem.ThresholdSystemWithCheck;

public class MainClass {
	private static final Logger LOG = LoggerFactory.getLogger(MainClass.class);
	public static int 	 		monitoringInterval	=	Settings.decisionInterval;							//storm monitoring interval (>=60000)
	public static final String	PROMETHEUS_URL		=	Settings.PROMETHEUS_URL;	//prometheus server url
	public static final String	PROMETHEUS_PUSHG	=	Settings.PROMETHEUS_PUSHG;	//prometheus push gateway
	public static final String	STORMUI_URL			=	Settings.STORMUI_URL;	//storm UI url
	public static final	Gauge	REWARD_VAL			=	Gauge.build().name("bench_rewardVal").help("Reward received value").register();	//prometheus metric to be monitored on Graphana
	public static final	Gauge	LATENCY_VAL			=	Gauge.build().name("bench_latencyRead").help("Latency read by decisor").register();	//prometheus metric to be monitored on Graphana
	public static final	Gauge	PARALLELISM_VAL		=	Gauge.build().name("bench_parallelism").help("Parallelism level decided").register();	//prometheus metric to be monitored on Graphana
	public static final	Gauge	SYST_UTIL			=	Gauge.build().name("bench_utilization").help("System utilization").register();	//prometheus metric to be monitored on Graphana
	public static final Counter BENCH_EMITTED		=	Counter.build().name("bench_emitted").help("Emitted tuples fine source").register();
	public static final	Gauge	SYST_UTIL_FINE		=	Gauge.build().name("bench_utilization_fine").help("System utilization at fine grain").register();	//prometheus metric to be monitored on Graphana
	public static final	Gauge	GRADIENT_DELTA		=	Gauge.build().name("bench_gradient_desc_delta").help("Delta from predicted value").register();
	public static final Gauge	STATE_READ			=	Gauge.build().name("state_read").help("State read on decision").register();
	public static final Gauge	TUPLE_LOSS			=	Gauge.build().name("tuple_loss").help("Tuple loss").register();
	public static final int 	STATES_NUM			=	3;		//states
	public static int			ACTIONS_NUM			=	4;		//actions
	public static Gauge.Child[][]	qMatrix;				//prometheus variables
	public static Gauge.Child[]		operatorsLevel;			//prometheus variables
	public static HashMap<String,Gauge.Child> 	levelMap	=	new HashMap<String,Gauge.Child>();

	public static void main(String[] args) {	//arguments (opt): topology name
		BasicConfigurator.configure();			//default logging configuration
		if (args != null && args.length > 0) {
			Settings.topologyName	=	args[0];
		}
		dynamicSteps();
		//thrSystem();
	}
	
	public static void thrSystem(){
		//ThresholdSystem	ts	=	new ThresholdSystem(0.3,0.8);
		ThresholdSystemWithCheck	ts	=	new ThresholdSystemWithCheck(0.6,0.9);
		Thread th			=	new Thread(ts);
		th.start();
	}
	
	public static void dynamicSteps(){
		int maxParallelism	=	Settings.maxParallelism;
		double loadOKTh		=	Settings.loadOKTh; //IOT 0.7
		int    latMax		=	Settings.latMax; //fibonacci was 4500
		int    latObj		=	Settings.latObj; //fibonacci was 3000
		int    latDelta		=	Settings.latDelta; //fibonacci was 300
		double latSensib	=	Settings.latSensibility; //Iot 0.3
		double loadOKBonus	=	Settings.loadOKBonus;//default 30
		Jedis jedis = new Jedis("127.0.0.1",6379);
	    jedis.flushAll();	    
	    
		StormMonitor 		sm		=	new StormMonitor(PROMETHEUS_URL,PROMETHEUS_PUSHG);
		Thread			sm_th	=	new Thread(sm);
		sm_th.start();
		
		NewStormMonitor 	rm		=	new NewStormMonitor(PROMETHEUS_URL,STORMUI_URL,10000);
		Thread			rm_th	=	new Thread(rm);
		ArrayList<String> boltsName	=	new ArrayList();
		while(boltsName.size()==0){
			boltsName	=	rm.getBoltsName();	
		}
		
		SystemStatus.bolts	=	boltsName;
		StateTranslator translator	=	new StateTranslator(boltsName.size(), 3, 32, jedis);
		rm_th.start();
		while(SystemStatus.executors.size()==0){
			try {
				Thread.sleep(1000);
				LOG.debug("waiting operators level");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		//TEST

		
		
		//RewardCalculator					 	rewarder	=	new ParabolicComplexResponseTimeRewarder(3000,125,4500,ACTIONS_NUM);
		//RewardCalculator					 	rewarder	=	new DeltaRewarder(3000,4500,15,0.2);
		StateReader								reader		=	new ProcessTimeStateReaderEvoCapacityWithLowCheck(latMax,translator,maxParallelism,loadOKTh);
		RewardCalculator						rewarder	=	new DeltaNonNegativeRewarderRelativeStepsWithCapacity(latDelta,latObj,latMax,maxParallelism,loadOKTh,latSensib,reader,loadOKBonus);
		//RewardCalculator					 	rewarder	=	new DeltaRewarderSimplified(300,3000,4500,true);
		//RewardCalculator					 	rewarder	=	new CongestionDeltaRewarder(boltsName,4500,3000);
		//StateReader								reader		=	new ProcessTimeStateReaderEvo(1500,4500,translator,maxParallelism);
		FixedIntervalManager					intManager	=	new FixedIntervalManager(Settings.decisionInterval);
		//WorkerNumberExecutor					executor	=	new WorkerNumberExecutor(rewarder,intManager);
		rl.policies.PolicyChooser				chooser		=	new rl.policies.SoftmaxPolicyChooser(Settings.softmaxTemperature);//EpsilonGreedyChooser(0.1);
		StaticAlphaCalculator					alpha		=	new StaticAlphaCalculator(Settings.alpha);
		//Thread sarsaThread									=	new Thread(sarsa);
		//sarsaThread.start();
		
		
		int 									actionsN	=	(boltsName.size()*6)+1;	
		//actionsN	=	3; //TODO remove
		ACTIONS_NUM											=	actionsN;
		
		LOG.debug("start prometheus variables");
		initializePromVariables(boltsName);				//initializes variables for prometheus
		 
		int[] steps	=	new int[3];
		steps[0]	=	1;
		steps[1]	=	maxParallelism/16;
		steps[2]	=	maxParallelism/8;
		for(int i=0;i<steps.length;i++){
			System.out.println("steps "+i+" = "+steps[i]);
		}
		ActionExecutor							executor	=	new ExecutorsChangeMultipleSteps(boltsName,8, steps, maxParallelism, singletons.Settings.topologyName,intManager,rewarder);
		//BottleneckExecutor							executor	=	new BottleneckExecutor(rewarder,intManager,32);
		//chooser	=	new EpsilonGreedyWithFeasibilityCheck(executor,0.1);
		
		//ExpectedSarsa							sarsa		=	new	ExpectedSarsa(3,actionsN,1,chooser,executor,reader,alpha,"QMatrix.txt");
		//Thread									sarsaTh		=	new Thread(sarsa);
		
		
		FeaturesEvaluator evaluator	=	new SimpleFeaturesEvaluatorMultilevelExtended(boltsName,steps, 3,6,maxParallelism,translator,executor);
		//LinearGradientDescendSarsaLambda sarsa	=	new LinearGradientDescendSarsaLambda(chooser,evaluator.getFeaturesN(),0.1,0.2,0.01,reader,evaluator,executor,alpha,actionsN,actionsN-1);
		LinearGrandientDescendExpectedSarsa sarsa	=	new LinearGrandientDescendExpectedSarsa(chooser,evaluator.getFeaturesN(),0.1,0.2,0,reader,evaluator,executor,alpha,actionsN,actionsN-1);
		/*
		
		//TODO TEST
	    Jedis jedis = new Jedis("127.0.0.1",6379);
	    jedis.flushAll();
		
	    
	    StateTranslator translator	=	new StateTranslator(3, 3, 32, jedis);
		reader	=	new ProcessTimeStateReaderEvo(1500,3500,translator);
		
		
		
		try {
			Thread.sleep(20000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for(int i=0;i<30;i++){
			int stateC	=	reader.getCurrentState();
			System.out.println("Associated state "+stateC);
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//TODO ENDTEST
		
		
		*/
		Thread									sarsaTh		=	new Thread(sarsa);
		launchWebServerForPrometheus();			//launches a web server for prometheus monitoring
		sarsaTh.start();
	}
	
	public static void nonDynamicSteps(){
		
		StormMonitor 		sm		=	new StormMonitor(PROMETHEUS_URL,PROMETHEUS_PUSHG);
		Thread			sm_th	=	new Thread(sm);
		sm_th.start();
		
		NewStormMonitor 	rm		=	new NewStormMonitor(PROMETHEUS_URL,STORMUI_URL,20000);
		Thread			rm_th	=	new Thread(rm);
		rm_th.start();

		ArrayList<String> boltsName	=	new ArrayList<String>();
		boltsName.add("firststage");
		boltsName.add("secondstage");
		boltsName.add("thirdstage");
		boltsName	=	rm.getBoltsName();
		SystemStatus.bolts	=	boltsName;
		while(SystemStatus.executors.size()==0){
			try {
				Thread.sleep(1000);
				LOG.debug("waiting operators level");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		//TEST


		
		//RewardCalculator					 	rewarder	=	new ParabolicComplexResponseTimeRewarder(3000,125,4500,ACTIONS_NUM);
		//RewardCalculator					 	rewarder	=	new DeltaRewarder(3000,4500,15,0.2);
		RewardCalculator						rewarder	=	new DeltaNonNegativeRewarder(300,3000,4500);
		//RewardCalculator					 	rewarder	=	new DeltaRewarderSimplified(300,3000,4500,true);
		//RewardCalculator					 	rewarder	=	new CongestionDeltaRewarder(boltsName,4500,3000);
		StateReader								reader		=	new ProcessTimeStateReader(3000,0.5,1.5);
		FixedIntervalManager					intManager	=	new FixedIntervalManager(Settings.decisionInterval);
		//WorkerNumberExecutor					executor	=	new WorkerNumberExecutor(rewarder,intManager);
		//rl.policies.PolicyChooser				chooser		=	new rl.policies.SoftmaxPolicyChooser(0.2);//EpsilonGreedyChooser(0.1);
		rl.policies.PolicyChooser				chooser		=	new rl.policies.SoftmaxPolicyChooser(1);//EpsilonGreedyChooser(0.1);
		StaticAlphaCalculator					alpha		=	new StaticAlphaCalculator(0.8);
		//Thread sarsaThread									=	new Thread(sarsa);
		//sarsaThread.start();
		
		
		int 									actionsN	=	(boltsName.size()*2)+1;	
		//actionsN	=	3; //TODO remove
		ACTIONS_NUM											=	actionsN;
		initializePromVariables(boltsName);				//initializes variables for prometheus
		 
		int[] steps	=	new int[3];
		steps[0]	=	2;
		steps[1]	=	1;
		steps[2]	=	4;
		ExecutorsChange							executor	=	new ExecutorsChange(boltsName,steps, 32, singletons.Settings.topologyName,intManager,rewarder);
		//BottleneckExecutor							executor	=	new BottleneckExecutor(rewarder,intManager,32);
		//chooser	=	new EpsilonGreedyWithFeasibilityCheck(executor,0.1);
		
		//ExpectedSarsa							sarsa		=	new	ExpectedSarsa(3,actionsN,1,chooser,executor,reader,alpha,"QMatrix.txt");
		//Thread									sarsaTh		=	new Thread(sarsa);
		
		
		SimpleFeaturesEvaluator evaluator	=	new SimpleFeaturesEvaluator(boltsName,3,6);
		LinearGradientDescendSarsaLambda sarsa	=	new LinearGradientDescendSarsaLambda(chooser,(6*STATES_NUM),0.1,0.2,0.01,reader,evaluator,executor,alpha,(2*boltsName.size())+1,(2*boltsName.size()));
		/*
		
		//TODO TEST
	    Jedis jedis = new Jedis("127.0.0.1",6379);
	    jedis.flushAll();
		
	    
	    StateTranslator translator	=	new StateTranslator(3, 3, 32, jedis);
		reader	=	new ProcessTimeStateReaderEvo(1500,3500,translator);
		
		
		
		try {
			Thread.sleep(20000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for(int i=0;i<30;i++){
			int stateC	=	reader.getCurrentState();
			System.out.println("Associated state "+stateC);
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//TODO ENDTEST
		
		
		*/
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
	
	/**
	 * Auxiliary method in order to initialize Prometheus variable hierarchy
	 * @param boltsName bolts name list
	 */
	public static void initializePromVariables(ArrayList<String> boltsName){
		qMatrix			=	new Gauge.Child[STATES_NUM][ACTIONS_NUM];
		operatorsLevel	=	new Gauge.Child[boltsName.size()];
		for(int i=0;i<boltsName.size();i++){
			String boltName		=	boltsName.get(i);
			Gauge.Child opLevel	=	operatorsLevel[i];
			levelMap.put(boltName,opLevel);
		}
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
