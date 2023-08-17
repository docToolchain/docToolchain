package org.docToolchain.configuration

class ConfigService {
    final private File configFile
    private ConfigSlurper configSlurper = new ConfigSlurper()

    ConfigService(String docDir, String mainConfigFile) {
        this.configFile = new File(docDir, mainConfigFile)
    }

    ConfigObject parseConfigOrDo(closure){
        if(!configFile.exists()){
            closure.run()
        }
        return configSlurper.parse(configFile.text)
    }

    void writeNewConfigFromTemplate(String templatePath){
        configFile.write(new File(templatePath).text)
    }

    String getConfigFilePath() {
        return configFile.getCanonicalPath()
    }
}
