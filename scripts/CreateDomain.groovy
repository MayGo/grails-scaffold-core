includeTargets << new File(scaffoldCorePluginDir, 'scripts/_ScaffoldGenerate.groovy')

target (createModel: "Generates the model for a specified domain class") {
	depends(checkVersion, parseArguments, packageApp)

	promptForName(type: "Domain Class")

	generateDomain = true

	scaffoldGenerate()
}

setDefaultTarget( createDomain )
