package grails.plugin.scaffold.core

import java.io.File;
import java.io.IOException;
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils

class DefaultTemplatesLocator implements TemplatesLocator{
	String pluginName
	
	DefaultTemplatesLocator(String pluginName){
		this.pluginName = pluginName
	}
	
	File getPluginDir() throws IOException {
		GrailsPluginInfo info = GrailsPluginUtils.getPluginBuildSettings().getPluginInfoForName(pluginName);
		return info.getDescriptor().getFile().getParentFile();
	}
}
