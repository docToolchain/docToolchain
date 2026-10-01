inline_macro(name: "jira") { parent, target, attributes ->
    def jiraBaseUrl = parent.document.getAttribute('jira-base-url')
    def options = [
        "type": ":link",
        "target": "${jiraBaseUrl}/browse/${target}".toString(),
        "id": "${target}"
    ]
    if (!jiraBaseUrl) {
        println(">>> WARN: No Jira API URL found in config, the Jira extension may not work as expected.")
    }
    createPhraseNode(parent, "anchor", target, attributes, options).render()
}

include_processor(filter: { it.contains("example\$") }) { document, reader, target, attributes ->
    def baseDir = new File(reader.getDir()).parentFile
    def rawContent = new File(reader.getFile()).text.replace("example\$", "${baseDir}/examples/")
    def matcher = (rawContent =~ /include::[^\[]+/)

    if (matcher.find()) {
        def content = matcher.group().replace("example\$", "${baseDir}/examples/") + "[]"
        reader.pushInclude(content, target, target, 1, attributes)
    }
}