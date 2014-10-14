includeTargets << new File(scaffoldCorePluginDir, 'scripts/_ScaffoldGenerate.groovy')

target( createDemo:'Generate demo application (all artefacts)' ) {
  	depends(checkVersion, parseArguments, packageApp)

	generateDomain = true
	generateAssets = true
	generateApplication = true
	addAnnotations = false
	generateControllers = true

	scaffoldGenerate()
}

setDefaultTarget( createDemo )