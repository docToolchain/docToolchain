package org.docToolchain.configuration

class Config {
    final private File configFile

    Config(String docDir, String mainConfigFile) {
        this.configFile = new File(docDir, mainConfigFile)
    }

    Boolean exists(){
        configFile.exists()
    }

    ConfigObject parseConfig(){
        ConfigSlurper configSlurper = new ConfigSlurper()
        return configSlurper.parse(configFile.text)
    }

    void writeConfigFromTemplate(String templatePath){
        configFile.write(new File(templatePath).text)
    }
}
