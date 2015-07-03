import grails.build.logging.GrailsConsole
import grails.plugin.scaffold.core.ScaffoldingHelper
import grails.plugin.scaffold.core.TemplatesLocator
import groovy.text.SimpleTemplateEngine

import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.ArrayUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.validation.DomainClassPropertyComparator
import org.codehaus.groovy.runtime.IOGroovyMethods
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.util.Assert
import org.springframework.util.FileCopyUtils
import org.springframework.util.StringUtils

/**
 * implementation of the generator that generates extjs artifacts (controllers, models, store, views etc.)
 * from the domain model.
 *
 * @author Maigo Erit
 */
class CoreTemplateGenerator {

	boolean overwrite
	List ignoreFileNames
	List ignoreDomainNames
	boolean ignoreStatic

	static String DEFAULT_URL = 'http://localhost:8080/'
	static String APP_URL

	static String APPLICATION_DIR = ""
	static String SCAFFOLD_DIR = "/src/templates/scaffold/"

	static String DYNAMIC_FILE_PATTERN = /__[^__, ^\/]+__/
	static String PARTIAL_FILE_PATTERN = /__[^__, ^\/]+\.[^\/]+$/

	protected SimpleTemplateEngine engine = new SimpleTemplateEngine()

	GrailsApplication grailsApplication
	GrailsPluginManager pluginManager
	ConfigObject pluginConfig

	def excludedDomainClasses

	ScaffoldingHelper scaffoldingHelper

	enum ScaffoldType {
		DYNAMIC,
		STATIC
	}
	Map dynamicFoldersConf =
			[
					"__propertyName__": { it.propertyName },
					"__shortName__"   : { it.shortName },
					"__packageName__" : { it.packageName.replace(".", "/") }
			]


	CoreTemplateGenerator(ClassLoader classLoader, TemplatesLocator templatesLocator,
			GrailsApplication grailsApplication, GrailsPluginManager pluginManager) {

		pluginConfig = grailsApplication.config.grails.plugin.scaffold.core
		APP_URL = pluginConfig.appUrl ?: ''
		overwrite = pluginConfig.overwrite != null ? pluginConfig.overwrite : true
		ignoreFileNames = pluginConfig.ignoreFileNames ?: []
		ignoreDomainNames = pluginConfig.ignoreDomainNames ?: []
		ignoreStatic = pluginConfig.ignoreStatic ?: false

		engine = new SimpleTemplateEngine(classLoader)
		this.grailsApplication = grailsApplication
		this.pluginManager = pluginManager
		excludedDomainClasses = getExcludedDomainClasses()

		APPLICATION_DIR = new File("").absolutePath
		scaffoldingHelper = new ScaffoldingHelper(pluginManager, DomainClassPropertyComparator,
				getClass().classLoader, grailsApplication.config)
	}

	void generateScaffold(String applicationDir, Boolean containsSubDir, String domainClassName) throws IOException {
		println "Using templates dir: ${applicationDir}"
		Map scaffoldDirs = pluginConfig.folders
		println "Using scaffold dirs from config:$scaffoldDirs"
		for (Resource resource : gatherResources(applicationDir, containsSubDir)) {
			generateFile(resource, domainClassName)
		}
	}

