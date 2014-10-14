includeTargets << new File(scaffoldCorePluginDir, 'scripts/_ScaffoldGenerate.groovy')

target (createStatic: "Generates the static views for application") {
	depends(checkVersion, parseArguments, packageApp)

	generateApplication = true

	scaffoldGenerate()
}

setDefaultTarget( createApplication )
