package org.docToolchain.configuration

class ConfigService {
    final private ConfigObject config

    ConfigService(ConfigObject config) {
        this.config = config
    }

    Object getConfigProperty(String propertyPath) {
        def property = config.get(propertyPath)
        if(!property){
            Map flatConfig = config.flatten()
            property = flatConfig.get(propertyPath)
            if(!property){
                def configSubTree = flatConfig.inject([:]) { result, key, value-> {
                    if(key.startsWith(propertyPath)){
                        result.put(key.replaceFirst("${propertyPath}.", ""), value)
                    }
                    return result
                }}
                if(configSubTree.size() > 0){
                    property = configSubTree
                }
            }
        }
        return property
    }
}
