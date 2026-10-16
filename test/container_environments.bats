# SPDX-License-Identifier: MIT
# Copyright 2026, the docToolchain contributors

# Wrapper-level tests for the 'compose' and 'devcontainer' environments.
#
# These assert what the wrapper hands to the container runtime. The argv is
# captured through a mock side effect rather than mock_get_call_args(), because
# the latter flattens "$@" through echo and would hide exactly the word-splitting
# these tests are about.

setup() {
    load 'test_helper.bash'
    setup_environment

    export DTC_PROJECT_BRANCH=test

    DTCW="${PWD}/dtcw"

    # v4 installation — the lib/ directory is the marker
    mkdir -p "${DTC_HOME}/lib" "${DTC_HOME}/scripts"
    touch "${DTC_HOME}/lib/dummy.jar"

    # Run in a project sandbox: the compose file is looked up in the cwd
    project=$(mktemp -d)
    cd "${project}" || return 1

    export ARGV_LOG="${project}/argv.log"
    : > "${ARGV_LOG}"
}

teardown() {
    cd "${BATS_TEST_DIRNAME}/.." || true
    mock_teardown
    rm -rf "${DTC_ROOT}" "${project}"
}

# --- helpers ---

# Write a compose file with a doctoolchain service under the given name.
# $2 overrides the image tag, e.g. 'v3.5.0' or 'latest'; '' omits the tag.
compose_fixture() {
    local tag="${2-v4.0.0}"
    cat > "${1}" <<YAML
services:
  doctoolchain:
    image: doctoolchain/doctoolchain${tag:+:${tag}}
    entrypoint: ["sleep", "infinity"]
YAML
}

devcontainer_fixture() {
    mkdir -p .devcontainer
    cat > .devcontainer/devcontainer.json <<'JSON'
{
  "name": "docToolchain",
  "image": "doctoolchain/doctoolchain:v4.0.0"
}
JSON
}

# Mock that records its argv one element per '[...]', so quoting is visible.
mock_with_argv_log() {
    local mock
    mock=$(mock_create "${1}")
    mock_set_side_effect "${mock}" 'printf "[%s]" "$@" >> "${ARGV_LOG}"; echo "" >> "${ARGV_LOG}"'
    echo "${mock}"
}

# The dispatch is the last recorded call — find_compose_cmd() probes first.
last_argv() {
    tail -n 1 "${ARGV_LOG}"
}

# --- compose ---

@test "compose: a task is dispatched into the running service via the launcher" {
    compose_fixture docker-compose.yml
    mock_with_argv_log docker >/dev/null

    PATH="${minimal_system}" run -0 "${DTCW}" generateHTML
    assert_line "Using environment: compose"

    run last_argv
    assert_output --partial "[compose][-f][docker-compose.yml][exec]"
    assert_output --partial "[doctoolchain][bash][-c]"
    assert_output --partial "Launcher.groovy"
    assert_output --partial "generateHTML"
    # ADR-17: the launcher must find the installed scripts inside the image
    assert_output --partial "-Ddtc.scriptsHome=/opt/docToolchain/scripts"
}

@test "compose: 'tasks' is delegated to the container, not answered on the host" {
    # The host cannot know which tasks the image provides, so the listing has to
    # travel into the container instead of being served from the installation.
    compose_fixture docker-compose.yml
    mock_with_argv_log docker >/dev/null

    PATH="${minimal_system}" run -0 "${DTCW}" tasks
    assert_line "Using environment: compose"

    run last_argv
    assert_output --partial "Launcher.groovy"
    assert_output --partial "tasks"
}

@test "compose: every Compose-spec file name is detected" {
    # One mock for the whole loop: mock_teardown() would remove the chroot that
    # provides 'bash' to the remaining iterations.
    mock_with_argv_log docker >/dev/null

    for name in compose.yaml compose.yml docker-compose.yml docker-compose.yaml; do
        rm -f compose.yaml compose.yml docker-compose.yml docker-compose.yaml
        : > "${ARGV_LOG}"
        compose_fixture "${name}"

        PATH="${minimal_system}" run -0 "${DTCW}" generateHTML
        assert_line "Using environment: compose"

        run last_argv
        assert_output --partial "[-f][${name}]"
    done
}

@test "compose: DTC_SERVICE overrides the auto-detected service name" {
    compose_fixture docker-compose.yml
    mock_with_argv_log docker >/dev/null

    DTC_SERVICE=docs-builder PATH="${minimal_system}" run -0 "${DTCW}" generateHTML

    run last_argv
    assert_output --partial "[exec][docs-builder]"
}

@test "compose: a compose path containing spaces stays one argument" {
    # main() builds a command string that is re-parsed by 'bash -c', so an
    # unquoted interpolation would word-split the path here.
    compose_fixture "my compose.yml"
    mock_with_argv_log docker >/dev/null

    DTC_COMPOSE="my compose.yml" PATH="${minimal_system}" run -0 "${DTCW}" generateHTML

    run last_argv
    assert_output --partial "[-f][my compose.yml][exec]"
}

@test "devcontainer: a workspace path containing spaces stays one argument" {
    devcontainer_fixture
    mkdir -p "my workspace"
    mock_with_argv_log devcontainer >/dev/null

    DTC_WORKSPACE="my workspace" PATH="${minimal_system}" run -0 "${DTCW}" generateHTML

    run last_argv
    assert_output --partial "[--workspace-folder][my workspace]"
}

