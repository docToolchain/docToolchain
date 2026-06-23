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
