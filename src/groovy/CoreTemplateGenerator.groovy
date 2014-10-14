import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import groovy.text.SimpleTemplateEngine
import groovy.text.Template

import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.codehaus.groovy.grails.scaffolding.AbstractGrailsTemplateGenerator
import org.codehaus.groovy.grails.scaffolding.AbstractGrailsTemplateGenerator.GrailsControllerType;
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.util.Assert
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.springframework.util.StringUtils;

import groovy.io.FileType
import grails.plugin.scaffold.core.TemplatesLocator
import grails.util.Holders
import java.nio.file.Paths
import org.springframework.util.FileCopyUtils
import groovy.util.CharsetToolkit

/**
 * implementation of the generator that generates extjs artifacts (controllers, models, store, views etc.)
 * from the domain model.
 *
 * @author Maigo Erit
 */
class CoreTemplateGenerator extends AbstractGrailsTemplateGenerator {

	static EXTJS_DIR = Holders.config.grails.plugin.extjsscaffolding.exportLocation?:"web-app/extapp/"
	boolean shouldOverwrite = Holders.config.grails.plugin.extjsscaffolding.overwrite?:true
	static APP_URL = 'http://localhost:8080/'
	
	static SCAFFOLDING_ASSETS_DIR = "assets/"
	static SCAFFOLDING_APPLICATION_DIR = "application/"
	static SCAFFOLDING_DOMAIN_DIR = "domain/"
	
	protected Template gridRenderEditorTemplate;
	protected Template detailViewRenderEditorTemplate;
	
	TemplatesLocator templatesLocator

	CoreTemplateGenerator(ClassLoader classLoader) {
		super(classLoader)
		setOverwrite(shouldOverwrite)
	}
	
	
	public void generateAssets(String destDir) throws IOException {
		Assert.hasText(destDir, "Argument [destdir] not specified");

		Resource[] resources
		String templatesDirPath
		(resources, templatesDirPath) = gatherResources(SCAFFOLDING_ASSETS_DIR)

		for (Resource resource : resources) {
			Path dirPath = Paths.get(templatesDirPath);
			Path filePath = Paths.get(resource.file.path);
			Path relativeFilePath = dirPath.relativize(filePath);
			Path relativeFilePathWithoutStaticDir = relativeFilePath.subpath(1, relativeFilePath.nameCount)

			String fileName = EXTJS_DIR + relativeFilePathWithoutStaticDir

			File destFile = new File(destDir, fileName);
			if (canWrite(destFile)) {
				destFile.getParentFile().mkdirs();
				FileCopyUtils.copy(resource.inputStream, new FileOutputStream(destFile))
			}
			
		}
	}
	
	
	
	public void generateApplication(String destDir) throws IOException {
		Assert.hasText(destDir, "Argument [destdir] not specified");

		Resource[] resources
		String templatesDirPath
		(resources, templatesDirPath) = gatherResources(SCAFFOLDING_APPLICATION_DIR)

		for (Resource resource : resources) {
			Path dirPath = Paths.get(templatesDirPath);
			Path filePath = Paths.get(resource.file.path);
			Path relativeFilePath = dirPath.relativize(filePath);
			Path relativeFilePathWithoutStaticDir = relativeFilePath.subpath(1, relativeFilePath.nameCount)

			String fileName = EXTJS_DIR + relativeFilePathWithoutStaticDir
			createApplicationFileFromTemplate(destDir, fileName, resource)
		}
	}
	
