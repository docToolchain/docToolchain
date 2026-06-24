// docToolchain v4 — AgentHints
//
// Upserts a short, heading-anchored note into a project's agent instructions
// file (AGENTS.md or CLAUDE.md) so that LLM agents know an ecosystem tool is
// available and when to use it.
//
// The note is anchored on its own Markdown H2 heading rather than HTML comment
// markers: on re-run the existing section (from its heading up to the next
// heading of the same or higher level) is replaced in place, so the operation
// is idempotent and never touches content outside that section. The only
// caveat — documented for callers — is that a manually renamed heading makes
// the next upsert append a second block instead of replacing the old one.

class AgentHints {

    // Targeted in this order; the first that exists is updated.
    static final List<String> AGENT_FILES = ['AGENTS.md', 'CLAUDE.md']

    // When no agent file exists yet, ensure() seeds this vendor-neutral one.
    static final String DEFAULT_AGENT_FILE = 'AGENTS.md'

    /**
     * Upsert {@code block} into the project's agent file under {@code docDir}.
     * Does not create a file: returns {@code null} when none exists, leaving the
     * decision to bootstrap one to the caller (see {@link #ensure}).
     * @return the file written, or {@code null} if no agent file exists.
     */
    static File upsert(File docDir, String block) {
        def target = AGENT_FILES.collect { new File(docDir, it) }.find { it.exists() }
        if (!target) return null
        target.setText(merge(target.getText('UTF-8'), block), 'UTF-8')
        return target
    }

    /**
     * Like {@link #upsert}, but bootstraps a fresh {@value #DEFAULT_AGENT_FILE}
     * seeded with {@code block} when the project has neither agent file yet.
     * An existing file is updated in place, idempotently. So after install the
     * project always carries the contract, whether or not it had an agent file.
     * @return the file written (never {@code null}).
     */
    static File ensure(File docDir, String block) {
        def target = AGENT_FILES.collect { new File(docDir, it) }.find { it.exists() }
        boolean created = target == null
        if (created) target = new File(docDir, DEFAULT_AGENT_FILE)
        def existing = target.exists() ? target.getText('UTF-8') : ''
        target.setText(merge(existing, block), 'UTF-8')
        return target
    }

    /**
     * Pure string merge of {@code block} into {@code existing}. Exposed so the
     * behaviour can be unit-tested without touching the filesystem.
     */
    static String merge(String existing, String block) {
        def blockLines = block.trim().readLines()
        def heading = blockLines[0].trim()
        int level = heading.takeWhile { it == ('#' as char) }.length()
        if (level == 0) {
            throw new IllegalArgumentException("AgentHints block must start with a Markdown heading, got: ${heading}")
        }

        def lines = existing.readLines()
        int start = lines.findIndexOf { it.trim() == heading }

        if (start < 0) {
            // Heading not present yet — append the block at the end.
            def base = existing.replaceAll(/\s+$/, '')
            return (base.isEmpty() ? '' : base + '\n\n') + block.trim() + '\n'
        }

        // Replace from the heading up to the next heading of the same or
        // higher level (or end of file).
        int end = lines.size()
        for (int i = start + 1; i < lines.size(); i++) {
            def m = (lines[i] =~ /^(#{1,6})\s/)
            if (m && (m[0][1] as String).length() <= level) { end = i; break }
        }

        def head = lines[0..<start]
        while (!head.isEmpty() && head[-1].trim().isEmpty()) head.remove(head.size() - 1)

        def tail = (end < lines.size()) ? new ArrayList(lines[end..<lines.size()]) : []
        while (!tail.isEmpty() && tail[0].trim().isEmpty()) tail.remove(0)

        def out = []
        out.addAll(head)
        if (!head.isEmpty()) out.add('')
        out.addAll(block.trim().readLines())
        if (!tail.isEmpty()) { out.add(''); out.addAll(tail) }
        return out.join('\n') + '\n'
    }
}
