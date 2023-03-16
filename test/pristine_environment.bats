# SPDX-License-Identifier: MIT
# Copyright 2023, Max Hofer and the docToolchain contributors

# Test behavior on a clean environment

#
# One of the following pre-condition have to be met to run:
# - local install: wget or curl, unzip
# - sdkman
# - container runtime (like docker)
#

#
# This means: no wget, no curl, no docToolchain, no Java, no SDKMAN!, no docker
#

setup() {
    bats_load_library 'bats-support'
    bats_load_library 'bats-assert'

    # Needed for use of 'run -<expected_exit_code>', otherwise we get BW02 errors
    bats_require_minimum_version 1.5.0

    load 'test_helper.bash'
}

teardown() {
    # Restore environment
    enable_command unzip
    enable_command curl
    enable_command wget
}

@test "no arguments shows usage" {
    run -1 ./dtcw
    assert_line 'Usage: ./dtcw [option...] [task...]'
    assert_line 'Use "./dtcw tasks --group doctoolchain" to see available tasks.'
    assert_line 'Use "local", "sdk" or "docker" as first argument to force the use of a local, sdkman or docker install.'
}

@test "any argument asks for local installation - No" {
    # TODO: Why do we exit here with an error if we say don't install anything
    DTC_HEADLESS=false run -1 ./dtcw tasks --group doctoolchain <<< "2"

    assert_line 'docToolchain not installed.'
    assert_line 'sdkman not found'
    assert_line "Do you wish to install doctoolchain to '/root/.doctoolchain'?"
    assert_line '1) Yes'
    assert_line '2) No'
    assert_line '#? you need docToolchain as CLI-Tool installed or docker.'
    assert_line 'to install docToolchain as CLI-Tool, please install'
    assert_line 'sdkman and re-run this command.'
    assert_line 'https://sdkman.io/install'
    assert_line '$ curl -s "https://get.sdkman.io" | bash'
}

# TODO: split test for wget and curl. We have to add curl to our test image
@test "no curl/wget/docker installed" {
    disable_command wget
    disable_command curl

    # Asks for installation even if no curl/wget/sdkman/docker installed
    # TODO: Should quit with error messages explaining the preconditions are not
    # met before trying to download.
    DTC_HEADLESS=false run -1 ./dtcw tasks --group doctoolchain <<< "1"

    assert_line "Do you wish to install doctoolchain to '/root/.doctoolchain'?"
    assert_line "#? installing doctoolchain"
    assert_line --partial ">>> https://github.com/docToolchain/docToolchain/releases/download/"
    assert_line "you need either wget or curl installed"
    assert_line "please install it and re-run the command"
}

@test "wget installed, but no unzip" {
    disable_command unzip

    # Asks for installation even if no unzip installed
    # TODO: Should quit with error messages explaining the preconditions are not
    # met before trying to download.
    DTC_HEADLESS=false run -1 ./dtcw tasks --group doctoolchain <<< "1"

    assert_line "Do you wish to install doctoolchain to '/root/.doctoolchain'?"
    assert_line "#? installing doctoolchain"
    assert_line --partial ">>> https://github.com/docToolchain/docToolchain/releases/download/"
    assert_line "you need unzip installed"
    assert_line "please install it and re-run the command"
}

@test "sdk tasks" {
    # TODO: bug - why here no error code?
    run -0 ./dtcw sdk tasks

    assert_line "force use of sdkman"
    assert_line "docToolchain not installed."
    assert_line "please use sdkman to install docToolchain"
    # TODO: test will fail on version change
    assert_line "$ sdk install doctoolchain 2.2.1"
}

@test "docker tasks" {
    DTC_HEADLESS=false run -1 ./dtcw docker tasks <<< "2"

    assert_line "force use of docker"
    assert_line "docToolchain not installed."
    assert_line "sdkman not found"
    # TODO: bug - here we should abort since docker is not installed
    assert_line "Do you wish to install doctoolchain to '/root/.doctoolchain'?"
    # 1) Yes
    # 2) No
    # #? 2
    # you need docToolchain as CLI-Tool installed or docker.
    # to install docToolchain as CLI-Tool, please install
    # sdkman and re-run this command.
    # https://sdkman.io/install
    # $ curl -s "https://get.sdkman.io" | bash
}
