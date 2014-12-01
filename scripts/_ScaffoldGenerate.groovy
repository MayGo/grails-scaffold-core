includeTargets << grailsScript("_GrailsBootstrap")
includeTargets << grailsScript("_GrailsCreateArtifacts")

generateTemplatesSubdir=""

target(scaffoldGenerate: "Generates controllers and extjs views for all domain classes.") {
	depends(configureProxy, packageApp, classpath)

	def domainClasses = grailsApp.domainClasses

	if (!domainClasses) {
		println "No domain classes found in grails-app/domain, trying hibernate mapped classes..."
		bootstrap()
		domainClasses = grailsApp.domainClasses
	}

	if (!domainClasses) {
		event("StatusFinal", ["No domain classes found"])
		return
	}

	def DefaultGrailsTemplateGenerator = classLoader.loadClass('CoreTemplateGenerator')

	Map templatesLocators = appCtx.getBeansOfType(classLoader.loadClass("grails.plugin.scaffold.core.TemplatesLocator"));
	templatesLocators.collect{it.value}.sort{it.order}.each{templatesLocator->
		def templateGenerator = DefaultGrailsTemplateGenerator.newInstance(classLoader, templatesLocator)

		templateGenerator.grailsApplication = grailsApp
		templateGenerator.pluginManager = pluginManager

		event("StatusUpdate", [
			"Generating application files from plugin ${templatesLocator.getPluginDir()} templates: $generateTemplatesSubdir"
		])
		templateGenerator.generateScaffold(generateTemplatesSubdir)
		event("StatusFinal", ["Finished generation of application files from plugin ${templatesLocator.getPluginDir()} templates."])
	}
}