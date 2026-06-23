#!/usr/bin/env groovy
// @task
// v4: Install the daCLI docs-as-code CLI / MCP server and hint agents to use it

def docDir = System.getProperty('docDir', '.')
def configFile = System.getProperty('mainConfigFile', 'docToolchainConfig.groovy')
def scriptDir = System.getProperty('dtc.scriptsHome') ? new File(System.getProperty('dtc.scriptsHome')) : new File(getClass().protectionDomain.codeSource.location.toURI()).parentFile

def DtcConfig = new GroovyClassLoader(this.class.classLoader).parseClass(new File(scriptDir, 'lib/DtcConfig.groovy'))
def config = DtcConfig.load(docDir, configFile).getRaw()
def inputPath = config.inputPath ?: 'src/docs'

def AgentHints = new GroovyClassLoader(this.class.classLoader)
    .parseClass(new File(scriptDir, 'lib/AgentHints.groovy'))

def color = { c, text ->
    def colors = [red: 31, green: 32, yellow: 33, cyan: 36]
    return new String((char) 27) + "[${colors[c]}m${text}" + new String((char) 27) + "[0m"
}

println "docToolchain v4 — installDacli"

// --- 1. uv is the installer for daCLI (a Python tool) -----------------------
def uvAvailable = {
    try {
        def p = ['uv', '--version'].execute()
        p.waitFor()
        return p.exitValue() == 0
    } catch (IOException ignored) {
        return false
    }
}()

if (!uvAvailable) {
    System.err.println color('red', "daCLI is a Python tool installed with 'uv', which is not on your PATH.")
    System.err.println "Install uv, then re-run './dtcw installDacli':"
    System.err.println "  curl -LsSf https://astral.sh/uv/install.sh | sh      # Linux/macOS"
    System.err.println "  powershell -c \"irm https://astral.sh/uv/install.ps1 | iex\"   # Windows"
    System.err.println "  (see https://docs.astral.sh/uv/getting-started/installation/)"
    System.exit(1)
}

// --- 2. Install daCLI from its repository ------------------------------------
// Installed from git on purpose: the PyPI package named 'dacli' is an unrelated
// project — docToolchain's daCLI lives only in its GitHub repository.
def ref = System.getenv('DTC_DACLI_VERSION') ?: ''
def source = "git+https://github.com/docToolchain/dacli" + (ref ? "@${ref}" : '')
def cmd = ['uv', 'tool', 'install', source]
if (ref) cmd << '--force'   // force a reinstall when a specific ref is pinned

println "Installing daCLI: ${cmd.join(' ')}"
def proc = cmd.execute()
proc.waitForProcessOutput(System.out, System.err)
if (proc.exitValue() != 0) {
    // uv exits non-zero only on a real failure; "already installed" is a 0 exit.
    System.err.println color('red', "uv tool install failed (exit ${proc.exitValue()}).")
    System.exit(1)
}
println "${color('green', 'daCLI installed (dacli, dacli-mcp).')}"

// --- 3. Hint agents to use it (only if the project has an agent file) --------
def block = """\
## Working with these docs — use daCLI
**Only when you are reading or editing this project's documentation:** use daCLI for
structured access instead of grepping files — e.g. `dacli --docs-root ${inputPath} search <query>`.
Run `dacli --help` first to discover the current commands.
Not installed? Run `./dtcw installDacli`.
Docs: https://doctoolchain.org/dacli"""

def updated = AgentHints.upsert(new File(docDir), block)
if (updated) {
    println "${color('green', "Added a daCLI hint for agents to ${updated.name}")}"
} else {
    println "${color('yellow', 'No AGENTS.md or CLAUDE.md found — skipped the agent hint.')}"
    println "  (Create one of them and re-run to let LLM agents know about daCLI.)"
}

println ""
println "Run the MCP server for this project with:"
println "  dacli-mcp --docs-root ${new File(docDir, inputPath).canonicalPath}"
