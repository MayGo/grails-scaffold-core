# grails-scaffold-core

Core functionality for scaffolding own tempates or  templates provided by plugin. e.g. grails-scaffold-angular

# Usage

```grails create-demo```

Generates everything in src/templates/scaffold directory.

# Architecture
Plugins templates are should be in directory
```src\templates\scaffold\```

Then  comes "package" name. With that you can configure generated content location.

Template folder: ```src\templates\scaffold\backendSrc\```
Scaffolding is generated in src folder with config: ```folders = ['backendSrc':'src/']```

In every "package" folders there should be ```static``` and ```dynamic``` folder
```static``` - Static files that are not treated as templates (images, js plugins, css). Static files are generated only once if ```ignoreStatic = true``` is set
```dynamic``` - Every file in there is treated as template file.

**Generated for every domain class.**
In every "dynamic" folder there can be folders:
 e.g: foo.bar.someDomainObject
* __propertyName__ - someDomainObject/
* __shortName__ - SomeDomainObject/
* __packageName__ foo/bar/
In every "dynamic" folder there can be files:

* __propertyName__Service.groovy - someDomainObjectService.groovy
* __shortName__Service.groovy - SomeDomainObjectService.groovy
* __packageName__ - don't use in filenames

Other examples:
* ```src\templates\scaffold\\dynamic\controllers\__packageName__\__shortName__Controller.groovy```
* ```src\templates\scaffold\dynamic\conf\__Bootstrap.groovy```
* ```src\templates\scaffold\dynamic\conf\CustomMarshallerRegistrar.groovy```

**Generated only once.**

...
...\grails-app\conf\__Bootstrap.groovy
Map[ regex: closure ]

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

# Developing new plugin
Lines to be added to Scaffold*GrailsPlugin
```
def dependsOn = [scaffoldCore: "0.1"]

def doWithSpring = {
    templatesLocator(grails.plugin.scaffold.core.DefaultTemplatesLocator, "%%pluginName%%")
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





