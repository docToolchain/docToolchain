# This helper file borrows from the Bespoke converter
# https://github.com/asciidoctor/asciidoctor-bespoke
require 'asciidoctor'
require 'json'

if Gem::Version.new(Asciidoctor::VERSION) <= Gem::Version.new('1.5.3')
  fail 'asciidoctor: FAILED: reveal.js backend needs Asciidoctor >=1.5.4!'
end

unless defined? Slim::Include
  fail 'asciidoctor: FAILED: reveal.js backend needs Slim >= 2.1.0!'
end

# This module gets mixed in to every node (the context of the template) at the
# time the node is being converted. The properties and methods in this module
# effectively become direct members of the template.
module Slim::Helpers

  EOL = %(\n)
  SliceHintRx = /  +/

  def slice_text str, active = nil
    if (active || (active.nil? && (option? :slice))) && (str.include? '  ')
      (str.split SliceHintRx).map {|line| %(<span class="line">#{line}</span>) }.join EOL
    else
      str
    end
  end

  def to_boolean val
    val && val != 'false' && val.to_s != '0' || false
  end

end

# More custom functions can be added in another namespace if required
#module Helpers
#end
