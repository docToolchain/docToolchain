# SPDX-License-Identifier: MIT
# Copyright 2022 - 2023, Ralf D. Müller and the docToolchain contributors

$ErrorActionPreference = "Stop"

# The main purpose of the wrapper script is to make 'docToolchain' easy to use.
# - it helps to install 'docToolchain' if not installed
# - you may manage different 'docToolchain' environments

# See https://github.com/docToolchain/docToolchain/releases for available versions.
# Set DTC_VERSION to "latest" to get the latest, yet unreleased version.
$DTC_VERSION = "3.3.1"
if ($env:DTC_VERSION) { $DTC_VERSION = $env:DTC_VERSION }

#here you can specify the URL of a theme to use with generateSite-task
#$env:DTC_SITETHEME = "https://....zip"

# The 'downloadTemplate' tasks uses DTC_TEMPLATE1, DTC_TEMPLATE2, ...
# with which you can specify the location of additional templates
# export DTC_TEMPLATE1=https://....zip
# export DTC_TEMPLATE2=https://....zip

# docToolchain configurtion file may be overruled by the user
$DTC_CONFIG_FILE = "docToolchainConfig.groovy"
if ($env:DTC_CONFIG_FILE) { $DTC_CONFIG_FILE = $env:DTC_CONFIG_FILE }

# Contains the current project git branch, "-" if not available
if (Test-Path ".git" ) { $env:DTCW_PROJECT_BRANCH = (git branch --show-current) } else { $env:DTCW_PROJECT_BRANCH = "" }

# Options passed to docToolchain
$DTC_OPTS = "$env:DTC_OPTS -PmainConfigFile='$DTC_CONFIG_FILE' --warning-mode=none --no-daemon -Dfile.encoding=UTF-8 "

$distribution_url = "https://github.com/docToolchain/docToolchain/releases/download/v$DTC_VERSION/docToolchain-$DTC_VERSION.zip"

# Here you find the project
$GITHUB_PROJECT_URL = "https://github.com/docToolchain/docToolchain"

# Bump this version up if something is changed in the wrapper script
$DTCW_VERSION = "0.53"
# Template replaced by the GitHub value upon releasing dtcw
$DTCW_GIT_HASH = "##DTCW_GIT_HASH##"

# Exit codes
$ERR_DTCW = 1
$ERR_ARG = 2
$ERR_CODING = 3

function main($_args) {

    # For debugging purpose at the top of the script
    $global:arch = "x64"
    $global:os = "windows"
    if ($isLinux) {
        $global:os = (uname -s)
        $global:arch = (uname -m)
    }

    print_version_info

    assert_argument_exists $_args

    if ($_args[0] -eq "--version" ) {
        exit 0
    }

    $available_environments = get_available_environments
    Write-Output "Available docToolchain environments: $available_environments"

    $dtc_installations = get_dtc_installations "$available_environments" "$DTC_VERSION"
    Write-Output "Environments with docToolchain [$DTC_VERSION]:$dtc_installations"

    if (is_supported_environment $_args[0] -And assert_environment_available $_args[0] $DTC_VERSION) {
        # User enforced environment
        $environment = $_args[0]
        # shift 1
        $null, $_args = $_args
        assert_argument_exists $_args
    }
    else {
        $environment = get_environment $_args
    }
    Write-Output "Using environment: $environment"

    if ( $_args[0] -eq "install" ) {
        # shift 1
        $null, $_args = $_args
        install_component_and_exit $environment $_args
    }
    elseif ( $_args[0] -eq "getJava" ) {
        # TODO: remove getJava in the next major release
        Write-Warning "Warning: 'getJava' is deprecated and and will be removed. Use './dtcw install java' instead."
        install_component_and_exit $environment "java"
    }
    # No install command, so forward call to docToolchain but first we check if
    # everything is there.
    $docker_image_name = ""
    $docker_extra_arguments = ""
    if ($environment -ne "docker") {
        assert_doctoolchain_installed "$environment" "$DTC_VERSION"
        assert_java_version_supported
        # TODO: what if 'doctoolchain' found by $PATH does not match the one from the local environment?
        # The version provided by $DTC_VERSION could be a different one.
    }
    else {
        $docker_image_name = "doctoolchain/doctoolchain"
        if ( $_args[0] -eq "install" ) {
            # shift 1
            $null, $_args = $_args
            $docker_image_name = $_args[0]
            # shift 1
            $null, $_args = $_args
            assert_argument_exists $_args
        }
        Write-Output "Using docker image: $docker_image_name"
        if ($args[0] -eq "extra_arguments") {
            # shift 1
            $null, $_args = $_args
            $docker_extra_arguments = $args[0]
            # shift 1
            $null, $_args = $_args
            assert_argument_exists $args
            Write-Host "Extra arguments passed to docker run $docker_extra_arguments"
        }
    }
    # TODO: can generateDeck, bakePreview be used in combination with other commands?
    # The code below assumes we have just one task.

    $global:command = build_command "$environment" "$DTC_VERSION" "$docker_image_name" "$docker_extra_arguments" $_args

    #TODO: implement HEADLESS mode
    # [[ "${DTC_HEADLESS}" = true ]] && echo "Using headless mode since there is no (terminal) interaction possible"
    #
    show_os_related_info

    #Write-Output "Command to invoke: $global:command"
    Invoke-Expression "$global:command"
}

