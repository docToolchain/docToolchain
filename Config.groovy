
// Path where the docToolchain will produce the output files.
// This path is appended to the docDir property specified in gradle.properties
// or in the command line, and therefore must be relative to it.
outputPath = 'build/docs'

// Path where the docToolchain will search for the input files.
// This path is appended to the docDir property specified in gradle.properties
// or in the command line, and therefore must be relative to it.
inputPath = 'src/docs'

inputFiles = [[file: 'test.adoc',              formats: ['html','pdf','docbook']],
              [file: 'manual.adoc',            formats: ['html','pdf']],
              [file: 'ppt/Demo.pptx.ad',       formats: ['html','revealjs']],
             ]

taskInputsDirs = ["${inputPath}/images",
                 ]

taskInputsFiles = []
