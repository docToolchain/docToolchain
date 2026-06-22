package org.docToolchain.scripts

import spock.lang.Specification
import spock.lang.TempDir

/**
 * Acceptance tests for the v4 copyLandingPage task script (issue #1663).
 *
 * The script is run the way dtcw4 and the v4 smoke test run task scripts:
 * direct JVM execution via groovy.ui.GroovyMain, with -DdocDir pointing at the
 * project. This exercises the real path resolution, the overwrite guard and the
 * actual file copy — no logic is duplicated in the test.
 */
class CopyLandingPageSpec extends Specification {

    @TempDir
    File projectDir

    // repo root: where scripts/copyLandingPage.groovy lives (core:test runs from core/)
    private static final File REPO_ROOT = ['.', '..', '../..']
            .collect { new File(it).canonicalFile }
            .find { new File(it, 'scripts/copyLandingPage.groovy').exists() }

    def setupSpec() {
        assert REPO_ROOT != null: 'could not locate scripts/copyLandingPage.groovy'
    }

    private void writeConfig(String micrositeBody) {
        new File(projectDir, 'docToolchainConfig.groovy').text = """
            outputPath = 'build'
            inputPath  = 'src/docs'
            microsite {
            ${micrositeBody}
            }
        """.stripIndent()
        new File(projectDir, 'src/docs').mkdirs()
    }

    // the place the script writes to for siteFolder '../site': <inputPath>/../site/doc
    private File landingPage() {
        new File(projectDir, 'src/site/doc/landingpage.gsp')
    }

    private Map run(List<String> args) {
        def java = new File(System.getProperty('java.home'), 'bin/java').absolutePath
        def cmd = [java,
                   '-cp', System.getProperty('java.class.path'),
                   "-DdocDir=${projectDir.absolutePath}".toString(),
                   '-DDTC_HEADLESS=true',
                   'groovy.ui.GroovyMain',
                   'scripts/copyLandingPage.groovy'] + args
        def proc = cmd.execute((String[]) null, REPO_ROOT)
        def out = new StringBuilder()
        def err = new StringBuilder()
        proc.consumeProcessOutput(out, err)
        proc.waitForOrKill(120000)
        [exit: proc.exitValue(), out: out.toString(), err: err.toString()]
    }

    def "scaffolds a landing page as the microsite start page"() {
        given: 'a project without a custom landing page'
            writeConfig("    siteFolder = '../site'\n    landingPage = 'landingpage.gsp'")
            assert !landingPage().exists()

        when: 'copyLandingPage runs'
            def r = run([])

        then: 'it succeeds and creates the landing page in the site theme doc folder'
            r.exit == 0
            landingPage().exists()
            landingPage().text.contains('dtc-landing')
    }

    def "does not overwrite an existing landing page without --force"() {
        given: 'a landing page the user has already edited'
            writeConfig("    siteFolder = '../site'\n    landingPage = 'landingpage.gsp'")
            landingPage().parentFile.mkdirs()
            landingPage().text = 'MY OWN LANDING PAGE'

        when: 'copyLandingPage runs without --force'
            def r = run([])

        then: 'the existing file is left unchanged and the run reports the conflict'
            r.exit != 0
            landingPage().text == 'MY OWN LANDING PAGE'
    }

    def "--force overwrites an existing landing page"() {
        given: 'a landing page the user wants to reset to the template'
            writeConfig("    siteFolder = '../site'\n    landingPage = 'landingpage.gsp'")
            landingPage().parentFile.mkdirs()
            landingPage().text = 'MY OWN LANDING PAGE'

        when: 'copyLandingPage runs with --force'
            def r = run(['--force'])

        then: 'the landing page is replaced with the template'
            r.exit == 0
            landingPage().text.contains('dtc-landing')
    }
}
