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
	def templateGenerator = DefaultGrailsTemplateGenerator.newInstance(classLoader, appCtx.getBean('templatesLocator'))
	
	templateGenerator.grailsApplication = grailsApp
	templateGenerator.pluginManager = pluginManager
	
	event("StatusUpdate", ["Generating application files from templates: $generateTemplatesSubdir"])
	templateGenerator.generateScaffold(generateTemplatesSubdir)
	event("StatusFinal", ["Finished generation of application files from templates."])
}