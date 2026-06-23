#!/usr/bin/env groovy
//
// v4 task dispatcher (ADR-18). The wrapper invokes it directly by path:
//
//   java ... groovy.ui.GroovyMain <scriptsHome>/Launcher.groovy <task> [args...]
//
// It owns task discovery, project-first resolution (ADR-16), the tasks listing,
// unknown-task guidance, and dispatch to the chosen task script — all the logic
// that used to live in the Bash dtcw. The platform wrappers keep only JVM
// bootstrapping (environment, Java, classpath).
//
// NOTE: this dispatcher is deliberately NOT a runnable task — it carries no task
// marker, so it never appears in `dtcw tasks` and cannot be invoked as a task.
// (The marker string is omitted here on purpose; see ADR-14 for the convention.)

def scriptsHome = System.getProperty('dtc.scriptsHome')
def docDir = System.getProperty('docDir', '.')
def projectScriptsDirName = System.getenv('DTC_PROJECT_SCRIPTS_DIR') ?: 'scripts'

if (!scriptsHome) {
    System.err.println "Error: dtc.scriptsHome is not set. Run tasks via ./dtcw."
    System.exit 1
}
if (!args || args.length == 0) {
    System.err.println "Error: no task given. Run './dtcw tasks' to list available tasks."
    System.exit 2
}

def scriptDir = new File(scriptsHome)
def TaskLauncher = new GroovyClassLoader(this.class.classLoader)
        .parseClass(new File(scriptDir, 'lib/TaskLauncher.groovy'))
def launcher = TaskLauncher.newInstance()
launcher.scriptsHome = scriptsHome
launcher.docDir = docDir
launcher.projectScriptsDirName = projectScriptsDirName

def task = args[0]
def taskArgs = (args.length > 1) ? args[1..-1] : []

// `dtcw tasks` — list available tasks (installed + project-local).
if (task == 'tasks') {
    if (taskArgs.contains('--group')) {
        System.err.println "Note: v4 uses direct script invocation. --group is ignored."
    }
    print launcher.renderTaskList()
    System.exit 0
}

def res = launcher.resolve(task)
if (!res.script) {
    System.err.println ""
    System.err.println "Error: Unknown task '${task}'"
    System.err.println ""
    def sugg = launcher.suggestions(task)
    if (sugg) {
        System.err.println "Did you mean:"
        sugg.each { System.err.println "  ${it}" }
        System.err.println ""
    }
    System.err.println "Run './dtcw tasks' to see all available tasks."
    System.err.println ""
    System.exit 2
}

// Transparency: announce a project-local override / custom task (ADR-16).
if (res.note) System.err.println res.note

// Run the resolved task script in this JVM. dtc.scriptsHome is already set, so
// the task resolves its lib/ helpers from the installation (ADR-17) regardless
// of where the script file lives.
def shell = new GroovyShell(this.class.classLoader, new Binding())
shell.run(res.script as File, taskArgs as String[])
