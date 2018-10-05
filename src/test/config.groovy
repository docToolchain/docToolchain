
outputPath = 'build/test/docs'

inputPath = 'src/test/docs'

inputFiles = [
        [file: 'test.adoc', formats: ['html','revealjs','pdf','docbook']],
             ]

taskInputsDirs = [
                  "${inputPath}/",
                 ]

taskInputsFiles = [
                    "${inputPath}/test.adoc"
                  ]
