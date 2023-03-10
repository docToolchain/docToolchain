
inputPath = './src/test/docs'

outputPath = 'build/test/docs'

//this input path is ignored
//inputPath = 'src/test/docs'

inputFiles = [
        [file: 'simplePresentation.adoc',   formats: ['revealjs',]],
        [file: 'test.adoc',                 formats: ['html','pdf','docbook']],
             ]

taskInputsDirs = [
                  "${inputPath}/",
                 ]

taskInputsFiles = [
                    "${inputPath}/test.adoc",
                    "${inputPath}/simplePresentation.adoc"
                  ]

changelog = [:]

changelog.with {
    dir = 'src/test/docs'
    cmd = 'git log --pretty=format:%x7c%x20%ad%x20%n%x7c%x20%an%x20%n%x7c%x20%s%x20%n --date=short'

}

confluence = [:]

confluence.with {
// for exportConfluence-Task
    export = [
        srcDir : 'src/test/testConfluenceSpace',
        destDir: 'src/test/build/exportConfluenceSpec'
    ]
}