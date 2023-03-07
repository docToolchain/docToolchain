
#here you can specify the URL of a theme to use with generateSite-task
#$env:DTC_SITETHEME = "https://....zip"

$main_config_file = "docToolchainConfig.groovy"
$version = "2.2.1"
$dockerVersion = "2.2.1"
$distribution_url = "https://github.com/docToolchain/docToolchain/releases/download/v$version/docToolchain-$version.zip"

if (Test-Path ".git" ) {
    $env:DTCW_PROJECT_BRANCH = (git branch --show-current)
} else {
    $env:DTCW_PROJECT_BRANCH = ""
}


# https://docs.microsoft.com/en-us/windows/deployment/usmt/usmt-recognized-environment-variables
$home_path = $env:USERPROFILE
$folder_name = ".doctoolchain"
$dtcw_path = "$home_path\$folder_name"
$doJavaCheck = $True

$dtc_opts="$env:dtc_opts -PmainConfigFile='$main_config_file' --warning-mode=none --no-daemon "

function java_help_and_die()
{
    Write-Host @"
to install a local java for docToolchain, you can run

./dtcw getJava

or you can download it from https://adoptium.net/

make sure that your java version is between 8 and 14

If you do not want to use a local java installtion, you can also use docToolchain as docker container.
In that case, specify 'docker' as first parameter in your statement.
example: ./dtcw docker generateSite
"@

    exit 1
}

function checkJava()
{
    if (Get-Command java -ErrorAction SilentlyContinue)
    {
        $java = $True
        $javaversion = ((Get-Command java | Select-Object -ExpandProperty Version).toString() -split "[.]")[0]
        echo "Java Version $javaversion"

        if ([int]$javaversion -lt 8 ) {
            Write-Warning @"
your java version $javaversion is too old (<8)
"@
            java_help_and_die
        } else {
            if ([int]$javaversion -gt 14 ) {
                Write-Warning @"
your java version $javaversion is too new (>14)
please update your java installation and try again
"@
                java_help_and_die
            }
        }
    }
    else
    {
        # Text adapted
        Write-Warning @'
docToolchain depends on java, but the java command couldn't be found.

To install java, you can type

./dtcw getJava

to invoke an automatic installer. This will install JDK 11 only for docToolchain without interfering with your current Java installations.

...or you could follow the next link and manually install JDK 11 on your system for global use:

>> https://adoptium.net/
please choose Temurin 11
'@
        exit 1
    }
}

Write-Host "dtcw - docToolchain wrapper V0.39(PS)"

if ($args.Count -lt 1) {
    # Help text adapted to Win/PS: /<command>.ps1
    Write-Warning @'
Usage: ./dtcw [option...] [task...]

You can use the same options and tasks as in underlying gradle.
Use "./dtcw.ps1 tasks --group doctoolchain" to see available tasks.
Use "local" or "docker" as first argument to force the use of a local or docker install.

Examples:

    Download and install arc42 Template:
    ./dtcw.ps1 downloadTemplate

    Generate PDF:
    ./dtcw.ps1 generatePDF

    Generate HTML:
    ./dtcw.ps1 generateHTML

    Publish HTML to Confluence:
    ./dtcw.ps1 publishToConfluence

    get more documentation at https://doctoolchain.github.io
'@
    exit 1
}

# check if CLI or docker are installed:
$cli = $docker = $exist_home =  $False

if (Get-Command dooctoolchain -ErrorAction SilentlyContinue) {
    Write-Host "docToolchain as CLI available"
    $cli = $True
}

if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Host "docker available"
    $docker = $True
    $doJavaCheck = $False
}

if (Test-Path "$dtcw_path\docToolchain-$version" ) {
    Write-Host "dtcw folder exist: '$dtcw_path'"
    $exist_home = $True
    $docker = $False
    $doJavaCheck = $True
}

