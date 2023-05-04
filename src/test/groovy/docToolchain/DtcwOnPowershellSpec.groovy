package docToolchain

import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Unroll

class DtcwOnPowershellSpec extends Specification {
    List powershell(List command) {
        def shell = ['pwsh', '-ExecutionPolicy', 'Unrestricted']
        //def setup = ['$HOME = \\"'+new File('./build/home').canonicalPath+'\\"',';']
        def process = (shell+command).execute(null, new File('.'))
        def sout = new StringBuilder()
        def serr = new StringBuilder()
        process.consumeProcessOutput(sout, serr)
        def p = process.waitForOrKill(5000)
        return [sout.toString(), serr.toString()]
    }

    @Unroll
    void 'test Powershell'() {
        when: 'pwd is executed'
            def (out,err) = powershell(['-Command', 'Get-Location'])
        then: 'there is no error and the output contains "Path"'
            err == ""
            out.contains('Path')
    }

    @Unroll
    void 'test dtcw without parameters'() {
        when: '"./dtcw.ps1" is executed without any parameters'
            def (out,err) = powershell(['./dtcw.ps1'])
        then: 'we get an assertion and the usage is printed out'
            err == ""
            out.contains('Usage: ./dtcw')
            out.contains('Examples:')
    }

    @Unroll
    void 'test dtcw --version'() {
        when: '"./dtcw.ps1 --version" is executed'
        def (out,err) = powershell(['./dtcw.ps1', '--version'])
        then: 'we get no error and just the version'
        err == ''
        out.contains('dtcw ')
        out.contains('docToolchain ')
        !out.toLowerCase().contains('available doctoolchain environments')
    }

    @Unroll
    void 'test local installation of jdk'() {
        when: '"./dtcw.ps1 install java" is executed'
            def (out,err) = powershell(['./dtcw.ps1', 'install', 'java'])
            println "out: "+out
            println "err: "+err
        then: 'there is no error'
            err == ""
    }

    @Unroll
    void 'test local installation of doctoolchain'() {
        //setup: 'remove jdk folder'
        //    rm "$HOME/.doctoolchain/jdk"
        when: '"./dtcw.ps1 local install" is executed for the first time'
            def (out,err) = powershell(['./dtcw.ps1','local','install'])
            println "out: "+out
            println "err: "+err
        then: 'there is no error'
            err == ""
        when: '"./dtcw.ps1 local install" is executed a second time'
            (out,err) = powershell(['./dtcw.ps1','local','install'])
            println "out: "+out
            println "err: "+err
        then: 'there is no error'
            err == ""
    }
    // tests provided by chatGPT

    @Unroll
    void 'test dtcw with invalid command'() {
        when: 'an invalid command is provided'
            def (out, err) = powershell(['./dtcw.ps1', 'local', 'invalidCommand'])
        then: 'there is an error and the output contains "Invalid command"'
            out.contains('WARNING: doctoolchain - command not found')
            err == ""
    }

    @Unroll
    void 'test dtcw with tasks command'() {
        when: 'an unsupported environment is provided'
            def (out, err) = powershell(['./dtcw.ps1', 'tasks'])
        then: 'there is an error and the output contains "Unsupported environment"'
            err.contains('Unsupported environment')
            out == ""
    }

    @Unroll
    void 'test dtcw with docker environment'() {
        when: 'docker environment is provided and docker is installed'
            def (out, err) = powershell(['./dtcw.ps1', 'docker', 'tasks'])
        then: 'there is no error and the output contains "All tasks runnable"'
            // This test assumes that Docker is installed on the system
            err.contains('Pulling from doctoolchain/doctoolchain')
            out.contains('Using environment: docker')
    }

}
