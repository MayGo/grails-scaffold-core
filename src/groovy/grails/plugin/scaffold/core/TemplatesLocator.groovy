package grails.plugin.scaffold.core

import java.io.File;
import java.io.IOException;

interface TemplatesLocator {
	File getPluginDir() throws IOException 
}
