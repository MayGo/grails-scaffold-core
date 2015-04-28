package grails.plugin.scaffold.core

import grails.util.Environment
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication


@Slf4j
@CompileStatic
class ConfigUtility {

	static void mergeDefaultConfig(DefaultGrailsApplication application, String configFileName) {
		GroovyClassLoader classLoader = new GroovyClassLoader(ConfigUtility.classLoader)
		ConfigSlurper slurper = new ConfigSlurper(Environment.current.name)
		ConfigObject secondary
		try {
			secondary = slurper.parse(classLoader.loadClass(configFileName))
		} catch (Exception e) {
			log.error "Error loading default configuration."
		}

		ConfigObject config = new ConfigObject()
		if (secondary == null) {
			config.putAll(application.config)
		} else {
			config.putAll(secondary.merge(application.config))
		}
		application.config = config
	}
}
