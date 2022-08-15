#!/usr/bin/env python3

from docutils.core import publish_parts
import sys

source = sys.argv[1]
print ("python")
print (source)
with open(source, 'r', encoding='utf-8') as input, open(source+'.html', 'w', encoding='utf-8') as output:
    content = input.read()
    customStyle = "<style>div.system-messages { display: none; }</style>\n"
    header = ""
    if content.find('~~~~~~')>-1:
        header  = content.split('~~~~~~')[0] + "\n~~~~~~\n"
        content = content.split('~~~~~~')[1]
    html = publish_parts(content, writer_name='html')['html_body']
    output.write(header + customStyle + html)
