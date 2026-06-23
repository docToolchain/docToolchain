# SPDX-License-Identifier: MIT
# Test deprecation warnings for v3-era Gradle commands in v4 mode

setup() {
    load 'test_helper.bash'
    setup_environment

    export DTC_PROJECT_BRANCH=test

    # Create a v4 installation (lib/ directory present)
    mkdir -p "${DTC_HOME}/lib"
    mkdir -p "${DTC_HOME}/scripts"
    touch "${DTC_HOME}/lib/dummy.jar"

    # Create a task script with @task marker
    echo '// @task' > "${DTC_HOME}/scripts/generateHTML.groovy"

    # Installed local java
    java_mock=$(mock_create_java "${DTC_ROOT}/jdk/bin/java" "17.0.14")
}

teardown() {
    mock_teardown
    rm -rf "${DTC_ROOT}"
}

# Since ADR-18 the `tasks` listing and the `--group` deprecation note are emitted
# by the Groovy launcher, not the wrapper. With java mocked we therefore assert
# that the wrapper *delegates* `tasks` (the note/listing itself is verified by
# scripts/lib/TaskLauncherTest.groovy and the launcher).

# Scenario: User runs dtcw tasks --group doctoolchain
@test "v4: tasks --group is delegated to the Groovy launcher" {
    run ./dtcw tasks --group doctoolchain
    assert_success
    run mock_get_call_args "${java_mock}"
    assert_output --partial "Launcher.groovy"
    assert_output --partial "tasks"
}

# Scenario: User runs dtcw tasks --group (without value)
@test "v4: tasks --group without value is still delegated" {
    run ./dtcw tasks --group
    assert_success
    run mock_get_call_args "${java_mock}"
    assert_output --partial "Launcher.groovy"
}
