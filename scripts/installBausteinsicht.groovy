#!/usr/bin/env groovy
// @task
// v4: Install the Bausteinsicht CLI (architecture-as-code) and hint agents to use it

def docDir = System.getProperty('docDir', '.')
def scriptDir = System.getProperty('dtc.scriptsHome') ? new File(System.getProperty('dtc.scriptsHome')) : new File(getClass().protectionDomain.codeSource.location.toURI()).parentFile
def AgentHints = new GroovyClassLoader(this.class.classLoader)
    .parseClass(new File(scriptDir, 'lib/AgentHints.groovy'))

def color = { c, text ->
    def colors = [red: 31, green: 32, yellow: 33, cyan: 36]
    return new String((char) 27) + "[${colors[c]}m${text}" + new String((char) 27) + "[0m"
}

println "docToolchain v4 — installBausteinsicht"

// --- 1. Resolve target platform and release asset ---------------------------
// Bausteinsicht ships GoReleaser tarballs named bausteinsicht_<os>_<arch>.<ext>
def osName = System.getProperty('os.name').toLowerCase()
def osArch = System.getProperty('os.arch').toLowerCase()
def os = osName.contains('win') ? 'windows' : (osName.contains('mac') || osName.contains('darwin')) ? 'darwin' : 'linux'
def arch = (osArch in ['aarch64', 'arm64']) ? 'arm64' : 'amd64'
def ext = (os == 'windows') ? 'zip' : 'tar.gz'
def binName = (os == 'windows') ? 'bausteinsicht.exe' : 'bausteinsicht'

def releases = 'https://github.com/docToolchain/Bausteinsicht/releases'

// The release asset name embeds the version (Bausteinsicht_<ver>_<os>_<arch>.<ext>),
// so GitHub's latest/download shortcut cannot be used — resolve the tag first.
def pinned = (System.getenv('DTC_BAUSTEINSICHT_VERSION') ?: '') != ''
def version = (System.getenv('DTC_BAUSTEINSICHT_VERSION') ?: '').replaceFirst('^v', '')
def tag
if (pinned) {
    tag = "v${version}"
} else {
    def c = new URL("${releases}/latest").openConnection()
    c.instanceFollowRedirects = false
    c.connectTimeout = 15000
    c.readTimeout = 15000
    def loc = c.getHeaderField('Location')
    def m = (loc =~ /\/tag\/(.+)$/)
    if (!m) {
        System.err.println color('red', "Could not resolve the latest Bausteinsicht release (no redirect from ${releases}/latest).")
        System.err.println "Pin a version with DTC_BAUSTEINSICHT_VERSION, or install manually from ${releases}"
        System.exit(1)
    }
    tag = m[0][1]
    version = tag.replaceFirst('^v', '')
}
def asset = "Bausteinsicht_${version}_${os}_${arch}.${ext}"
def url = "${releases}/download/${tag}/${asset}"

// --- 2. Install location ----------------------------------------------------
def dtcHome = new File(System.getenv('DTC_HOME') ?: "${System.getProperty('user.home')}/.doctoolchain")
def binDir = new File(dtcHome, 'bin')
binDir.mkdirs()
def binary = new File(binDir, binName)

// --- 3. Install (skip the download if already present) ----------------------
def alreadyInstalled = binary.exists() && binary.canExecute()
if (alreadyInstalled && !pinned) {
    println "${color('green', "Bausteinsicht already installed at ${binary.absolutePath}")} (set DTC_BAUSTEINSICHT_VERSION to reinstall a specific version)"
} else {
    println "Downloading ${url}"
    def tmp = File.createTempFile('bausteinsicht', ".${ext}")
    try {
        def conn = new URL(url).openConnection()
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 15000
        conn.readTimeout = 120000
        conn.inputStream.withCloseable { tmp.bytes = it.bytes }
    } catch (Exception e) {
        System.err.println color('red', "Could not download ${asset}: ${e.message}")
        System.err.println "Check the available assets at ${releases} and install manually, or:"
        System.err.println "  go install github.com/docToolchain/Bausteinsicht/cmd/bausteinsicht@latest"
        tmp.delete()
        System.exit(1)
    }

    // Extract just the binary into binDir (the archive also holds LICENSE/README).
    if (ext == 'tar.gz') {
        def proc = ['tar', '-xzf', tmp.absolutePath, '-C', binDir.absolutePath, binName].execute()
        proc.waitForProcessOutput(System.out, System.err)
        if (proc.exitValue() != 0) {
            System.err.println color('red', "tar extraction failed")
            tmp.delete(); System.exit(1)
        }
    } else {
        new java.util.zip.ZipInputStream(new FileInputStream(tmp)).withCloseable { zis ->
            def entry
            while ((entry = zis.nextEntry) != null) {
                if (entry.isDirectory()) continue
                if (new File(entry.name).name != binName) continue
                new File(binDir, binName).bytes = zis.readAllBytes()
            }
        }
    }
    tmp.delete()
    binary.setExecutable(true)
    println "${color('green', "Installed Bausteinsicht to ${binary.absolutePath}")}"
}

// --- 4. Hint agents to use it (only if the project has an agent file) --------
def block = """\
## Diagrams — use Bausteinsicht
**Only when you are creating or changing architecture diagrams:** use Bausteinsicht
(architecture-as-code) instead of hand-writing PlantUML — edit the JSON model and run
`bausteinsicht sync`. Run `bausteinsicht --help` first to discover the current commands.
Not installed? Run `./dtcw installBausteinsicht`.
Docs: https://doctoolchain.org/Bausteinsicht/"""

def updated = AgentHints.upsert(new File(docDir), block)
if (updated) {
    println "${color('green', "Added a Bausteinsicht hint for agents to ${updated.name}")}"
} else {
    println "${color('yellow', 'No AGENTS.md or CLAUDE.md found — skipped the agent hint.')}"
    println "  (Create one of them and re-run to let LLM agents know about Bausteinsicht.)"
}

// --- 5. PATH guidance -------------------------------------------------------
def path = System.getenv('PATH') ?: ''
if (!(path.split(File.pathSeparator) as List).contains(binDir.absolutePath)) {
    println ""
    println "${color('cyan', "'${binDir.absolutePath}' is not on your PATH.")}"
    println "To call 'bausteinsicht' directly, add it — e.g.:"
    if (os == 'windows') {
        println "  setx PATH \"${binDir.absolutePath};%PATH%\""
    } else {
        println "  export PATH=\"${binDir.absolutePath}:\$PATH\"   # add to ~/.bashrc or ~/.zshrc to persist"
    }
}

