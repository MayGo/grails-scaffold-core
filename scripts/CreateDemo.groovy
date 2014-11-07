includeTargets << new File(scaffoldCorePluginDir, 'scripts/_ScaffoldGenerate.groovy')

target( createDemo:'Generate demo application (all artefacts)' ) {
  	depends(checkVersion, parseArguments, packageApp)
	  
	  
	String name = argsMap["params"][0]
	generateTemplatesSubdir = (name)?:""
	scaffoldGenerate()
}

setDefaultTarget( createDemo )