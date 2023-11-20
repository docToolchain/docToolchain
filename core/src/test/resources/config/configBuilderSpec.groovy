outputPath = 'build/docs'

inputPath = 'src/docs'

jbake.with {
    // possibility to configure additional asciidoctorj plugins used by jbake
    plugins = [ ]

    // possibiltiy to configure additional asciidoctor attributes passed to the jbake task
    asciidoctorAttributes = [ ]
}

confluence = [:]

confluence.with {
    input = [
        [ file: "build/docs/html5/arc42-template-de.html" ],
    ]
    api = 'https://my.confluence'
    spaceKey = 'asciidoc'
}
