package org.docToolchain.configuration

import org.docToolchain.util.TestUtils
import spock.lang.Specification

class ConfigBuilderSpec extends Specification {

    def "test config builder"() {
        String MAIN_CONFIG_FILE = "configBuilderSpec.groovy"
        String DOCS_DIR = "${TestUtils.TEST_RESOURCES_DIR}/config"

        ConfigBuilder configBuilder = new ConfigBuilder(DOCS_DIR, MAIN_CONFIG_FILE)

        when: 'i pass existing config file parameters'
            def property = configBuilder.build()
        then: 'the config is parsed and returned'
            property.size() == 6
            property.getProperty("outputPath") == "build/docs"
            property.getProperty("inputPath") == "src/docs"
            property.getProperty("docDir") == "${TestUtils.TEST_RESOURCES_DIR}/config"
            noExceptionThrown()
    }

    def "test config builder with invalid file parameters"() {
        String MAIN_CONFIG_FILE = "non-existing.groovy"
        String DOCS_DIR = "${TestUtils.TEST_RESOURCES_DIR}/config"

        ConfigBuilder configBuilder = new ConfigBuilder(DOCS_DIR, MAIN_CONFIG_FILE)

        when: 'i pass non-existing config file parameters'
            configBuilder.build()
        then: 'builder throws an exception'
            thrown(FileNotFoundException)
    }

    def "test config builder with with on-the-fly config creation"() {
        String MAIN_CONFIG_FILE = "config.groovy"
        String DOCS_DIR = "${TestUtils.TEST_OUTPUT_DIR}/${this.getClass().getSimpleName()}/config"

        File testConfigFile = new File(DOCS_DIR, MAIN_CONFIG_FILE)

        setup: 'a clean environment'
            if(!new File(DOCS_DIR).exists()){
                testConfigFile.mkdirs()
            }
            if(testConfigFile.exists()){
                testConfigFile.delete()
            }
            ConfigBuilder configBuilder = new ConfigBuilder(DOCS_DIR, MAIN_CONFIG_FILE)

        when: 'i pass non-existing config file parameters and call prepareConfigFileIfNotExists() to create the file'
            configBuilder.prepareConfigFileIfNotExists({
                testConfigFile.text = "outputPath = 'build/docs'"
            })
        then: 'the builder creates the config file'
            new File(DOCS_DIR, MAIN_CONFIG_FILE).exists()
        then: 'and the builder returns the new created config file'
            def config = configBuilder.build()
            config.docDir == DOCS_DIR
            config.mainConfigFile == MAIN_CONFIG_FILE
            config.outputPath == "build/docs"
            config.size() == 3
            noExceptionThrown()
    }
}
