
outputPath = '../build/test/docs'

inputPath = 'docs'

inputFiles = [
        [file: 'test.adoc', formats: ['html','revealjs','pdf','docbook']],
             ]

taskInputsDirs = [
                  "${inputPath}/",
                 ]

taskInputsFiles = [
                    "${inputPath}/test.adoc"
                  ]
