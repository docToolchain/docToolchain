@Grapes(
    [@Grab('org.jsoup:jsoup:1.8.2'),]
)
import org.jsoup.Jsoup
import org.jsoup.select.NodeVisitor
import org.jsoup.parser.Parser
import org.jsoup.nodes.Entities.EscapeMode
import org.jsoup.nodes.Document
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements

public class MyVisitor implements org.jsoup.select.NodeVisitor {
    static String out = ""
    static Map data = [:]
    static parent

    @Override
    public void head(org.jsoup.nodes.Node node, int depth) {
        headOrTail(node, depth as Integer, 'enter')
    }
    @Override
    public void tail(org.jsoup.nodes.Node node, int depth) {
        headOrTail(node, depth as Integer, 'exit')
    }
    public static void tail(org.jsoup.nodes.Node node, Integer depth) {
        headOrTail(node, depth as Integer, 'exit')
    }
    public static void head(org.jsoup.nodes.Node node, Integer depth) {
        headOrTail(node, depth as Integer, 'enter')
    }
    public static void headOrTail(node, depth, event) {
        if (node instanceof TextNode) {
            if (event == 'enter') {
                out += node.text()
            }
        } else {
            def nodeName = node.nodeName().toLowerCase()
            def args = [event:event, node:node, depth:depth]
            switch(nodeName) {
                case '#document':
                    break;
                case ~/h[0-9]/: //headline
                    def level   = (nodeName-"h") as Integer
                    nodeName    = "h"
                    args.level = level
                default:
                    if (parent.metaClass.respondsTo(parent, nodeName)) {
                        out += parent.(nodeName)(args)
                    } else if (parent.metaClass.respondsTo(parent, event + "_" + nodeName)) {
                        out += parent.(event + "_" + nodeName)(args)
                    } else {
                        // if there is no general handler
                        // and no handler for $event
                        // check if there is also no handler for the opposite event
                        // and output a warning that there is no handler at all for this tag
                        if (!parent.metaClass.respondsTo(parent, (event=='enter'?'exit':'enter') + "_" + nodeName)) {
                            println "no handler for " + nodeName
                        }
                    }
            }
        }
    }
}

def html = """
<div>
<h1>test</h1>
<p>lorem <b>ipsum</b></p>
</div>
<div>zwei</div>
<test></test>
"""
Document dom = Jsoup.parse(html, 'utf-8', Parser.xmlParser())
dom.outputSettings().prettyPrint(false);//makes html() preserve linebreaks and spacing
dom.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml); //This will ensure xhtml validity regarding entities
dom.outputSettings().charset("UTF-8"); //does no harm :-)

def myVisitor = new MyVisitor(parent:this)

dom.traverse(myVisitor)
println myVisitor.out

def exit_div(params) {
    '\n\n'
}
def exit_p(params) {
    '\n\n'
}
def b(params) {
    '**'
}
def i(params) {
    '__'
}
def enter_h(params) {
    '\n'+'=' * (params.level+1)+' '
}
def exit_h(params) {
    '\n\n'
}
