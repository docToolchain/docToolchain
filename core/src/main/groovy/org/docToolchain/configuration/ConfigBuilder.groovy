package org.docToolchain.configuration

class ConfigBuilder {

    final private File configFile
    private ConfigSlurper configSlurper = new ConfigSlurper()

    ConfigBuilder(String docDir, String mainConfigFile) {
        this.configFile = new File(docDir, mainConfigFile)
    }

    ConfigBuilder prepareConfigFileIfNotExists(closure){
        if(!configFile.exists()){
            closure.run()
        }
        return this
    }

    ConfigObject build(){
        if(!configFile.exists()){
            throw new FileNotFoundException("Config file does not exist: ${configFile.getCanonicalPath()}")
        }
        ConfigObject config = configSlurper.parse(configFile.text)
        config.put("docDir", configFile.getParent())
        config.put("mainConfigFile", configFile.getName())
        return config
    }
}
