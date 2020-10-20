#!/bin/bash
mkdir docs
cp -r build/docs/html5/. docs/.
#rm docs/index.html
mv docs/manual.html docs/index.html
mkdir docs/htmlchecks
cp -r build/docs/report/htmlchecks/. docs/htmlchecks/.
