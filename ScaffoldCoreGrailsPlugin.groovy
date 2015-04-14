import grails.plugin.scaffold.core.ConfigUtility

class ScaffoldCoreGrailsPlugin {
	def version = "0.4.2"
	// the version or versions of Grails the plugin is designed for
	def grailsVersion = "2.4 > *"

	List loadAfter = ['bootstrap', 'hibernate', 'hibernate4']

	def title = "Scaffold Core Plugin"
	def author = "Maigo Erit"
	def authorEmail = "maigo.erit@gmail.com"
	def description = '''\
Core functionality for scaffolding own tempates or templates provided by plugin. e.g. grails-scaffold-extjs
'''
	// URL to the plugin's documentation
	def documentation = "http://grails.org/plugin/scaffold-core"

	def watchedResources = [
			"file:./src/templates/scaffold/*"
	]

	// Extra (optional) plugin metadata

	// License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"


	// Online location of the plugin's browseable source code.
	def scm = [ url: "https://github.com/MayGo/grails-scaffold-core" ]



	def doWithSpring = {
		ConfigUtility.mergeDefaultConfig(application, 'ScaffoldCoreDefaultConfig')
		def bootStrapClass = getClass().classLoader.loadClass("BootStrap");

		"bootStrap"(bootStrapClass) { bean ->
			bean.autowire = 'byName'
		}
		coreTemplatesLocator(grails.plugin.scaffold.core.DefaultTemplatesLocator, "scaffold-core", 100)
		templateGenerator(CoreTemplateGenerator, application.classLoader, ref('coreTemplatesLocator'), application, manager)
	}

	def onChange = { event ->
		if(event.source && event.ctx){
			println "Reloading template: ${event.source}"
			def templateGenerator = event.ctx.getBean('templateGenerator')
			templateGenerator.generateFile(event.source)
		}
	}

}
