:jbake-title: exportMarkdown
:filename: 015_tasks/03_task_exportMarkdown.adoc
include::_config.adoc[]

include::../_feedback.adoc[]

// the source is in exportMarkdownDocs.md !!
include::{projectRootDir}/build/exportMarkdownDocs.adoc[]

== Source

.exportMarkdown.gradle
[source,groovy]
----
include::{projectRootDir}/scripts/exportMarkdown.gradle[tags=exportMarkdown]
----
