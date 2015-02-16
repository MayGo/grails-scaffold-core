grails{
	plugin{
		scaffold{
			core{
				overwrite = true // false = Ask before replacing file
				//folders = ['backend':'', 'frontend':'web-app/jsapp/']//example
				// Map of domain class names. contains list of maps
				displayNames = [:]//e.g 'User':['group':['name']]
			}
		}
	}
}
