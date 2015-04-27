import grails.plugin.scaffold.core.ConfigUtility
import grails.plugin.scaffold.core.DefaultTemplatesLocator

class ScaffoldCoreGrailsPlugin {
	def version = "1.0"
	def grailsVersion = "2.4 > *"
	def loadAfter = ['bootstrap', 'hibernate', 'hibernate4']
	def title = "Scaffold Core Plugin"
	def author = "Maigo Erit"
	def authorEmail = "maigo.erit@gmail.com"
	def description = 'Core functionality for scaffolding own tempates or templates provided by plugin. e.g. grails-scaffold-extjs'
	def documentation = "http://grails.org/plugin/scaffold-core"
	def watchedResources = [
		"file:./src/templates/scaffold/*"
	]
	def license = "APACHE"
	def scm = [ url: "https://github.com/MayGo/grails-scaffold-core" ]
	def issueManagement = [url: 'https://github.com/MayGo/grails-scaffold-core/issues']

	def doWithSpring = {
		ConfigUtility.mergeDefaultConfig(application, 'ScaffoldCoreDefaultConfig')
		def bootStrapClass = getClass().classLoader.loadClass("BootStrap")

		bootStrap(bootStrapClass) { bean ->
			bean.autowire = 'byName'
		}
		coreTemplatesLocator(DefaultTemplatesLocator, "scaffold-core", 100)
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
