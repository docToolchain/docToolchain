
inputPath = 'testPdfDefaultThemeDocs'

outputPath = '../build/test/docs'

inputFiles = [
        [file: 'test2.adoc', formats: ['pdf']],
             ]

taskInputsDirs = [
                  "${inputPath}/",
                 ]

taskInputsFiles = [
                    "${inputPath}/test2.adoc"
                  ]
