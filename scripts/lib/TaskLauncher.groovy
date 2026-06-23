// v4: Task dispatch logic for the Groovy launcher (ADR-18).
//
// Pure logic, no side effects beyond reading files and no System.exit, so it can
// be unit-tested directly (see TaskLauncherTest.groovy). The wiring — argument
// parsing, printing, exit codes, and running the resolved task — lives in the
// thin scripts/Launcher.groovy (IOSP: this is the Operation, that is the
// Integration).
//
// Resolution mirrors ADR-16: a project-local task (a *.groovy with the // @task
// marker, ADR-14) is preferred over the installed one; a same-named installed
// task makes it an override, otherwise it is a custom task.
class TaskLauncher {

    /** Installed scripts directory (holds the shipped tasks and lib/). */
    String scriptsHome
    /** Project root; project tasks are resolved relative to it. */
    String docDir = '.'
    /** Project scripts directory name (relative to docDir, or absolute). */
    String projectScriptsDirName = 'scripts'

    File installedDir() { new File(scriptsHome) }

    File projectDir() {
        def name = projectScriptsDirName ?: 'scripts'
        new File(name).isAbsolute() ? new File(name) : new File(docDir ?: '.', name)
    }

    /** A file is a runnable task iff it carries // @task in its first 5 lines. */
    static boolean isTask(File f) {
        if (f == null || !f.isFile()) return false
        f.withReader { r -> (1..5).any { (r.readLine() ?: '').contains('// @task') } }
    }

    File projectTaskScript(String task) {
        def f = new File(projectDir(), "${task}.groovy")
        isTask(f) ? f : null
    }

    File installedTaskScript(String task) {
        def f = new File(installedDir(), "${task}.groovy")
        isTask(f) ? f : null
    }

    // Display helpers: build shown paths via File so separators are normalized
    // across platforms (relevant once the Windows wrappers use this launcher) and
    // absolute / trailing-separator values render sanely.
    private String taskDisplayPath(String name) {
        new File(projectScriptsDirName ?: 'scripts', "${name}.groovy").path
    }
    private String dirDisplay() {
        new File(projectScriptsDirName ?: 'scripts').path + File.separator
    }

    private List<String> markedTaskNames(File dir) {
        def files = dir?.isDirectory() ? dir.listFiles({ d, n -> n.endsWith('.groovy') } as FilenameFilter) : null
        ((files ?: []) as List).findAll { isTask(it) }.collect { it.name - '.groovy' }.sort()
    }

    List<String> installedTaskNames() { markedTaskNames(installedDir()) }
    List<String> projectTaskNames() { markedTaskNames(projectDir()) }

    /**
     * Resolve which script to run for a task name.
     * @return a map: [script: File|null, kind: 'override'|'custom'|'installed'|null, note: String|null]
     */
    Map resolve(String task) {
        def proj = projectTaskScript(task)
        if (proj) {
            def shown = taskDisplayPath(task)
            if (installedTaskScript(task)) {
                return [script: proj, kind: 'override',
                        note: "Note: running project-local override of task '${task}' from " +
                              "'${shown}' (shadows the installed task)."]
            }
            return [script: proj, kind: 'custom',
                    note: "Note: running project-local custom task '${task}' from '${shown}'."]
        }
        def inst = installedTaskScript(task)
        if (inst) return [script: inst, kind: 'installed', note: null]
        return [script: null, kind: null, note: null]
    }

    /** Render the `dtcw tasks` listing (installed + override annotations + custom). */
    String renderTaskList() {
        def installed = installedTaskNames()
        def project = projectTaskNames()
        def sb = new StringBuilder()
        sb << '\n' << 'Available tasks:\n' << '\n'
        installed.each { name ->
            if (project.contains(name)) {
                sb << "  ${name} (overridden by ${taskDisplayPath(name)})\n"
            } else {
                sb << "  ${name}\n"
            }
        }
        def customOnly = project.findAll { !installed.contains(it) }
        if (customOnly) {
            sb << '\n' << "Project-local custom tasks (${dirDisplay()}):\n" << '\n'
            customOnly.each { sb << "  ${it}\n" }
        }
        sb << '\n' << 'Usage: ./dtcw [environment] <task>\n' << '\n'
        sb.toString()
    }

    /** Near-name suggestions for an unknown task (substring match, both directions). */
    List<String> suggestions(String task) {
        def t = (task ?: '').toLowerCase()
        (installedTaskNames() + projectTaskNames()).unique().findAll { n ->
            def ln = n.toLowerCase()
            ln.contains(t) || (t && t.contains(ln))
        }.sort()
    }
}
