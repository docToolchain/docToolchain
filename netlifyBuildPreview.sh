#!/usr/bin/env bash

# create a locally installed version of the current repo
mkdir -p "$HOME/.doctoolchain/docToolchain-dev"
cp -r ./* "$HOME/.doctoolchain/docToolchain-dev"
# set 'dev' as version in dtcw
sed -i 's/VERSION=[-0-9.a-z]*/VERSION=dev/' dtcw
# ensure that dtcw is installed
./dtcw tasks
# export information about contributors for those little avatars on top of each file
./dtcw exportContributors
# for the excel demo to work, we need to export the excel file
./dtcw exportExcel
# the docs for exportMarkdown are written in markdown, so let's export them
./dtcw exportMarkdown
# and now we are ready to generate the site
./dtcw generateSite
