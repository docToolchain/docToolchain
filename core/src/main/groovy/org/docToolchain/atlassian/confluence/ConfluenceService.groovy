package org.docToolchain.atlassian.confluence

import org.docToolchain.configuration.ConfigService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities
import org.jsoup.parser.Parser

class ConfluenceService {

    private final ConfigService configService

    ConfluenceService(ConfigService configService) {
        this.configService = configService
    }

    String checkAndBuildCanonicalFileName(String filename) {
            String canonicalFilePath = "${configService.getConfigProperty("docDir")}/${filename.trim()}".trim()
            println "publish ${filename}"

            if (filename ==~ /.*[.](ad|adoc|asciidoc)$/) {
                println "HINT:"
                println "please first convert ${canonicalFilePath} to html by executing generateHTML"
                println "the generated file will be found in ${configService.getConfigProperty("outputPath")}/html5/. and has to be referenced instead of the .adoc file"
                throw new RuntimeException("config problem")
            }
            return canonicalFilePath
    }

    Document parseFile(File htmlFile) {
        String html = htmlFile.getText('utf-8')
        Document dom = Jsoup.parse(html, 'utf-8', Parser.xmlParser())
        dom.outputSettings().prettyPrint(false)//makes html() preserve linebreaks and spacing
        dom.outputSettings().escapeMode(Entities.EscapeMode.xhtml) //This will ensure xhtml validity regarding entities
        dom.outputSettings().charset("UTF-8") //does no harm :-)
        return dom
    }

    ArrayList getKeywords(Document dom) {
        ArrayList keywords = new ArrayList()
        dom.select('meta[name=keywords]').each { kw ->
            kw.attr('content').split(',').each { skw ->
                keywords << skw.trim()
            }
            println "Keywords:" + keywords
        }
        return keywords
    }
}
