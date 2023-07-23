// Minimal configuration parameters needed for the test suite
outputPath = 'build/test/docs/collectIncludes'

collectIncludes = [:]

collectIncludes.with {
    excludeDirectories = ['excludedDir']
}
