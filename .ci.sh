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
  ./gradlew && ./copyDocs.sh
}

cleaning
dependency_info
unit_tests
integration_tests
check_for_clean_worktree
create_doc
