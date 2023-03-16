// Minimal configuration parameters needed for the test suite
outputPath = 'build/test/docs'

// Minimal configuration parameters for `exportStructurizr` task
structurizr = [:]

structurizr.with {

    workspace = {
        path = './src/test/testData/structurizr/default'
        // The workspace filename is provided without extension. 
        // filename = 'workspace'
    }

    export = {
        outputPath = './src/test/docs/structurizr'
        // format = 'plantuml'
    }
}
