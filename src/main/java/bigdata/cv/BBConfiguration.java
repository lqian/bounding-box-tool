/**
 * 
 */
package bigdata.cv;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

/**
 * @author link
 *
 */
public class BBConfiguration {
	
	public static final String DEFAULT_PROPERTIES_RESOURCE = "cfg.properties";
	
	Configurations configs = new Configurations();
	Configuration config;
	
	static BBConfiguration _instance;
	
	public static synchronized BBConfiguration getInstance() {
		if (_instance == null) {
			_instance = new BBConfiguration(DEFAULT_PROPERTIES_RESOURCE);
		}
		
		return _instance;
	}
	
	private BBConfiguration(String resource) {
		try {
			config = configs.properties(resource);
		} catch (ConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public String[] getClazz() {
		return config.getString("clazz").split(",");
	}
	
	public static void main(String[] args) {
		System.out.println(BBConfiguration.getInstance().getClazz());
	}

}
