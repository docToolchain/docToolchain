
// changelogDir is a directory of which the exportChangelog task will export the changelog using git
changelogDir = 'src/docs'
changelogCmd = 'git log --pretty=format:%x7c%x20%ad%x20%n%x7c%x20%an%x20%n%x7c%x20%s%x20%n --date=short'