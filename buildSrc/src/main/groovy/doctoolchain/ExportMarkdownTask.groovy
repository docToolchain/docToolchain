package doctoolchain

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

import static nl.jworks.markdown_to_asciidoc.Converter.convertMarkdownToAsciiDoc

class ExportMarkdownTask extends DefaultTask {

    private static final String MARKDOWN_EXTENSION = '.md'
    private static final String ADOC_EXTENSION = '.adoc'

    @InputDirectory
    @SkipWhenEmpty
    File srcDir

    @OutputDirectory
    File destinationDir

    @TaskAction
    exportMarkdown() {

        project.copy {
            from(srcDir)
            rename(/(.+)$MARKDOWN_EXTENSION/, "\$1$ADOC_EXTENSION")
            filter(Markdown2AdocFilter)
            into(destinationDir)
        }
    }

    static class Markdown2AdocFilter extends FilterReader {
        Markdown2AdocFilter(Reader input) {
            super(new StringReader(convertMarkdownToAsciiDoc(input.text)))
        }

    }
}

