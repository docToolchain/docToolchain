
inputPath = 'docs'

outputPath = '../../../build/test/docs/asciidoctor'

inputFiles = [
        [file: 'broken_images.adoc', formats: ['html']],
             ]

taskInputsDirs = [
    "${inputPath}/",
]

taskInputsFiles = [
    "${inputPath}/broken_images.adoc"
]