function assert_argument_exists($_args) {
    if ($_args.length -eq 0) {
        Write-Warning "argument missing"
        usage
        exit $ERR_ARG
    }
}

function usage() {
    Write-Output @"
dtcw - Create awesome documentation the easy way with docToolchain.

Usage: ./dtcw.ps1 [environment] [option...] [task...]
       ./dtcw.ps1 [local] install {doctoolchain | java }

Use 'local' or 'docker' as first argument to force the use of a specific
docToolchain environment:
- local: installation in '$HOME/.doctoolchain'
- docker: use docToolchain container image

Examples:

    Download and install docToolchain in '${DTC_ROOT}':
    ./dtcw.ps1 local install doctoolchain

    Download and install project documentation template from https://arc42.org/ :
    ./dtcw.ps1 downloadTemplate

    Generate PDF:
    ./dtcw.ps1 generatePDF

    Generate HTML:
    ./dtcw.ps1 generateHTML

    Publish HTML to Confluence:
    ./dtcw.ps1 publishToConfluence

    Multiple tasks may be provided at once:
    ./dtcw.ps1 generatePDF generateHTML

Detailed documentation how to use docToolchain may be found at https://doctoolchain.org/

Use './dtcw.ps1 tasks --group doctoolchain' to see docToolchain related tasks.
Use './dtcw.ps1 tasks' to see all tasks.
"@
}

function print_version_info() {
    Write-Host "dtcw ${DTCW_VERSION} - ${DTCW_GIT_HASH}"
    if (is_doctoolchain_development_version ${DTC_VERSION}) {
        $dtc_git_hash = "unknown"
        $dtc_git_hash = (git -C "$DTC_HOME" rev-parse --short=8 HEAD 2> $null)
        Write-Host "docToolchain ${DTC_VERSION} - ${dtc_git_hash}"
    }
    else {
        Write-Host "docToolchain ${DTC_VERSION}"
    }
    Write-Host "OS/arch: pwsh $os $arch"
}

function get_available_environments() {
    # The local environment is alway available - even if docToolchain is not installed
    $envs = "local"

    if (has docker) {
        $envs += " docker"
    }
    return $envs
}

function has($global:command) {
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        return $True
    }
    else {
        return $False
    }
}

