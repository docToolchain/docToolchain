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
        // Primary processing: Handle standard pre > code structure
        def processedElements = body.select('pre > code').each { code ->
            processCodeElement(code)
        }
        
        // Fallback processing: Handle code elements without pre wrapper
        // This handles cases where AsciiDoctor generates different HTML structure
        body.select('code').findAll { code ->
            !code.parent().tagName().equals('pre') && 
            !code.hasClass('hljs') && 
            code.text().contains('\n') // Multi-line code blocks
        }.each { code ->
            // Wrap standalone multi-line code elements in pre
            code.wrap('<pre></pre>')
            processCodeElement(code)
        }
        
        // Additional fallback: Handle div.listingblock structure (common in newer AsciiDoctor)
        body.select('div.listingblock pre').each { pre ->
            if (!pre.select('code').isEmpty()) {
                def code = pre.select('code').first()
                processCodeElement(code)
            } else {
                // Handle pre without code wrapper
                def language = extractLanguageFromContext(pre)
                wrapPreElementAsCode(pre, language)
            }
        }
        
        return processedElements
    }
    
    private void processCodeElement(Element code) {
        def language = detectLanguage(code)
        
        // Handle special XML content with CDATA sections
        if (language.equals("xml")) {
            String xmlDocument = code.wholeOwnText()
            if (xmlDocument.contains("<![CDATA[") && xmlDocument.contains("]]>")) {
                xmlDocument = xmlDocument.replaceAll("]]>", "]]]]><![CDATA[>")
                code.text(xmlDocument)
            }
        }
        
        // Clean up syntax highlighting elements that may interfere with Confluence
        cleanupSyntaxHighlighting(code)
        
        // Transform to Confluence code macro structure
        transformToConfluenceMacro(code, language)
    }
    
    private String detectLanguage(Element code) {
        def language = null
        
        // Try different methods to detect language
        // 1. Check data-lang attribute
        language = code.attr('data-lang')
        
        // 2. Check class attribute for language hints
        if (!language) {
            def classAttr = code.attr('class')
            if (classAttr) {
                // Common patterns: "language-java", "hljs java", "java", etc.
                def langMatch = classAttr =~ /(?:language-|hljs\s+|^)([a-zA-Z0-9+#-]+)/
                if (langMatch) {
                    language = langMatch[0][1]
                }
            }
        }
        
        // 3. Check parent elements for language information
        if (!language) {
            language = extractLanguageFromContext(code.parent())
        }
        
        // Apply language mapping and validation
        if (language) {
            if (LANGUAGE_MAPPING.containsKey(language)) {
                language = LANGUAGE_MAPPING[language]
            }
            if (!(language in SUPPORTED_LANGUAGES)) {
                language = 'text'
            }
        } else {
            // Confluence default is Java, so prefer explicit plain text
            language = 'text'
        }
        
        return language
    }
    
    private String extractLanguageFromContext(Element element) {
        // Check for language information in parent elements
        def current = element
        while (current != null) {
            // Check for data-lang or class attributes
            def lang = current.attr('data-lang')
            if (lang) return lang
            
            def classAttr = current.attr('class')
            if (classAttr) {
                // Look for language patterns in class names
                def patterns = [
                    /language-([a-zA-Z0-9+#-]+)/,
                    /highlight-([a-zA-Z0-9+#-]+)/,
                    /([a-zA-Z0-9+#-]+)-code/
                ]
                
                for (pattern in patterns) {
                    def matcher = classAttr =~ pattern
                    if (matcher) {
                        return matcher[0][1]
                    }
                }
            }
            
            current = current.parent()
        }
        
        return null
    }
    
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
        
        // Remove other common syntax highlighting elements
        code.select('.hljs-keyword, .hljs-string, .hljs-comment, .hljs-number').each { elem ->
            elem.unwrap()
        }
    }
    
    private void transformToConfluenceMacro(Element code, String language) {
        // Add language parameter before the code element
        code.before("<ac:parameter ac:name=\"language\">${language}</ac:parameter>")
        
        // Wrap the pre element (code's parent) with Confluence code macro
        code.parent() // pre now
            .wrap('<ac:structured-macro ac:name="code"></ac:structured-macro>')
            .unwrap()
            
        // Wrap code content with CDATA placeholder for proper escaping
        code.wrap("<ac:plain-text-body>" +
            "${ConfluenceTags.CDATA_PLACEHOLDER_START}${ConfluenceTags.CDATA_PLACEHOLDER_END}" +
            "</ac:plain-text-body>")
            .unwrap()
    }
    
    private void wrapPreElementAsCode(Element pre, String language) {
        // Handle pre elements that don't have code wrapper
        if (!language) {
            language = 'text'
        }
        
        // Apply language mapping and validation
        if (LANGUAGE_MAPPING.containsKey(language)) {
            language = LANGUAGE_MAPPING[language]
        }
        if (!(language in SUPPORTED_LANGUAGES)) {
            language = 'text'
        }
        
        // Create code wrapper
        def codeContent = pre.html()
        pre.html("<code>${codeContent}</code>")
        
        def code = pre.select('code').first()
        processCodeElement(code)
    }
}
