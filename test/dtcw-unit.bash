#!/bin/bash
# Unit tests for dtcw internal functions (Chicago School)
#
# Sources dtcw via BASH_SOURCE guard, then tests functions directly.
# No container runtime or docToolchain installation needed.
set -e -u -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Source dtcw — BASH_SOURCE guard skips main()
source "${REPO_ROOT}/dtcw"

passed=0
failed=0

pass() { echo "  PASS"; passed=$((passed + 1)); }
fail() { echo "  FAIL: $1"; failed=$((failed + 1)); }

# --- find_dtc_service ---

test_find_dtc_service_valid() {
    echo "Testing: find_dtc_service with valid compose file..."
    local service
    service=$(find_dtc_service "${SCRIPT_DIR}/fixtures/compose-dtc-valid.yml")
    [[ "${service}" == "doctoolchain" ]] || { fail "expected 'doctoolchain', got '${service}'"; return; }
    pass
}

test_find_dtc_service_custom_name() {
    echo "Testing: find_dtc_service with custom service name..."
    local service
    service=$(find_dtc_service "${SCRIPT_DIR}/fixtures/compose-custom-service.yml")
    [[ "${service}" == "docs-builder" ]] || { fail "expected 'docs-builder', got '${service}'"; return; }
    pass
}

test_find_dtc_service_no_match() {
    echo "Testing: find_dtc_service with no doctoolchain service..."
    local service
    service=$(find_dtc_service "${SCRIPT_DIR}/fixtures/compose-no-dtc.yml")
    [[ -z "${service}" ]] || { fail "expected empty, got '${service}'"; return; }
    pass
}

# --- detect_compose_path ---

test_detect_compose_path_via_env() {
    echo "Testing: detect_compose_path via DTC_COMPOSE..."
    local result
    result=$(DTC_COMPOSE="${SCRIPT_DIR}/fixtures/compose-dtc-valid.yml" detect_compose_path)
    [[ "${result}" == "${SCRIPT_DIR}/fixtures/compose-dtc-valid.yml" ]] || { fail "got '${result}'"; return; }
    pass
}

test_detect_compose_path_auto_detect() {
    echo "Testing: detect_compose_path auto-detects in current directory..."
    local tmpdir
    tmpdir=$(mktemp -d)
    cp "${SCRIPT_DIR}/fixtures/compose-dtc-valid.yml" "${tmpdir}/docker-compose.yml"
    local result
    result=$(cd "${tmpdir}" && DTC_COMPOSE="" detect_compose_path)
    [[ "${result}" == "docker-compose.yml" ]] || { fail "got '${result}'"; return; }
    rm -rf "${tmpdir}"
    pass
}

test_detect_compose_path_spec_names() {
    echo "Testing: detect_compose_path accepts the full Compose-spec name set..."
    local name tmpdir result
    for name in compose.yaml compose.yml docker-compose.yml docker-compose.yaml; do
        tmpdir=$(mktemp -d)
        cp "${SCRIPT_DIR}/fixtures/compose-dtc-valid.yml" "${tmpdir}/${name}"
        result=$(cd "${tmpdir}" && DTC_COMPOSE="" detect_compose_path) || result="<failed>"
        rm -rf "${tmpdir}"
        [[ "${result}" == "${name}" ]] || { fail "'${name}' not detected (got '${result}')"; return; }
    done
    pass
}

