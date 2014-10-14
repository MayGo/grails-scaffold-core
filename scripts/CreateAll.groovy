includeTargets << new File(scaffoldCorePluginDir, 'scripts/_ScaffoldGenerate.groovy')

target( createAll:'Generate all domain artifacts' ) {
  	depends(checkVersion, parseArguments, packageApp)
	  
    generateDomain = true
    generateAssets = true
    generateApplication = true

		scaffoldGenerate()
}

setDefaultTarget( createAll )