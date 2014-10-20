grails{
	plugin{
		scaffold{
			core{
				overwrite = true // false = Ask before replacing file
				displaynames = ['name', 'username', 'authority'] // Domain property names that are included as displaynames
				folders = ['backend':'', 'frontend':'web-app/extapp/']
			}
		}
	}
}
