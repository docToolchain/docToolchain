
// Path where the docToolchain will produce the output files.
// This path is appended to the docDir property specified in gradle.properties
// or in the command line, and therefore must be relative to it.
outputPath = 'build'

// Path where the docToolchain will search for the input files.
// This path is appended to the docDir property specified in gradle.properties
// or in the command line, and therefore must be relative to it.
inputPath = '.'

//
inputFiles = [
        [file: 'arc42-template.adoc', formats: ['html','pdf','docbook']],
        [file: 'ppt/Demo.pptx.ad', formats: ['revealjs']]
             ]

taskInputsDirs = ["${inputPath}/src",
                  "${inputPath}/images",
                 ]

taskInputsFiles = ["${inputPath}/arc42-template.adoc"]
