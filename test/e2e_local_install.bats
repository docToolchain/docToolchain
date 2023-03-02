# SPDX-License-Identifier: MIT
# Copyright 2023, Max Hofer and the docToolchain contributors

#
# End to end test (e2e) of local installation on a clean environment
# This means: no docToolchain, no Java, no SDKMAN!, no docker
#
# The e2e test follow the installation instructions based on the output provided by `dtcw`.
# They download external software packages (Java, docToolchain) which makes them slow.
#
# ATTENTION: contrary to good test patterns, the tests in this file depend on
# each other to keep the test execution (and downloads) as short as possible.
#

setup_file() {
    apt-get -qq update && apt-get -qq -y --no-install-recommends install curl ca-certificates git unzip
}

setup() {
    # Needed for use of 'run -<expected_exit_code>', otherwise we get BW02 errors
    bats_require_minimum_version 1.5.0

    load 'test_helper/bats-support/load'
    load 'test_helper/bats-assert/load'
    load 'test_helper/bats-file/load'

    load 'test_helper.bash'

    # Environment variables used in the tests
    set_dtc_enviroment
}

teardown_file() {
    rm -rf "$HOME/.doctoolchain"
    rm -rf docToolchainConfig.groovy
    apt-get -qq purge -y curl git unzip || true
}

# bats test_tags=e2e
@test "local install doctoolchain" {
    run -1 ./dtcw install doctoolchain

    # Output of wget doesn't have a final \n
    assert_line --partial "Installed docToolchain successfully in '${DTC_HOME}'."
    assert_line "Error: unable to locate a Java Runtime"
}

# bats test_tags=e2e
@test "local install java" {
    run -0 ./dtcw local install java

    assert_line "Environments with docToolchain [${DTC_VERSION}]: local"
    assert_line "Using environment: local"
    assert_line "Successfully installed Java in '${DTC_ROOT}/jdk'."

    run -0 "${HOME}/.doctoolchain/jdk/bin/java" --version
}

# bats test_tags=e2e
@test "create docToolchainConfig" {
    # The answer if want to create the default configuration with "n"
    DTC_HEADLESS=false run -0 ./dtcw tasks --group doctoolchain <<< "y"

    # Use '--partial' due to color codes in the console output
    assert_line --partial "Config file '/code/docToolchainConfig.groovy' does not exist"
    assert_line --partial "do you want me to create a default one for you?"

    assert_file_exist docToolchainConfig.groovy
}

# bats test_tags=e2e
@test "show available tasks" {
    run -0 ./dtcw tasks --group doctoolchain

    # TODO: 'gradlew' is not visible for the usevisible for the user
    assert_line "To see all tasks and more detail, run gradlew tasks --all"
    assert_line "To see more detail about a task, run gradlew help --task <task>"
}

# bats test_tags=e2e
@test "generateDeck uses revealjs gradle plugin - NO-SOURCES" {
    run -0 ./dtcw generateDeck

    assert_line "> Task :generateDeck NO-SOURCE"
}
