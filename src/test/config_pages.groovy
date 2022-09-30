
inputPath = './src/test/docs'

outputPath = 'build/test/docs'

//this input path is ignored
//inputPath = 'src/test/docs'

inputFiles = [
        [file: 'withPreamble.adoc',   formats: ['html']],
        [file: 'withoutPreamble.adoc',formats: ['html']],
             ]

taskInputsDirs = [
                  "${inputPath}/",
                 ]
