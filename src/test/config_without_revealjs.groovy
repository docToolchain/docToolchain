
outputPath = 'build/test/docs'

inputPath = 'src/test/docs'

inputFiles = [
        [file: 'test.adoc', formats: ['html','pdf','docbook']],
             ]

taskInputsDirs = [
                  "${inputPath}/",
                 ]

taskInputsFiles = [
                    "${inputPath}/test.adoc"
                  ]
