includeTargets << new File(scaffoldCorePluginDir, 'scripts/_ScaffoldGenerate.groovy')

target (createStatic: "Generates the assets for application") {
	depends(checkVersion, parseArguments, packageApp)

	generateAssets = true

	scaffoldGenerate()
}

setDefaultTarget( createStatic )
