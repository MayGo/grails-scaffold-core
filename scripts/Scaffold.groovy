includeTargets << new File(scaffoldCorePluginDir, 'scripts/_ScaffoldGenerate.groovy')

target( scaffold:'Generate scaffold artifact' ) {
  	depends(checkVersion, parseArguments, packageApp)
	  
	  
	String name = argsMap["params"][0]
	generateTemplatesSubdir = (!"*".equals(name) && name)?:""
	String domainClassNameParam = argsMap["params"][1]
	domainClassName = (domainClassNameParam)?:"*"
	scaffoldGenerate()
}

setDefaultTarget( scaffold )