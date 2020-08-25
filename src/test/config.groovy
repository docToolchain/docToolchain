
inputPath = './src/test/docs'

outputPath = 'build/test/docs'

//this input path is ignored
//inputPath = 'src/test/docs'

inputFiles = [
        [file: 'test.adoc', formats: ['html','revealjs','pdf','docbook']],
             ]

taskInputsDirs = [
                  "${inputPath}/",
                 ]

taskInputsFiles = [
                    "${inputPath}/test.adoc"
                  ]

changelog = [:]

changelog.with {
    dir = 'src/test/docs'
    cmd = 'git log --pretty=format:%x7c%x20%ad%x20%n%x7c%x20%an%x20%n%x7c%x20%s%x20%n --date=short'

}
