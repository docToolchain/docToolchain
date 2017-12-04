
// Directory of which the exportChangelog task will export the changelog.
// It should be relative to the docDir directory provided in the gradle.properties file.
changelogDir = 'src/docs'

// Command used to fetch the list of changes.
// It should be a single command taking a directory as a parameter.
// You cannot use multiple commands with pipe between.
// This command will be executed in the directory specified by changelogDir it the environment inherited from the parent process.
// This command should produce asciidoc text directly. The exportChangelog task does not do any post-processing
// of the output of that command.
//
// See also https://git-scm.com/docs/pretty-formats
changelogCmd = 'git log --pretty=format:%x7c%x20%ad%x20%n%x7c%x20%an%x20%n%x7c%x20%s%x20%n --date=short'
