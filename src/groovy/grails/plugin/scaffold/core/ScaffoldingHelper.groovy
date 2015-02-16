package grails.plugin.scaffold.core

import java.util.List;
import java.util.Map;

import grails.persistence.Event

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty


class ScaffoldingHelper {
	def domainClass
	def pluginManager
	def comparator
	def classLoader
	def config

	ScaffoldingHelper(def pluginManager, def comparator, def classLoader, def config){
		this.pluginManager = pluginManager
		this.comparator = comparator
		this.classLoader = classLoader
		this.config = config
	}

	def getProps(def domainClass) {
		def excludedProps = Event.allEvents.toList() << 'version' << 'dateCreated' << 'lastUpdated'
		def persistentPropNames = domainClass.persistentProperties*.name
		boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate') || pluginManager?.hasGrailsPlugin('hibernate4')
		if (hasHibernate) {
			def GrailsDomainBinder = classLoader.loadClass('org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder')
			//TODO:
			/*if (GrailsDomainBinder.newInstance().getMapping(domainClass)?.identity?.generator == 'assigned') {
				persistentPropNames << domainClass.identifier.name
			}*/
		}
		def props = domainClass.properties.findAll {
			persistentPropNames.contains(it.name) && !excludedProps.contains(it.name) && (domainClass.constrainedProperties[it.name] ? domainClass.constrainedProperties[it.name].display : true)
		}
		//sorting. id is first, then simple fields by name, then assaciations by type and then by name
		props.sort{a, b ->
			if (a.equals(domainClass.getIdentifier())) {
				return -1;
			}
			if (b.equals(domainClass.getIdentifier())) {
				return 1;
			}

			if(getAssociationType(a).equals(getAssociationType(b))) {
				return a.getName().compareTo(b.getName());
			}else {
				return getAssociationType(a).compareTo(getAssociationType(b))
			}
		}
		return props
	}
	/**
	 * Finds relations in other domain classes. Defining relationship with hasMany, belongsTo,  etc is not compulsory.
	 * @param domainClasses
	 * @return
	 */
	Map<GrailsDomainClassProperty, GrailsDomainClass> findRelationsProps(def domainClass, List<GrailsDomainClass>
			domainClasses) {

		Map<GrailsDomainClassProperty, GrailsDomainClass> relationDomainClasses = [:]
		domainClasses.each{
			def relationProps = it.persistentProperties.findAll{it.type == domainClass.clazz}
			relationProps.each {relProp->
				relationDomainClasses[relProp]=it
			}
		}

		return relationDomainClasses
	}

	private String getAssociationType(def property) {
		String assType = "";
		if(property.isManyToMany()) {
			assType = "4";
		} else if(property.isOneToMany()) {
			assType = "1";
		} else if(property.isOneToOne()) {
			assType = "2";
		} else if(property.isManyToOne()) {
			assType = "3";
		} else if(property.isEmbedded()) {
			assType = "0";
		}
		return assType
	}

	Map getDomainClassDisplayNames(def domainClass, def property = null){

		Map displayNames = { [:].withDefault{ owner.call() } }()

		displayNames.putAll((config.grails.plugin.scaffold.core.displayNames)?:[:])
		/*displayNames = displayNames.collectEntries {
			(it.respondsTo('containsKey'))? it : [it:null]
		}*/
		boolean returnDefaults = (!property || !displayNames[domainClass.name]?.containsKey(property.name) )

		if(returnDefaults){
			List defaultDisplayNames = config.grails.plugin.scaffold.core.defaultDisplayNames
			Map names = { [:].withDefault{ owner.call() } }()
			def usedDomainClass = (property?.getReferencedDomainClass())?:domainClass
			Map firstLevelMap = displayNames[usedDomainClass.shortName]?.findAll{k,v->!v}
			if(firstLevelMap){
				names += firstLevelMap
			}else{
				usedDomainClass.persistentProperties.findAll{it.name in defaultDisplayNames}.each{names[it.name]}
			}
			return names
		}else{
			//Make list to map

			if(displayNames[domainClass.name][property.name] instanceof List){
				displayNames[domainClass.name][property.name] = displayNames[domainClass.name][property.name].collectEntries {[(it): null]}
			}
			return displayNames[domainClass.name][property.name]
		}
	}
}
