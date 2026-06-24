#!/usr/bin/env groovy
// @task
// v4: Generate microsite using MicrositeBaker (a self-contained GSP renderer
// that replaces jBake — and with it OrientDB and the Groovy-3 coupling).

import groovy.io.FileType

def docDir = System.getProperty('docDir', '.')
def configFile = System.getProperty('mainConfigFile', 'docToolchainConfig.groovy')
def dtcHome = System.getProperty('dtc.scriptsHome') ? new File(System.getProperty('dtc.scriptsHome')).parentFile : new File(getClass().protectionDomain.codeSource.location.toURI()).parentFile.parentFile

def scriptDir = System.getProperty('dtc.scriptsHome') ? new File(System.getProperty('dtc.scriptsHome')) : new File(getClass().protectionDomain.codeSource.location.toURI()).parentFile
def DtcConfig = new GroovyClassLoader(this.class.classLoader).parseClass(new File(scriptDir, 'lib/DtcConfig.groovy'))
def dtcConfig = DtcConfig.load(docDir, configFile)
def config = dtcConfig.getRaw()

def inputPath = config.inputPath ?: 'src/docs'
def outputPath = config.outputPath ?: 'build'
def targetDir = new File(docDir, outputPath).absolutePath

println "docToolchain v4 — generateSite"
println "  inputPath:  ${new File(docDir, inputPath).absolutePath}"
println "  outputPath: ${targetDir}/microsite/output"
println ""

// --- Helper closures ---

def color = { c, text ->
    def colors = [black: 30, red: 31, green: 32, yellow: 33, blue: 34, magenta: 35, cyan: 36, white: 37]
    return new String((char) 27) + "[${colors[c]}m${text}" + new String((char) 27) + "[0m"
}

def copyDir = { File from, File to ->
    if (!from.exists()) return
    to.mkdirs()
    from.eachFileRecurse(FileType.FILES) { f ->
        def rel = from.toPath().relativize(f.toPath())
        def target = new File(to, rel.toString())
        target.parentFile.mkdirs()
        target.bytes = f.bytes
    }
}

// --- 1. Prepare temp site directory ---

def tmpDir = new File("${targetDir}/microsite/tmp")
tmpDir.mkdirs()
def siteDir = new File(tmpDir, "site")

// Copy internal theme from dtcHome/src/site
def internalTheme = new File(dtcHome, 'src/site')
if (internalTheme.exists()) {
    println "Copying internal theme from ${internalTheme.absolutePath}"
    copyDir(internalTheme, siteDir)
}

// Download/copy external site theme from DTC_SITETHEME env var
def siteThemeUrl = System.getenv('DTC_SITETHEME') ?: ''
if (siteThemeUrl) {
    def isHeadless = System.getProperty('DTC_HEADLESS', System.getenv('DTC_HEADLESS') ?: 'false') == 'true'
    def themeCacheDir = new File(dtcHome, "themes/${siteThemeUrl.md5()}")
    if (!themeCacheDir.exists()) {
        if (!isHeadless) {
            print "${color('green', "Theme '${siteThemeUrl}' is not cached. Download? [y/n]: ")}"
            def reader = System.console() ?: new BufferedReader(new InputStreamReader(System.in))
            def answer = (reader instanceof Console ? reader.readLine() : reader.readLine())?.trim()
            if (answer?.toLowerCase() != 'y') {
                println "Continuing without external theme."
                siteThemeUrl = ''
            }
        }
        if (siteThemeUrl) {
            println "Downloading site theme from ${siteThemeUrl}"
            themeCacheDir.mkdirs()
            def themeZip = new File(themeCacheDir, 'siteTheme.zip')
            def conn = new URL(siteThemeUrl).openConnection()
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            conn.inputStream.withCloseable { themeZip.bytes = it.bytes }
            new java.util.zip.ZipInputStream(new FileInputStream(themeZip)).withCloseable { zis ->
                def entry
                while ((entry = zis.nextEntry) != null) {
                    def target = new File(themeCacheDir, entry.name)
                    if (!target.canonicalPath.startsWith(themeCacheDir.canonicalPath + File.separator)) {
                        throw new SecurityException("Zip entry '${entry.name}' would escape target directory")
                    }
                    if (entry.isDirectory()) { target.mkdirs() } else { target.parentFile.mkdirs(); target.bytes = zis.readAllBytes() }
                }
            }
            themeZip.delete()
        }
    }
    if (siteThemeUrl && themeCacheDir.exists()) {
        // v3 convention: theme zip contains a site/ directory, extracted into tmp/
        // so site/ contents land at tmp/site/ — copy into siteDir's parent
        println "Applying external site theme"
        copyDir(themeCacheDir, siteDir.parentFile)
    }
}

// Copy project theme if configured
if (config.microsite?.siteFolder) {
    def projectTheme = new File(new File(docDir, inputPath), config.microsite.siteFolder)
    if (projectTheme.exists()) {
        println "Copying project theme from ${projectTheme.absolutePath}"
        copyDir(projectTheme, siteDir)
    }
}

