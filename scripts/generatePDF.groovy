#!/usr/bin/env groovy
// @task
// v4: Generate PDF from AsciiDoc sources using AsciidoctorJ PDF

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
// guidance instead of calling System.exit.
try {
    def dtcConfig = DtcConfig.load(docDir, configFile)
    def config = dtcConfig.getRaw()

    def inputPath = new File(docDir, config.inputPath ?: 'src/docs')
    def outputPath = new File(docDir, config.outputPath ?: 'build')
    def pdfOutputDir = new File(outputPath, 'pdf')
    pdfOutputDir.mkdirs()

    println "docToolchain v4 — generatePDF"
    println "  inputPath:  ${inputPath.absolutePath}"
    println "  outputPath: ${pdfOutputDir.absolutePath}"
    println ""

    def inputFiles = config.inputFiles ?: []
    if (!inputFiles) {
        throw DtcConfigException.newInstance(
            "No inputFiles configured in ${configFile}. " +
            "Add e.g.: inputFiles = [[file: 'arc42/arc42.adoc', formats: ['pdf']]]")
    }

    def pdfFiles = inputFiles.findAll { entry ->
        def formats = entry.formats ?: ['html']
        formats.any { it.toLowerCase().contains('pdf') }
    }

    if (!pdfFiles) {
        println "No input files configured for PDF output."
        return
    }

    def imageDirs = config.imageDirs ?: ['images']

    // asciidoctor-pdf embeds images, so imagesdir must point at the actual files.
    // The configured imageDirs are relative to inputPath, but the document baseDir
    // is the source file's folder (e.g. the arc42 subfolder) — so a path like
    // 'images/.' resolves to the wrong place and the images silently drop out.
    // Consolidate every imageDir into <output>/pdf/images and point imagesdir at
    // that absolute path, which resolves regardless of how deep the source sits.
    def imagesOutDir = new File(pdfOutputDir, 'images')
    imageDirs.each { imageDir ->
        def src = new File(inputPath, imageDir as String)
        if (src.exists() && src.isDirectory()) {
            src.eachFileRecurse { f ->
                if (!f.isFile()) return
                def rel = src.toPath().relativize(f.toPath()).toString()
                def to = new File(imagesOutDir, rel)
                to.parentFile.mkdirs()
                to.bytes = f.bytes
            }
        }
    }

    def asciidoctor = Asciidoctor.Factory.create()

    // Register PDF converter by requiring the gem
    asciidoctor.requireLibrary('asciidoctor-pdf')
    asciidoctor.requireLibrary('asciidoctor-diagram')
    def diagramHints = new GroovyClassLoader(this.class.classLoader)
        .parseClass(new File(scriptDir, 'lib/DiagramToolHints.groovy')).newInstance()
    diagramHints.register(asciidoctor)
    println "AsciidoctorJ PDF ready (with diagram support)"

    def failed = false

    pdfFiles.each { entry ->
        def sourceFile = new File(inputPath, entry.file)
        if (!sourceFile.exists()) {
            System.err.println "Source file not found: ${sourceFile.absolutePath}"
            failed = true
            return
        }

        println "Processing: ${entry.file}"

        def pdfThemeDir = config.pdfThemeDir ?: null
        if (pdfThemeDir?.startsWith('.')) {
            pdfThemeDir = new File(docDir, pdfThemeDir).canonicalPath
        }
        def pdfTheme = config.pdfTheme ?: null
        def pdfFontsDir = config.pdfFontsDir ?: (pdfThemeDir ? "${pdfThemeDir}/fonts".toString() : null)

        // Coerce values to plain String: asciidoctor-pdf (JRuby) calls Ruby String
        // methods (e.g. .sub) on attribute values; a Groovy GString has no such
        // method and fails with a NoMethodError (notably under Groovy 4).
        def attrsBuilder = Attributes.builder()
            .imagesDir(imagesOutDir.canonicalPath as String)
            .sourceHighlighter((config.sourceHighlighter ?: 'rouge') as String)
            .attribute('icons', (config.icons ?: 'font') as String)
            .attribute('doctype', 'book')

        if (pdfTheme) attrsBuilder.attribute('pdf-theme', pdfTheme as String)
        if (pdfThemeDir) attrsBuilder.attribute('pdf-themesdir', pdfThemeDir as String)
        if (pdfFontsDir) attrsBuilder.attribute('pdf-fontsdir', pdfFontsDir as String)

        def safeMode = SafeMode.valueOf((config.safeMode ?: 'UNSAFE').toUpperCase())

        def options = Options.builder()
            .backend('pdf')
            .safe(safeMode)
            .toDir(pdfOutputDir)
            .mkDirs(true)
            .baseDir(sourceFile.parentFile)
            .attributes(attrsBuilder.build())
            .build()

        try {
            asciidoctor.convertFile(sourceFile, options)
            def outputFile = new File(pdfOutputDir, sourceFile.name.replaceAll(/\.(adoc|ad|asciidoc)$/, '.pdf'))
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
            "PDF generation completed with errors — see the failing files above and fix the AsciiDoc source.")
    }
    println "PDF generation completed successfully."
} catch (Throwable t) {
    System.exit(DtcError.report(t))
}
