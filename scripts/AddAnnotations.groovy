includeTargets << new File(scaffoldCorePluginDir, 'scripts/_ScaffoldGenerate.groovy')

target (addAnnotations: "Generates the CRUD views for a specified domain class") {
	depends(checkVersion, parseArguments, packageApp)

	addAnnotations = true
	scaffoldGenerate()
}

setDefaultTarget( addAnnotations )
