#!/usr/bin/env groovy
// @task
// v4: Download and install documentation templates (arc42, req42, or custom)

import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipInputStream

def docDir = System.getProperty('docDir', '.')
def configFile = System.getProperty('mainConfigFile', 'docToolchainConfig.groovy')
def isHeadless = System.getProperty('DTC_HEADLESS', System.getenv('DTC_HEADLESS') ?: 'false') == 'true'

def scriptDir = System.getProperty('dtc.scriptsHome') ? new File(System.getProperty('dtc.scriptsHome')) : new File(getClass().protectionDomain.codeSource.location.toURI()).parentFile
def gcl = new GroovyClassLoader(this.class.classLoader)
def DtcConfig = gcl.parseClass(new File(scriptDir, 'lib/DtcConfig.groovy'))
gcl.parseClass(new File(scriptDir, 'lib/DtcException.groovy'))
def DtcError = gcl.loadClass('DtcError')
def DtcConfigException = gcl.loadClass('DtcConfigException')
def DtcApiException = gcl.loadClass('DtcApiException')
def dtcConfig = DtcConfig.load(docDir, configFile)
def config = dtcConfig.getRaw()
def inputPath = config.inputPath ?: 'src/docs'

def color = { c, text ->
    def colors = [black: 30, red: 31, green: 32, yellow: 33, blue: 34, magenta: 35, cyan: 36, white: 37]
    return new String((char) 27) + "[${colors[c]}m${text}" + new String((char) 27) + "[0m"
}

