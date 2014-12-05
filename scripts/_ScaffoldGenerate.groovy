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

	def templateGenerator = appCtx.getBean('templateGenerator')
	
	Map templatesLocators = appCtx.getBeansOfType(classLoader.loadClass("grails.plugin.scaffold.core.TemplatesLocator"));
	event("StatusUpdate", [
		"Using plugins: ${templatesLocators.collect{it.key}}"
	])
	templatesLocators.collect{it.value}.sort{it.order}.each{templatesLocator->

		event("StatusUpdate", [
			"Generating application files from plugin ${templatesLocator.getPluginDir()} templates: $generateTemplatesSubdir"
		])
		
		String templatesDir = templatesLocator.getPluginDir().path + templateGenerator.SCAFFOLD_DIR
		//Check if has templates in plugin(core plugin has none), if has not use application templates
		if(!templateGenerator.templatesExists(templatesDir)) templatesDir = templateGenerator.APPLICATION_DIR + templateGenerator.SCAFFOLD_DIR
		
		templateGenerator.generateScaffold(templatesDir + generateTemplatesSubdir)
		event("StatusFinal", ["Finished generation of application files from plugin ${templatesLocator.getPluginDir()} templates."])
	}
}