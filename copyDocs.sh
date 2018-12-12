cp -r build/docs/html5/. docs/.
rm docs/index.html
mv docs/manual.html docs/index.html
cp -r build/docs/report/htmlchecks/. docs/htmlchecks/.
