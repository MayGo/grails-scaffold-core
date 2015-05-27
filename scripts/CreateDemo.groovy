includeTargets << new File(scaffoldCorePluginDir, 'scripts/_ScaffoldGenerate.groovy')

target( createDemo:'Generate demo application (all artefacts)' ) {
  	depends(checkVersion, parseArguments, packageApp)
	  
	  
	String name = argsMap["params"][0]
	generateTemplatesSubdir = (!"*".equals(name) && name)? name :""
	String domainClassNameParam = argsMap["params"][1]
	domainClassName = (domainClassNameParam)?:"*"
	scaffoldGenerate()
}

setDefaultTarget( createDemo )