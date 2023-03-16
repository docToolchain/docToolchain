# SPDX-License-Identifier: MIT
# Copyright 2023, Max Hofer and the docToolchain contributors

# Test installation of docToolchain with SDKMAN! on a clean environment

# System setup: SDKMAN! (which implies curl, unzip, zip) installed, no docToolchain, no Java, no docker
#
# The tests follow the installation instructions based on the output provided by `dtcw`.
# They download external software packages (Java, docToolchain) which makes them slow.
#
# ATTENTION: contrary to good test patterns, the tests in this file depend on
# each other to keep the test execution (and downloads) as short as possible.
#
# TODO: write mocks for wget/curl which use a local cache instead of downloading
# the packages each time.
# See https://advancedweb.hu/how-to-mock-in-bash-tests/

setup_file() {
    apt install -y curl zip
    export SDKMAN_DIR="$HOME/.local/share/sdkman"
    curl -s "https://get.sdkman.io" | bash

    # The installation with sdk doesn't make the component available in the tests.
    # This means we have to adjust the PATH manually.
    # TODO: Is there is a better way to set this?
    PATH="${SDKMAN_DIR}/candidates/doctoolchain/current/bin":$PATH
    export PATH="${SDKMAN_DIR}/candidates/java/current/bin":$PATH
}

setup() {
    bats_load_library 'bats-support'
    bats_load_library 'bats-assert'

    # Needed for use of 'run -<expected_exit_code>', otherwise we get BW02 errors
    bats_require_minimum_version 1.5.0

    load 'test_helper.bash'
}

teardown() {
    mock_delete docker
}

teardown_file() {
    rm -rf "$SDKMAN_DIR"
    rm -rf docToolchainConfig.groovy
    apt purge -y curl zip
}

@test "any argument show how to install docToolchain with sdk" {
    # TODO: Why do we exit here with an error if we say don't install anything
    DTC_HEADLESS=false run -1 ./dtcw tasks --group doctoolchain <<< "2"

    # TODO: Instead of asking to install locally we show the installation
    # instructions how to install docToolchain with sdkman. Or the alternative
    # how to install  docToolchain locally with './dtcw local tasks ...'
    # > sdk install doctoolchain 2.2.0
    assert_line 'docToolchain not installed.'
    # TODO: bug - sdkman is not found
    assert_line 'sdkman not found'
    assert_line "Do you wish to install doctoolchain to '/root/.doctoolchain'?"
    assert_line '1) Yes'
    assert_line '2) No'
    assert_line '#? you need docToolchain as CLI-Tool installed or docker.'
    assert_line 'to install docToolchain as CLI-Tool, please install'
    assert_line 'sdkman and re-run this command.'
    assert_line 'https://sdkman.io/install'
    assert_line '$ curl -s "https://get.sdkman.io" | bash'
}

# bats test_tags=download
@test "install docToolchain with sdk" {
    # Test setup
    source "${SDKMAN_DIR}/bin/sdkman-init.sh"
    sdk install doctoolchain 2.2.0

    # Error since we still miss JDK
    run -1 ./dtcw tasks --group doctoolchain

    # Shows which docToolchain we use
    assert_line "docToolchain as CLI available"
    assert_line "use cli install /root/.local/share/sdkman/candidates/doctoolchain/current/bin/doctoolchain"

    # Shows information about missing JDK
    assert_line "docToolchain depends on java, but the java command couldn't be found in this shell (bash)"
    assert_line 'it might be that you have installed the needed version java in another shell from which you started dtcw'
    # TODO Since docToolchain was installed with sdkman, prefer sdkman for the java installation.
    # Provide the java version which docToolchain needs!
    assert_line 'dtcw is running in bash and uses the PATH to find java'
    assert_line 'to install a local java for docToolchain, you can run'
    assert_line './dtcw getJava'
    assert_line 'another way to install or update java is to install'
    assert_line 'sdkman and then java via sdkman'
    assert_line 'https://sdkman.io/install'
    assert_line '$ curl -s "https://get.sdkman.io" | bash'
    assert_line '$ sdk install java'
    assert_line 'or you can download it from https://adoptium.net/'
    assert_line 'make sure that your java version is between 8 and 14'
    assert_line 'If you do not want to use a local java installation, you can also use docToolchain as docker container.'
    assert_line "In that case, specify 'docker' as first parameter in your statement."
    assert_line 'example: ./dtcw docker generateSite'
}

# TODO: test if system already available from system
# - correct java version
# - java version is not the one we expect

# bats test_tags=download
@test "create docToolchainConfig" {
    # Test setup - fix missing precondition
    source "${SDKMAN_DIR}/bin/sdkman-init.sh"

    # TODO: where do we get this version?
    run -0 sdk install java 11.0.18-tem

    run -0 java --version

    # The answer if want to create the default configuration with "y"
    DTC_HEADLESS=false run -0 ./dtcw tasks --group doctoolchain <<< "y"

    assert_line "Java Version 11"

    # TODO: 'gradlew' is not available
    assert_line "To see all tasks and more detail, run gradlew tasks --all"
    assert_line "To see more detail about a task, run gradlew help --task <task>"
}

# bats test_tags=download
@test "use local with sdk installation" {
    # Pre-conditions
    run -0 java --version
    run -1 doctoolchain

    # Will ask for a local installation which we reject
    DTC_HEADLESS=false run -1 ./dtcw local tasks --group doctoolchain <<< "2"

    assert_line "force use of local install"
    # TODO: bug - this is misleading - docToolchain is not installed locally
    assert_line "docToolchain not installed."
    # TODO: bug - this is wrong
    assert_line "sdkman not found"

    assert_line "Do you wish to install doctoolchain to '/root/.doctoolchain'?"
    assert_line '1) Yes'
    assert_line '2) No'
    assert_line "#? you need docToolchain as CLI-Tool installed or docker."
    assert_line "to install docToolchain as CLI-Tool, please install"
    assert_line "sdkman and re-run this command."
    assert_line "https://sdkman.io/install"
    assert_line '$ curl -s "https://get.sdkman.io" | bash'
}

# bats test_tags=download
@test "use docker with sdk installation" {
    skip "this use case is completly buggy"

    run ./dtcw docker tasks

    # TODO: same output as the test case with "local" - and buggy
    assert_line "force use of docker"
}

# bats test_tags=download
@test "no side effect when installing docker" {
    mock_create docker

    # The installation of docker should not have any effect
    run -0 ./dtcw tasks --group doctoolchain

    assert_line "docToolchain as CLI available"
    assert_line "docker available"
    assert_line "use cli install /root/.local/share/sdkman/candidates/doctoolchain/current/bin/doctoolchain"
    assert_line "Java Version 11"
}
