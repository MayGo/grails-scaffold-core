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
		// Add Constraints, required proeprty and joinProperty to properties
		props.each{p->

			boolean required = false
			def cp
			if (hasHibernate) {
				cp = domainClass.constrainedProperties[p.name]?:[:]
				required = (cp ? !(cp.propertyType in [boolean, Boolean]) && !cp.nullable : false)
			}
			p.metaClass.cp = cp
			p.metaClass.required = required
			// Find if property is actually joinTable property. e.g. UserRole
			def joinProperty
			if(p.referencedDomainClass)  {
				def persistentProperties = p.referencedDomainClass.persistentProperties
				joinProperty = persistentProperties.find{
					it != p &&
							persistentProperties.size() == 2 && it.referencedDomainClass
				}
			}
			p.metaClass.joinProperty = joinProperty
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

		Map displayNames = (config.grails.plugin.scaffold.core.displayNames)?:[:]

		Map propDisplayNames = fixDisplayNamesMap(displayNames[domainClass.shortName])

		if(property && propDisplayNames?.containsKey(property.name) ){
			log.debug "Has property display names."
			return propDisplayNames[property.name]
		}else if(!property && propDisplayNames){
			log.debug "Has simple display names."
			return propDisplayNames
		}

		def usedDomainClass = property?.getReferencedDomainClass()
		if(usedDomainClass){
			Map refPropDisplayNames = fixDisplayNamesMap(displayNames[usedDomainClass.shortName])
			if(refPropDisplayNames){
				log.debug "Has referenced property names"
				return refPropDisplayNames
			}
		}

		log.debug "Returning no displaynames"
		return [:]
	}

	private fixDisplayNamesMap(def displayNames){
		//Make all simple string as map with null value
		return displayNames?.collectEntries {
			(it.respondsTo('containsKey'))? it : ["$it":null]
		}
	}
}
