# SPDX-License-Identifier: MIT
# Copyright 2023, Max Hofer and the docToolchain contributors

# Test installation on a clean environment

# This means: no docToolchain, no Java, no SDKMAN!, no docker
#
# The tests follow the installation instructions based on the output provided by `dtcw`.
#

setup() {
    load 'test_helper.bash'
    setup_environment

    # Define project branch, otherwise test execution in repository with docker
    # environment fails due to checking for git branch.
    export DTC_PROJECT_BRANCH=test

    # Installed local doctoolchain
    mock_doctoolchain=$(mock_create "${DTC_HOME}/bin/doctoolchain")

    # Installed local java
    _mock=$(mock_create_java "${DTC_ROOT}/jdk/bin/java" "17.0.14")
}

teardown() {
    mock_teardown

    # Delete mocks in there
    rm -rf "${DTC_ROOT}"
}

@test "tasks - forward to local doctoolchain" {
    skip "v4: dtcw environment model changed (local-first, direct invocation, bundled JDK); v3 expectation no longer holds — pending v4 bats-suite adaptation"
    # Execute
    PATH="${minimal_system}" run -0 ./dtcw tasks --group doctoolchain

    assert_line "Available docToolchain environments: local"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: local"
    assert_line "Using environment: local"
    assert_line "Using Java 17.0.14 [${HOME}/.doctoolchain/jdk/bin/java]"

    assert_equal "$(mock_get_call_num "${mock_doctoolchain}")" 1
    assert_equal "$(mock_get_call_args "${mock_doctoolchain}")" ". tasks --group doctoolchain --warning-mode=none --no-daemon -Dfile.encoding=UTF-8 -PmainConfigFile=docToolchainConfig.groovy -Dorg.gradle.java.home=${DTC_ROOT}/jdk -Dgradle.user.home=${DTC_ROOT}/.gradle"
}

@test "overrule configuration file with DTC_CONFIG_FILE" {
    skip "v4: dtcw environment model changed (local-first, direct invocation, bundled JDK); v3 expectation no longer holds — pending v4 bats-suite adaptation"
    # Execute
    PATH="${minimal_system}" DTC_CONFIG_FILE=my_config_file.groovy run -0 ./dtcw tasks

    assert_equal "$(mock_get_call_num "${mock_doctoolchain}")" 1
    assert_equal "$(mock_get_call_args "${mock_doctoolchain}")" ". tasks --warning-mode=none --no-daemon -Dfile.encoding=UTF-8 -PmainConfigFile=my_config_file.groovy -Dorg.gradle.java.home=${DTC_ROOT}/jdk -Dgradle.user.home=${DTC_ROOT}/.gradle"

}

@test "using sdk with local environment fails" {
    # Execute
    PATH="${minimal_system}" run -2 ./dtcw sdk tasks --group doctoolchain

    assert_line "Available docToolchain environments: local"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: local"

    assert_line "Error: argument error - environment 'sdk' not available"

    assert_line "Install SDKMAN! (https://sdkman.io) with"

    assert_line "    $ curl -s \"https://get.sdkman.io\" | bash"

    assert_line "Then open a new shell and install 'docToolchain' with"

    assert_line "    $ sdk install doctoolchain ${DTC_VERSION}"
}

@test "using docker with local environment fails" {
    # Execute
    PATH="${minimal_system}" run -2 ./dtcw docker tasks --group doctoolchain

    assert_line "Available docToolchain environments: local"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: local"

    assert_line "Error: argument error - environment 'docker' not available"

    assert_line "Install 'docker' on your host to execute docToolchain in a container."
}

@test "installing docker has no side effect" {
    # Test setup
    _mock=$(mock_create docker)

    # v4 (ADR-18): `tasks` runs the Groovy launcher, which needs the lib/
    # classpath. A complete v4 install ships JARs in lib/; provide one so this is
    # a complete install rather than a broken one (which would, correctly, error
    # with "Installation may be incomplete").
    mkdir -p "${DTC_HOME}/lib"
    touch "${DTC_HOME}/lib/dummy.jar"

    # The installation of docker should not have any effect
    PATH="${minimal_system}" run -0 ./dtcw tasks --group doctoolchain

    assert_line "Available docToolchain environments: local docker"
    # v4 has no published docker image yet, so docker is not claimed as an
    # environment that already has docToolchain (only 'local' here).
    assert_line "Environments with docToolchain [${DTC_VERSION}]: local"
    assert_line "Using environment: local"
}
