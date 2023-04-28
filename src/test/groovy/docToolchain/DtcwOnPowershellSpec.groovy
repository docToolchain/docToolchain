package docToolchain

import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Unroll

class DtcwOnPowershellSpec extends Specification {
    List powershell(List command) {
        def shell = ['powershell.exe', '-ExecutionPolicy', 'Unrestricted']
        // def setup = ['$HOME = \\"'+new File('./build/home').canonicalPath+'\\"',';']
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
            def (out,err) = powershell(['pwd'])
        then: 'there is no error and the output contains "Path"'
            err == ""
            out.contains('Path')
    }

    void 'test dtcw without parameters'() {
        when: '"./dtcw.ps1" is executed without any parameters'
            def (out,err) = powershell(['./dtcw.ps1'])
        then: 'we get an assertion and the usage is printed out'
            err.contains('WARNING : argument missing')
            out.contains('Usage: ./dtcw')
            out.contains('Examples:')
    }

    void 'test dtcw --version'() {
        when: '"./dtcw.ps1 --version" is executed'
        def (out,err) = powershell(['./dtcw.ps1', '--version'])
        then: 'we get no error and just the version'
        err == ''
        out.contains('dtcw ')
        out.contains('docToolchain ')
        !out.toLowerCase().contains('available doctoolchain environments')
    }

    void 'test local installation of jdk'() {
        when: '"./dtcw.ps1 install java" is executed'
            def (out,err) = powershell(['./dtcw.ps1', 'install', 'java'])
            println "out: "+out
            println "err: "+err
        then: 'there is no error'
            err == ""
    }

    void 'test local installation of doctoolchain'() {
        setup: 'remove jdk folder'
            rm "$HOME/.doctoolchain/jdk"
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
}
