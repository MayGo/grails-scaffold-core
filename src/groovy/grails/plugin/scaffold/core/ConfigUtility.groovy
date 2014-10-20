package grails.plugin.scaffold.core

import grails.util.Environment
import groovy.util.ConfigObject;
import groovy.util.logging.Slf4j
import org.codehaus.groovy.grails.commons.GrailsApplication;

@Slf4j
class ConfigUtility {

	public static void mergeDefaultConfig(GrailsApplication application, String configFileName) {
		GroovyClassLoader classLoader = new GroovyClassLoader(ConfigUtility.class.getClassLoader());
		ConfigSlurper slurper = new ConfigSlurper(Environment.getCurrent().getName());
		ConfigObject secondary = null;
		try {
			secondary = (ConfigObject)slurper.parse(classLoader.loadClass(configFileName));
		} catch (Exception e) {
			log.error "Error loading deafult configuration."
		}
		
		ConfigObject config = new ConfigObject();
		if (secondary == null) {
			config.putAll(application.config);
		} else {
			config.putAll(secondary.merge(application.config));
		}
		application.config = config
	}
}