// --- 2. Copy docs ---

def docSrcDir = new File(docDir, inputPath)
def docDestDir = new File(siteDir, "doc")
println "Copying docs from ${docSrcDir.absolutePath}"
copyDir(docSrcDir, docDestDir)

// --- 2b. Run additional format converters (e.g. RST/MD pre-processing) ---
if (config.microsite?.additionalConverters) {
    println "Running additional converters..."
    docDestDir.traverse(type: FileType.FILES) { file ->
        def extension = '.' + file.name.split("[.]")[-1]
        def converter = config.microsite.additionalConverters[extension]
        if (converter) {
            def command = converter.command
            def type = converter.type
            def binding = new Binding([file: file, config: config])
            def shell = new GroovyShell(getClass().getClassLoader(), binding)
            switch (type) {
                case 'groovy':
                    shell.evaluate(command); break
                case 'groovyFile':
                    shell.evaluate(new File(docDir, command).text); break
                case 'bash':
                    if (command == 'dtcw:rstToHtml.py') {
                        command = new File(dtcHome, 'scripts/rstToHtml.py').canonicalPath
                    }
                    def proc = ['bash', '-c', command + ' "' + file + '"'].execute([], new File(docDir))
                    proc.waitFor()
                    if (proc.exitValue()) {
                        throw new Exception("Converter failed for ${file.name}: ${proc.err.text}")
                    }
                    break
            }
        }
    }
}

// --- 3. Fix metadata headers (ported from v3 generateSite.gradle) ---

def parseAsciiDocAttribs = { origText, jbake ->
    def parseAttribs = true
    def text = ""
    def beforeToc = ""
    origText.eachLine { line ->
        if (parseAttribs && line.startsWith(":jbake")) {
            def parsed = (line - ":jbake-").split(": +", 2)
            if (parsed.length == 2) {
                jbake[parsed[0]] = parsed[1]
            } else {
                System.err.println "Warning: malformed jbake attribute (ignored): ${line}"
            }
        } else {
            if (line.startsWith("[")) parseAttribs = false
            text += line + "\n"
            if (line.startsWith(":toc")) beforeToc += line + "\n"
        }
    }
    return [text, beforeToc]
}

def parseOtherAttribs = { origText, jbake ->
    if (origText.contains('~~~~~~')) {
        def parseAttribs = true
        def text = ""
        origText.eachLine { line ->
            if (parseAttribs && line.contains("=")) {
                def parts = (line - "jbake-").split("=", 2)
                jbake[parts[0]] = parts[1]
            } else {
                if (line.startsWith("~~~~~~")) {
                    parseAttribs = false
                } else {
                    text += line + "\n"
                }
            }
        }
        return text
    }
    return origText
}

def renderHeader = { fileName, jbake ->
    def header = ''
    if (fileName.toLowerCase() ==~ '^.*(html|md)$') {
        jbake.each { key, value ->
            if (key == 'order') {
                header += "jbake-${key}=${(value ?: '1') as Integer}\n"
            } else if (key in ['type', 'status']) {
                header += "${key}=${value}\n"
            } else {
                header += "jbake-${key}=${value}\n"
            }
        }
        header += "~~~~~~\n\n"
    } else {
        jbake.each { key, value ->
            if (key == 'order') {
                header += ":jbake-${key}: ${(value ?: '1') as Integer}\n"
            } else {
                header += ":jbake-${key}: ${value}\n"
            }
        }
    }
    return header
}

