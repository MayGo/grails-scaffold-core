package grails.plugin.scaffold.core

import java.io.File;
import java.io.IOException;
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils

class DefaultTemplatesLocator implements TemplatesLocator{
	File getPluginDir() throws IOException {
		GrailsPluginInfo info = GrailsPluginUtils.getPluginBuildSettings().getPluginInfoForName("scaffold-core");
		return info.getDescriptor().getFile().getParentFile();
	}
}
