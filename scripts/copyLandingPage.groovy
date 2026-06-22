#!/usr/bin/env groovy
// @task
// v4: Copy a landing page template into your project so you have a start page to customize

def docDir = System.getProperty('docDir', '.')
def configFile = System.getProperty('mainConfigFile', 'docToolchainConfig.groovy')

def dtcHome = new File(getClass().protectionDomain.codeSource.location.toURI()).parentFile.parentFile
def scriptDir = new File(getClass().protectionDomain.codeSource.location.toURI()).parentFile
def gcl = new GroovyClassLoader(this.class.classLoader)
def DtcConfig = gcl.parseClass(new File(scriptDir, 'lib/DtcConfig.groovy'))
gcl.parseClass(new File(scriptDir, 'lib/DtcException.groovy'))
def DtcError = gcl.loadClass('DtcError')
def DtcException = gcl.loadClass('DtcException')
def dtcConfig = DtcConfig.load(docDir, configFile)
def config = dtcConfig.getRaw()
def inputPath = config.inputPath ?: 'src/docs'

def color = { c, text ->
    def colors = [black: 30, red: 31, green: 32, yellow: 33, blue: 34, magenta: 35, cyan: 36, white: 37]
    return new String((char) 27) + "[${colors[c]}m${text}" + new String((char) 27) + "[0m"
}

// Single top-level handler (ADR-8): the body throws DtcException with guidance.
try {
    println "docToolchain v4 — copyLandingPage"

    // Parse command-line args passed after the task name
    def cliArgs = this.args as List
    def force = ('--force' in cliArgs) || ('-f' in cliArgs)

    def source = new File(dtcHome, 'src/site/doc/landingpage.gsp')
    if (!source.exists()) {
        throw DtcException.newInstance("Source landing page not found: ${source.absolutePath}")
    }

    // The landing page lives in the project's site theme so it overlays the
    // built-in theme during generateSite (index.gsp includes doc/<landingPage>).
    def siteFolder = config.microsite?.siteFolder ?: '../site'
    def landingPageName = config.microsite?.landingPage ?: 'landingpage.gsp'
    def targetDir = new File(new File(docDir, inputPath), siteFolder + '/doc')
    def target = new File(targetDir, landingPageName)

    if (target.exists() && !force) {
        throw DtcException.newInstance(
            "Landing page already exists: ${target.canonicalPath}\n" +
            "  Re-run 'copyLandingPage --force' to overwrite it, or just edit the existing file.")
    }

    targetDir.mkdirs()
    target.bytes = source.bytes
    println "Landing page copied into ${target.canonicalPath}"

    if (!config.microsite?.siteFolder) {
        println color('green', "Hint: set microsite.siteFolder = '${siteFolder}' in ${configFile} so generateSite picks up your theme folder.")
    }
    if (!config.microsite?.landingPage) {
        println color('green', "Hint: set microsite.landingPage = '${landingPageName}' in ${configFile} so generateSite uses your landing page.")
    }
    println color('green', "Edit ${landingPageName} and run 'dtcw4 generateSite' to see your start page.")
} catch (Throwable t) {
    System.exit(DtcError.report(t))
}
