# SPDX-License-Identifier: MIT
# Copyright 2023, Max Hofer and the docToolchain contributors

# Test installation on a clean environment

# This means: no docToolchain, no Java, no SDKMAN!, no docker
#
# The tests follow the installation instructions based on the output provided by `dtcw`.
# They download external software packages (Java, docToolchain) which makes them slow.
#
# ATTENTION: contrary to good test patterns, the tests in this file depend on
# each other to keep the test execution (and downloads) as short as possible.
#
# TODO: write mocks for wget/curl which use a local cache instead of downloading
# the packages each time.
# See https://advancedweb.hu/how-to-mock-in-bash-tests/

setup() {
    bats_load_library 'bats-support'
    bats_load_library 'bats-assert'
    bats_load_library 'bats-file'

    # Needed for use of 'run -<expected_exit_code>', otherwise we get BW02 errors
    bats_require_minimum_version 1.5.0

    load 'test_helper.bash'
}

teardown() {
    mock_delete docker
}

teardown_file() {
    echo "disable teardown"
    rm -rf $HOME/.doctoolchain
    rm -rf docToolchainConfig.groovy
}

# bats test_tags=download
@test "install docToolchain locally" {
    # TODO: bug - why do we return an error even when docToolchain was installed successfully?
    DTC_HEADLESS=false run -1 ./dtcw tasks --group doctoolchain <<< "1"

    # Output from wget and unzip - changes on environment/version
    # assert_line --partial 'Connecting to github.com'
    # assert_line "saving to '/root/.doctoolchain/source.zip'"
    # assert_line "'/root/.doctoolchain/source.zip' saved"

    # TODO: unzip is too spammy
    assert_line "docToolchain depends on java, but the java command couldn't be found in this shell (bash)"
    assert_line 'it might be that you have installed the needed version java in another shell from which you started dtcw'
    assert_line 'dtcw is running in bash and uses the PATH to find java'
    assert_line 'to install a local java for docToolchain, you can run'
    assert_line './dtcw getJava'
    assert_line 'another way to install or update java is to install'
    assert_line 'sdkman and then java via sdkman'
    assert_line 'https://sdkman.io/install'
    assert_line '$ curl -s "https://get.sdkman.io" | bash'
    assert_line '$ sdk install java'
    assert_line 'or you can download it from https://adoptium.net/'
    assert_line 'make sure that your java version is between 8 and 14'
    assert_line 'If you do not want to use a local java installation, you can also use docToolchain as docker container.'
    assert_line "In that case, specify 'docker' as first parameter in your statement."
    assert_line 'example: ./dtcw docker generateSite'
}

# bats test_tags=download
@test "install java with getJava" {
    # TODO: bug - why do we return an error even when Java was installed successfully?
    run -1 ./dtcw getJava

    assert_line 'this script assumes that you have linux as operating system (x64 / linux)'
    assert_line 'it now tries to install Java for you'
    assert_line 'downloading JDK Temurin 11 from Adoptium to /root/.doctoolchain/jdk.tar.gz'
    # TODO: download is really spammy

    # Output from wget and unzip - changes on environment/version
    # assert_line --partial 'Connecting to api.adoptium.net'
    # assert_line --partial 'Connecting to github.com'
    # assert_line --partial 'Connecting to objects.githubusercontent.com'
    # assert_line "saving to '/root/.doctoolchain/jdk/jdk.tar.gz'"
    # assert_line "'/root/.doctoolchain/jdk/jdk.tar.gz' saved"
    assert_line 'expanding JDK'

    run -0 "${HOME}/.doctoolchain/jdk/bin/java" --version
}

# bats test_tags=download
@test "skip create docToolchainConfig" {
    # The answer if want to create the default configuration with "n"
    DTC_HEADLESS=false run -1 ./dtcw tasks --group doctoolchain <<< "n"

    # TODO: Gradle  materiaization/start is spammy - can we turn it off?

    # Use '--partial' due to color codes in the console output
    assert_line --partial "Config file '/code/docToolchainConfig.groovy' does not exist"
    assert_line --partial "do you want me to create a default one for you?"

    assert_line "FAILURE: Build failed with an exception."
    assert_line "* What went wrong:"
    assert_line "A problem occurred evaluating script."
    assert_line "> can't continue without a config file"
}

# bats test_tags=download
@test "create docToolchainConfig" {
    # The answer if want to create the default configuration with "n"
    DTC_HEADLESS=false run -0 ./dtcw tasks --group doctoolchain <<< "y"

    # TODO: 'gradlew' is not available
    assert_line "To see all tasks and more detail, run gradlew tasks --all"
    assert_line "To see more detail about a task, run gradlew help --task <task>"
}

# bats test_tags=download
@test "fail on unknown task" {
    # Once eveything is installed an error is shown for an unknown task
    run -1 ./dtcw unknown_task

    assert_line "FAILURE: Build failed with an exception."
    assert_line "* What went wrong:"
    assert_line "Task 'unknown_task' not found in root project 'docToolchain'."
}

# bats test_tags=download
@test "use sdk with local installation" {
    # TODO: bug - why here no error code?
    run -0 ./dtcw sdk tasks

    assert_line "home folder exists"
    assert_line "force use of sdkman"
    assert_line "local java JDK found"
    assert_line "use /root/.doctoolchain/jdk as JDK"
    # TODO: bug - this is clearly wrong - docToolchain is installed
    assert_line "docToolchain not installed."
    # TODO: bug - sdkman is not installed - so we should tell to install it
    assert_line "please use sdkman to install docToolchain"
    # TODO: test will fail on version change
    assert_line "$ sdk install doctoolchain 2.2.1"

}

# bats test_tags=download
@test "use docker with local installation" {
    skip "this use case is completly buggy"

    run ./dtcw docker tasks

    assert_line "home folder exists"
    assert_line "force use of docker"
    # TODO: bug - if we use docker we don't care about Java
    assert_line "local java JDK found"
    assert_line "use /root/.doctoolchain/jdk as JDK"
    # TODO: bug - this is clearly wrong - docToolchain is installed
    assert_line "docToolchain not installed."
    # TODO: bug - if we use docker we don't care abouf sdkman
    assert_line "please use sdkman to install docToolchain"
    assert_line "sdkman not found"
    # TODO: bug
    # Do you wish to install doctoolchain to '/root/.doctoolchain'?
    # 1) Yes
    # 2) No
    # #? 2
    # you need docToolchain as CLI-Tool installed or docker.
    # to install docToolchain as CLI-Tool, please install
    # sdkman and re-run this command.
    # https://sdkman.io/install
    # $ curl -s "https://get.sdkman.io" | bash
}

# bats test_tags=download
@test "no side effect when installing docker" {
    mock_create docker

    # The installation of docker should not have any effect
    run -1 ./dtcw tasks --group doctoolchain

    assert_line "docker available"
    assert_line "home folder exists"
    assert_line "use local homefolder install /root/.doctoolchain/"

    # TODO: bug - instaling docker should not affect the current installation
    # We have local Java installed.
    assert_line "docToolchain depends on java, but the java command couldn't be found in this shell (bash)"
}
