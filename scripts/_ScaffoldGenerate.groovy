import org.springframework.transaction.support.TransactionSynchronizationManager

includeTargets << grailsScript("_GrailsBootstrap")
includeTargets << grailsScript("_GrailsCreateArtifacts")

generateTemplatesSubdir=""

target(scaffoldGenerate: "Generates controllers and views for all domain classes.") {
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
	
	//Init bootstrap to generate build-test-data data
	println "Init bootstrap to generate build-test-data data"
	def bootstrap = appCtx.getBean('bootStrap')
	try {
		configureHibernateSession()
		bootstrap.init()
	}catch (Exception ex) {
		if (ex.getClass().getSimpleName() == "ConstraintHandlerException") {
			event("StatusUpdate", [
					"If ConstraintHandlerException: Could not bootstrap application. There is a constraint error. Fix it in TestDataConfig.groovy and then run again 'grails createDemo'."
			])
		} else {
			event("StatusUpdate", [
					"Otherwise: Unknown error for scaffold plugin. Continuing...."
			])
		}
		
		ex.printStackTrace();
	}
	println "Bootstrap init done"

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
def configureHibernateSession() {
	// without this you'll get a lazy initialization exception when using a many-to-many relationship
	def SessionFactoryUtils = classLoader.loadClass("org.springframework.orm.hibernate4.SessionFactoryUtils");
	def SessionHolder = classLoader.loadClass("org.springframework.orm.hibernate4.SessionHolder");
	if(!SessionFactoryUtils){
		println "No hibernate4 SessionFactoryUtils."
		SessionFactoryUtils = classLoader.loadClass("org.springframework.orm.hibernate3.SessionFactoryUtils");
		if(!SessionFactoryUtils){
			println "And no hibernate3 SessionFactoryUtils"
		}else{
			println "Using hibernate3 SessionFactoryUtils"
		}
	}
	if(!SessionHolder){
		println "No hibernate4 SessionHolder."
		SessionFactoryUtils = classLoader.loadClass("org.springframework.orm.hibernate3.SessionHolder");
		if(!SessionHolder){
			println "And no hibernate3 SessionHolder"
		}else{
			println "Using hibernate3 SessionHolder"
		}
	}
	if(SessionHolder && SessionFactoryUtils) {
		def sessionFactory = appCtx.getBean("sessionFactory")
		def session = SessionFactoryUtils.getSession(sessionFactory, true)

		TransactionSynchronizationManager.bindResource(sessionFactory, SessionHolder.newInstance(session))
	}else{
		println "No SessionHolder or SessionFactoryUtils found"
	}
}