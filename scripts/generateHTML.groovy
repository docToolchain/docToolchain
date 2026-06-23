#!/usr/bin/env groovy
// @task
// v4: Generate HTML5 from AsciiDoc sources using AsciidoctorJ (no external Ruby needed)

import org.asciidoctor.Asciidoctor
import org.asciidoctor.Options
import org.asciidoctor.Attributes
import org.asciidoctor.SafeMode

def docDir = System.getProperty('docDir', '.')
def configFile = System.getProperty('mainConfigFile', 'docToolchainConfig.groovy')

def scriptDir = System.getProperty('dtc.scriptsHome') ? new File(System.getProperty('dtc.scriptsHome')) : new File(getClass().protectionDomain.codeSource.location.toURI()).parentFile
def gcl = new GroovyClassLoader(this.class.classLoader)
def DtcConfig = gcl.parseClass(new File(scriptDir, 'lib/DtcConfig.groovy'))
gcl.parseClass(new File(scriptDir, 'lib/DtcException.groovy'))
def DtcError = gcl.loadClass('DtcError')
def DtcException = gcl.loadClass('DtcException')
def DtcConfigException = gcl.loadClass('DtcConfigException')

// Single top-level handler (ADR-8): the body throws DtcException with actionable
// guidance instead of calling System.exit; here it maps to a differentiated exit
// code and a clean, secret-redacted message.
try {
    def dtcConfig = DtcConfig.load(docDir, configFile)
    def config = dtcConfig.getRaw()

    def inputPath = new File(docDir, config.inputPath ?: 'src/docs')
    def outputPath = new File(docDir, config.outputPath ?: 'build')
    def htmlOutputDir = new File(outputPath, 'html5')
    htmlOutputDir.mkdirs()

    println "docToolchain v4 — generateHTML"
    println "  inputPath:  ${inputPath.absolutePath}"
    println "  outputPath: ${htmlOutputDir.absolutePath}"
    println ""

    def inputFiles = config.inputFiles ?: []
    if (!inputFiles) {
        throw DtcConfigException.newInstance(
            "No inputFiles configured in ${configFile}. " +
            "Add e.g.: inputFiles = [[file: 'arc42/arc42.adoc', formats: ['html']]]")
    }

    def htmlFiles = inputFiles.findAll { entry ->
        def formats = entry.formats ?: ['html']
        formats.any { it.toLowerCase().contains('html') }
    }

    if (!htmlFiles) {
        println "No input files configured for HTML output."
        return
    }

    def imageDirs = config.imageDirs ?: ['images']

    // Copy the configured image folders into <output>/images so the relative
    // `images/...` references in the generated HTML actually resolve — without this
    // the HTML points at images that were never placed next to it.
    def copyImages = { File destImagesDir ->
        imageDirs.each { imageDir ->
            def src = new File(inputPath, imageDir as String)
            if (src.exists() && src.isDirectory()) {
                src.eachFileRecurse { f ->
                    if (!f.isFile()) return
                    def rel = src.toPath().relativize(f.toPath()).toString()
                    def to = new File(destImagesDir, rel)
                    to.parentFile.mkdirs()
                    to.bytes = f.bytes
                }
            }
        }
    }
    copyImages(new File(htmlOutputDir, 'images'))

    def asciidoctor = Asciidoctor.Factory.create()
    asciidoctor.requireLibrary('asciidoctor-diagram')
    def diagramHints = new GroovyClassLoader(this.class.classLoader)
        .parseClass(new File(scriptDir, 'lib/DiagramToolHints.groovy')).newInstance()
    diagramHints.register(asciidoctor)
    println "AsciidoctorJ ${Asciidoctor.class.package.implementationVersion ?: '2.5.x'} ready (with diagram support)"

    def failed = false

    htmlFiles.each { entry ->
        def sourceFile = new File(inputPath, entry.file)
        if (!sourceFile.exists()) {
            System.err.println "Source file not found: ${sourceFile.absolutePath}"
            failed = true
            return
        }

        println "Processing: ${entry.file}"

        def attrs = Attributes.builder()
            .imagesDir('images')
            .sourceHighlighter(config.sourceHighlighter ?: 'rouge')
            .tableOfContents(true)
            .attribute('toc', config.toc ?: 'left')
            .attribute('toclevels', (config.toclevels ?: 3) as String)
            .attribute('icons', config.icons ?: 'font')
            .attribute('doctype', 'book')
            .build()

        def safeMode = SafeMode.valueOf((config.safeMode ?: 'UNSAFE').toUpperCase())

        def options = Options.builder()
            .backend('html5')
            .safe(safeMode)
            .toDir(htmlOutputDir)
            .mkDirs(true)
            .baseDir(sourceFile.parentFile)
            .attributes(attrs)
            .build()

        try {
            asciidoctor.convertFile(sourceFile, options)
            def outputFile = new File(htmlOutputDir, sourceFile.name.replaceAll(/\.(adoc|ad|asciidoc)$/, '.html'))
            println "  -> ${outputFile.absolutePath} (${String.format('%.1f', outputFile.length() / 1024.0)} KB)"
        } catch (Exception e) {
            System.err.println "Failed: ${entry.file} — ${e.message}"
            failed = true
        }
    }

    diagramHints.printHints()
    asciidoctor.close()
    println ""
    if (failed) {
        throw DtcException.newInstance(
            "HTML generation completed with errors — see the failing files above and fix the AsciiDoc source.")
    }
    println "HTML generation completed successfully."
} catch (Throwable t) {
    System.exit(DtcError.report(t))
}
