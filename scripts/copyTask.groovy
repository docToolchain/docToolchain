#!/usr/bin/env groovy
// @task
// v4: Copy an installed task script into the current project so it can be
// modified (monkey-patching, see ADR-16). The project copy shadows the
// installed task of the same name; dtcw warns when it runs the override.
// Usage: ./dtcw copyTask <installedTaskName>

def docDir = System.getProperty('docDir', '.')
def scriptsDirName = System.getenv('DTC_PROJECT_SCRIPTS_DIR') ?: 'scripts'
def scriptsHome = System.getProperty('dtc.scriptsHome')

if (!args || args.length == 0) {
    System.err.println """
Error: no task name given.
Usage: ./dtcw copyTask <installedTaskName>
Run './dtcw tasks' to see which tasks you can copy.
"""
    System.exit 2
}
def taskName = args[0]

// Validate the name before using it to build a path. This is both a usability
// check and a path-traversal guard: without it, '../foo' could copy files from
// outside the installed scripts directory.
if (!(taskName ==~ /^[a-zA-Z][a-zA-Z0-9_-]*$/)) {
    System.err.println """
Error: invalid task name '${taskName}'.
Task names must start with a letter and contain only letters, digits,
hyphens or underscores. Run './dtcw tasks' to see the available tasks.
"""
    System.exit 2
}

if (!scriptsHome) {
    System.err.println """
Error: cannot locate the docToolchain installation (dtc.scriptsHome is unset).
Run this task via ./dtcw so the installation directory is known.
"""
    System.exit 1
}

def source = new File(scriptsHome, "${taskName}.groovy")
if (!source.exists()) {
    System.err.println """
Error: installed task '${taskName}' not found at:
  ${source.path}
Run './dtcw tasks' to see the available tasks.
"""
    System.exit 1
}

// Only runnable tasks (carrying the '// @task' marker in the first 5 lines,
// see ADR-14) may be copied — not the non-task Groovy helpers that also live in
// the installed scripts/ directory.
def isTask = source.withReader { r ->
    (1..5).any { (r.readLine() ?: '').contains('// @task') }
}
if (!isTask) {
    System.err.println """
Error: '${taskName}' is not a runnable task (no '// @task' marker), so it cannot
be copied and overridden. Run './dtcw tasks' to see the available tasks.
"""
    System.exit 1
}

def scriptsDir = new File(docDir, scriptsDirName)
scriptsDir.mkdirs()
def target = new File(scriptsDir, "${taskName}.groovy")
if (target.exists()) {
    System.err.println """
Error: '${target.path}' already exists.
Refusing to overwrite your local copy. Edit it directly, or remove it first.
"""
    System.exit 1
}

target.bytes = source.bytes

println """
Copied installed task '${taskName}' to:

  ${target.canonicalPath}

This project-local copy now overrides the installed task. Edit it freely —
its bundled helpers (lib/) and resources still resolve from the installation,
so the copy keeps working. When you run

  ./dtcw ${taskName}

dtcw runs your copy and prints a note that the installed task is being
overridden. Delete the file to fall back to the installed task.
"""
