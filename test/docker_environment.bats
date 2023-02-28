# SPDX-License-Identifier: MIT
# Copyright 2023, Max Hofer and the docToolchain contributors

# Test dtcw wrapper in a pure docker environment

setup_file() {
    load 'test_helper.bash'
    mock_create docker
}

teardown_file() {
    mock_delete docker
}

setup() {
    bats_load_library 'bats-support'
    bats_load_library 'bats-assert'

    # Needed for use of 'run -<expected_exit_code>', otherwise we get BW02 errors
    bats_require_minimum_version 1.5.0
}

@test "forward call to docker" {
    run -0 ./dtcw tasks --group doctoolchain
    assert_line --partial "dtcw - docToolchain wrapper"
    assert_line --partial "docToolchain"
    assert_line "docker available"
    assert_line "/usr/local/bin/docker"
    assert_line "use docker installation"
    assert_line --partial "docker mock called"
}