switch ($args[0]) {
    "local" {
        Write-Host "force use of local install"
        $docker = $False
        $doJavaCheck = $True
        $firstArgsIndex = 1   # << Shift first param
    }
    "docker" {
        Write-Host "force use of docker"
        $cli = $exist_home = $False
        $firstArgsIndex = 1   # << Shift first param
        $doJavaCheck = $False
        $docker = $True
    }
    "getJava" {
        Write-Host "this script assumes that you have a 64 bit Windows installation"
        Write-Host "it now tries to install Java for you"
        New-Item -Path $home_path -Name $folder_name -ItemType "directory" -Force | Out-Null
        $jdkDistribution = "https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.15%2B10/OpenJDK11U-jdk_x64_windows_hotspot_11.0.15_10.zip"
        Write-Host "downloadting JDK temurin11 from adoptiom to $dtcw_path/jdk.zip"
        Invoke-WebRequest $jdkDistribution -OutFile "$dtcw_path\jdk.zip"
        Write-Host "expanding JDK"
        Expand-Archive -LiteralPath "$dtcw_path\jdk.zip" -DestinationPath "$dtcw_path\"
        $firstArgsIndex = 1   # << Shift first param
        exit 1
    }
    default {
        $firstArgsIndex = 0   # << Use all params
    }
}
if ($args[0] -eq "generateDeck") {
    if (Test-Path "$dtcw_path\docToolchain-$version\resources\reveal.js" ) {
        # reveal.js already exists
        Write-Host ""
    } else {
        # clone reveal.js
        Write-Host "cloning reveal.js"
        Invoke-Expression "Push-Location $dtcw_path/docToolchain-$version/resources/. ; ./clone.ps1 ; Pop-Location"
    }
}
if ($docker)
{
    # nothing to do
}
else
{
    $dtc_opts = "$dtc_opts '--gradle-user-home=$dtcw_path\.gradle'"
    if (Test-Path "$dtcw_path\jdk-11.0.15+10")
    {
        Write-Host "local java JDK-11 found"
        $java = $True
        $dtc_opts = "$dtc_opts '-Dorg.gradle.java.home=$dtcw_path\jdk-11.0.15+10' "
        if (test-path env:JAVA_HOME)
        {
        }
        else
        {
            $env:JAVA_HOME = "$dtcw_path\jdk-11.0.15+10"
        }
        $doJavaCheck = $False
    }
}

#if bakePreview is called, deactivate deamon
if ( $args[0] -eq "bakePreview" ) {
    $dtc_opts="$dtc_opts -Dorg.gradle.daemon=false"
}

$commandArgs = $args[$firstArgsIndex..$args.Count] -Join " "

if ($cli) {
    # Execute local
    $command = "doctoolchain . $commandArgs $DTC_OPTS"
}
elseif ($exist_home) {
    $command = "&'$dtcw_path\docToolchain-$version\bin\doctoolchain.bat' . $commandArgs $DTC_OPTS"
}
elseif ($docker) {
    # Check Docker is running...
    if (-not (Invoke-Expression "docker ps")) {
        Write-Host ""
        Write-Host "Docker does not seem to be running, run it first and retry"
        Write-Host "if you want to use a local installation of doctoolchain instead"
        Write-Host "use 'local' as first argument to force the installation and use of a local install."
        Write-Host ""
        Write-Host "Example: ./dtcw.ps1 local install"
        Write-Host ""
        exit 1
    }
    # Write-Host "Docker is running :)"
    $docker_cmd = Get-Command docker
    Write-Host $docker_cmd
    $command = "$docker_cmd run --name doctoolchain${dockerVersion} -e DTC_HEADLESS=1 -e DTC_SITETHEME -p 8042:8042 --rm -it --entrypoint /bin/bash -v '${PWD}:/project' 'doctoolchain/doctoolchain:v$dockerVersion' -c ""doctoolchain . $commandArgs $DTC_OPTS && exit"""
    $doJavaCheck = $False
}
else {
    Write-Host "docToolchain $version is not installed."

    $confirmation = Read-Host "Do you wish to install doctoolchain to '$dtcw_path\'? [Y/N]"
    if ($confirmation -eq 'y') {
        New-Item -Path $home_path -Name $folder_name -ItemType "directory" -Force | Out-Null
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
            Invoke-WebRequest $distribution_url -OutFile "$dtcw_path\source.zip" -Proxy $proxy -ProxyUseDefaultCredentials
        } else {
            Invoke-WebRequest $distribution_url -OutFile "$dtcw_path\source.zip"
        }
        Expand-Archive -LiteralPath "$dtcw_path\source.zip" -DestinationPath "$dtcw_path\"
        # Remove-Item "$dtcw_path\source.zip"     #  << Remove .zip ?
        $command = "&'$dtcw_path\docToolchain-$version\bin\doctoolchain.bat' . $commandArgs $DTC_OPTS"
    } else {
        Write-Warning @'
you need docToolchain as CLI-Tool installed or docker.

'@
        exit 1
    }
}

if ($doJavaCheck) {
    checkJava
}
# Write-Host "Command to invoke: '$command'" # << line for debugging
Invoke-Expression "$command"
