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
}

teardown() {
    mock_teardown

    # Delete mocks in there
    rm -rf "${DTC_ROOT}"
}

@test "local install doctoolchain - show java missing" {
    mock_curl=$(mock_create curl)
    mock_unzip=$(mock_create unzip)

    PATH="${minimal_system}" run -1 ./dtcw install doctoolchain

    assert_equal "$(mock_get_call_args "${mock_curl}")" "--fail --silent --location --output ${DTC_ROOT}/source.zip https://github.com/docToolchain/docToolchain/releases/download/v${DTC_VERSION}/docToolchain-${DTC_VERSION}.zip"
    assert_equal "$(mock_get_call_args "${mock_unzip}")" "-q ${DTC_ROOT}/source.zip -d ${DTC_ROOT}"

    assert_line "Environments with docToolchain [${DTC_VERSION}]: none"
    assert_line "Using environment: local"

    # Output of wget doesn't have a final \n
    assert_line "Installed docToolchain successfully in '${DTC_HOME}'."

    assert_line "Error: unable to locate a Java Runtime"

    assert_line "docToolchain supports Java versions 11 (preferred), 14, or 17. In case one of those"
    assert_line "Java versions is installed make sure 'java' is found with your PATH environment"
    assert_line "variable. As alternative you may provide the location of your Java installation"
    assert_line "with JAVA_HOME."

    assert_line "Apart from installing Java with the package manager provided by your operating"
    assert_line "system, dtcw facilitates the Java installation into a local environment:"

    assert_line "    # Install Java in '$DTC_ROOT/jdk'"
    assert_line "    $ ./dtcw local install java"

    assert_line "Alternatively you can use SDKMAN! (https://sdkman.io) to manage your Java installations"

    assert_line "    # First install SDKMAN!"
    assert_line "    $ curl -s \"https://get.sdkman.io\" | bash"
    assert_line "    # Then open a new shell and install Java 11 with"
    # TODO: This will break when we change Java version
    assert_line "    $ sdk install java 11.0.18-tem"

    assert_line "If you prefer not to install Java on your host, you can run docToolchain in a"
    assert_line "docker container. For this case dtcw provides the 'docker' execution environment."

    assert_line 'Example: ./dtcw docker generateSite'
}

@test "local install doctoolchain - system with supported Java" {
    # Test setup
    mock_curl=$(mock_create curl)
    mock_unzip=$(mock_create unzip)
    _mock=$(mock_create_java java "11.0.18")

    # Execute
    PATH="${minimal_system}" run -0 ./dtcw install doctoolchain

    # Validate
    assert_line "Environments with docToolchain [${DTC_VERSION}]: none"
    assert_line "Installed docToolchain successfully in '${DTC_HOME}'."
    assert_line --partial "Using Java 11.0.18"
    assert_line "Use './dtcw tasks --group doctoolchain' to see docToolchain related tasks."
}

@test "local install - missing component" {
    # No component
    PATH="${minimal_system}" run -2 ./dtcw install

    assert_line "Available docToolchain environments: local"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: none"

    assert_line "Error: component missing - available components are 'doctoolchain', or 'java'"

    assert_line "Use './dtcw local install doctoolchain' to install docToolchain ${DTC_VERSION}."
    assert_line "Use './dtcw local install java' to install a Java version supported by docToolchain."
}

@test "local install - unnknown component" {
    # No component
    PATH="${minimal_system}" run -2 ./dtcw install foo

    assert_line "Available docToolchain environments: local"
    assert_line "Environments with docToolchain [${DTC_VERSION}]: none"

    assert_line "Error: unknown component 'foo' - available components are 'doctoolchain', or 'java'"

    assert_line "Use './dtcw local install doctoolchain' to install docToolchain ${DTC_VERSION}."
    assert_line "Use './dtcw local install java' to install a Java version supported by docToolchain."
}

@test "local install doctoolchain fails - no HTTP download program installed" {
    _mock=$(mock_create unzip)

    PATH="${minimal_system}" run -1 ./dtcw install doctoolchain

    assert_line "Error: no HTTP download program (curl, wget, fetch) found, exiting…"
    assert_line "Install either 'curl', 'wget', or 'fetch' and try to install docToolchain again."
}

@test "local install doctoolchain fails - no unzip installed" {
    _mock=$(mock_create wget)

    PATH="${minimal_system}" run -1 ./dtcw install doctoolchain

    assert_line "Error: no unzip program installed, exiting…"
    assert_line "Install 'unzip' and try to install docToolchain again."
}

@test "DTC_VERSION=latest local install doctoolchain - git command not found" {
    # Execute
    DTC_VERSION=latest PATH="${minimal_system}" run -1 ./dtcw install doctoolchain

    assert_line "Error: git - command not found"
    assert_line "Please install 'git' for working with a 'doctToolchain' development version"
}

@test "DTC_VERSION=latestdev local install doctoolchain - git command not found" {
    # Execute
    DTC_VERSION=latestdev PATH="${minimal_system}" run -1 ./dtcw install doctoolchain

    assert_line "Error: git - command not found"
    assert_line "Please install 'git' for working with a 'doctToolchain' development version"
}

@test "install doctoolchain twice - skip installation" {
    _mock=$(mock_create_java java "11.0.18")
    _mock=$(mock_create "${DTC_HOME}/bin/doctoolchain")

    # Execute
    PATH="${minimal_system}" run -0 ./dtcw install doctoolchain

    refute_line "Installed docToolchain successfully in '${DTC_HOME}'."

    assert_line "Environments with docToolchain [${DTC_VERSION}]: local"
    assert_line "Skipped installation of docToolchain: already installed in '${DTC_HOME}'"
    assert_line --partial "Using Java 11.0.18"
    assert_line "Use './dtcw tasks --group doctoolchain' to see docToolchain related tasks."
}

@test "install doctoolchain - download failed" {
    mock_curl=$(mock_create curl)
    # Failed download returns error code 22
    mock_set_status "${mock_curl}" 22
    _mock=$(mock_create unzip)

    # Execute
    PATH="${minimal_system}" run -22 ./dtcw install doctoolchain

    assert_line --partial "Error: Command failed (exit code 22): curl"
}
