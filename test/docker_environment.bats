# SPDX-License-Identifier: MIT
# Copyright 2023, Max Hofer and the docToolchain contributors

# Test dtcw wrapper in a pure docker environment.
# This means no docToolchain locally installed.

setup() {
    load 'test_helper.bash'
    setup_environment

    # Define project branch, otherwise test execution in repository with docker
    # environment fails due to checking for git branch.
    export DTC_PROJECT_BRANCH=test
}

teardown() {
    mock_teardown
}

@test "forward call to docker" {
    # No local install, but docker is present
    mock_docker=$(mock_create docker)

    PATH="${minimal_system}" run -0 ./dtcw tasks --group doctoolchain

    assert_line "Available docToolchain environments: local docker"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: docker"
    assert_line "Using environment: docker"

    expected_cmd="run --rm -i --platform linux/amd64 -u $(id -u):$(id -g) \
--name doctoolchain${DTC_VERSION} -e DTC_HEADLESS=true -e DTC_SITETHEME -e DTC_PROJECT_BRANCH=test \
-p 8042:8042 --entrypoint /bin/bash -v ${PWD}:/project doctoolchain/doctoolchain:v${DTC_VERSION} \
-c doctoolchain . tasks --group doctoolchain  -PmainConfigFile=docToolchainConfig.groovy --warning-mode=none --no-daemon && exit"
    assert_equal "$(mock_get_call_args "${mock_docker}")" "${expected_cmd}"
    # TODO: the mock doesn't handles quotes correctly
    # assert_line "$expected_cmd"
}
