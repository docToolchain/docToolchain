
inputPath = 'src/test/testPandoc/docs'

outputPath = 'build/test/docs/pandoc'

inputFiles = [
        [file: 'simple.adoc', formats: ['docbook']],
             ]

taskInputsDirs = [
    "${inputPath}/",
]

taskInputsFiles = [
    "${inputPath}/simple.adoc"
]