function get_dtc_installations($envs, $version) {
    $installations = ""

    if (Test-Path "$DTC_HOME\bin\doctoolchain") {
        $installations += " local"
    }

    if ($envs.Contains("docker")) {
        # Having docker installed means docToolchain is available
        $installations += " docker"
    }

    #TODO: was bedeutet das?
    #[ -z "${installations}" ] && installations=" none"

    return "$installations"
}

function sdk_home_doctoolchain() {
    # not implemented
}

function is_supported_environment($environment) {
    $supported_environments = "local docker"
    return $supported_environments.Contains($environment)
}

function assert_environment_available($environment, $version) {
    # If environment not available, exit with error
    if ((is_environment_available $environment) -eq $False) {
        Write-Error "argument error - environment '$environment' not available"
        if ($environment -eq "sdk") {
            Write-Output "sdkman is not supported on windows"
        }
        else {
            # docker
            Write-Output "Install 'docker' on your host to execute docToolchain in a container."
        }
        exit $ERR_ARG
    }
    if ((is_doctoolchain_development_version $version) -And ($environment -ne "local")) {
        Write-Error "argument error - invalid environment '$environment'."
        Write-Output "Development version '$version' can only be used in a local environment."
        exit $ERR_ARG
    }
}

function is_environment_available($environment) {
    return $available_environments.Contains($environment)
}

function is_doctoolchain_development_version($version) {
    # Is 'latest' a good name? It maybe interpreted as latest stable release.
    # Alternatives: 'testing', 'dev' (used for development)
    if ( ($version -eq "latest") -Or ($version -eq "latestdev") ) {
        return $True
    }
    else {
        return $False
    }
}

# No environment provided - try to pick the right one
function get_environment($_args) {
    # 'install' works only with 'local' environment
    if ( ($_args[0] -eq 'install') -Or $dtc_installations.Contains('none')) {
        return "local"
    }

    # Pick the first one which has an installed docToolchain.
    # Note: the preference is defined by the order we searched for available environments.

    ForEach ($environment in ($available_environments -split " ")) {
        if (is_doctoolchain_installed $environment) {
            return $environment
        }
    }

}
function is_doctoolchain_installed($environment) {
    return $dtc_installations.Contains($environment)
}

function install_component_and_exit($environment, $component) {
    if ( $componet -eq '' ) {
        error_install_component_and_die "component missing"
    }
    $exit_code = 1
    switch ($environment) {
        "local" {
            switch ($component) {
                "doctoolchain" {
                    local_install_doctoolchain $DTC_VERSION
                    Write-Output ""
                    Write-Output "Use './dtcw.ps1 tasks --group doctoolchain' to see docToolchain related tasks."
                    assert_java_version_supported
                }
                "java" {
                    local_install_java
                    if ( (is_doctoolchain_installed $environment) -eq $False) {
                        how_to_install_doctoolchain $DTC_VERSION
                    }
                }
                * {
                    error_install_component_and_die "unknown component '$component'"
                }
            }
            $exit_code = 0

        }
        "sdk" {
            Write-Warning "argument error - '$environemnt install' not supported."
            Write-Output ""
            Write-Output "SDKMAN! is not supported on windows"
            $exit_code = $ERR_ARG
        }
        "docker" {
            Write-Warning "argument error - '$environment install' not supported."
            Write-Output "Executing a task in the 'docker' environment will pull the docToolchain container image."
            $exit_code = $ERR_ARG
        }
    }
    exit $exit_code
}

function error_install_component_and_die($component) {
    Write-Error  "$component - available components are 'doctoolchain' or 'java'"
    Write-Output ""
    Write-Output "Use './dtcw.ps1 local install doctoolchain' to install docToolchain $DTC_VERSION."
    Write-Output "Use './dtcw.ps1 local install java' to install a Java version supported by docToolchain."
    exit $ERR_ARG
}