	void generateFile(Resource resource, String domainClassName = '*') {
		String filePath = resource.file.path
		String templatesDir = extractPluginDir(filePath) + SCAFFOLD_DIR
		Path relativeFilePath = Paths.get(templatesDir).relativize(Paths.get(filePath))

		if (!resource.isReadable()) {
			log.debug "Resource is not readable: $relativeFilePath"
			return
		}

		println "Generating file: $relativeFilePath"
		Path fileRealPath = (relativeFilePath.nameCount > 2) ? relativeFilePath.subpath(2, relativeFilePath.nameCount) : relativeFilePath

		Map scaffoldDirs = pluginConfig.folders
		log.info "Using scaffold dirs from config:$scaffoldDirs"
		// locate files output directory
		String scaffoldDir = relativeFilePath.subpath(0, 1).toString()

		if (!scaffoldDirs.containsKey(scaffoldDir) || !scaffoldDirs[scaffoldDir]) {
			println "Dir $scaffoldDir not in config grails.plugin.scaffold.core.folders: $scaffoldDirs. Skipping file."
			return
		}
		String outputFileName = scaffoldDirs[scaffoldDir] + fileRealPath

		ScaffoldType scaffoldType = relativeFilePath.subpath(1, 2).toString().toUpperCase() as ScaffoldType
		if (scaffoldType == ScaffoldType.STATIC) {
			File destFile = new File(APPLICATION_DIR, outputFileName)
			if (canWrite(destFile)) {
				destFile.getParentFile().mkdirs()
				FileCopyUtils.copy(resource.inputStream, new FileOutputStream(destFile))
			}
		} else if (scaffoldType == ScaffoldType.DYNAMIC) {
			boolean generateForEachDomain = outputFileName.find(~DYNAMIC_FILE_PATTERN)
			boolean generatePartialFile = outputFileName.find(~PARTIAL_FILE_PATTERN)
			if (generateForEachDomain) {
                for (GrailsDomainClass domainClass : getExcludedDomainClasses()) {
                    if("*".equals(domainClassName) || domainClass.clazz.simpleName.equals(domainClassName)){
                        String parsedOutputFileName = outputFileName
                        outputFileName.findAll(~DYNAMIC_FILE_PATTERN).each {
                            Closure parse = dynamicFoldersConf[it]
                            parsedOutputFileName = parsedOutputFileName.replace(it, parse(domainClass))
                        }
                        createFileFromTemplate(APPLICATION_DIR, parsedOutputFileName, resource, domainClass)
                    }
                }
			} else if (generatePartialFile) {
				String parsedOutputFileName = outputFileName.replace("__", "")
				createFileFromPartial(APPLICATION_DIR, parsedOutputFileName, resource)
			} else {
				createFileFromTemplate(APPLICATION_DIR, outputFileName, resource, null)
			}
		}
	}

	private String extractPluginDir(String filePath) {
		String unixFilePath = FilenameUtils.separatorsToUnix(filePath)
		String unixScaffoldDirReqEx = FilenameUtils.separatorsToUnix(SCAFFOLD_DIR) + ".*"
		String pluginDir = unixFilePath.replaceAll(unixScaffoldDirReqEx, "")
		return pluginDir
	}

