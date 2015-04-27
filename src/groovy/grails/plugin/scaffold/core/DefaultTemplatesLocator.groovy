package grails.plugin.scaffold.core

import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils

@CompileStatic
class DefaultTemplatesLocator implements TemplatesLocator{
	String pluginName
	int order = 0

	DefaultTemplatesLocator(String pluginName, int order){
		this.pluginName = pluginName
		this.order = order
	}
	DefaultTemplatesLocator(String pluginName){
		this(pluginName, 0)
	}

	File getPluginDir() throws IOException {
		GrailsPluginInfo info = GrailsPluginUtils.getPluginBuildSettings().getPluginInfoForName(pluginName)
		return info.descriptor.file.parentFile
	}
}
