#!/bin/bash
[ -d docs ] || mkdir docs
cp -r build/docs/html5/. docs/.
#rm docs/index.html
mv docs/manual.html docs/index.html
[ -d  docs/htmlchecks ] || mkdir docs/htmlchecks
cp -r build/docs/report/htmlchecks/. docs/htmlchecks/.
