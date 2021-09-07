#!/bin/bash
DIR_TO_WATCH='src/'
#COMMAND='rm -r build || true && mkdir -p build/microsite/output/images/ && ./dtcw generateSite 2>&1 | tee build/generateSite.log'
COMMAND='mkdir -p build/microsite/output/images/ && ./dtcw generateSite 2>&1 | tee build/generateSite.log'

#execute first time
cp src/docs/images/ready.png build/microsite/output/images/status.png
#eval $COMMAND

#wait for changes and execute
while [[ 1=1 ]] ; do
  watch --no-title --chgexit "ls -lR ${DIR_TO_WATCH} | sha1sum"
  cp src/docs/images/building.png build/microsite/output/images/status.png
  eval $COMMAND
  cp src/docs/images/ready.png build/microsite/output/images/status.png
  sleep 6
done
