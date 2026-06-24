package org.docToolchain.scripts

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

/**
 * Locks in {@code scripts/lib/AgentHints.groovy}: the heading-anchored, idempotent
 * upsert of an agent-contract block into AGENTS.md / CLAUDE.md, and the ensure()
 * bootstrap that seeds a fresh AGENTS.md when a project has no agent file yet
 * (issue #1676). Loaded via GroovyClassLoader exactly as the install task scripts
 * load it at runtime.
 */
class AgentHintsSpec extends Specification {

    @Shared Class agentHints

    @TempDir
    File projectDir

    private static final String BLOCK = '''\
## Diagrams — use Bausteinsicht
**Only when you change diagrams:** use Bausteinsicht.'''

    def setupSpec() {
        def scriptFile = ['../scripts/lib/AgentHints.groovy', 'scripts/lib/AgentHints.groovy']
            .collect { new File(it) }.find { it.exists() }
        assert scriptFile != null : 'could not locate scripts/lib/AgentHints.groovy'
        def gcl = new GroovyClassLoader(getClass().classLoader)
        agentHints = gcl.parseClass(scriptFile)
    }

    def "upsert returns null and writes nothing when no agent file exists"() {
        when:
            def result = agentHints.upsert(projectDir, BLOCK)

        then:
            result == null
            !new File(projectDir, 'AGENTS.md').exists()
            !new File(projectDir, 'CLAUDE.md').exists()
    }

    def "ensure creates AGENTS.md with the block when no agent file exists"() {
        when:
            def written = agentHints.ensure(projectDir, BLOCK)

        then:
            written.name == 'AGENTS.md'
            written.exists()
            written.text.contains('## Diagrams — use Bausteinsicht')
            !new File(projectDir, 'CLAUDE.md').exists()
    }

    def "ensure updates an existing AGENTS.md in place without clobbering other content"() {
        given: 'an AGENTS.md the user already maintains'
            def agents = new File(projectDir, 'AGENTS.md')
            agents.text = '# My project\n\nSome existing guidance.\n'

        when:
            def written = agentHints.ensure(projectDir, BLOCK)

        then:
            written == agents
            agents.text.contains('Some existing guidance.')
            agents.text.contains('## Diagrams — use Bausteinsicht')
    }

    def "ensure prefers an existing CLAUDE.md over creating AGENTS.md"() {
        given:
            new File(projectDir, 'CLAUDE.md').text = '# Claude\n'

        when:
            def written = agentHints.ensure(projectDir, BLOCK)

        then:
            written.name == 'CLAUDE.md'
            !new File(projectDir, 'AGENTS.md').exists()
    }

    def "ensure is idempotent — re-running does not duplicate the block"() {
        when: 'ensure runs twice'
            agentHints.ensure(projectDir, BLOCK)
            def written = agentHints.ensure(projectDir, BLOCK)

        then: 'the heading appears exactly once'
            written.text.count('## Diagrams — use Bausteinsicht') == 1
    }
}