	public void createApplicationFileFromTemplate(String destDir, String fileName, Resource templateFile) throws IOException {
		Assert.hasText(destDir, "Argument [destdir] not specified");

		File destFile = new File(destDir, fileName);
		if (canWrite(destFile)) {
			destFile.getParentFile().mkdirs();
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(destFile));
				addBindingForApplicationAndCreateFile(writer, templateFile);

				try {
					writer.flush();
				} catch (IOException ignored) {}
			}
			finally {
				IOGroovyMethods.closeQuietly(writer);
			}
			log.info("Static generated at [" + destFile + "]");
		}
	}

	protected void addBindingForApplicationAndCreateFile(Writer out, Resource templateFile) throws IOException {
		String templateText = getTemplateTextFromResource(templateFile);
		if (!StringUtils.hasLength(templateText)) {
			log.error "No lenght for template file."
			return;
		}
		
		def config = grailsApplication.config
		
		Map<String, Object> binding = new HashMap<String, Object>();
		
		binding.put("appName", grailsApplication.metadata['app.name'].capitalize().replace(" ", ""));
		binding.put("config", config);
		def domainClasses = grailsApplication.domainClasses
		binding.put("domainClasses", domainClasses);
		String appUrl = (config.grails.serverURL)?:APP_URL + grailsApplication.metadata['app.name']
		binding.put("appUrl", Holders.config.grails.plugin.extjsscaffolding.appUrl?:appUrl)

		generate(templateText, binding, out);
	}
	
	
	
	
	
	public void generateDomain(GrailsDomainClass domainClass, String destDir) throws IOException {
		Assert.hasText(destDir, "Argument [destdir] not specified");
	
		Resource[] resources
		String templatesDirPath
		(resources, templatesDirPath) = gatherResources(SCAFFOLDING_DOMAIN_DIR)

		for (Resource resource : resources) {
			Path dirPath = Paths.get(templatesDirPath);
			Path filePath = Paths.get(resource.file.path);
			Path relativeFilePath = dirPath.relativize(filePath);
			Path relativeFilePathWithoutStaticDir = relativeFilePath.subpath(1, relativeFilePath.nameCount)
			

			String fileName = EXTJS_DIR + relativeFilePathWithoutStaticDir
			fileName = fileName.replace("__propertyName__", domainClass.propertyName)
			fileName = fileName.replace("__shortName__", domainClass.shortName)
			createDomainFileFromTemplate(destDir, fileName, resource, domainClass)
		}
	}
	
	public void createDomainFileFromTemplate(String destDir, String fileName, Resource templateFile, GrailsDomainClass domainClass) throws IOException {
		Assert.hasText(destDir, "Argument [destdir] not specified");

		File destFile = new File(destDir, fileName);
		if (canWrite(destFile)) {
			destFile.getParentFile().mkdirs();
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(destFile));
				addBindingForDomainAndCreateFile(writer, templateFile, domainClass);

				try {
					writer.flush();
				} catch (IOException ignored) {}
			}
			finally {
				IOGroovyMethods.closeQuietly(writer);
			}
			log.info("Static generated at [" + destFile + "]");
		}
	}
	
	protected void addBindingForDomainAndCreateFile(Writer out, Resource templateFile, GrailsDomainClass domainClass) throws IOException {
		String templateText = getTemplateTextFromResource(templateFile);
		if (!StringUtils.hasLength(templateText)) {
			log.error "No lenght for template file."
			return;
		}

		GrailsDomainClassProperty multiPart = null;
		for (GrailsDomainClassProperty property : domainClass.getProperties()) {
			if (property.getType() == Byte[].class || property.getType() == byte[].class) {
				multiPart = property;
				break;
			}
		}

		String packageName = StringUtils.hasLength(domainClass.getPackageName()) ? "<%@ page import=\"" + domainClass.getFullName() + "\" %>" : "";
		Map<String, Object> binding = createBinding(domainClass);
		binding.put("packageName", packageName);
		binding.put("multiPart", multiPart);
		def config = grailsApplication.config
		binding.put("config", config);
		def domainClasses = grailsApplication.domainClasses
		binding.put("domainClasses", domainClasses);
		binding.put("propertyName", getPropertyName(domainClass));
		binding.put("appName", grailsApplication.metadata['app.name'].capitalize().replace(" ", ""));
		String appUrl = (config.grails.serverURL)?:APP_URL + grailsApplication.metadata['app.name']
		binding.put("appUrl", Holders.config.grails.plugin.extjsscaffolding.appUrl?:appUrl)
		
		generate(templateText, binding, out);
	}
	
	
	private gatherResources(String dir){
		Resource[] resources = []
		String templatesDirPath
		if (resourceLoader != null && grailsApplication.isWarDeployed()) {
			templatesDirPath = "/WEB-INF/templates/scaffolding/"+dir
			try {
				PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
				resources = resolver.getResources(templatesDirPath + dir + "**/*.*")
			}catch (Exception e) {
				log.error("Error while loading assets from " + templatesDirPath, e);
			}
		}else {
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

			templatesDirPath = basedir + "/src/templates/scaffolding/"+dir
			Resource templatesDir = new FileSystemResource(templatesDirPath + dir + "**/*.*");
			if (templatesDir.exists()) {
				try {
					resources = resolver.getResources(templatesDirPath);
				}catch (Exception e) {
					log.error("Error while loading assets from " + basedir, e);
				}
			}
			if(!resources) {
				File pluginDir = templatesLocator.getPluginDir();
				try {
					templatesDirPath = pluginDir.path + "/src/templates/scaffolding/"
					resources = resolver.getResources("file:" + templatesDirPath + dir + "**/*.*");
				} catch (Exception e) {
					// ignore
					log.error("Error locating assets from " + pluginDir + ": " + e.getMessage(), e);
				}
			}
		}
		return [resources, templatesDirPath]
	}
	

	def renderEditor = { GrailsDomainClassProperty property, boolean isDetailView = false ->
		def domainClass = property.domainClass
		def cp
		boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate') || pluginManager?.hasGrailsPlugin('hibernate4')
		if (hasHibernate) {
			cp = domainClass.constrainedProperties[property.name]
		}
		File pluginDir = templatesLocator.getPluginDir();
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		try {
			String templatesDirPath = pluginDir.path + "/src/templates/scaffolding/"
			//TODO: Add something more dynamic 
			Resource r1 = resolver.getResources("file:" + templatesDirPath + "gridEditor.template")[0];
			Resource r2 = resolver.getResources("file:" + templatesDirPath + "detailViewEditor.template")[0];
		
			if (!gridRenderEditorTemplate) {
				// create template once for performance
				gridRenderEditorTemplate = engine.createTemplate(getTemplateTextFromResource(r1))
			}
			if (!detailViewRenderEditorTemplate) {
				// create template once for performance
				detailViewRenderEditorTemplate = engine.createTemplate(getTemplateTextFromResource(r2))
			}
		} catch (Exception e) {
			// ignore
			log.error("Error locating assets from " + pluginDir + ": " + e.getMessage(), e);
		}

		def binding = [
			pluginManager: pluginManager,
			property: property,
			domainClass: domainClass,
			cp: cp,
			domainInstance:getPropertyName(domainClass)]
		if(isDetailView) return detailViewRenderEditorTemplate.make(binding).toString()
		else return gridRenderEditorTemplate.make(binding).toString()
	}

	
	protected String getTemplateTextFromResource(Resource templateFile) throws IOException {
		InputStream inputStream = templateFile.getInputStream();
		
		return inputStream == null ? null : IOGroovyMethods.getText(inputStream);
	}
	
	
	public void addAnnotation(GrailsDomainClass domainClass) throws IOException {

		if (domainClass == null) {
			return;
		}

		String fullName = domainClass.getFullName();
		
		String pathName = fullName.replace(".", File.separator)
		String domainClassFilePath = "grails-app/domain/" + pathName+ ".groovy"
		
		File destFile = new File(domainClassFilePath);
		if (canWrite(destFile) && !destFile.text.contains("@Resource")) {

			CharsetToolkit toolkit = new CharsetToolkit(destFile);
			// guess the encoding
			Charset guessedCharset = toolkit.getCharset();
			
			//lowercase plural domain name as url
			String restName = domainClass.getShortName().toLowerCase() + "s"
			
			String linesToAdd = "import grails.rest.Resource\n\n"
			linesToAdd += "@Resource(uri = '/$restName', formats = ['json'], superClass = extjsScaffoldingPlugin.CustomRestfulController)\n"
			
			//Add annotation line without modifying file encoding
			destFile.write(destFile.getText(guessedCharset.toString()).replaceFirst(/(.*class .+\{)/){
				linesToAdd + it[0]
			}, guessedCharset.toString())
						
			log.info("Annotation added to [" + destFile + "]");
		}
	}
	public void addUrlMappings() throws IOException {
		String classFilePath = "grails-app/conf/UrlMappings.groovy"
		
		File destFile = new File(classFilePath);
		if (canWrite(destFile)){
			def domainClasses = grailsApplication.domainClasses
			String linesToAdd = ""
			domainClasses.each{domainClass->
				String shortName = domainClass.getShortName();
				String shortNameLower = shortName.toLowerCase();
				String line = "'/${shortNameLower}s'(resources:'$shortName')"

				if(!destFile.text.contains(line)) {
					linesToAdd += "\t\t" + line +"\n"
				}
			}
			
			//Write lines to file
			if(linesToAdd) {
				CharsetToolkit toolkit = new CharsetToolkit(destFile);
				// guess the encoding
				Charset guessedCharset = toolkit.getCharset();
				destFile.write(destFile.getText(guessedCharset.toString()).replaceFirst(/(.*mappings\s*\=\s*\{)/){ 
					it[0] + "\n\n" + linesToAdd 
				}, guessedCharset.toString())
							
			}
		}
	}
	
	
	@Override
	public void generateViews(GrailsDomainClass domainClass, String destDir) throws IOException {
		
	}

	@Override
	public void generateView(GrailsDomainClass domainClass, String viewName, Writer out) throws IOException {
	}

	@Override
	public void generateView(GrailsDomainClass domainClass, String viewName, String destDir) throws IOException {
	
	}
	


	@Override
	public void generateAsyncController(GrailsDomainClass domainClass, String destDir) throws IOException {

	}

}