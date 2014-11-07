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

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
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
import org.codehaus.groovy.grails.validation.DomainClassPropertyComparator;
import grails.build.logging.GrailsConsole;

/**
 * implementation of the generator that generates extjs artifacts (controllers, models, store, views etc.)
 * from the domain model.
 *
 * @author Maigo Erit
 */
class CoreTemplateGenerator {

	boolean overwrite = Holders.config.grails.plugin.scaffold.core.overwrite?:true
	static String DEFAULT_URL = 'http://localhost:8080/'
	static String APP_URL = (Holders.config.grails.plugin.scaffold.core.appUrl)?:''
	
	static String APPLICATION_DIR = ""
	static String SCAFFOLD_DIR = "/src/templates/scaffold/"
	static String TEMPLATES_DIR = ""

	static String DYNAMIC_FILE_PATTERN = /__[^__, ^\/]+__/
	static String PARTIAL_FILE_PATTERN = /__[^__, ^\/]+\.[^\/]+$/
	
	protected SimpleTemplateEngine engine = new SimpleTemplateEngine();
	
	GrailsApplication grailsApplication
	GrailsPluginManager pluginManager
	
	enum ScaffoldType{
		DYNAMIC,
		STATIC
	}
	Map dynamicFoldersConf = 
		[
			"__propertyName__" : {it.propertyName},
			"__shortName__": {it.shortName},
			"__packageName__": {it.packageName.replace(".", "/")}
		]
	
	
	CoreTemplateGenerator(ClassLoader classLoader, TemplatesLocator templatesLocator) {
		engine = new SimpleTemplateEngine(classLoader);
		APPLICATION_DIR = new File("").absolutePath
		TEMPLATES_DIR = APPLICATION_DIR + SCAFFOLD_DIR
		//Check if has templates in application, if not use plugins templates
		if(!templatesExists(TEMPLATES_DIR)) TEMPLATES_DIR = templatesLocator.getPluginDir().path + SCAFFOLD_DIR
	}
	
	public void generateScaffold(String applicationDir) throws IOException
	{	
		log.info "Using templates dir: ${TEMPLATES_DIR+applicationDir}"
		
		for (Resource resource : gatherResources(TEMPLATES_DIR + applicationDir))
		{
			generateFile(resource)
		}
	}
	
