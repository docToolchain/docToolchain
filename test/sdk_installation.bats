# SPDX-License-Identifier: MIT
# Copyright 2023, Max Hofer and the docToolchain contributors

# Test installation of docToolchain with SDKMAN! on a clean environment

# System setup: SDKMAN! (which implies curl, unzip, zip) installed, no docToolchain, no Java, no docker
#
# The tests follow the installation instructions based on the output provided by `dtcw`.
# They download external software packages (Java, docToolchain) which makes them slow.
#

setup() {
    load 'test_helper.bash'
    setup_environment

    mock_create_sdk

    # Define project branch, otherwise test execution in repository with docker
    # environment fails due to checking for git branch.
    export DTC_PROJECT_BRANCH=test

    # Mock installation of doctoolchain and java with sdk
    mock_doctoolchain=$(mock_create "${SDKMAN_DIR}/candidates/doctoolchain/${DTC_VERSION}/bin/doctoolchain")
    mock_java=$(mock_create_java "${SDKMAN_DIR}/candidates/java/current/bin/java" "17.0.7")
    path=$(path_override "${minimal_system}" "$(path_rm /bin "$(path_rm /usr/bin)")")
}

teardown() {
    mock_teardown

    rm -rf "${SDKMAN_DIR}"
}

mock_create_sdk() {
    export SDKMAN_DIR="/tmp/sdkman"

    # Mock SDKMAN! installation
    mkdir -p "${SDKMAN_DIR}/bin"
    cat <<EOF > "${SDKMAN_DIR}/bin/sdkman-init.sh"
# Mock call of 'sdk home doctoolchain'
sdk() {
    local cmd=\${1}
    local candidate=\${2}
    local version=\${3}
    local candidate_dir="\${SDKMAN_DIR}/candidates/\${candidate}/\${version}"
    if [ -d "\${candidate_dir}" ]; then
        echo "\${candidate_dir}"
    else
        >&2 echo "\${candidate} \${version} is not installed on your system"
        return 1
    fi
}
EOF

    # Mock we installed docToolchain and
    PATH="${SDKMAN_DIR}/candidates/doctoolchain/current/bin":$PATH
    export PATH="${SDKMAN_DIR}/candidates/java/current/bin":$PATH
}

@test "don't show how to install SDKMAN!" {
    # Test setup: remove doctoolchain and Java
    rm -rf "${SDKMAN_DIR}/candidates"

    PATH="${minimal_system}" run -1 ./dtcw tasks --group doctoolchain

    assert_line "Available docToolchain environments: local sdk"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: none"
    assert_line "Using environment: local"

    refute_output "    # First install SDKMAN!"
    refute_output '    $ curl -s "https://get.sdkman.io" | bash'

    assert_line "Error: doctoolchain - command not found [environment 'local']"
    assert_line "It seems docToolchain ${DTC_VERSION} is not installed. dtcw supports the"
    assert_line "following docToolchain environments:"
    assert_line "1. 'local': to install docToolchain in [${DTC_ROOT}] use"
    assert_line "    $ ./dtcw local install doctoolchain"
    assert_line "2. 'sdk': to install docToolchain with SDKMAN! (https://sdkman.io)"
    assert_line "    $ sdk install doctoolchain ${DTC_VERSION}"
    assert_line "Note that running docToolchain in 'local' or 'sdk' environment needs a"
    assert_line "Java runtime (major version 11, 14, or 17) installed on your host."
    assert_line "3. 'docker': pull the docToolchain image and execute docToolchain in a container environment."
    assert_line "    $ ./dtcw docker tasks --group doctoolchain"
}

