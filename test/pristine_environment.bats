# SPDX-License-Identifier: MIT
# Copyright 2023, Max Hofer and the docToolchain contributors

# Test behavior on a clean environment: no wget, no curl, no docToolchain,
# no Java, no SDKMAN!, no docker.

setup() {
    load 'test_helper.bash'
    setup_environment
}

teardown() {
    mock_teardown
}

@test "no arguments shows usage" {
    PATH="${minimal_system}" run -2 ./dtcw

    # Version information is only shown when we start something

    # Show how to use it
    assert_line --index  0 "dtcw ${DTCW_VERSION} - ##DTCW_GIT_HASH##"
    assert_line --index  1 "docToolchain ${DTC_VERSION}"
    assert_line --index  2 "OS/arch: $(uname -s)/$(uname -m)"
    assert_line --index  3 "Error: argument missing"
    assert_line --index  4 "dtcw - Create awesome documentation the easy way with docToolchain."
    assert_line --index  5 "Usage: ./dtcw [environment] [option...] [task...]"
    assert_line --index  6 "       ./dtcw [local] install {doctoolchain | java }"
    assert_line --index  7 "Use 'local', 'sdk' or 'docker' as first argument to force the use of a specific"
    assert_line --index  8 "docToolchain environment:"
    assert_line --index  9 "    - local: installation in '$HOME/.doctoolchain'"
    assert_line --index 10 "    - sdk: installation with SDKMAN! (https://sdkman.io/)"
    assert_line --index 11 "    - docker: use docToolchain container image"
    assert_line "Detailed documentation how to use docToolchain may be found at https://doctoolchain.org/"
    assert_line "Use './dtcw tasks --group doctoolchain' to see docToolchain related tasks."
    assert_line "Use './dtcw tasks' to see all tasks."
}

@test "local - no arguments shows usage" {
    PATH="${minimal_system}" run -2 ./dtcw local
    assert_line "Error: argument missing"
}

@test "sdk - no arguments shows usage" {
    PATH="${minimal_system}" run -2 ./dtcw sdk
    assert_line "Error: argument error - environment 'sdk' not available"
}

@test "docker - no arguments shows usage" {
    PATH="${minimal_system}" run -2 ./dtcw docker
    assert_line "Error: argument error - environment 'docker' not available"
}

@test "show version info and exit" {
    run -0 ./dtcw --version
    assert_line --index 0 "dtcw ${DTCW_VERSION} - ##DTCW_GIT_HASH##"
    assert_line --index 1 "docToolchain ${DTC_VERSION}"
    assert_line --index 2 "OS/arch: $(uname -s)/$(uname -m)"

    refute_output "Error: argument missing"
    refute_output "Available docToolchain environments: local"
}

@test "show version info - additional arguments are ignored" {
    run -0 ./dtcw --version tasks
    assert_line --index 0 "dtcw ${DTCW_VERSION} - ##DTCW_GIT_HASH##"
    assert_line --index 1 "docToolchain ${DTC_VERSION}"

    refute_output "Error: argument missing"
    refute_output "Available docToolchain environments: local"
}

@test "tasks uses local environment - shows install docToolchain" {
    PATH="${minimal_system}" run -1 ./dtcw tasks

    # Shows useful environment information
    assert_line "Available docToolchain environments: local"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: none"
    assert_line "Using environment: local"

    assert_line "Error: doctoolchain - command not found [environment 'local']"
    assert_line "It seems docToolchain ${DTC_VERSION} is not installed. dtcw supports the"
    assert_line "following docToolchain environments:"
    assert_line "1. 'local': to install docToolchain in [${DTC_ROOT}] use"
    assert_line "    \$ ./dtcw local install doctoolchain"
    assert_line "2. 'sdk': to install docToolchain with SDKMAN! (https://sdkman.io)"
    assert_line "    # First install SDKMAN!"
    assert_line '    $ curl -s "https://get.sdkman.io" | bash'
    assert_line "    # Then open a new shell and install docToolchain with"
    assert_line "    \$ sdk install doctoolchain ${DTC_VERSION}"
    assert_line "Note that running docToolchain in 'local' or 'sdk' environment needs a"
    assert_line "Java runtime (major version 8, 11, 14, or 17) installed on your host."
    assert_line "3. 'docker': pull the docToolchain image and execute docToolchain in a container environment."
    assert_line "    \$ ./dtcw docker tasks --group doctoolchain"
}

# TODO: exit code is inconsistent - should it be 2?
@test "sdk tasks shows install SDKMAN!" {
    PATH="${minimal_system}" run -2 ./dtcw sdk tasks

    # Shows useful environment information
    assert_line "Available docToolchain environments: local"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: none"
    refute_output "Using environment: local"

    assert_line "Error: argument error - environment 'sdk' not available"
    assert_line "Install SDKMAN! (https://sdkman.io) with"
    assert_line "    $ curl -s \"https://get.sdkman.io\" | bash"
    assert_line "Then open a new shell and install 'docToolchain' with"
    assert_line "    $ sdk install doctoolchain ${DTC_VERSION}"
}

# TODO: exit code is inconsistent - should it be 2?
@test "docker tasks shows install docker" {
    PATH="${minimal_system}" run -2 ./dtcw docker tasks

    assert_line "Error: argument error - environment 'docker' not available"
    assert_line "Install 'docker' on your host to execute docToolchain in a container."
}
