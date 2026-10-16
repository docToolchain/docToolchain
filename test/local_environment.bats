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

@test "local v3: an install path with spaces stays one argument" {
    # dtcw derives DTC_ROOT from HOME at runtime, so a spaced HOME gives a
    # spaced install path. build_command() returns a string that main() hands to
    # 'bash -c', so an unquoted ${dtc_home} would word-split here and the v3
    # environment would die with "<prefix>: No such file or directory" -- which
    # is what happens on Windows/cygwin under "C:\Users\First Last".
    #
    # A plain stub instead of mock_create(): the argv has to be recorded with its
    # word boundaries intact, and mock_get_call_args() flattens "$@" via echo.
    #
    # DTC_VERSION has to name a 3.x release: is_v4_installation() falls back to
    # the version major when no lib/ directory is present, so a v3 tree under the
    # default 4.0.0 would still be dispatched through the v4 launcher.
    local v3_version=3.5.0
    local spaced_home="${BATS_TEST_TMPDIR}/home with space"
    local dtc_home="${spaced_home}/.doctoolchain/docToolchain-${v3_version}"
    local argv_log="${BATS_TEST_TMPDIR}/argv.log"

    # v3 installation: bin/doctoolchain present, no lib/ directory
    mkdir -p "${dtc_home}/bin"
    cat > "${dtc_home}/bin/doctoolchain" <<STUB
#!/usr/bin/env bash
printf '[%s]' "\$@" >> '${argv_log}'
STUB
    chmod +x "${dtc_home}/bin/doctoolchain"
    _mock_java=$(mock_create_java "${spaced_home}/.doctoolchain/jdk/bin/java" "17.0.14")

    HOME="${spaced_home}" DTC_VERSION="${v3_version}" DTC_CONFIG_FILE="my config.groovy" \
        PATH="${minimal_system}" run -0 ./dtcw generateHTML

    run cat "${argv_log}"
    # The stub was reached at all -> the spaced install path survived
    assert_output --partial "[.][generateHTML]"
    # ... and the spaced config file arrived as a single argument
    assert_output --partial "[-PmainConfigFile=my config.groovy]"
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
