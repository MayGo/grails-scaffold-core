package grails.plugin.scaffold.core

import groovy.transform.CompileStatic

@CompileStatic
interface TemplatesLocator {
	File getPluginDir() throws IOException
}