function local_install_doctoolchain($version) {
    if (is_doctoolchain_development_version $version) {
        # User decided to pick a floating version - which means a git clone
        # into the local environment.
        assert_git_installed "Please install 'git' for working with a 'doctToolchain' develpment version"

        # Checkout code if we use a develpment version.
        # TODO: cleanup code here
        # check if we have to clone or just pull
        if (Test-Path "$DTC_HOME/.git" ) {
            Invoke-Expression "git -C ""$DTC_HOME"" pull"
            Write-Output "Updated docToolchain in local environment to latest version"
        }
        else {
            if ($version -eq "latest") {
                Invoke-Expression "git clone ""${GITHUB_PROJECT_URL}.git"" ""$DTC_HOME"" "
            }
            else {
                # TODO: derive the ssh URL from GITHUB_PROJECT_URL
                Invoke-Expression "git clone git@github.com:docToolchain/docToolchain.git ""$DTC_HOME"" "
            }
            Write-Output "Cloned docToolchain in local environment to latest version"
        }
    }
    else {
        New-Item -Path ${DTC_ROOT} -ItemType "directory" -Force > $null
        if ($proxy) {
            # Pass Proxy-Settings to Gradle
            $gradleFile = "gradle.properties"
            if (-not(Test-Path -Path $gradleFile -PathType Leaf)) {
                $proxyHost = $proxy.Scheme + "://" + $proxy.Host
                $proxyPort = $proxy.Port
                "# Generated by dtcw.ps1"               | Out-File -FilePath $gradleFile -Append
                "systemProp.http.proxyHost=$proxyHost"  | Out-File -FilePath $gradleFile -Append
                "systemProp.http.proxyPort=$proxyPort"  | Out-File -FilePath $gradleFile -Append
                "systemProp.https.proxyHost=$proxyHost" | Out-File -FilePath $gradleFile -Append
                "systemProp.https.proxyPort=$proxyPort" | Out-File -FilePath $gradleFile -Append
            }
            # Use Proxy for downloading the distribution
            Invoke-WebRequest $distribution_url -OutFile "${DTC_ROOT}\source.zip" -Proxy $proxy -ProxyUseDefaultCredentials
        }
        else {
            Invoke-WebRequest $distribution_url -OutFile "${DTC_ROOT}\source.zip"
        }
        Expand-Archive -Force -LiteralPath "${DTC_ROOT}\source.zip" -DestinationPath "${DTC_ROOT}\"
        Remove-Item "${DTC_ROOT}\source.zip"     #  << Remove .zip ?
        if ($global:os -eq "windows") {
            $global:command = "&'${DTC_ROOT}\docToolchain-$DTC_VERSION\bin\doctoolchain.bat' . $global:commandArgs $DTC_OPTS"
        }
        else {
            $global:command = "&'${DTC_ROOT}\docToolchain-$DTC_VERSION\bin\doctoolchain' . $global:commandArgs $DTC_OPTS"
        }
        Write-Output "Installed docToolchain successfully in '${DTC_HOME}'."
    }
    # Add it to the existing installations so the output to guide the user can adjust accordingly.
    $dtc_installations += " local"
}

function assert_git_installed($message) {
    if (has git) {
        return
    }
    else {
        Write-Warning "git - command not found"
        Write-Warning $message
        exit $ERR_DTCW
    }
}

function download_file($url, $file) {
    Invoke-WebRequest $url -OutFile $file
}

