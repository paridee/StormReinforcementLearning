package singletons;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mainClasses.MainClass;

public class PropertiesReader {
	Properties prop		=	new Properties();
	
	PropertiesReader(String filename){
		
		InputStream input	=	null;
		try{
			input	=	new FileInputStream(filename);
			prop.load(input);
		}catch(IOException e){
			e.printStackTrace();
		}finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	Object getValue(String propName){
		if(prop.containsKey(propName)){
			System.out.println("Proprieta richiesta "+propName+" valore "+prop.getProperty(propName));
			return prop.getProperty(propName);	
		}
		return null;
	}
	
	public static void main(String[] args) {
		PropertiesReader read	=	new PropertiesReader("props.properties");
		System.out.println("PROVA "+read.getValue("decisionInterval"));
		System.out.println("PROVA2 "+read.getValue("stormPath"));
	}

}
