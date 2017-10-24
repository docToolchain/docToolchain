#!/bin/bash

set -u
set -e
set -o pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
    echo "Usage: docs.sh <doctoolchain> [<ConfluenceConfig file>]"
    echo ""
    echo "Examples:"
    echo "   Generate HTML and PDF output, but do not publish" 
    echo "      docs.sh ~/Git/docToolchain"
    echo ""
    echo "   Generate HTML and PDF output and publish to official Confluence space"
    echo "      docs.sh ~/Git/docToolchain ConfluenceConfig.groovy"
    echo ""
    echo "   Generate HTML and PDF output and publish to custom Confluence space"
    echo "      docs.sh ~/Git/docToolchain MyConfluenceConfig.groovy"
    exit 1
fi

# relative path from this script to the main 
base="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "base: " $base

doctoolchain=$1
echo "doctoolchain: " $doctoolchain

cd $doctoolchain

echo
echo "======================================================="
echo "Generating HTML"
echo "-------------------------------------------------------"
./gradlew generateHTML -PdocDir=$base

echo
echo "======================================================="
echo "Generating PDF"
echo "-------------------------------------------------------"
./gradlew generatePDF -PdocDir=$base

if [[ $# -eq 2 ]]; then
    confluenceconfig=$2
    echo "confluenceconfig: " $confluenceconfig
    space=$(grep -F confluenceSpaceKey $base/$confluenceconfig)
    prefix=$(grep -F confluencePagePrefix $base/$confluenceconfig)

    echo
    echo "======================================================="
    echo "Publishing to $space with $prefix"
    echo "-------------------------------------------------------"
    ./gradlew publishToConfluence -PdocDir=$base -PconfluenceConfigFile=$confluenceconfig --no-daemon -q
fi
