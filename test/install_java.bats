# SPDX-License-Identifier: MIT
# Copyright 2023, Max Hofer and the docToolchain contributors

# Test installation of Java in a clean environment

# This means: no docToolchain, no Java, no SDKMAN!, no docker
#
# The tests follow the installation instructions based on the output provided by `dtcw`.
#

setup() {
    load 'test_helper.bash'
    setup_environment

    mock_curl=$(mock_create curl)
    mock_tar=$(mock_create tar)

    mock_set_side_effect "${mock_tar}" - <<EOF
mkdir -p "${DTC_ROOT}/jdk/bin"
cat <<JAVA > "${DTC_ROOT}/jdk/bin/java"
echo 'openjdk version "17.0.6" 2023-01-17'
echo 'OpenJDK Runtime Environment Temurin-17.0.6+10 (build 17.0.6+10)'
echo 'OpenJDK 64-Bit Server VM Temurin-17.0.6+10 (build 17.0.6+10, mixed mode, sharing)'
echo
JAVA
chmod +x "${DTC_ROOT}/jdk/bin/java"
EOF

}

teardown() {
    mock_teardown

    # Delete mocks in there
    rm -rf "${DTC_ROOT}"
}

@test "local install java (linux/x64) before doctoolchain installed" {
    # Test download Java for Linux on x86_64
    rm "${minimal_system}/uname" && _mock=$(mock_create uname)
    mock_set_output "${_mock}" "x86_64"
    mock_set_output "${_mock}" "Linux" 2

    # Execute
    PATH="${minimal_system}" run -0 ./dtcw local install java

    assert_equal "$(mock_get_call_args "${mock_curl}")" "--fail --silent --location --output ${DTC_ROOT}/jdk/jdk.tar.gz https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk"

    assert_line "Environments with docToolchain [${DTC_VERSION}]: none"
    assert_line "Using environment: local"

    assert_line "Downloading JDK Temurin 17 [linux/x64] from Adoptium to ${DTC_ROOT}/jdk/jdk.tar.gz"
    assert_line "Extracting JDK from archive file."
    assert_line "Successfully installed Java in '${DTC_ROOT}/jdk'."

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
    assert_line "Java runtime (major version 11, 14, or 17) installed on your host."
    assert_line "3. 'docker': pull the docToolchain image and execute docToolchain in a container environment."
    assert_line "    \$ ./dtcw docker tasks --group doctoolchain"

    refute_line "Use './dtcw tasks --group doctoolchain' to see docToolchain related tasks."
}

@test "local install java (linux/x64) after doctoolchain installed" {
    # Test setup
    _mock=$(mock_create "${DTC_HOME}/bin/doctoolchain")
    rm "${minimal_system}/uname" && _mock=$(mock_create uname)
    mock_set_output "${_mock}" "x86_64"
    mock_set_output "${_mock}" "Linux" 2

    # Execute
    PATH="${minimal_system}" run -0 ./dtcw local install java

    assert_equal "$(mock_get_call_args "${mock_curl}")" "--fail --silent --location --output ${DTC_ROOT}/jdk/jdk.tar.gz https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk"

    assert_line "Environments with docToolchain [${DTC_VERSION}]: local"
    assert_line "Using environment: local"

    assert_line "Downloading JDK Temurin 17 [linux/x64] from Adoptium to ${DTC_ROOT}/jdk/jdk.tar.gz"
    assert_line "Extracting JDK from archive file."
    assert_line "Successfully installed Java in '${DTC_ROOT}/jdk'."

    assert_line "Use './dtcw tasks --group doctoolchain' to see docToolchain related tasks."

    refute_line "It seems docToolchain ${DTC_VERSION} is not installed. dtcw supports the"
    refute_line "following docToolchain environments:"
}

@test "local install java (mac/x64) before doctoolchain installed" {
    skip "Test not implemented yet"
}

@test "local install java (mac/aarch64) before doctoolchain installed" {
    skip "Test not implemented yet"
}

@test "getJava issues deprecation warning" {
    # Execute
    PATH="${minimal_system}" run -0 ./dtcw getJava

    assert_line "Successfully installed Java in '${DTC_ROOT}/jdk'."
    assert_line "Warning: 'getJava' is deprecated and and will be removed. Use './dtcw install java' instead."
}

@test "Java download failed" {
    skip "not implemented yet"
}