function assert_java_version_supported() {
    # Defines the order in which Java is searched.
    $JAVA_CMD = $null

    if ( Test-Path "$DTC_JAVA_HOME") {
        Write-Host "Check Java from $javaHome"
        # Get the list of directories that start with 'jdk-'
        $javaDirs = Get-ChildItem -Path $DTC_JAVA_HOME -Directory | Where-Object { $_.Name -like "jdk-*" }
        # Select the first directory from the list
        $selectedJavaDir = $javaDirs | Select-Object -First 1
        # Construct the complete Java path
        $javaHome = Join-Path -Path $DTC_JAVA_HOME -ChildPath $selectedJavaDir.Name

        $JAVA_CMD = Get-Command "$javaHome\bin\java" -ErrorAction SilentlyContinue
        $dtc_opts = "$dtc_opts '-Dorg.gradle.java.home=$javaHome' "
    }

    if ( $null -eq $JAVA_CMD) {
        Write-Host "Check Java from Path"
        $JAVA_CMD = Get-Command java -ErrorAction SilentlyContinue
    }

    if ( $null -eq $JAVA_CMD -and $null -ne $env:JAVA_HOME -and $env:JAVA_HOME -ne "") {
        Write-Host "Check Java from $env:JAVA_HOME"
        $JAVA_CMD = Get-Command "$env:JAVA_HOME\bin\java" -ErrorAction SilentlyContinue
    }

    if ($null -eq $JAVA_CMD) {
        Write-Warning "unable to locate a Java Runtime"
        java_help_and_die
    }

    # We got a Java version
    $javaversion = ($JAVA_CMD | Select-Object -ExpandProperty Version).Major

    Write-Output "Java Version $javaversion"

    if ([int]$javaversion -lt 11 ) {
        Write-Warning @"
unsupported Java version ${javaversion} [$JAVA_CMD]
"@
        java_help_and_die
    }
    else {
        if ([int]$javaversion -gt 17 ) {
            Write-Warning @"
unsupported Java version ${javaversion} [$JAVA_CMD]
"@
            java_help_and_die
        }
    }
    Write-Output "Using Java ${javaversion} [${JAVA_CMD}]"
    return
}

function java_help_and_die() {
    Write-Host @"
docToolchain supports Java versions 11, 14 or 17 (preferred). In case one of those
Java versions is installed make sure 'java' is found with your PATH environment
variable. As alternative you may provide the location of your Java installation
with JAVA_HOME.

Apart from installing Java with the package manager provided by your operating
system, dtcw facilitates the Java installation into a local environment:

    # Install Java in '${DTC_JAVA_HOME}'
    $ ./dtcw local install java

If you prefer not to install Java on your host, you can run docToolchain in a
docker container. For this case dtcw provides the 'docker' execution environment.

Example: ./dtcw docker generateSite

"@
    exit $ERR_DTCW
}

function how_to_install_sdkman() {
    # sdkman is not supported on windows
}

function local_install_java() {
    $version = "17"
    $implementation = "hotspot"
    $heapsize = "normal"
    $imagetype = "jdk"
    $releasetype = "ga"

    switch ($global:arch) {
        'x86_64' { $global:arch = "x64" }
        'arm64' { $global:arch = "aarch64" }
    }
    if ( ${os} -eq "MINGW64" ) {
        Write-Error "MINGW64 is not supported, Please use powershell or WSL"
    }
    switch ($global:os) {
        'Linux' { $global:os = 'linux' }
        'Darwin' { $global:os = 'max' }
        'Cygwin' { $global:os = 'linux' }
    }
    if ($global:os -eq 'windows') { $targetFile = 'jdk.zip' } else { $targetFile = 'jdk.tar.gz' }

    if (!(Test-Path -Path $DTC_JAVA_HOME )) {
        New-Item -ItemType directory -Path $DTC_JAVA_HOME > $null
    }
    Write-Output "Downloading JDK Temurin $version [$global:os/$global:arch] from Adoptium to $DTC_JAVA_HOME/${targetFile}"
    $adoptium_java_url = "https://api.adoptium.net/v3/binary/latest/$version/$releasetype/$global:os/$global:arch/$imagetype/$implementation/$heapsize/eclipse?project=jdk"
    download_file "$adoptium_java_url" "$DTC_JAVA_HOME/${targetFile}"
    Write-Output "Extracting JDK from archive file."
    # TODO: should we not delete a previsouly installed on?
    # Otherwise we may get a mix of different Java versions.
    if ($global:os -eq 'windows') {
        Expand-Archive -Force -LiteralPath "$DTC_JAVA_HOME\${targetFile}" -DestinationPath "$DTC_JAVA_HOME"
    }
    else {
        tar -zxf "${DTC_JAVA_HOME}/${targetFile}" --strip 1 -C "${DTC_JAVA_HOME}/."
    }
    Remove-Item "$DTC_JAVA_HOME\${targetFile}"

    Write-Output "Successfully installed Java in '$DTC_JAVA_HOME'."
}