test_detect_compose_path_precedence() {
    echo "Testing: detect_compose_path prefers the same file 'docker compose' would..."
    # Order must match the compose implementation, so dtcw never forces a
    # different file via -f than plain 'docker compose' would have chosen.
    local -a expected=(compose.yaml compose.yml docker-compose.yml docker-compose.yaml)
    local tmpdir result i drop
    tmpdir=$(mktemp -d)
    for drop in "${expected[@]}"; do
        cp "${SCRIPT_DIR}/fixtures/compose-dtc-valid.yml" "${tmpdir}/${drop}"
    done
    for ((i = 0; i < ${#expected[@]}; i++)); do
        result=$(cd "${tmpdir}" && DTC_COMPOSE="" detect_compose_path) || result="<failed>"
        [[ "${result}" == "${expected[i]}" ]] || {
            fail "expected '${expected[i]}', got '${result}'"
            rm -rf "${tmpdir}"
            return
        }
        rm -f "${tmpdir}/${expected[i]}"
    done
    rm -rf "${tmpdir}"
    pass
}

test_detect_compose_path_no_match() {
    echo "Testing: detect_compose_path fails without doctoolchain service..."
    local tmpdir
    tmpdir=$(mktemp -d)
    cp "${SCRIPT_DIR}/fixtures/compose-no-dtc.yml" "${tmpdir}/docker-compose.yml"
    local rc=0
    (cd "${tmpdir}" && DTC_COMPOSE="" detect_compose_path) >/dev/null 2>&1 || rc=$?
    [[ ${rc} -ne 0 ]] || { fail "expected failure"; return; }
    rm -rf "${tmpdir}"
    pass
}

# --- detect_devcontainer ---

test_detect_devcontainer_match() {
    echo "Testing: detect_devcontainer with doctoolchain image..."
    local tmpdir
    tmpdir=$(mktemp -d)
    mkdir -p "${tmpdir}/.devcontainer"
    cp "${SCRIPT_DIR}/fixtures/devcontainer-dtc.json" "${tmpdir}/.devcontainer/devcontainer.json"
    (cd "${tmpdir}" && detect_devcontainer)
    local rc=$?
    [[ ${rc} -eq 0 ]] || { fail "expected success"; return; }
    rm -rf "${tmpdir}"
    pass
}

test_detect_devcontainer_no_match() {
    echo "Testing: detect_devcontainer without doctoolchain image..."
    local tmpdir
    tmpdir=$(mktemp -d)
    mkdir -p "${tmpdir}/.devcontainer"
    cp "${SCRIPT_DIR}/fixtures/devcontainer-no-dtc.json" "${tmpdir}/.devcontainer/devcontainer.json"
    local rc=0
    (cd "${tmpdir}" && detect_devcontainer) || rc=$?
    [[ ${rc} -ne 0 ]] || { fail "expected failure"; return; }
    rm -rf "${tmpdir}"
    pass
}

test_detect_devcontainer_missing() {
    echo "Testing: detect_devcontainer without devcontainer.json..."
    local tmpdir
    tmpdir=$(mktemp -d)
    local rc=0
    (cd "${tmpdir}" && detect_devcontainer) || rc=$?
    [[ ${rc} -ne 0 ]] || { fail "expected failure"; return; }
    rm -rf "${tmpdir}"
    pass
}

# --- detect_container_runner ---

test_detect_container_runner_finds_something() {
    echo "Testing: detect_container_runner finds a runtime on this system..."
    unset DTC_CONTAINER_RUNNER 2>/dev/null || true
    local rc=0
    detect_container_runner || rc=$?
    if [[ ${rc} -eq 0 ]]; then
        [[ -n "${DTC_CONTAINER_RUNNER}" ]] || { fail "DTC_CONTAINER_RUNNER empty after success"; return; }
        echo "  PASS (found: ${DTC_CONTAINER_RUNNER})"
        ((passed++))
    else
        echo "  SKIP (no container runtime installed)"
    fi
}

# Create a stub runtime in ${1}. ${3} decides whether its 'compose' subcommand
# reports success -- nerdctl and finch implement compose natively, podman only
# wraps an external provider, and Apple's 'container' has none at all.
make_runtime_stub() {
    local dir=${1} name=${2} has_compose=${3} rootless=${4:-false}
    local compose_exit=1
    [[ "${has_compose}" == "yes" ]] && compose_exit=0
    mkdir -p "${dir}"
    # Only ${compose_exit} and ${rootless} are interpolated; the stub's own
    # positional parameters stay escaped so they survive into the generated file.
    cat > "${dir}/${name}" <<STUB
#!/usr/bin/env bash
# 'compose version' probes whether this runtime carries compose at all.
[ "\$1" = compose ] && exit ${compose_exit}
# 'info --format {{.Host.Security.Rootless}}' drives podman_userns_args.
[ "\$1" = info ] && { echo '${rootless}'; exit 0; }
exit 0
STUB
    chmod +x "${dir}/${name}"
}

test_find_compose_cmd_follows_runtime() {
    echo "Testing: find_compose_cmd pairs compose with the detected runtime..."
    local tmpdir result
    tmpdir=$(mktemp -d)
    # Both present: podman is preferred, so compose must not cross over to docker
    make_runtime_stub "${tmpdir}" podman yes
    make_runtime_stub "${tmpdir}" docker yes
    result=$(
        unset DTC_CONTAINER_RUNNER
        PATH="${tmpdir}:${PATH}"
        detect_container_runner
        find_compose_cmd
    )
    rm -rf "${tmpdir}"
    [[ "${result}" == *"/podman compose" ]] || { fail "expected podman compose, got '${result}'"; return; }
    pass
}

test_find_compose_cmd_subcommand_only_runtime() {
    echo "Testing: find_compose_cmd finds compose in a subcommand-only runtime..."
    local tmpdir result
    tmpdir=$(mktemp -d)
    make_runtime_stub "${tmpdir}" nerdctl yes
    result=$(
        unset DTC_CONTAINER_RUNNER
        PATH="${tmpdir}:/usr/bin:/bin"
        find_compose_cmd
    )
    rm -rf "${tmpdir}"
    [[ "${result}" == *"/nerdctl compose" ]] || { fail "expected nerdctl compose, got '${result}'"; return; }
    pass
}

test_find_compose_cmd_matching_standalone() {
    echo "Testing: find_compose_cmd falls back to the runtime's own standalone..."
    local tmpdir result
    tmpdir=$(mktemp -d)
    make_runtime_stub "${tmpdir}" podman no
    printf '#!/usr/bin/env bash\nexit 0\n' > "${tmpdir}/podman-compose"
    printf '#!/usr/bin/env bash\nexit 0\n' > "${tmpdir}/docker-compose"
    chmod +x "${tmpdir}/podman-compose" "${tmpdir}/docker-compose"
    result=$(
        unset DTC_CONTAINER_RUNNER
        PATH="${tmpdir}:/usr/bin:/bin"
        find_compose_cmd
    )
    rm -rf "${tmpdir}"
    # docker-compose is present but belongs to another runtime
    [[ "${result}" == "podman-compose" ]] || { fail "expected podman-compose, got '${result}'"; return; }
    pass
}

test_find_compose_cmd_explicit_runner_without_compose() {
    echo "Testing: find_compose_cmd fails loudly for a runtime without compose..."
    local tmpdir rc=0 output
    tmpdir=$(mktemp -d)
    make_runtime_stub "${tmpdir}" container no
    output=$(
        PATH="${tmpdir}:/usr/bin:/bin"
        DTC_CONTAINER_RUNNER="${tmpdir}/container" find_compose_cmd 2>&1
    ) || rc=$?
    rm -rf "${tmpdir}"
    [[ ${rc} -ne 0 ]] || { fail "expected failure, got '${output}'"; return; }
    [[ "${output}" == *"has no compose support"* ]] || { fail "unhelpful message: '${output}'"; return; }
    pass
}

# --- podman_userns_args ---

test_podman_userns_rootless() {
    echo "Testing: podman_userns_args adds keep-id for rootless podman..."
    local tmpdir result
    tmpdir=$(mktemp -d)
    make_runtime_stub "${tmpdir}" podman yes true
    result=$(DTC_CONTAINER_RUNNER="${tmpdir}/podman" podman_userns_args)
    rm -rf "${tmpdir}"
    [[ "${result}" == "--userns=keep-id" ]] || { fail "expected --userns=keep-id, got '${result}'"; return; }
    pass
}

test_podman_userns_rootful_and_others() {
    echo "Testing: podman_userns_args stays empty for rootful podman and docker..."
    local tmpdir result
    tmpdir=$(mktemp -d)
    make_runtime_stub "${tmpdir}" podman yes false
    make_runtime_stub "${tmpdir}" docker yes true
    result=$(DTC_CONTAINER_RUNNER="${tmpdir}/podman" podman_userns_args)
    [[ -z "${result}" ]] || { fail "rootful podman got '${result}'"; rm -rf "${tmpdir}"; return; }
    # docker reports rootless=true as well, but the flag is podman-only
    result=$(DTC_CONTAINER_RUNNER="${tmpdir}/docker" podman_userns_args)
    rm -rf "${tmpdir}"
    [[ -z "${result}" ]] || { fail "docker got '${result}'"; return; }
    pass
}

test_detect_container_runner_respects_preset() {
    echo "Testing: detect_container_runner respects pre-set value..."
    DTC_CONTAINER_RUNNER="/usr/bin/fake-runner"
    detect_container_runner
    [[ "${DTC_CONTAINER_RUNNER}" == "/usr/bin/fake-runner" ]] || { fail "preset was overwritten"; return; }
    unset DTC_CONTAINER_RUNNER
    pass
}

# --- is_supported_environment ---

test_supported_environments() {
    echo "Testing: is_supported_environment for all environments..."
    for env in compose devcontainer local sdk docker; do
        is_supported_environment "${env}" || { fail "'${env}' not supported"; return; }
    done
    pass
}

test_unsupported_environment() {
    echo "Testing: is_supported_environment rejects unknown..."
    local rc=0
    is_supported_environment "generateHTML" || rc=$?
    [[ ${rc} -ne 0 ]] || { fail "'generateHTML' should not be a supported environment"; return; }
    pass
}

# --- Run all tests ---
echo "=== dtcw unit tests ==="
test_find_dtc_service_valid
test_find_dtc_service_custom_name
test_find_dtc_service_no_match
test_detect_compose_path_via_env
test_detect_compose_path_auto_detect
test_detect_compose_path_spec_names
test_detect_compose_path_precedence
test_detect_compose_path_no_match
test_detect_devcontainer_match
test_detect_devcontainer_no_match
test_detect_devcontainer_missing
test_detect_container_runner_finds_something
test_detect_container_runner_respects_preset
test_find_compose_cmd_follows_runtime
test_find_compose_cmd_subcommand_only_runtime
test_find_compose_cmd_matching_standalone
test_find_compose_cmd_explicit_runner_without_compose
test_podman_userns_rootless
test_podman_userns_rootful_and_others
test_supported_environments
test_unsupported_environment
echo ""
echo "=== Results: ${passed} passed, ${failed} failed ==="
[[ ${failed} -eq 0 ]] || exit 1
