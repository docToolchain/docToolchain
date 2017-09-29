
// Path where the docToolchain will produce the output files.
// This path is appended to the docDir property specified in gradle.properties
// or in the command line, and therefore must be relative to it.
outputPath = 'build/docs'

// Path where the docToolchain will search for the input files.
// This path is appended to the docDir property specified in gradle.properties
// or in the command line, and therefore must be relative to it.
inputPath = 'src/docs'

inputFiles = [[file: 'arc42-template-de.adoc', formats: ['html','pdf','docbook']],
              [file: 'arc42-template-en.adoc', formats: ['html','pdf','docbook']],
              [file: 'arc42-template-es.adoc', formats: ['html','pdf','docbook']],
              [file: 'test.adoc',              formats: ['html','pdf','docbook']],
              [file: 'manual.adoc',            formats: ['html']],
              [file: 'ppt/Demo.pptx.ad',       formats: ['html','revealjs']],
             ]

taskInputsDirs = ["${inputPath}/arc42",
                  "${inputPath}/images",
                 ]

taskInputsFiles = ["${inputPath}/arc42-template-de.adoc",
                   "${inputPath}/config-de.adoc",
                   "${inputPath}/arc42-template-en.adoc",
                   "${inputPath}/config-en.adoc",
                   "${inputPath}/arc42-template-es.adoc",
                   "${inputPath}/config-es.adoc",
                   "${outputPath}/changelog.adoc",
                  ]
