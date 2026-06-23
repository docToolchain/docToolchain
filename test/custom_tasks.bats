# SPDX-License-Identifier: MIT
# Copyright 2026, the docToolchain contributors

# Tests for project-local custom tasks and monkey-patching (ADR-16, ADR-17).
#
# A *.groovy file with the '// @task' marker in a project-local scripts
# directory is discovered by dtcw. It either adds a new task (custom task) or,
# when it shares a name with an installed task, overrides it (monkey-patching).

setup() {
    load 'test_helper.bash'
    setup_environment

    export DTC_PROJECT_BRANCH=test

    # Create a v4 installation (lib/ directory present)
    mkdir -p "${DTC_HOME}/lib"
    mkdir -p "${DTC_HOME}/scripts"
    touch "${DTC_HOME}/lib/dummy.jar"

    # An installed task
    printf '// @task\n' > "${DTC_HOME}/scripts/generateHTML.groovy"

    # Installed local java (so the v4 java invocation is captured, not real)
    java_mock=$(mock_create_java "${DTC_ROOT}/jdk/bin/java" "17.0.14")

    # Isolate the project scripts directory from the repository's own scripts/
    export DTC_PROJECT_SCRIPTS_DIR="${BATS_TEST_TMPDIR}/project-scripts"
    mkdir -p "${DTC_PROJECT_SCRIPTS_DIR}"
}

teardown() {
    mock_teardown
    rm -rf "${DTC_ROOT}"
}

# --- Discovery / listing -----------------------------------------------------

@test "custom: a project-local task with @task marker is listed" {
    printf '// @task\n' > "${DTC_PROJECT_SCRIPTS_DIR}/myCustomTask.groovy"
    run ./dtcw tasks
    assert_success
    assert_output --partial "Project-local custom tasks"
    assert_output --partial "myCustomTask"
}

@test "custom: a project-local *.groovy without the marker is NOT a task" {
    printf '// not a task\n' > "${DTC_PROJECT_SCRIPTS_DIR}/justAHelper.groovy"
    run ./dtcw tasks
    assert_success
    refute_output --partial "justAHelper"
}

@test "override: a project task shadowing an installed one is flagged in the listing" {
    printf '// @task\n' > "${DTC_PROJECT_SCRIPTS_DIR}/generateHTML.groovy"
    run ./dtcw tasks
    assert_success
    assert_output --partial "generateHTML (overridden by"
}

@test "custom: a project task matching a non-task installed helper is custom, not an override" {
    # The installation also holds non-task *.groovy helpers (no // @task marker).
    # A project task sharing such a name must be a custom task, never an override.
    printf '// a helper, not a task\nclass Foo {}\n' > "${DTC_HOME}/scripts/asciidoctorExtensions.groovy"
    printf '// @task\n' > "${DTC_PROJECT_SCRIPTS_DIR}/asciidoctorExtensions.groovy"
    run ./dtcw tasks
    assert_success
    assert_output --partial "Project-local custom tasks"
    assert_output --partial "asciidoctorExtensions"
    refute_output --partial "asciidoctorExtensions (overridden"
}

# --- Validation --------------------------------------------------------------

@test "custom: an unknown task name is still rejected" {
    run ./dtcw thisTaskDoesNotExist
    assert_failure
    assert_output --partial "Unknown task"
}

# --- Execution / resolution --------------------------------------------------

@test "custom: running a project-local task invokes its script with dtc.scriptsHome" {
    printf '// @task\n' > "${DTC_PROJECT_SCRIPTS_DIR}/myCustomTask.groovy"
    run ./dtcw myCustomTask
    assert_success
    # Informational note that a project-local custom task is being run
    assert_output --partial "project-local custom task 'myCustomTask'"
    # The most recent java call runs GroovyMain on the project script and passes
    # the installed scripts directory so lib/ resolves from the installation.
    # (java is also called once for the -version check, hence no call-count assert.)
    run mock_get_call_args "${java_mock}"
    assert_output --partial "-Ddtc.scriptsHome=${DTC_HOME}/scripts"
    assert_output --partial "${DTC_PROJECT_SCRIPTS_DIR}/myCustomTask.groovy"
}

@test "override: running an overridden task uses the project copy and warns" {
    printf '// @task\n' > "${DTC_PROJECT_SCRIPTS_DIR}/generateHTML.groovy"
    run ./dtcw generateHTML
    assert_success
    assert_output --partial "override of task 'generateHTML'"
    run mock_get_call_args "${java_mock}"
    # The project copy is executed, not the installed one
    assert_output --partial "${DTC_PROJECT_SCRIPTS_DIR}/generateHTML.groovy"
    refute_output --partial "${DTC_HOME}/scripts/generateHTML.groovy"
}
