# SPDX-License-Identifier: MIT
# Copyright 2023, Max Hofer and the docToolchain contributors

#
# End to end test (e2e) for installation with SDKMAN!
# System setup: SDKMAN! (which implies curl, unzip, zip installed,
#               no docToolchain, no Java, no docker
#

#
# The tests follow the installation instructions based on the output provided by `dtcw`.
# They download external software packages (Java, docToolchain) which makes them slow.
#
# ATTENTION: contrary to good test patterns, the tests in this file depend on
# each other to keep the test execution (and downloads) as short as possible.
#

setup_file() {
    apt-get -qq update && apt-get -qq -y --no-install-recommends install curl zip unzip ca-certificates
    export SDKMAN_DIR="$HOME/.local/share/sdkman"
    curl -s "https://get.sdkman.io" | bash
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

    # We have to source the file since each test runs in its own sub-shell
    source "${SDKMAN_DIR}/bin/sdkman-init.sh"
}

teardown_file() {
    rm -rf docToolchainConfig.groovy
    rm -rf "$SDKMAN_DIR"
    apt purge -y curl zip unzip
}

# bats test_tags=e2e
@test "installed docToolchain with sdk - show Java missing" {
    # Test setup
    sdk install doctoolchain "${DTC_VERSION}"

    # Error since we still miss JDK
    run -1 ./dtcw tasks --group doctoolchain

    # Shows which docToolchain we use
    assert_line "Available docToolchain environments: local sdk"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: sdk"
    assert_line "Using environment: sdk"

    assert_line "Error: unable to locate a Java Runtime"

    # The rest is already is tested by sdk_installation
}

# bats test_tags=e2e
@test "install Java - create docToolchainConfig" {
    # Fail if pre-conditions are not met
    run -0 sdk home doctoolchain "${DTC_VERSION}"
    assert_output "${SDKMAN_DIR}/candidates/doctoolchain/${DTC_VERSION}"

    # Test setup
    # TODO: where do we get this version?
    sdk install java 11.0.18-tem
    java --version

    # Execute
    # No need to specify an environment - picks the correct environment automatically
    # The answer if want to create the default configuration with "y"
    DTC_HEADLESS=false run -0 ./dtcw tasks --group doctoolchain <<< "y"

    assert_line "Using environment: sdk"

    # TODO: 'gradlew' is not available
    assert_line "To see all tasks and more detail, run gradlew tasks --all"
    assert_line "To see more detail about a task, run gradlew help --task <task>"

    assert_file_exist docToolchainConfig.groovy
}

# bats test_tags=e2e
@test "show available tasks" {
    # Fail if pre-conditions are not met
    run -0 sdk home doctoolchain "${DTC_VERSION}"
    assert_output "${SDKMAN_DIR}/candidates/doctoolchain/${DTC_VERSION}"
    run -0 java --version
    assert_file_exist docToolchainConfig.groovy

    # Execute
    run -0 ./dtcw tasks --group doctoolchain

    assert_line "Using environment: sdk"

    # TODO: 'gradlew' is not visible for the usevisible for the user
    assert_line "To see all tasks and more detail, run gradlew tasks --all"
    assert_line "To see more detail about a task, run gradlew help --task <task>"
}
