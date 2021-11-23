def xml=new XmlSlurper().parse(new File("overview.xml"))
xml=new XmlSlurper().parse(new File("overview.dio.xml.drawio"))
println "test"
println xml.dump()
println xml.inspect()
xml.diagram.mxGraphModel.root.mxCell.each {  cell ->
    if (cell.attributes().edge) {
        println cell.attributes().source +" => "+cell.attributes().target
    } else {
        println cell.attributes().value
    }
}
