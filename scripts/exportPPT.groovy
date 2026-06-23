#!/usr/bin/env groovy
// @task
// v4: Export PowerPoint slides as images and extract notes using Apache POI
// Works on all platforms (no Windows COM/VBScript needed)

import org.apache.poi.xslf.usermodel.XMLSlideShow
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

def docDir = System.getProperty('docDir', '.')
def configFile = System.getProperty('mainConfigFile', 'docToolchainConfig.groovy')

def scriptDir = System.getProperty('dtc.scriptsHome') ? new File(System.getProperty('dtc.scriptsHome')) : new File(getClass().protectionDomain.codeSource.location.toURI()).parentFile
def DtcConfig = new GroovyClassLoader(this.class.classLoader).parseClass(new File(scriptDir, 'lib/DtcConfig.groovy'))
def dtcConfig = DtcConfig.load(docDir, configFile)
def config = dtcConfig.getRaw()

def inputPath = config.inputPath ?: 'src/docs'
def srcDir = new File(docDir, inputPath)

println "docToolchain v4 — exportPPT"
println "  srcDir: ${srcDir.absolutePath}"
println ""

def imagesDir = new File(srcDir, 'images/ppt')
def notesDir = new File(srcDir, 'ppt')
imagesDir.deleteDir()
notesDir.deleteDir()
imagesDir.mkdirs()
notesDir.mkdirs()

def readme = """\
This folder contains exported slides from PowerPoint.
These are generated files — **contents will be overwritten with each re-export!**
Use './dtcw exportPPT' to re-export.
"""
new File(imagesDir, 'readme.ad').write(readme, 'UTF-8')
new File(notesDir, 'readme.ad').write(readme, 'UTF-8')

def pptFiles = []
srcDir.eachFileRecurse { f ->
    if (f.name.endsWith('.pptx') && !f.name.startsWith('~')) pptFiles << f
    else if (f.name.endsWith('.ppt') && !f.name.startsWith('~')) {
        System.err.println "Skipping legacy .ppt file (only .pptx supported): ${f.name}"
    }
}

if (!pptFiles) {
    println "No .pptx files found in ${srcDir.absolutePath}"
    System.exit(0)
}

def failed = false

pptFiles.each { File pptFile ->
    println "file: ${pptFile.name}"
    def baseName = pptFile.name.replaceAll(/\.(pptx?|PPTX?)$/, '')
    def slideImagesDir = new File(imagesDir, baseName)
    slideImagesDir.mkdirs()
    def slideNotesDir = new File(notesDir, baseName)
    slideNotesDir.mkdirs()

    try {
        new FileInputStream(pptFile).withCloseable { inp ->
            def ppt = new XMLSlideShow(inp)
            try {
                def pgSize = ppt.pageSize
                def scale = 2.0

                ppt.slides.eachWithIndex { slide, idx ->
                    def slideNum = String.format('%03d', idx + 1)
                    println "  slide ${slideNum}: ${slide.title ?: '(no title)'}"

                    // Render slide as PNG
                    def width = (int) (pgSize.width * scale)
                    def height = (int) (pgSize.height * scale)
                    def img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                    def g2d = img.createGraphics()
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                    g2d.setPaint(java.awt.Color.WHITE)
                    g2d.fill(new Rectangle2D.Float(0, 0, width, height))
                    g2d.scale(scale, scale)

                    try {
                        slide.draw(g2d)
                    } catch (Exception e) {
                        System.err.println "  Warning: Could not fully render slide ${slideNum} — ${e.message}"
                    }
                    g2d.dispose()

                    def imgFile = new File(slideImagesDir, "slide_${slideNum}.png")
                    ImageIO.write(img, 'png', imgFile)

                    // Extract notes
                    def notes = slide.notes
                    if (notes) {
                        def noteText = notes.textParagraphs?.flatten()?.collect { it.text }?.join('\n')?.trim()
                        if (noteText) {
                            new File(slideNotesDir, "slide_${slideNum}_notes.adoc").write(noteText, 'utf-8')
                        }
                    }
                }

                // Generate AsciiDoc include file
                def includeFile = new File(slideNotesDir, "${baseName}.adoc")
                def includeText = new StringBuilder()
                includeText.append("// Generated from ${pptFile.name}\n\n")
                ppt.slides.eachWithIndex { slide, idx ->
                    def slideNum = String.format('%03d', idx + 1)
                    def title = slide.title ?: "Slide ${idx + 1}"
                    includeText.append("=== ${title}\n\n")
                    includeText.append("image::ppt/${baseName}/slide_${slideNum}.png[${title}]\n\n")
                    def notesFile = new File(slideNotesDir, "slide_${slideNum}_notes.adoc")
                    if (notesFile.exists()) {
                        includeText.append("include::slide_${slideNum}_notes.adoc[]\n\n")
                    }
                }
                includeFile.write(includeText.toString(), 'utf-8')
                println "  -> ${ppt.slides.size()} slides exported"
            } finally {
                ppt.close()
            }
        }
    } catch (Exception e) {
        System.err.println "Failed to process ${pptFile.name}: ${e.message}"
        failed = true
    }
}

println ""
if (failed) {
    System.err.println "PowerPoint export completed with errors."
    System.exit(1)
} else {
    println "PowerPoint export completed."
}
