includeTargets << new File(scaffoldCorePluginDir, 'scripts/_ScaffoldGenerate.groovy')

target( createDemo:'Generate demo application (all artefacts)' ) {
  	depends(checkVersion, parseArguments, packageApp)
	  
	scaffoldGenerate()
}

setDefaultTarget( createDemo )