function assert_doctoolchain_installed($environment, $version) {

    if ( (is_doctoolchain_installed $environment) -eq $False) {
        # We reach this point if the user executes a command in an
        # environment where docToolchain is not installed.
        # Note that 'docker' always has a command (no instalation required)
        Write-Warning "doctoolchain - command not found [environment '$environment']"
        how_to_install_doctoolchain "$version"
        exit $ERR_DTCW
    }
}

function how_to_install_doctoolchain($version) {
    Write-Output @"

It seems docToolchain $version is not installed. dtcw supports the
following docToolchain environments:

1. 'local': to install docToolchain in [$DTC_ROOT] use

    > ./dtcw.ps1 local install doctoolchain

Note that running docToolchain in 'local' environment needs a
Java runtime (major version 11, 14 or 17) installed on your host.

2. 'docker': pull the docToolchain image and execute docToolchain in a container environment.

    > ./dtcw.ps1 docker tasks --group doctoolchain

"@
}

function build_command($environment, $version, $docker_image, $docker_extra_arguments , $_args) {
    $cmd = ""
    if ( $environment -eq "docker") {
        if (-not (Invoke-Expression "docker ps")) {
            Write-Output ""
            Write-Output "Docker does not seem to be running, run it first and retry again."
            Write-Output "If you want to use a local installation of doctoolchain instead,"
            Write-Output "use 'local' as first argument to force the installation and use of a local install."
            Write-Output ""
            Write-Output "Example: ./dtcw.ps1 local install doctoolchain"
            Write-Output ""
            exit 1
        }
        $container_name = "doctoolchain-${version}-$(Get-date -uFormat '+%Y%m%d_%H%M%S')"
        $docker_cmd = Get-Command docker

        $docker_env_file = "dtcw_docker.env"
        $env_file_option = ""
        if (Test-Path $docker_env_file) {
            $env_file_option = "--env-file ${docker_env_file}"
        }

        # TODO: DTC_PROJECT_BRANCH is  not passed into the docker environment
        # See https://github.com/docToolchain/docToolchain/issues/1087
        $docker_args = "run --rm -i --name ${container_name} -e DTC_HEADLESS=1 -e DTC_SITETHEME -e DTC_PROJECT_BRANCH=${DTC_PROJECT_BRANCH} $docker_extra_arguments $env_file_option --entrypoint /bin/bash -v '${PWD}:/project' ${docker_image}:v${version}"
        $cmd = "$docker_cmd ${docker_args} -c ""doctoolchain . $_args ${DTC_OPTS} && exit "" "

    }
    else {
        if ( $environment -eq "local" ) {
            if ($global:os -eq "windows") {
                $cmd = "${DTC_HOME}/bin/doctoolchain.bat . $_args ${DTC_OPTS}"
            }
            else {
                $cmd = "${DTC_HOME}/bin/doctoolchain . $_args ${DTC_OPTS}"
            }
        }
    }
    return $cmd
}

function show_os_related_info() {
    # not needed on windows
}

# Location where local installations are placed.
$DTC_ROOT = "$HOME/.doctoolchain"

# More than one docToolchain version may be installed locally.
# This is the directory for the specific version.
$DTC_HOME = "$DTC_ROOT/docToolchain-$DTC_VERSION"

# Directory for local Java installation
$DTC_JAVA_HOME = "$DTC_ROOT/jdk"

main $args
