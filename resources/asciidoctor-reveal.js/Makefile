# Makefile for asciidoc based presentation

TESTS=$(wildcard test/*.adoc)
MOST_RECENT=$(shell ls -t test/*.html | head -1)

default: $(patsubst %.adoc,%.html,$(TESTS))

%.html: %.adoc
	bundle exec ./bin/asciidoctor-revealjs $<

open:
	"$(shell which xdg-open || which open || which x-www-browser)" $(MOST_RECENT)

clean:
	rm -f test/*.html

serve:
	"$(shell which xdg-open || which open || which x-www-browser)" \
		http://localhost:8000/$(MOST_RECENT)
	python2 -m SimpleHTTPServer 8000
