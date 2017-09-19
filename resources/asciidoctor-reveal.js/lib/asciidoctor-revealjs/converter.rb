unless RUBY_ENGINE == 'opal'
  require 'asciidoctor/converter/html5'
  require 'asciidoctor/converter/composite'
  require 'asciidoctor/converter/template'
end

module Asciidoctor; module Revealjs

  class Converter < ::Asciidoctor::Converter::CompositeConverter
    ProvidedTemplatesDir = RUBY_ENGINE == 'opal' ? 'node_modules/asciidoctor-reveal.js/templates' : (::File.expand_path '../../../templates', __FILE__)
    register_for 'revealjs'

    def initialize backend, opts = {}
        # merge user templates with provided templates (user wins)
        template_dirs = [ProvidedTemplatesDir]
        if (user_template_dirs = opts[:template_dirs])
          template_dirs += user_template_dirs.map {|d| ::File.expand_path d }
        end
        # Engine Opal means we need to use the Javascript based templates
        if RUBY_ENGINE == 'opal'
          template_engine = 'jade'
        else
          template_engine = 'slim'
        end
        # create the main converter
        template_converter = ::Asciidoctor::Converter::TemplateConverter.new backend,
            template_dirs,
            (opts.merge htmlsyntax: 'html', template_engine: template_engine)
        # create the delegate / fallback converter
        html5_converter = ::Asciidoctor::Converter::Html5Converter.new backend, opts
        # fuse the converters together
        super backend, template_converter, html5_converter
        basebackend 'html'
        htmlsyntax 'html'
    end
  end

end; end
