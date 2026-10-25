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
                // Try to detect language from class attribute or parent elements
                language = detectLanguageFromElement(code)
                if (!language) {
                    // Confluence default is Java, so prefer explicit plain text
                    language = 'text'
                }
            }

            // Enhanced cleanup for better compatibility with AsciiDoctor v3.4.2+
            cleanupSyntaxHighlighting(code)
            
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
    
    /**
     * Enhanced language detection for compatibility with newer AsciiDoctor versions
     */
    private String detectLanguageFromElement(Element code) {
        // Check class attribute for language hints
        def classAttr = code.attr('class')
        if (classAttr) {
            // Common patterns: "language-java", "hljs java", "java", etc.
            def langMatch = classAttr =~ /(?:language-|hljs\s+|^)([a-zA-Z0-9+#-]+)/
            if (langMatch) {
                def language = langMatch[0][1]
                if (LANGUAGE_MAPPING.containsKey(language)) {
                    language = LANGUAGE_MAPPING[language]
                }
                if (language in SUPPORTED_LANGUAGES) {
                    return language
                }
            }
        }
        
        // Check parent elements for language information
        def current = code.parent()
        while (current != null) {
            def lang = current.attr('data-lang')
            if (lang) {
                if (LANGUAGE_MAPPING.containsKey(lang)) {
                    lang = LANGUAGE_MAPPING[lang]
                }
                if (lang in SUPPORTED_LANGUAGES) {
                    return lang
                }
            }
            
            def parentClass = current.attr('class')
            if (parentClass) {
                // Look for language patterns in parent class names
                def patterns = [
                    /language-([a-zA-Z0-9+#-]+)/,
                    /highlight-([a-zA-Z0-9+#-]+)/,
                    /listingblock\s+([a-zA-Z0-9+#-]+)/
                ]
                
                for (pattern in patterns) {
                    def matcher = parentClass =~ pattern
                    if (matcher) {
                        def language = matcher[0][1]
                        if (LANGUAGE_MAPPING.containsKey(language)) {
                            language = LANGUAGE_MAPPING[language]
                        }
                        if (language in SUPPORTED_LANGUAGES) {
                            return language
                        }
                    }
                }
            }
            
            current = current.parent()
        }
        
        return null
    }
    
    /**
     * Enhanced cleanup for better compatibility with newer AsciiDoctor versions
     * that may generate different HTML structures
     */
    private void cleanupSyntaxHighlighting(Element code) {
        // Remove syntax highlighting spans that may interfere with Confluence rendering
        code.select('span[class]').each { span ->
            span.unwrap()
        }
        
        // Remove italic highlighting elements  
        code.select('i[class]').each { i ->
            i.unwrap()
        }
        
        // Convert bold elements to comments (preserve semantic meaning)
        code.select('b').each { b ->
            b.before(" // ")
            b.unwrap()
        }
        
        // Remove other common syntax highlighting elements that may be present
        // in newer AsciiDoctor versions
        code.select('.hljs-keyword, .hljs-string, .hljs-comment, .hljs-number, .hljs-literal, .hljs-symbol, .hljs-name').each { elem ->
            elem.unwrap()
        }
        
        // Remove any remaining highlighting wrapper elements
        code.select('[class*="highlight"], [class*="hljs"]').each { elem ->
            elem.unwrap()
        }
    }
}
