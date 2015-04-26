grails.project.work.dir = 'target'

grails.project.dependency.resolver = 'maven'
grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		mavenLocal()
		grailsCentral()
		mavenCentral()
	}

	plugins {
		build(':release:3.0.1') {
			export = false
		}

		compile ':plugin-config:0.2.0'
	}
}