	public void generateFile(Resource resource){
		Path relativeFilePath = Paths.get(TEMPLATES_DIR).relativize(Paths.get(resource.file.path));
		if(!resource.isReadable())
		{
			 log.debug "Resource is not readable: $relativeFilePath"
			 return
		}
		
		Path fileRealPath = relativeFilePath.subpath(2, relativeFilePath.nameCount)
		
		Map scaffoldDirs = Holders.config.grails.plugin.scaffold.core.folders
		log.info "Using scaffold dirs from config:$scaffoldDirs"
		// locate files output directory
		String scaffoldDir = relativeFilePath.subpath(0, 1).toString()
		
		if(!scaffoldDirs.containsKey(scaffoldDir)) log.error "Dir $scaffoldDir not in config grails.plugin.scaffold.core.folders"
		String outputFileName = scaffoldDirs[scaffoldDir] + fileRealPath
	
		ScaffoldType scaffoldType = relativeFilePath.subpath(1, 2).toString().toUpperCase() as ScaffoldType
		if(scaffoldType == ScaffoldType.STATIC)
		{
			 File destFile = new File(APPLICATION_DIR, outputFileName);
			 if (canWrite(destFile))
			 {
				destFile.getParentFile().mkdirs();
				FileCopyUtils.copy(resource.inputStream, new FileOutputStream(destFile))
			 }
		}
		else if(scaffoldType == ScaffoldType.DYNAMIC)
		{
			boolean generateForEachDomain = outputFileName.find(~DYNAMIC_FILE_PATTERN)
			boolean generatePartialFile = outputFileName.find(~PARTIAL_FILE_PATTERN)
			if(generateForEachDomain)
			{
				for (GrailsDomainClass domainClass : grailsApplication.domainClasses)
				{
					String parsedOutputFileName = outputFileName
					outputFileName.findAll(~DYNAMIC_FILE_PATTERN).each{
						Closure parse = dynamicFoldersConf[it]
						parsedOutputFileName = parsedOutputFileName.replace(it, parse(domainClass))
					}
					createFileFromTemplate(APPLICATION_DIR, parsedOutputFileName, resource, domainClass)
				}
			}
			else if(generatePartialFile)
			{
				String parsedOutputFileName = outputFileName.replace("__", "")
				createFileFromPartial(APPLICATION_DIR, parsedOutputFileName, resource)
			}
			else
			{
				createFileFromTemplate(APPLICATION_DIR, outputFileName, resource, null)
			}
		}
	}
	
	
	public void createFileFromTemplate(String destDir, String fileName, Resource templateFile, GrailsDomainClass domainClass) throws IOException {
		Assert.hasText(destDir, "Argument [destdir] not specified");

		File destFile = new File(destDir, fileName);
		
		if (canWrite(destFile)) {
			log.info "Writing file $fileName"
			destFile.getParentFile().mkdirs();
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(destFile));
				addBindingAndCreateFile(writer, templateFile, domainClass);

				try {
					writer.flush();
				} catch (IOException ignored) {}
			}
			finally {
				IOGroovyMethods.closeQuietly(writer);
			}
		}
	}
	
	public void createFileFromPartial(String destDir, String fileName, Resource templateFile) throws IOException {
		Assert.hasText(destDir, "Argument [destdir] not specified");

		File destFile = new File(destDir, fileName);
		
		if (canWrite(destFile)) {
			log.info "Writing file from partial $fileName"
			GroovyShell groovyShell = new GroovyShell()
			String tmpl = templateFile.file.text.trim()
			if(tmpl.startsWith("[") && tmpl.endsWith("]")){
				Map regExClosures = groovyShell.evaluate(tmpl)
				regExClosures.each{regEx, closure->
					closure.delegate = this
					String linesToAdd = closure(destFile)
					if(linesToAdd) {
						CharsetToolkit toolkit = new CharsetToolkit(destFile);
						// guess the encoding
						Charset guessedCharset = toolkit.getCharset();
						destFile.write(destFile.getText(guessedCharset.toString()).replaceFirst(regEx){
							it[0] + "\n\n" + linesToAdd
						}, guessedCharset.toString())
					}
				}
				log.info "Partial file appended to $destFile"
			} else {
				log.error "This is not a partial file. Map is missing from file"
			}
			
			
			
		
			
		}
	}
	
	protected void addBindingAndCreateFile(Writer out, Resource templateFile, GrailsDomainClass domainClass) throws IOException {
		String templateText = getTemplateTextFromResource(templateFile);
		if (!StringUtils.hasLength(templateText)) {
			log.error "No lenght for template file."
			return;
		}

		/*GrailsDomainClassProperty multiPart = null;
		for (GrailsDomainClassProperty property : domainClass.getProperties()) {
			if (property.getType() == Byte[].class || property.getType() == byte[].class) {
				multiPart = property;
				break;
			}
		}*/

		def config = grailsApplication.config
		def domainClasses = grailsApplication.domainClasses
		String defaultUrl = (config.grails.serverURL)?:DEFAULT_URL + grailsApplication.metadata['app.name']
		
		Map<String, Object> binding = new HashMap<String, Object>()
		binding.put("pluginManager", pluginManager)
		binding.put("comparator", DomainClassPropertyComparator.class);
		binding.put("config", config)
		binding.put("domainClasses", domainClasses)
		binding.put("appName", grailsApplication.metadata['app.name'].capitalize().replace(" ", ""))
		binding.put("appUrl", (APP_URL)?:defaultUrl)
		if(domainClass)
		{
			binding.put("domainClass", domainClass)
			binding.put("packageName", domainClass.packageName)
			//binding.put("multiPart", multiPart)
			binding.put("className", domainClass.shortName)
			binding.put("propertyName", domainClass.propertyName)
			
		}
		
		generate(templateText, binding, out);
	}
	
	protected void generate(String templateText, Map<String, Object> binding, Writer out) {
		try {
			engine.createTemplate(templateText).make(binding).writeTo(out);
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected boolean canWrite(File testFile) {
		if (overwrite || !testFile.exists()) {
			return true;
		}

		try {
			String relative = testFile.absolutePath;
			String response = GrailsConsole.getInstance().userInput("File " + relative + " already exists. Overwrite?", ["y", "n", "a"])
			overwrite = overwrite || "a".equals(response);
			return overwrite || "y".equals(response);
		}
		catch (Exception e) {
			// failure to read from standard in means we're probably running from an automation tool like a build server
			return true;
		}
	}


	
	
	private boolean templatesExists(String templatesDir)
	{
		Resource templatesResource = new FileSystemResource(templatesDir);
		return templatesResource.exists()
	}
	
	private Resource[] gatherResources(String templatesDir)
	{
		// Add trailing / to folder path if does not exsits
		if(templatesDir.charAt(templatesDir.length()-1) != File.separatorChar){
			templatesDir += File.separator;
		}
		Resource[] resources = []
		
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource templatesResource = new FileSystemResource(templatesDir );
		if (templatesResource.exists()) {
			try {
				resources = resolver.getResources("file:" + templatesDir + "**/*");
			}catch (Exception e) {
				log.error("Error while loading assets from " + templatesDir, e);
			}
		}else{
			log.info "Templates dir $templatesDir does not exists."
		}
				
		return resources
	}
	
	protected String getTemplateTextFromResource(Resource templateFile) throws IOException {
		InputStream inputStream = templateFile.getInputStream();
		
		return inputStream == null ? null : IOGroovyMethods.getText(inputStream);
	}
	
}