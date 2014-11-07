grails-scaffold-core
====================

Core functionality for scaffolding own tempates or  templates provided by plugin. e.g. grails-scaffold-extjs

Usage
=====

// generates everything in src/templates/scaffold directory
// it uses plugins dir if application has none
grails createDemo

//sometimes dev db is  locked, so use
grails test createDemo 

// when there is a lot of static files that you do not want to always generate
// then you can just scaffold subdir:
grails createDemo 'frontend/dynamic'

Developing new plugin
======
Lines to be added to Scaffold*GrailsPlugin
def dependsOn = [scaffoldCore: "0.1"]

def doWithSpring = {
    templatesLocator(grails.plugin.scaffold.core.DefaultTemplatesLocator, "%%pluginName%%")
}

And add templates to /src/templates/scaffold




