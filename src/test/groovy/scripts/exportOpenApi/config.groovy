outputPath = 'build'

openApi = [:]

openApi.with {
    specFile = 'src/petstore-v3.0.yaml'
    infoUrl = 'https://my-api.company.com'
    infoEmail = 'info@company.com'
}