	void createFileFromTemplate(String destDir, String fileName, Resource templateFile, GrailsDomainClass domainClass) throws IOException {
		Assert.hasText(destDir, "Argument [destdir] not specified")

		File destFile = new File(destDir, fileName)

		if (canWrite(destFile)) {
			log.info "Writing file $fileName"
			destFile.parentFile.mkdirs()
			BufferedWriter writer

			try {
				writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(destFile), "UTF8"))
				addBindingAndCreateFile(writer, templateFile, domainClass)

				try {
					writer.flush()
				} catch (IOException ignored) {
				}
			}
			finally {
				IOGroovyMethods.closeQuietly(writer)
			}
		}
	}

	void createFileFromPartial(String destDir, String fileName, Resource templateFile) throws IOException {
		Assert.hasText(destDir, "Argument [destdir] not specified")

		File destFile = new File(destDir, fileName)

		if (canWrite(destFile)) {
			log.info "Writing file from partial $fileName"
			GroovyShell groovyShell = new GroovyShell()
			String tmpl = templateFile.file.text.trim()
			if (tmpl.startsWith("[") && tmpl.endsWith("]")) {
				Map regExClosures = groovyShell.evaluate(tmpl)
				regExClosures.each { regEx, closure ->
					closure.delegate = this
					String linesToAdd = closure(destFile)
					if (linesToAdd) {
						CharsetToolkit toolkit = new CharsetToolkit(destFile)
						// guess the encoding
						Charset guessedCharset = toolkit.getCharset()
						destFile.write(destFile.getText(guessedCharset.toString()).replaceFirst(regEx) {
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
		String templateText = getTemplateTextFromResource(templateFile)
		if (!StringUtils.hasLength(templateText)) {
			log.error "No lenght for template file: ${templateFile.file.name}."
			return
		}

		/*GrailsDomainClassProperty multiPart = null;
		for (GrailsDomainClassProperty property : domainClass.getProperties()) {
			if (property.getType() == Byte[].class || property.getType() == byte[].class) {
				multiPart = property;
				break;
			}
		}*/

		def config = grailsApplication.config
		def excludedDomainClasses = getExcludedDomainClasses()

		String defaultUrl = config.grails.serverURL ?: DEFAULT_URL + grailsApplication.metadata['app.name']

		Map<String, Object> binding = [
			pluginManager: pluginManager,
			comparator: DomainClassPropertyComparator,
			config: config,
			domainClasses: excludedDomainClasses,
			allDomainClasses: grailsApplication.domainClasses,
			scaffoldingHelper: scaffoldingHelper,
			appName: grailsApplication.metadata.getApplicationName().capitalize().replace(" ", ""),
			grailsApplication: grailsApplication,
			appUrl: APP_URL ?: defaultUrl
		]
		if (domainClass) {
			binding.domainClass = domainClass
			binding.packageName = domainClass.packageName
			//binding.multiPart = multiPart
			binding.className = domainClass.shortName
			binding.propertyName = domainClass.propertyName
		}

		generate(templateText, binding, out)
	}

	protected getExcludedDomainClasses(){
		return grailsApplication.domainClasses.grep{!ignoreDomainNames.contains(it.name)}
	}

	protected void generate(String templateText, Map<String, Object> binding, Writer out) {
		try {
			engine.createTemplate(templateText).make(binding).writeTo(out)
		}
		catch (ClassNotFoundException | IOException e) {
			throw new RuntimeException(e)
		} catch (ex) {
			if (ex.getClass().simpleName == "ConstraintHandlerException") {
				log.error "If ConstraintHandlerException: Could not bootstrap application. There is a constraint error." +
						"Fix it in TestDataConfig.groovy and then run again 'grails createDemo'.\n", ex
			} else {
				log.error "Unknown error for scaffold plugin. Continuing....", ex
			}
		}
	}

	protected boolean canWrite(File testFile) {
		if (overwrite || !testFile.exists()) {
			return true
		}

		try {
			String relative = testFile.absolutePath
			String response = GrailsConsole.instance.userInput("File $relative already exists. Overwrite?", ["y", "n", "a"])
			overwrite = overwrite || "a".equals(response)
			return overwrite || "y".equals(response)
		}
		catch (e) {
			// failure to read from standard in means we're probably running from an automation tool like a build server
			return true
		}
	}

	private boolean templatesExists(String templatesDir) {
		return new FileSystemResource(templatesDir).exists()
	}

	private Resource[] gatherResources(String templatesDir, Boolean containsSubDir) {
		// Add trailing / to folder path
		templatesDir = Paths.get(templatesDir).toString()

		Resource[] resources = []

		Resource templatesResource = new FileSystemResource(templatesDir)
		if (templatesResource.exists()) {
			try {
				String staticFolderName = ScaffoldType.STATIC.name().toLowerCase()
				String dynamicFolderName = ScaffoldType.DYNAMIC.name().toLowerCase()

				Resource[] dynamicResources = getResources(templatesDir, containsSubDir, dynamicFolderName)
				resources = ArrayUtils.addAll(resources, dynamicResources)
				if (!ignoreStatic) {
					Resource[] staticResources = getResources(templatesDir, containsSubDir, staticFolderName)
					resources = ArrayUtils.addAll(resources, staticResources)
				}
			} catch (e) {
				log.error("Error while loading assets from " + templatesDir, e)
			}
		} else {
			log.info "Templates dir $templatesDir does not exist."
		}

		return resources.grep{!ignoreFileNames.contains(it.filename)}
	}

    private Resource[] getResources(String templatesDir, Boolean containsSubDir, folderName){
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()
        String path = "file:" + templatesDir + (containsSubDir ? "/" : "/*/") + folderName + "/**/*"
        return resolver.getResources(path)
    }

	protected String getTemplateTextFromResource(Resource templateFile) throws IOException {
		InputStream inputStream = templateFile.inputStream

		return inputStream == null ? null : IOGroovyMethods.getText(inputStream)
	}

}
