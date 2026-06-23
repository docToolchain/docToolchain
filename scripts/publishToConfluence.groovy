#!/usr/bin/env groovy
// @task
// v4: Self-contained Groovy task for Confluence publishing (replaces publishToConfluence.gradle)
// Invoked by: java -cp <lib/*.jar> groovy.ui.GroovyMain scripts/publishToConfluence.groovy
//
// ADR-12 / #1626: this task has NO compiled core/ dependency. The Confluence
// publishing class graph (Asciidoc2ConfluenceTask and its transitive
// org.docToolchain.* closure) lives as SOURCE under scripts/lib/org/docToolchain/...
// It is compiled on demand at runtime by a GroovyClassLoader whose classpath
// is the scripts/lib source root: addClasspath(scriptDir/lib) lets Groovy
// resolve every inter-class reference in that graph from source. The only JARs
// on the JVM classpath are the third-party libs in lib/ (jsoup, httpclient,
// guava, poi) — the same ones GroovyClassLoader inherits from this.class.classLoader.

def docDir = System.getProperty('docDir', '.')
def configFile = System.getProperty('mainConfigFile', 'docToolchainConfig.groovy')

// scriptDir = the installed scripts/ directory that holds lib/ (ADR-17). dtcw
// passes it via -Ddtc.scriptsHome so a project-local copy of this script still
// finds the bundled helpers; the fallback (codeSource location, i.e. the script
// file itself) applies when run directly via groovy.ui.GroovyMain without dtcw.
def scriptDir = System.getProperty('dtc.scriptsHome') ? new File(System.getProperty('dtc.scriptsHome')) : new File(getClass().protectionDomain.codeSource.location.toURI()).parentFile

def libGcl = new GroovyClassLoader(this.class.classLoader)
def DtcConfig = libGcl.parseClass(new File(scriptDir, 'lib/DtcConfig.groovy'))
libGcl.parseClass(new File(scriptDir, 'lib/DtcException.groovy'))
def DtcError = libGcl.loadClass('DtcError')
def DtcException = libGcl.loadClass('DtcException')
def DtcConfigException = libGcl.loadClass('DtcConfigException')
def DtcApiException = libGcl.loadClass('DtcApiException')

// Single top-level handler (ADR-8): a config problem throws DtcConfigException
// (exit 2); a publish/network failure becomes a DtcApiException (exit 3) whose
// message is secret-redacted by DtcError.report() before it reaches the console.
try {
    def dtcConfig = DtcConfig.load(docDir, configFile)
    def config = dtcConfig.getRaw()

    if (!config.confluence?.api) {
        throw DtcConfigException.newInstance(
            "Confluence API URL not configured. Add to your ${configFile}:\n" +
            "  confluence = [\n" +
            "    api: 'https://your-wiki.atlassian.net/wiki/rest/api',\n" +
            "    spaceKey: 'YOUR_SPACE',\n" +
            "  ]")
    }

    println "docToolchain v4 — publishToConfluence"
    println "  target: ${config.confluence.api}"
    println ""

    // Load the Confluence publishing class graph from source. addClasspath points
    // the GroovyClassLoader at the scripts/lib source root; loadClass then triggers
    // on-demand compilation of Asciidoc2ConfluenceTask and every org.docToolchain.*
    // class it (transitively) references.
    def gcl = new GroovyClassLoader(this.class.classLoader)
    gcl.addClasspath("${scriptDir}/lib")
    def taskClass = gcl.loadClass('org.docToolchain.tasks.Asciidoc2ConfluenceTask')

    // Two-arg constructor (config, docDir). execute() dereferences docDir, so the
    // one-arg form used previously was a latent bug.
    def task = taskClass.getConstructor(ConfigObject, String).newInstance(config, docDir.toString())
    task.execute()
} catch (Throwable t) {
    // Unwrap reflection wrappers so the real cause (e.g. a network/auth error)
    // is what the user sees.
    def cause = t
    while (cause in java.lang.reflect.InvocationTargetException && cause.cause) {
        cause = cause.cause
    }
    // A DtcException already carries the right exit code and guidance; anything
    // else is wrapped as an API error so its message is redacted (exit 3).
    def toReport = (cause in DtcException) ? cause : DtcApiException.newInstance(
        "publishToConfluence failed: ${cause.class.simpleName}: ${cause.message}", cause)
    System.exit(DtcError.report(toReport))
}
