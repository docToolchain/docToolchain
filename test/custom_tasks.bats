# SPDX-License-Identifier: MIT
# Copyright 2026, the docToolchain contributors

# Wrapper-level tests for v4 task dispatch (ADR-16 + ADR-18).
#
# Since ADR-18, task discovery, the `tasks` listing, project-first resolution and
# unknown-task guidance live in the Groovy launcher (scripts/Launcher.groovy),
# unit-tested by scripts/lib/TaskLauncherTest.groovy. These bats tests verify
# only the wrapper's remaining job: validate the task-name format and hand the
# task off to the launcher with the right JVM options.

setup() {
    load 'test_helper.bash'
    setup_environment

    export DTC_PROJECT_BRANCH=test

    # Create a v4 installation (lib/ directory present)
    mkdir -p "${DTC_HOME}/lib" "${DTC_HOME}/scripts"
    touch "${DTC_HOME}/lib/dummy.jar"
    # Mocked java captures the invocation instead of really running the launcher
    java_mock=$(mock_create_java "${DTC_ROOT}/jdk/bin/java" "17.0.14")
}

teardown() {
    mock_teardown
    rm -rf "${DTC_ROOT}"
}

@test "v4: a task is handed to the Groovy launcher with dtc.scriptsHome" {
    run ./dtcw generateHTML
    assert_success
    run mock_get_call_args "${java_mock}"
    assert_output --partial "groovy.ui.GroovyMain"
    assert_output --partial "${DTC_HOME}/scripts/Launcher.groovy"
    assert_output --partial "generateHTML"
    assert_output --partial "-Ddtc.scriptsHome=${DTC_HOME}/scripts"
}

@test "v4: an arbitrary task name is delegated (resolution is the launcher's job)" {
    # The wrapper no longer decides whether a task exists — it forwards the name
    # and the launcher resolves project-local vs installed (or rejects it).
    run ./dtcw someProjectTask
    assert_success
    run mock_get_call_args "${java_mock}"
    assert_output --partial "Launcher.groovy"
    assert_output --partial "someProjectTask"
}

@test "v4: the tasks command is delegated to the launcher" {
    run ./dtcw tasks
    assert_success
    run mock_get_call_args "${java_mock}"
    assert_output --partial "Launcher.groovy"
    assert_output --partial "tasks"
}

@test "v4: an invalid task name is rejected by the wrapper" {
    run ./dtcw "bad name"
    assert_failure
    assert_output --partial "Invalid task name"
}