@test "doctoolchain installed with sdk - java missing" {
    # Test setup
    # Delete Java from the setup
    rm "${mock_java}"

    # Error since we still miss JDK
    PATH="${minimal_system}" run -1 ./dtcw tasks --group doctoolchain

    assert_line "Available docToolchain environments: local sdk"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: sdk"
    assert_line "Using environment: sdk"

    assert_line "Error: unable to locate a Java Runtime"

    assert_line "docToolchain supports Java versions 11, 14, or 17 (preferred). In case one of those"
    assert_line "Java versions is installed make sure 'java' is found with your PATH environment"
    assert_line "variable. As alternative you may provide the location of your Java installation"
    assert_line "with JAVA_HOME."

    assert_line "Apart from installing Java with the package manager provided by your operating"
    assert_line "system, dtcw facilitates the Java installation into a local environment:"

    assert_line "    # Install Java in '${DTC_ROOT}/jdk'"
    assert_line "    \$ ./dtcw local install java"

    assert_line "Alternatively you can use SDKMAN! (https://sdkman.io) to manage your Java installations"

    # Don't show how SDKMAN! is installed since it is already installed
    refute_output --partial "# Install SDKMAN!"
    refute_output --partial "$ curl -s \"https://get.sdkman.io\" | bash"
    refute_output --partial "Then open a new shell and install Java 17 with"

    # TODO: This will break when we change Java version
    assert_line "    \$ sdk install java 17.0.7-tem"

    assert_line "If you prefer not to install Java on your host, you can run docToolchain in a"
    assert_line "docker container. For this case dtcw provides the 'docker' execution environment."

    assert_line 'Example: ./dtcw docker generateSite'
}

@test "tasks - forward to sdk doctoolchain" {
    # Execute
    PATH="${path}" run -0 ./dtcw tasks --group doctoolchain

    assert_equal "$(mock_get_call_num "${mock_doctoolchain}")" 1
    assert_equal "$(mock_get_call_args "${mock_doctoolchain}")" ". tasks --group doctoolchain -PmainConfigFile=docToolchainConfig.groovy --warning-mode=none --no-daemon -Dfile.encoding=UTF-8  -Dgradle.user.home=${DTC_ROOT}/.gradle"
}

@test "using local with sdk environment fails" {
    # Execute
    PATH="${path}" run -1 ./dtcw local tasks --group doctoolchain

    assert_line "Available docToolchain environments: local sdk"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: sdk"
    assert_line "Using environment: local"
    assert_line "Error: doctoolchain - command not found [environment 'local']"
}

@test "using docker with sdk environment fails" {
    # Execute
    PATH="${path}" run -2 ./dtcw docker tasks --group doctoolchain

    assert_line "Available docToolchain environments: local sdk"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: sdk"
    assert_line "Error: argument error - environment 'docker' not available"
    assert_line "Install 'docker' on your host to execute docToolchain in a container."
}

@test "installing docker has no side effects" {
    # Test setup
    mock_docker=$(mock_create docker)

    # The installation of docker should not have any effect
    PATH="${path}" run -0 ./dtcw tasks --group doctoolchain

    assert_line "Available docToolchain environments: local sdk docker"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: sdk docker"
    assert_line "Using environment: sdk"

    assert_equal "$(mock_get_call_num "${mock_doctoolchain}")" 1
    assert_equal "$(mock_get_call_num "${mock_docker}")"  0
}

@test "using docker with sdk installation" {
    # Test setup
    mock_docker=$(mock_create docker)

    PATH="${path}" run ./dtcw docker tasks

    assert_line "Available docToolchain environments: local sdk docker"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: sdk docker"
    assert_line "Using environment: docker"

    assert_equal "$(mock_get_call_num "${mock_doctoolchain}")" 0
    assert_equal "$(mock_get_call_num "${mock_docker}")" 1
}

@test "DTC_VERSION=latest sdk tasks - fails" {
    # Test stup
    DTC_VERSION=latest

    # Execute
    DTC_VERSION=${DTC_VERSION} PATH="${minimal_system}" run -2 ./dtcw sdk tasks

    assert_line "Error: argument error - invalid environment 'sdk'."
    assert_line "Development version '${DTC_VERSION}' can only be used in a local environment."
}

@test "DTC_VERSION=latestdev sdk tasks - fails" {
    # Test stup
    DTC_VERSION=latestdev

    # Execute
    DTC_VERSION=${DTC_VERSION} PATH="${minimal_system}" run -2 ./dtcw sdk tasks

    assert_line "Error: argument error - invalid environment 'sdk'."
    assert_line "Development version '${DTC_VERSION}' can only be used in a local environment."
}

@test "specific doctoolchain version not installed" {
    # Use a docToolchain version which is not installed
    local not_installed_dtc_version=1.3.0

    # Execute
    PATH="${path}" DTC_VERSION=${not_installed_dtc_version} run -1 ./dtcw sdk tasks --group doctoolchain

    assert_line "Available docToolchain environments: local sdk"
    assert_line "Environments with docToolchain [${not_installed_dtc_version}]: none"
    assert_line "Using environment: sdk"
    assert_line "Error: doctoolchain - command not found [environment 'sdk']"
}
