package thresholdTestSystem;

import java.io.IOException;
import java.util.HashMap;

import javax.swing.text.html.HTMLDocument.Iterator;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ConfigurationExecutor {
	public final static Logger logger	=	LogManager.getLogger(ConfigurationExecutor.class);
	public void executeConfiguration(HashMap<String,Integer> newConf,String topologyName){
		int totalExecutors	=	0;
		String execFlags	=	"";
		java.util.Iterator<String> it			=	newConf.keySet().iterator();
		while(it.hasNext()){
			String boltName	=	it.next();
			totalExecutors	=	totalExecutors	+	newConf.get(boltName);
			execFlags		=	execFlags+" -e "+boltName+"="+newConf.get(boltName);
		}
		totalExecutors	=	totalExecutors/8;
		String command	=	singletons.Settings.stormPath+"storm rebalance -w 0 "+topologyName+" -n "+totalExecutors+execFlags;
		logger.debug("sending command "+command);
		Runtime rt 		= 	Runtime.getRuntime();
		try {
			Process pr 		= 	rt.exec(command);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.debug(e.getMessage());
		}
	}
}
