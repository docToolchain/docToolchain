package doctoolchain

import groovy.transform.CompileStatic
import org.gradle.api.tasks.Copy

import static nl.jworks.markdown_to_asciidoc.Converter.convertMarkdownToAsciiDoc

@CompileStatic
class ExportMarkdownTask extends Copy {

    private static final String MARKDOWN_EXTENSION = '.md'
    private static final String ADOC_EXTENSION = '.adoc'

    ExportMarkdownTask() {

        //configure default copy task:
        // 1) to only include markdown files
        include("**/*$MARKDOWN_EXTENSION")
        // 2) to rename all files from *.md to *.adoc
        rename(/(.+)$MARKDOWN_EXTENSION/, "\$1$ADOC_EXTENSION")
        // 3) to convert the file content from Markdown to AsciiDoc
        filter(Markdown2AdocFilter)
        // 4) Do not copy empty folders
        includeEmptyDirs = false
    }

    static class Markdown2AdocFilter extends FilterReader {
        Markdown2AdocFilter(Reader input) {
            super(new StringReader(convertMarkdownToAsciiDoc(input.text)))
        }
    }
}