// Single top-level handler (ADR-8); body kept un-indented to minimise the diff.
try {
println "docToolchain v4 — downloadTemplate"

def lang = 'EN'
def help = 'plain'
def templates = [
    arc42: { -> "https://github.com/arc42/arc42-template/raw/master/dist/arc42-template-${lang}-${help}-asciidoc.zip" },
    req42: { -> "https://github.com/Hruschka/req42-framework/raw/main/dist/req42-framework-${lang}-${help}-asciidoc.zip" },
]
def languages = [arc42: ['CZ', 'DE', 'EN', 'ES', 'FR', 'IT', 'NL', 'UA'], req42: ['EN', 'DE']]

// Add custom templates from environment
for (int i = 1; i < 20; i++) {
    def tmpl = System.getenv("DTC_TEMPLATE${i}") ?: ""
    if (tmpl) {
        def name = tmpl.replaceAll("^.*/", "").replaceAll(/\.zip$/, "")
        templates[name] = { -> tmpl }
    }
}

// Parse command-line args passed after the task name
def args = this.args as List
def templateArg = args.find { !it.startsWith('-') }
def langArg = args.find { it.startsWith('--lang=') }?.split('=', 2)?.getAt(1)
def helpArg = args.find { it.startsWith('--help=') }?.split('=', 2)?.getAt(1)

def template
if (templateArg) {
    template = templateArg
} else if (isHeadless) {
    template = 'arc42'
    println "${color('green', "Headless mode: using default template 'arc42'.")}"
} else {
    def keys = templates.keySet() as List
    def reader = System.console() ?: new BufferedReader(new InputStreamReader(System.in))
    println "${color('green', 'Which template do you want to install?')}"
    keys.eachWithIndex { name, i -> println "  ${i + 1}) ${name}" }
    print "${color('green', "Choice [1-${keys.size()}] or name (default: 1 = ${keys[0]}): ")}"
    def input = (reader instanceof Console ? reader.readLine() : reader.readLine())?.trim()
    if (!input) {
        template = keys[0]
    } else if (input.isInteger() && (input as int) in (1..keys.size())) {
        template = keys[(input as int) - 1]
    } else {
        template = input
    }
}
if (!templates.containsKey(template)) {
    throw DtcConfigException.newInstance("Unknown template '${template}'. Available: ${templates.keySet().join(', ')}")
}

if (template in ['arc42', 'req42']) {
    println "For more information see https://${template == 'arc42' ? 'arc42.org' : 'req42.de/en'}"

    if (langArg) {
        lang = langArg.toUpperCase()
    } else if (isHeadless) {
        lang = 'EN'
    } else {
        def reader = System.console() ?: new BufferedReader(new InputStreamReader(System.in))
        print "${color('green', "Language [${languages[template].join(',')}] (default: EN): ")}"
        def input = reader instanceof Console ? reader.readLine() : reader.readLine()
        lang = input?.trim()?.toUpperCase() ?: 'EN'
    }

    if (helpArg) {
        help = helpArg
    } else if (isHeadless) {
        help = 'plain'
    } else {
        def reader = System.console() ?: new BufferedReader(new InputStreamReader(System.in))
        print "${color('green', "Help variant [withhelp,plain] (default: plain): ")}"
        def input = reader instanceof Console ? reader.readLine() : reader.readLine()
        help = input?.trim() ?: 'plain'
    }
}

println "${color('green', "Installing ${template} template (${lang}, ${help})...")}"

def url = templates[template].call()
def outputDir = new File(docDir, "${inputPath}/${template}")
outputDir.mkdirs()

def zipFile = new File(outputDir, 'template.zip')
println "Downloading ${url}"
def conn = (HttpURLConnection) new URL(url).openConnection()
conn.connectTimeout = 15000
conn.readTimeout = 60000
conn.instanceFollowRedirects = true
int status = conn.responseCode
if (status != HttpURLConnection.HTTP_OK) {
    throw DtcApiException.newInstance(
        "Download failed: HTTP ${status} for ${url}\n" +
        "The server did not return a template archive. Check the template name and " +
        "language — an unknown language yields a 404 whose HTML error page is not a valid zip.")
}
conn.inputStream.withCloseable { zipFile.bytes = it.bytes }

// Unzip
new ZipInputStream(new FileInputStream(zipFile)).withCloseable { zis ->
    def entry
    def canonicalDest = outputDir.canonicalPath + File.separator
    while ((entry = zis.nextEntry) != null) {
        def target = new File(outputDir, entry.name)
        if (!target.canonicalPath.startsWith(canonicalDest)) {
            throw new SecurityException("Zip entry '${entry.name}' would escape target directory")
        }
        if (entry.isDirectory()) {
            target.mkdirs()
        } else {
            target.parentFile.mkdirs()
            target.bytes = zis.readAllBytes()
        }
    }
}
zipFile.delete()
println "${template} template unpacked into ${outputDir}"

// Reorganize: move images, rename src -> chapters, add jbake headers
new File(outputDir, '../images/.').mkdirs()
def imagesDir = new File(outputDir, 'images')
if (imagesDir.exists()) {
    imagesDir.eachFile { f -> f.renameTo(new File(outputDir, "../images/${f.name}")) }
    imagesDir.deleteDir()
}

def srcDir = new File(outputDir, 'src')
def chaptersDir = new File(outputDir, 'chapters')
if (srcDir.exists()) {
    srcDir.renameTo(chaptersDir)
}

if (chaptersDir.exists()) {
    chaptersDir.eachFileRecurse { file ->
        // Guard hard `:imagesdir:` lines in non-chapter includes (e.g. config.adoc).
        // A hard setting there overrides the imagesdir the build provides, so the
        // combined document and the microsite would resolve images against the
        // wrong folder (./images relative to the page instead of the shared
        // images/ directory). ifndef makes it a fallback for editor preview only.
        if (file.name.endsWith('.adoc') && !(file.name ==~ /[0-9]+_.*/)) {
            def text = file.getText('utf-8')
            def guarded = text.replaceAll(/(?m)^:imagesdir: (.+)$/, 'ifndef::imagesdir[:imagesdir: $1]')
            if (guarded != text) file.write(guarded, 'utf-8')
        }
        if (file.name.endsWith('.adoc') && file.name ==~ /[0-9]+_.*/) {
            def text = file.getText('utf-8')
            def title = ""
            text.eachLine { line ->
                if (title == "" && line.startsWith('=')) {
                    title = line.split("[ \t]+", 2)[1]
                }
            }
            text = text.replaceAll(/ifndef::imagesdir\[:imagesdir: \.\.\/images]/, "")
            text = """\
:jbake-title: ${title}
:jbake-type: page_toc
:jbake-status: published
:jbake-menu: ${template}
:jbake-order: ${file.name.split("_")[0] as Integer}
:filename: ${file.canonicalPath - outputDir.canonicalPath}
ifndef::imagesdir[:imagesdir: ../../images]

:toc:

""" + text
            file.write(text, 'utf-8')
        }
    }
}

// Rename main template file
def mainSource = template == "arc42" ? "${template}-template.adoc" : "${template}-framework.adoc"
def mainFile = new File(outputDir, mainSource)
def targetFile = new File(outputDir, "${template}.adoc")
if (mainFile.exists()) {
    def rawMain = mainFile.text.replaceAll('src/', 'chapters/')
    // Drop the arc42 help-style include: common/styles/ exists only in the
    // arc42-template git repository, not in the distributed asciidoc zip, so the
    // include always resolves to "file not found" (a SEVERE) without it.
    rawMain = rawMain.readLines()
        .findAll { !(it =~ /include::.*common\/styles\/arc42-help-style\.adoc/) }
        .join('\n') + '\n'
    // ifndef so the build (generateSite/HTML/PDF) can set imagesdir to the right
    // folder; the ../images fallback is for editor preview of this file alone.
    def mainText = "ifndef::imagesdir[:imagesdir: ../images]\n:jbake-menu: -\n" + rawMain
    targetFile.write(mainText, 'utf-8')
    if (mainFile != targetFile) mainFile.delete()
}

// Write asciidoctorconfig for editor preview
new File(outputDir, 'chapters/.asciidoctorconfig.adoc').write(':imagesdir: ../../images\n\n', 'utf-8')
new File(outputDir, '.asciidoctorconfig.adoc').write(':imagesdir: ../images\n\n', 'utf-8')

// Update project config file
def configFileObj = new File(docDir, configFile)
def inputFileEntry = "[file: '${template}/${template}.adoc', formats: ['html','pdf']]"
if (configFileObj.exists()) {
    def configText = configFileObj.text
    if (configText.contains('/** inputFiles **/')) {
        // Scaffolded config (from template_config): inject into the markers.
        configText = configText
            .replaceAll('[, \\t\\r\\n]+/[*]{2} inputFiles [*]{2}/',
                ",\n\t${inputFileEntry},\n\t/** inputFiles **/")
            .replaceAll('[, \\t\\r\\n]+/[*]{2} imageDirs [*]{2}/',
                ",\n\t'images/.',\n\t/** imageDirs **/")
            .replaceAll("\\[,", "[")
        configFileObj.write(configText, 'utf-8')
        println "Updated ${configFile}"
    } else if (!(configText =~ /(?m)^\s*inputFiles\s*=/)) {
        // No marker and no inputFiles yet (e.g. an empty 'touch'-ed config):
        // append a working default so generateHTML/PDF/Site run out of the box.
        def addition = new StringBuilder()
        if (configText && !configText.endsWith('\n')) addition << '\n'
        addition << "\ninputFiles = [\n\t${inputFileEntry},\n]\n"
        if (!(configText =~ /(?m)^\s*imageDirs\s*=/)) {
            addition << "\nimageDirs = [\n\t'images/.',\n]\n"
        }
        configFileObj.append(addition.toString(), 'utf-8')
        println "Updated ${configFile} (added inputFiles for ${template})"
    } else {
        // Config already declares inputFiles but has no marker — don't clobber it.
        println "Note: ${configFile} already defines inputFiles; not modifying it."
        println "      Add this entry yourself if needed: ${inputFileEntry}"
    }
}

println ""
println "Use 'generateHTML', 'generatePDF' or 'generateSite' to convert the template."

} catch (Throwable t) {
    System.exit(DtcError.report(t))
}
