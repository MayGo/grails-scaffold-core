grails-scaffold-core
====================

Core functionality for scaffolding own tempates or  templates provided by plugin. e.g. grails-scaffold-extjs


Lines to be added to Scaffold*GrailsPlugin
def dependsOn = [scaffoldCore: "0.1"]

def doWithSpring = {
    templatesLocator(grails.plugin.scaffold.core.DefaultTemplatesLocator, "%%pluginName%%")
}

And add templates to /src/templates/scaffolding