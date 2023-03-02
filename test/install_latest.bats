# SPDX-License-Identifier: MIT
# Copyright 2023, Max Hofer and the docToolchain contributors

# Test installation on a clean environment

# This means: no docToolchain, no Java, no SDKMAN!, no docker
#
# The tests follow the installation instructions based on the output provided by `dtcw`.
#

setup() {
    load 'test_helper.bash'
    load 'test_helper/bats-file/load'
    setup_environment

    DTC_VERSION=latest
    mock_git=$(mock_create git)

    # Installed local Java
    _mock=$(mock_create_java "${DTC_ROOT}/jdk/bin/java" "11.0.18")
}

teardown() {
    mock_teardown

    # Delete mocks in there
    rm -rf "${DTC_ROOT}"
}

@test "DTC_VERSION=latest local install doctoolchain - git clone https" {
    # Execute
    DTC_VERSION=${DTC_VERSION} PATH="${minimal_system}" run -0 ./dtcw install doctoolchain

    assert_equal "$(mock_get_call_args "${mock_git}")" "clone https://github.com/docToolchain/docToolchain.git ${DTC_ROOT}/docToolchain-latest"
    assert_line "Cloned docToolchain in local environment to latest version"
}

@test "DTC_VERSION=latest local install doctoolchain - git pull on existing repository" {
    # Test stup
    # Simulate existing git repository
    mkdir -p "${DTC_ROOT}/docToolchain-${DTC_VERSION}/.git"

    # Execute
    DTC_VERSION=${DTC_VERSION} PATH="${minimal_system}" run -0 ./dtcw install doctoolchain

    assert_equal "$(mock_get_call_args "${mock_git}")" "-C ${DTC_ROOT}/docToolchain-${DTC_VERSION} pull"
    assert_line "Updated docToolchain in local environment to latest version"
}

@test "DTC_VERSION=latest tasks - show use of latest version" {
    # Test stup
    mock_doctoolchain=$(mock_create "${DTC_ROOT}/docToolchain-${DTC_VERSION}/bin/doctoolchain")
    # Simulate existing git repository
    mkdir -p "${DTC_ROOT}/docToolchain-${DTC_VERSION}/.git"

    # Execute
    DTC_VERSION=${DTC_VERSION} PATH="${minimal_system}" run -0 ./dtcw tasks

    assert_line "Available docToolchain environments: local"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: local"
    assert_line "Using environment: local"

    assert_equal "$(mock_get_call_num "${mock_doctoolchain}")" 1
    assert_equal "$(mock_get_call_args "${mock_doctoolchain}")" ". tasks -PmainConfigFile=docToolchainConfig.groovy --warning-mode=none --no-daemon -Dorg.gradle.java.home=${DTC_ROOT}/jdk -Dgradle.user.home=${DTC_ROOT}/.gradle"
}

@test "DTC_VERSION=latest tasks - git pull before execution" {
    skip "to be discussed - do we really want to auto-pull on task execution?"
    # Test stup
    mock_doctoolchain=$(mock_create "${DTC_ROOT}/docToolchain-${DTC_VERSION}/bin/doctoolchain")
    # Simulate existing git repository
    mkdir -p "${DTC_ROOT}/docToolchain-${DTC_VERSION}/.git"

    # Execute
    DTC_VERSION=${DTC_VERSION} PATH="${minimal_system}" run -0 ./dtcw tasks

    assert_equal "$(mock_get_call_num "${mock_doctoolchain}")" 1
    assert_equal "$(mock_get_call_args "${mock_git}")" "-C ${DTC_ROOT}/docToolchain-latest pull"
    assert_line "Updated docToolchain in local environment to latest version"
}

@test "DTC_VERSION=latestdev local install doctoolchain - git clone git@github.com" {
    # Execute
    DTC_VERSION=latestdev
    DTC_VERSION=${DTC_VERSION} PATH="${minimal_system}" run -0 ./dtcw install doctoolchain

    assert_equal "$(mock_get_call_args "${mock_git}")" "clone git@github.com:docToolchain/docToolchain.git ${DTC_ROOT}/docToolchain-${DTC_VERSION}"
    assert_line "Cloned docToolchain in local environment to latest version"
}

@test "DTC_VERSION=latestdev local install doctoolchain - git pull on existing repository" {
    # Test stup
    DTC_VERSION=latestdev
    mkdir -p "${DTC_ROOT}/docToolchain-${DTC_VERSION}/.git"

    # Execute
    DTC_VERSION=${DTC_VERSION} PATH="${minimal_system}" run -0 ./dtcw install doctoolchain

    assert_equal "$(mock_get_call_args "${mock_git}")" "-C ${DTC_ROOT}/docToolchain-${DTC_VERSION} pull"
    assert_line "Updated docToolchain in local environment to latest version"
}
