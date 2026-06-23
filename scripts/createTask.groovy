#!/usr/bin/env groovy
// @task
// v4: Scaffold a new project-local custom task (see ADR-16).
// Usage: ./dtcw createTask [taskName]
// Creates <projectScriptsDir>/<taskName>.groovy in the current project with a
// ready-to-edit skeleton that already carries the '// @task' marker, so dtcw
// discovers and runs it. No edit to any config or registry is required.

def docDir = System.getProperty('docDir', '.')
// dtcw exports the project scripts directory name; default mirrors the wrapper.
def scriptsDirName = System.getenv('DTC_PROJECT_SCRIPTS_DIR') ?: 'scripts'

def taskName = (args && args.length > 0) ? args[0] : 'customTask'

// Validate the name: it becomes a file name and a dtcw task argument.
if (!(taskName ==~ /^[a-zA-Z][a-zA-Z0-9_-]*$/)) {
    System.err.println """
Error: invalid task name '${taskName}'.
Task names must start with a letter and contain only letters, digits,
hyphens or underscores. Example: ./dtcw createTask exportFoo
"""
    System.exit 2
}

def scriptsDir = new File(docDir, scriptsDirName)
scriptsDir.mkdirs()
def target = new File(scriptsDir, "${taskName}.groovy")

if (target.exists()) {
    System.err.println """
Error: '${target.path}' already exists.
Pick another name, or edit the existing file directly.
"""
    System.exit 1
}

target.text = """\
#!/usr/bin/env groovy
// @task
// Custom task '${taskName}' — created by 'dtcw createTask'.
// This file lives in your project and is discovered automatically by dtcw.

// docDir is your project root; mainConfigFile is your docToolchainConfig.groovy.
def docDir = System.getProperty('docDir', '.')
def configFile = System.getProperty('mainConfigFile', 'docToolchainConfig.groovy')

// Optional: reuse docToolchain's bundled helpers from the installation.
// 'dtc.scriptsHome' points at the installed scripts/ directory, so this works
// even though this script lives in your project (see ADR-17).
def scriptsHome = System.getProperty('dtc.scriptsHome')
if (scriptsHome) {
    def gcl = new GroovyClassLoader(this.class.classLoader)
    def DtcConfig = gcl.parseClass(new File(scriptsHome, 'lib/DtcConfig.groovy'))
    def config = DtcConfig.load(docDir, configFile).getRaw()
    println "Custom task '${taskName}' — inputPath = \${config.inputPath}"
}

println "Hello from your custom task '${taskName}'!"
// TODO: add your own logic here.
"""

println """
Custom task created:

  ${target.canonicalPath}

Run it with:

  ./dtcw ${taskName}

List it together with the built-in tasks:

  ./dtcw tasks
"""
