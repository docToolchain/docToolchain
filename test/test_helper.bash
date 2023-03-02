# SPDX-License-Identifier: MIT
# Copyright 2023, Max Hofer and the docToolchain contributors

# Helper functions used by more  than one test suite

set_dtc_enviroment() {
    # This will fail if the --version output changes
    export DTCW_VERSION=$(./dtcw --version | sed -rn 's/dtcw (.*) - .*/\1/p')
    export DTC_VERSION=$(./dtcw --version | sed -rn 's/docToolchain (.*)$/\1/p')
    export DTC_ROOT="${HOME}/.doctoolchain"
    export DTC_HOME="${DTC_ROOT}/docToolchain-${DTC_VERSION}"
}

mock_create_java() {
    local cmd="${1}"
    local java_version=${2}
    local mock=
    mock=$(mock_create "${cmd}")
    mock_set_output "${mock}" - <<EOF
openjdk version "${java_version}" 2023-01-17
OpenJDK Runtime Environment (build not-important)
OpenJDK 64-Bit Server VM (build not-important, mixed mode, sharing)

EOF
    echo "${mock}"
}

setup_environment() {
    # Needed for use of 'run -<expected_exit_code>', otherwise we get BW02 errors
    bats_require_minimum_version 1.5.0

    load 'test_helper/bats-support/load'
    load 'test_helper/bats-assert/load'
    load 'test_helper/bats-mock/load'

    # Make sure installed Java VMs do not interfere
    unset JAVA_HOME

    # Make sure we disable SDKMAN!
    unset SDKMAN_DIR

    # Test runs in GitHub Actions Runners run without terminal.
    export DTC_HEADLESS=false

    # Environment variables used in the tests
    set_dtc_enviroment

    # System sandbox
    minimal_system=$(mock_chroot)
}
