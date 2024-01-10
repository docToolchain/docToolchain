package org.docToolchain.atlassian.transformer

import org.docToolchain.atlassian.constants.ConfluenceTags
import org.jsoup.nodes.Element

class CodeBlockTransformer {

    final Set<String> SUPPORTED_LANGUAGES = [
        'actionscript3',
        'applescript',
        'bash',
        'c#',
        'cpp',
        'css',
        'coldfusion',
        'delphi',
        'diff',
        'erl',
        'groovy',
        'xml',
        'java',
        'jfx',
        'js',
        'php',
        'perl',
        'text',
        'powershell',
        'py',
        'ruby',
        'sql',
        'sass',
        'scala',
        'vb',
        'yml'
    ]

    final LANGUAGE_MAPPING = [
        'json' : 'yml', // acceptable workaround
        'shell': 'bash',
        'yaml' : 'yml'
    ]

    protected List<Element> transformCodeBlock(Element body) {
        return body.select('pre > code').each { code ->
            def language = code.attr('data-lang')
            if (language) {
                if (LANGUAGE_MAPPING.containsKey(language)) {
                    // fix some known languages using a mapping
                    language = LANGUAGE_MAPPING[language]
                }
                if (!(language in SUPPORTED_LANGUAGES)) {
                    // fall back to plain text to avoid error messages when rendering
                    language = 'text'
                }
                // #1265 - pacoVK: fix for nested CDATA sections in XML code blocks
                if (language.equals("xml")) {
                    String xmlDocument = code.wholeOwnText()
                    if (xmlDocument.contains("<![CDATA[") && xmlDocument.contains("]]>")) {
                        xmlDocument = xmlDocument.replaceAll("]]>", "]]]]><![CDATA[>")
                        code.text(xmlDocument)
                    }
                }
            } else {
                // Confluence default is Java, so prefer explicit plain text
                language = 'text'
            }

            code.select('span[class]').each { span ->
                span.unwrap()
            }
            code.select('i[class]').each { i ->
                i.unwrap()
            }
            code.select('b').each { b ->
                b.before(" // ")
                b.unwrap()
            }
            code.before("<ac:parameter ac:name=\"language\">${language}</ac:parameter>")
            code.parent() // pre now
                .wrap('<ac:structured-macro ac:name="code"></ac:structured-macro>')
                .unwrap()
            code.wrap("<ac:plain-text-body>" +
                "${ConfluenceTags.CDATA_PLACEHOLDER_START}${ConfluenceTags.CDATA_PLACEHOLDER_END}" +
                "</ac:plain-text-body>")
                .unwrap()
        }
    }
}
