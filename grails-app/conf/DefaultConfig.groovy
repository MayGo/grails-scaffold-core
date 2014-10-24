grails{
	plugin{
		scaffold{
			core{
				overwrite = true // false = Ask before replacing file
				defaultDisplayNames = ['name', 'username', 'authority'] // Domain property names that are included as displaynames
				folders = ['backend':'', 'frontend':'web-app/extapp/']
				// Map of domain class names. contains list of maps
				displayNames = [:]//e.g 'User':['group':['name']]
			}
		}
	}
}
