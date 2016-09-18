package mainClasses;

import java.util.ArrayList;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import io.prometheus.client.exporter.MetricsServlet;
import monitors.NewStormMonitor;
import monitors.StormMonitor;
import singletons.Settings;
import thresholdTestSystem.ThresholdSystem;

public class MainClassSoglie {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		BasicConfigurator.configure();			//default logging configuration
		if (args != null && args.length > 0) {
			Settings.topologyName	=	args[0];
		}
		for(int i=0;i<100;i++){
			System.out.println("TEST");
		}
		ThresholdSystem	ts	=	new ThresholdSystem(0.3,0.8);
		Thread th			=	new Thread(ts);
		th.start();
	}
	


}
