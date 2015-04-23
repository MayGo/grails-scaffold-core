# grails-scaffold-core

Core functionality for scaffolding own tempates or  templates provided by plugin. e.g. grails-scaffold-angular

# Usage

```grails create-demo```

Generates everything in src/templates/scaffold directory.

# Architecture
Plugins templates are should be in directory
```src\templates\scaffold\```

Then  comes "package" name. With that you can configure
src\templates\scaffold\frontend\static
src\templates\scaffold\frontend\dynamic
...\grails-app\controllers\__packageName__\__shortName__Controller.groovy
...\grails-app\conf\__Bootstrap.groovy
Map[ regex: closure ]


# Developing new plugin
Lines to be added to Scaffold*GrailsPlugin
```
def dependsOn = [scaffoldCore: "0.1"]

def doWithSpring = {
    templatesLocator(grails.plugin.scaffold.core.DefaultTemplatesLocator, "%%pluginName%%")
}
```
# Config
```
grails{
    plugin{
        scaffold{
            core{
                overwrite = true // false = Ask before replacing file
                // Map of domain class names. contains list of maps
                displayNames = ['Division':['name':null], 'User':['group':['name']]]
                folders = ['backendSrc':'src/', 'backendTests':'test/', 'backendGrailsApp':'grails-app/', 'frontend':'angular/']
                // don't generate files or menu for domains
                ignoreDomainNames = []
                ignoreFileNames = ['TestDataGeneratorService.groovy', 'TestDataConfig.groovy']
                ignoreStatic = true
            }
        }
    }
}
```
And add templates to /src/templates/scaffold

# Misc problems
Reloading does not work when plugin is included like:
```
grails.plugin.location.'scaffold-angular' = "../grails-scaffold-angular"
```

Better use folder syncer (e.g DirSync Pro). Plugin development dir:

```grails-scaffold-angular\src\templates\ => app_dir/target\plugins\scaffold-angular-0.3\src\templates\```





