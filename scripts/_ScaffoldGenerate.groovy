includeTargets << grailsScript("_GrailsBootstrap")
includeTargets << grailsScript("_GrailsCreateArtifacts")

generateDomain = false
generateAssets = false
generateApplication = false
addAnnotations = false
generateControllers = false


target(scaffoldGenerate: "Generates controllers and extjs views for all domain classes.") {
	depends(configureProxy, packageApp, classpath, loadApp, configureApp)

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
	def templateGenerator = DefaultGrailsTemplateGenerator.newInstance(classLoader)
	
	templateGenerator.templatesLocator = appCtx.getBean('templatesLocator')
	templateGenerator.grailsApplication = grailsApp
	templateGenerator.pluginManager = pluginManager
	templateGenerator.generateScaffold(basedir)
	domainClasses.each { domainClass -> 
		
		/*if (generateDomain) {
			event("StatusUpdate", ["Generating extjs classes for domain class ${domainClass.fullName}"])
			templateGenerator.generateDomain(domainClass, basedir)
			event("GenerateStoreEnd", [domainClass.fullName])
		}
		if (addAnnotations) {
			event("StatusUpdate", ["Adding annotation to domain class ${domainClass.fullName}"])
			templateGenerator.addAnnotation(domainClass)
			event("AddAnnotationEnd", [domainClass.fullName])
		}
		if (generateControllers) {
			event("StatusUpdate", ["Adding controller to domain class ${domainClass.fullName}"])
			templateGenerator.generateRestfulController(domainClass, basedir)
			templateGenerator.generateRestfulTest(domainClass, "${basedir}/test/unit")
			event("GenerateControllerEnd", [domainClass.fullName])
		}*/
		
		
	}

	if (generateControllers) {
		event("StatusUpdate", ["Adding urlMappings"])
//		templateGenerator.addUrlMappings()
		event("AddUrlMappingsEnd", ["End"])
	}
	if (generateApplication) {
		event("StatusUpdate", ["Generating application views"])
//		templateGenerator.generateApplication(basedir)
		event("GenerateApplicationEnd", [])
	}
	
	if (generateAssets) {
		event("StatusUpdate", ["Generating static views"])
		//templateGenerator.generateAssets(basedir)
		event("GenerateAssetsEnd", [])
	}
	
	event("StatusFinal", ["Finished generation of extjs files."])
}