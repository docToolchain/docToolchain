#!/usr/bin/env sh

curDir=$(pwd)
cd ../../../.. || exit 1
checkDir=docToolchain-$(date +%Y-%m-%dT%H-%M-%S)
mkdir "$checkDir"
cd "$checkDir" || exit 1

# get the repository
git clone git@github.com:docToolchain/docToolchain.git

cd docToolchain || (printf "\n\e[31;1mINTEGRATION TEST FAILED!\e[0m\n"; exit 1)
git checkout tags/V1.0.0 -b dtc-test


./gradlew

# testing and evaluating what I got
printf "\n\n"
printf '************************************************************************'
printf '\n'

# HTML build successful?
if [ -e build/docs/html5/manual.html ]
then
    printf "HTML generation was successful!\n"
    error=0
else
    printf "HTML generation failed!!\n"
    error=1
fi

# HTML build successful?
if [ -e build/docs/pdf/manual.pdf ]
then
    printf "PDF  generation was successful!\n"
    error=0
else
    printf "PDF generation failed!\n"
    error=1
fi

# Any error occured?
if [ "$error" -eq 1 ]
then
    printf "\n\e[31;1mINTEGRATION TEST FAILED!\e[0m\n"
else
    printf "\n\e[00;32mINTEGRATION TEST PASSED\e[0m\n"
fi

# Some Gradle processes remains they prevent the deleting of the test download
PID=$(tasklist | grep java.exe | awk '{print $2}')
KILL="/c taskkill /f /t /pid "$PID
cmd "$KILL"

# Remove the test download
cd ../.. || exit 1
rm -rf "$checkDir"

# go home
cd "$curDir" || exit 1
exit $error
