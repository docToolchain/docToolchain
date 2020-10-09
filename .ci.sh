#!/bin/bash

set -o errtrace -o nounset -o pipefail -o errexit

# Goto directory of this script
cd "$(dirname "${BASH_SOURCE[0]}")"

cleaning () {
  echo "############################################"
  echo "#                                          #"
  echo "#        Cleaning                          #"
  echo "#                                          #"
  echo "############################################"
  ./gradlew clean
}

dependency_info() {
  echo "############################################"
  echo "#                                          #"
  echo "#        Check for dependency updates      #"
  echo "#                                          #"
  echo "############################################"
  ./gradlew -b init.gradle dependencyUpdates
  ./gradlew dependencyUpdates
}

unit_tests () {
  echo "############################################"
  echo "#                                          #"
  echo "#        Unit testing                      #"
  echo "#                                          #"
  echo "############################################"
  ./gradlew test --info
}

integration_tests () {
  echo "############################################"
  echo "#                                          #"
  echo "#        Integration testing               #"
  echo "#                                          #"
  echo "############################################"
  TEMPLATES='Arc42DE Arc42EN Arc42ES'
  for TEMPLATE in ${TEMPLATES}; do
    echo "### ${TEMPLATE}"
    TEST_DIR="build/${TEMPLATE}_test"

    ./gradlew -b init.gradle "init${TEMPLATE}" -PnewDocDir="${TEST_DIR}"
    ./bin/doctoolchain "${TEST_DIR}" generatePDF
    ./bin/doctoolchain "${TEST_DIR}" generateHTML
    # ./bin/doctoolchain "${TEST_DIR}" publishToConfluence

    echo "#### check for html result"
    if [ ! -f "${TEST_DIR}"/build/html5/arc42-template.html ]; then exit 1; fi
    echo "#### check for pdf result"
    if [ ! -f "${TEST_DIR}"/build/pdf/arc42-template.pdf ]; then exit 1; fi
  done
}

check_for_clean_worktree() {
  echo "############################################"
  echo "#                                          #"
  echo "#        Check for clean worktree          #"
  echo "#                                          #"
  echo "############################################"
  # To be executed as latest possible step, to ensures that there is no
  # uncommitted code and there are no untracked files, which means .gitignore is
  # complete and all code is part of a reviewable commit.
  GIT_STATUS="$(git status --porcelain)"
  if [[ $GIT_STATUS ]]; then
    echo "Your worktree is not clean, there is either uncommitted code or there are untracked files:"
    echo "${GIT_STATUS}"
    exit 1
  fi
}

create_doc () {
  echo "############################################"
  echo "#                                          #"
  echo "#        Create documentation              #"
  echo "#                                          #"
  echo "############################################"
  ./gradlew --stacktrace && ./copyDocs.sh
}

publish_doc () {
  # Take from and modified http://sleepycoders.blogspot.de/2013/03/sharing-travis-ci-generated-files.html
  # ensure publishing doesn't run on pull requests, only when token is available and only on JDK11 matrix build and on master or a travisci test branch
  if [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ -n "$GH_TOKEN" ] && [ "$TRAVIS_JDK_VERSION" == "openjdk11" ] && { [ "$TRAVIS_BRANCH" == "travisci" ] || [ "$TRAVIS_BRANCH" == "master" ]; } ; then
    echo "############################################"
    echo "#                                          #"
    echo "#        Publish documentation             #"
    echo "#                                          #"
    echo "############################################"
    echo -e "Starting to update gh-pages\n"

    #go to home and setup git
    cd "$HOME"
    git config --global user.email "travis@travis-ci.org"
    git config --global user.name "Travis"

    #using token clone gh-pages branch
    git clone --quiet --branch=gh-pages "https://${GH_TOKEN}@github.com/${TRAVIS_REPO_SLUG}.git" gh-pages > /dev/null

    #go into directory and copy data we're interested in to that directory
    cd gh-pages
    rm -rf ./*
    cp -Rf "$TRAVIS_BUILD_DIR"/docs/* .

    #add, commit and push files
    git add -f .
    git commit -m "Travis build $TRAVIS_BUILD_NUMBER pushed to gh-pages"
    git push -fq origin gh-pages > /dev/null

    echo -e "Done publishing to gh-pages.\n"
  fi
}

cleaning
dependency_info
unit_tests
integration_tests
check_for_clean_worktree
create_doc
publish_doc