# --- version derived from the image, not from the host ---

@test "compose: a 3.x image is dispatched through Gradle, not the v4 launcher" {
    # Plant a host installation that looks like v4 *at the version the image
    # pins*, so host state and image tag disagree. Deciding from the host would
    # pick the launcher and die on a missing Launcher.groovy in a 3.x container;
    # only the image tag gives the right answer.
    mkdir -p "${DTC_ROOT}/docToolchain-3.5.0/lib"
    touch "${DTC_ROOT}/docToolchain-3.5.0/lib/dummy.jar"

    compose_fixture docker-compose.yml v3.5.0
    mock_with_argv_log docker >/dev/null
    # the helper exports DTC_VERSION; derivation only runs without it
    unset DTC_VERSION

    PATH="${minimal_system}" run -0 "${DTCW}" generateHTML
    assert_line "Using docToolchain 3.5.0 from docker-compose.yml"

    run last_argv
    assert_output --partial "[doctoolchain . generateHTML"
    refute_output --partial "Launcher.groovy"
}

@test "compose: a 4.x image is dispatched through the launcher" {
    compose_fixture docker-compose.yml v4.0.0
    mock_with_argv_log docker >/dev/null
    # the helper exports DTC_VERSION; derivation only runs without it
    unset DTC_VERSION

    PATH="${minimal_system}" run -0 "${DTCW}" generateHTML
    assert_line "Using docToolchain 4.0.0 from docker-compose.yml"

    run last_argv
    assert_output --partial "Launcher.groovy"
}

@test "compose: an explicit DTC_VERSION overrides the image tag" {
    compose_fixture docker-compose.yml v3.5.0
    mock_with_argv_log docker >/dev/null

    DTC_VERSION=4.0.0 PATH="${minimal_system}" run -0 "${DTCW}" generateHTML
    assert_line "Using docToolchain 4.0.0 (DTC_VERSION overrides docker-compose.yml)"

    run last_argv
    assert_output --partial "Launcher.groovy"
}

@test "compose: an unversioned image tag fails instead of guessing" {
    compose_fixture docker-compose.yml latest
    mock_with_argv_log docker >/dev/null
    # the helper exports DTC_VERSION; derivation only runs without it
    unset DTC_VERSION

    PATH="${minimal_system}" run -2 "${DTCW}" generateHTML
    assert_line "Error: cannot determine the docToolchain version from docker-compose.yml"
    assert_line "Image reference: doctoolchain/doctoolchain:latest"
    assert_line --partial "DTC_VERSION="
}

@test "compose: a missing image tag fails instead of guessing" {
    compose_fixture docker-compose.yml ""
    mock_with_argv_log docker >/dev/null
    # the helper exports DTC_VERSION; derivation only runs without it
    unset DTC_VERSION

    PATH="${minimal_system}" run -2 "${DTCW}" generateHTML
    assert_line "Error: cannot determine the docToolchain version from docker-compose.yml"
}

@test "devcontainer: the version is derived from its image too" {
    mkdir -p .devcontainer
    cat > .devcontainer/devcontainer.json <<'JSON'
{
  "name": "docToolchain",
  "image": "doctoolchain/doctoolchain:v3.5.0"
}
JSON
    mock_with_argv_log devcontainer >/dev/null
    unset DTC_VERSION

    PATH="${minimal_system}" run -0 "${DTCW}" generateHTML
    assert_line "Using docToolchain 3.5.0 from .devcontainer/devcontainer.json"

    run last_argv
    refute_output --partial "Launcher.groovy"
}

@test "compose: the pseudo-TTY is disabled when headless" {
    # 'docker compose exec' allocates a TTY by default and aborts with
    # "the input device is not a TTY" when stdin is not a terminal.
    compose_fixture docker-compose.yml
    mock_with_argv_log docker >/dev/null

    DTC_HEADLESS=true PATH="${minimal_system}" run -0 "${DTCW}" generateHTML

    run last_argv
    assert_output --partial "[exec][-T][doctoolchain]"
}

@test "compose: an interactive run keeps the TTY" {
    compose_fixture docker-compose.yml
    mock_with_argv_log docker >/dev/null

    DTC_HEADLESS=false PATH="${minimal_system}" run -0 "${DTCW}" generateHTML

    run last_argv
    assert_output --partial "[exec][doctoolchain]"
    refute_output --partial "[-T]"
}

# --- devcontainer ---

@test "devcontainer: a task is dispatched via 'devcontainer exec'" {
    devcontainer_fixture
    mock_with_argv_log devcontainer >/dev/null

    PATH="${minimal_system}" run -0 "${DTCW}" generateHTML
    assert_line "Using environment: devcontainer"

    run last_argv
    assert_output --partial "[exec][--workspace-folder][.]"
    assert_output --partial "Launcher.groovy"
    assert_output --partial "generateHTML"
}

@test "devcontainer: compose takes precedence when both are present" {
    # A running compose service is ready immediately, so it wins over a
    # devcontainer that may still have to be started.
    compose_fixture docker-compose.yml
    devcontainer_fixture
    mock_with_argv_log docker >/dev/null
    mock_with_argv_log devcontainer >/dev/null

    PATH="${minimal_system}" run -0 "${DTCW}" generateHTML
    assert_line "Using environment: compose"
}