println "Fixing metadata headers..."
docDestDir.traverse(type: FileType.FILES) { file ->
    if (file.name.toLowerCase() ==~ '^.*(ad|adoc|asciidoc|html|md)$') {
        if (file.name.startsWith("_") || file.name.startsWith(".")) return

        def origText = file.text
        def text = ""
        def jbake = [status: "published", order: -1, type: 'page_toc']
        if (file.name.toLowerCase() ==~ '^.*(md|html)$') jbake.type = 'page'
        def beforeToc = ""

        if (file.name.toLowerCase() ==~ '^.*(ad|adoc|asciidoc)$') {
            (text, beforeToc) = parseAsciiDocAttribs(origText, jbake)
        } else {
            text = parseOtherAttribs(origText, jbake)
        }

        def name = file.canonicalPath - (docDestDir.canonicalPath + File.separator)
        name = name.split("[/\\\\]")

        if (name.size() > 1) {
            if (!jbake.menu) {
                jbake.menu = name[0]
                if (jbake.menu ==~ /[0-9]+[-_].*/) {
                    jbake.menu = jbake.menu.split("[-_]", 2)[1]
                }
            }
            def docname = name[-1]
            if (docname ==~ /[0-9]+[-_].*/) {
                jbake.order = docname.split("[-_]", 2)[0]
                docname = docname.split("[-_]", 2)[1]
            }
            if (name.size() > 2) {
                if ((jbake.order as Integer) == 0) {
                    def secondLevel = name[1]
                    if (secondLevel ==~ /[0-9]+[-_].*/) {
                        jbake.order = secondLevel.split("[-_]", 2)[0]
                    }
                } else if (((jbake.order ?: '1') as Integer) <= 0) {
                    jbake.status = "draft"
                }
            }
            if (jbake.order == -1 && docname.startsWith('index')) {
                jbake.order = -987654321
                jbake.status = "published"
            }
            if (jbake.order == -1 && jbake.type == 'post') {
                jbake.order = 0
                try {
                    jbake.order = Date.parse("yyyy-MM-dd", jbake.date).time / 100000
                } catch (ignored) {}
                jbake.status = "published"
            }

            def leveloffset = 0
            if (file.name.toLowerCase() ==~ '^.*(ad|adoc|asciidoc)$') {
                text.eachLine { line ->
                    if (!jbake.title && line ==~ "^=+ .*") {
                        jbake.title = (line =~ "^=+ (.*)")[0][1]
                        def level = (line =~ "^(=+) .*")[0][1]
                        if (level == "=") leveloffset = 1
                    }
                }
            } else if (file.name.toLowerCase() ==~ '^.*(html)$') {
                text.eachLine { line ->
                    if (!jbake.title && line ==~ "^<h[1-9]>.*</h.*") {
                        jbake.title = (line =~ "^<h[1-9]>(.*)</h.*")[0][1]
                    }
                }
            } else {
                text.eachLine { line ->
                    if (!jbake.title && line ==~ "^#+ .*") {
                        jbake.title = (line =~ "^#+ (.*)")[0][1]
                    }
                }
            }
            if (!jbake.title) jbake.title = docname
            if (leveloffset == 1) {
                text = text.replaceAll("(?ms)^(=+) ", '$1= ')
            }

            if (config.microsite?.customConvention) {
                def binding = new Binding([
                    file: file,
                    sourceFolder: docDestDir,
                    config: config,
                    headers: jbake,
                ])
                new GroovyShell(getClass().getClassLoader(), binding).evaluate(config.microsite.customConvention)
            }

            def header = renderHeader(file.name, jbake)
            if (file.name.toLowerCase() ==~ '^.*(ad|adoc|asciidoc)$') {
                file.write(header + "\nifndef::dtc-magic-toc[]\n:dtc-magic-toc:\n${beforeToc}\n\n:toc: left\n\n++++\n<!-- endtoc -->\n++++\nendif::[]\n" + text, "utf-8")
            } else {
                file.write(header + "\n" + text, "utf-8")
            }
        }
    }
}

// --- 4. Render the microsite (MicrositeBaker — our jBake replacement) ---

println "Rendering microsite..."
def outputDir = new File("${targetDir}/microsite/output")
outputDir.mkdirs()

def micrositeContextPath = config.microsite?.contextPath ?: '/'
if (!micrositeContextPath.endsWith('/')) micrositeContextPath += '/'

// Asciidoctor attributes for body rendering (the same set jBake used).
def asciidoctorAttrs = [
    "sourceDir=${targetDir}",
    'source-highlighter=prettify@',
    "imagesoutDir=${targetDir}/microsite/output/images@",
    "imagesDir=${micrositeContextPath}images@",
    "targetDir=${targetDir}",
    "docDir=${docDir}",
    "projectRootDir=${new File(docDir).canonicalPath}@",
    // Enable STEM (math) so stem:[…]/[latexmath]/[asciimath] are emitted as
    // MathJax delimiters; MathJax itself is bundled locally in the theme footer
    // (SVG output, no CDN). latexmath is the default notation; soft-set (@) so a
    // document can override it.
    'stem=latexmath@',
]*.toString()
if (config.jbake?.asciidoctorAttributes) {
    asciidoctorAttrs.addAll(config.jbake.asciidoctorAttributes*.toString())
}

def Baker = new GroovyClassLoader(this.class.classLoader)
        .parseClass(new File(scriptDir, 'lib/MicrositeBaker.groovy'))
def baker = Baker.newInstance()
baker.siteDir = siteDir
baker.outputDir = outputDir
baker.rawConfig = config
baker.asciidoctorAttributes = asciidoctorAttrs
baker.version = System.getProperty('dtc_version', '4.0')
baker.bake()

// --- 5. Copy images ---

println "Copying images..."
def imageDirs = config.imageDirs ?: ['images']
imageDirs.each { imageDir ->
    def imgSrc = new File(new File(docDir, inputPath), imageDir)
    if (imgSrc.exists()) {
        copyDir(imgSrc, new File(outputDir, 'images'))
    }
}

def resourceDirs = config.resourceDirs ?: []
resourceDirs.each { resource ->
    def resSrc = new File(new File(docDir, inputPath), resource.source)
    if (resSrc.exists()) {
        copyDir(resSrc, new File(outputDir, resource.target))
    }
}

println ""
println "Microsite generated at: ${outputDir.absolutePath}"
if (new File(outputDir, 'index.html').exists()) {
    println "Open ${outputDir.absolutePath}/index.html in your browser."
} else {
    println "No landing page (index.html) was generated."
    println "Configure microsite.landingPage to add one, or browse the pages under ${outputDir.absolutePath}/."
}
