package org.docToolchain.configuration

class ConfigService {
    final private ConfigObject config

    ConfigService(ConfigObject config) {
        this.config = config
    }

    Object getConfigProperty(String propertyPath) {
        def property = config.get(propertyPath)
        if(!property){
            property = config.flatten().get(propertyPath)
        }
        return property
    }

    Map getFlatConfigSubTree(String propertyPath) {
       return config.flatten().inject([:]) { result, key, value ->
            if(key.startsWith(propertyPath)){
                result.put(key.replaceFirst("${propertyPath}.", ""), value)
            }
            return result
        }
    }
}
