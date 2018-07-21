# exportMarkdown

The *exportMarkdown* task can be used to include [markdown](http://markdown.de) files into the documentation.

The task is scanning the directory `/src/docs` for markdown files (`*.md`) and converts them into AsciiDoc files. The converted files can then be referenced from within the `/build`-folder.