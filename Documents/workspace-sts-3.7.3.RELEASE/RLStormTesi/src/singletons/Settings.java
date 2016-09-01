package singletons;

public class Settings {
	public static PropertiesReader	propsReader			=	new PropertiesReader("props.properties");
	public static int 				decisionInterval	=	Integer.parseInt((String)propsReader.getValue("decisionInterval"));	//storm publishes datas every 60 sec
	public static String			topologyName		=	(String)propsReader.getValue("topologyName");
	public static String			stormPath			=	(String)propsReader.getValue("stormPath");
	public static String			PROMETHEUS_URL 		=	(String)propsReader.getValue("PROMETHEUS_URL");
	public static String			PROMETHEUS_PUSHG 	=	(String)propsReader.getValue("PROMETHEUS_PUSHG");
	public static String			STORMUI_URL			=	(String)propsReader.getValue("STORMUI_URL");
	public static double			softmaxTemperature	=	Double.parseDouble((String)propsReader.getValue("softmaxTemperature"));
	public static double			alpha				=	Double.parseDouble((String)propsReader.getValue("alpha"));
}
