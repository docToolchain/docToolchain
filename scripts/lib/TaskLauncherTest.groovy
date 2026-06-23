// v4: Self-contained test for TaskLauncher (runs without Spock/JUnit).
// Usage: groovy scripts/lib/TaskLauncherTest.groovy
//
// Mirrors the lightweight test style of DtcConfigTest.groovy: parse the class
// under test, exercise it against a temp filesystem, print PASS/FAIL, and exit
// non-zero if anything fails.

def TaskLauncher = new GroovyClassLoader(this.class.classLoader)
        .parseClass(new File('scripts/lib/TaskLauncher.groovy'))

int passed = 0
int failed = 0
def check = { String name, boolean ok ->
    if (ok) { println "  PASS: ${name}"; passed++ }
    else    { println "  FAIL: ${name}"; failed++ }
}

// --- Fixture: an installed scripts dir and a project scripts dir -------------
def tmp = File.createTempDir('tasklauncher-test', '')
def installed = new File(tmp, 'install/scripts'); installed.mkdirs()
def project = new File(tmp, 'project/myscripts'); project.mkdirs()

def marked = { File dir, String name -> new File(dir, "${name}.groovy").text = "// @task\nprintln 'x'\n" }
def helper = { File dir, String name -> new File(dir, "${name}.groovy").text = "// just a helper\nclass Foo {}\n" }

// installed: two real tasks + one non-task helper sharing a name with a project task
marked(installed, 'generateHTML')
marked(installed, 'generatePDF')
helper(installed, 'asciidoctorExtensions')
// installed dispatcher itself must never be listed (no marker)
helper(installed, 'Launcher')

// project: one custom, one override (same name as installed task),
// one custom colliding with a non-task installed helper, one unmarked helper
marked(project, 'exportNotion')
marked(project, 'generateHTML')
marked(project, 'asciidoctorExtensions')
helper(project, 'justAHelper')

def launcher = TaskLauncher.newInstance()
launcher.scriptsHome = installed.absolutePath
launcher.docDir = new File(tmp, 'project').absolutePath
launcher.projectScriptsDirName = 'myscripts'

// --- isTask ------------------------------------------------------------------
check('marked file is a task', TaskLauncher.isTask(new File(installed, 'generateHTML.groovy')))
check('unmarked helper is not a task', !TaskLauncher.isTask(new File(installed, 'asciidoctorExtensions.groovy')))
check('missing file is not a task', !TaskLauncher.isTask(new File(installed, 'nope.groovy')))
// Regression: the real dispatcher must never be detected as a task — its own
// comments must not contain the marker string within the first 5 lines.
def realLauncher = new File('scripts/Launcher.groovy')
if (realLauncher.isFile()) {
    check('the real Launcher.groovy is not a task', !TaskLauncher.isTask(realLauncher))
}

// --- resolve -----------------------------------------------------------------
def rCustom = launcher.resolve('exportNotion')
check('custom task resolves to project script', rCustom.script == new File(project, 'exportNotion.groovy'))
check('custom task kind is custom', rCustom.kind == 'custom')
check('custom note mentions custom task', rCustom.note?.contains("custom task 'exportNotion'"))

def rOverride = launcher.resolve('generateHTML')
check('override resolves to project script', rOverride.script == new File(project, 'generateHTML.groovy'))
check('override kind is override', rOverride.kind == 'override')
check('override note mentions shadows', rOverride.note?.contains('shadows the installed task'))

def rInstalled = launcher.resolve('generatePDF')
check('installed task resolves to installed script', rInstalled.script == new File(installed, 'generatePDF.groovy'))
check('installed kind is installed', rInstalled.kind == 'installed')
check('installed note is null', rInstalled.note == null)

// project task colliding with a NON-task installed helper => custom, not override
def rCollision = launcher.resolve('asciidoctorExtensions')
check('collision with non-task helper is custom', rCollision.kind == 'custom')

def rUnknown = launcher.resolve('doesNotExist')
check('unknown task resolves to null', rUnknown.script == null && rUnknown.kind == null)

// --- listing -----------------------------------------------------------------
def list = launcher.renderTaskList()
check('list shows Available tasks header', list.contains('Available tasks:'))
check('list shows installed task', list.contains('  generatePDF'))
check('list flags override', list.contains('generateHTML (overridden by myscripts/generateHTML.groovy)'))
check('list has custom-tasks section', list.contains('Project-local custom tasks (myscripts/):'))
check('list shows custom task', (list =~ /(?m)^  exportNotion$/).find())
check('list does not show non-task installed helper as installed', !(list =~ /(?m)^  asciidoctorExtensions \(/).find() )
check('list does not show the Launcher dispatcher', !list.contains('  Launcher'))

// --- suggestions -------------------------------------------------------------
def sugg = launcher.suggestions('generate')
check('suggestions match by substring', sugg.contains('generateHTML') && sugg.contains('generatePDF'))

println ""
println "TaskLauncher: ${passed} passed, ${failed} failed"
if (failed > 0) System.exit 1
