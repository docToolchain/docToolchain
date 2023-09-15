package org.docToolchain.configuration

class ConfigService {
    final private ConfigObject config

    ConfigService(ConfigObject config) {
        this.config = config
    }

    Object getConfigProperty(String propertyPath) {
        def property = config.get(propertyPath)
        if(!property){
            return config.flatten().get(propertyPath)
        }
        return property
    }
}
