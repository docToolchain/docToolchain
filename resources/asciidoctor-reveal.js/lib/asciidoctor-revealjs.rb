if RUBY_ENGINE == 'opal'
  require 'asciidoctor-revealjs/converter'
  `require('asciidoctor-template.js')`
else
  require 'asciidoctor' unless defined? Asciidoctor::Converter
  require_relative 'asciidoctor-revealjs/converter'
end
