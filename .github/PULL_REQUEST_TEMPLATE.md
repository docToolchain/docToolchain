### All Submissions:

* [ ] Does your PR affect the documentation?
* [ ] If yes, did you update the documentation or create an issue for updating it?

The source of the documentation can be found in `/src/docs/manual`.
To "publish" it, execute `./gradlew exportContributors && ./gradlew && ./copyDocs.sh`.
This will convert the `.adoc` file to HTML and copy them to the right folder so that github pages will pick them up.

If you didn't find the time to update docs, please create an issue as reminder to do so.

<!-- You can erase any parts of this template not applicable to your Pull Request. -->

### Your first submission

* [ ] Welcome to the list of contributors! If you have any questions, feel free to ask them by creating a new issue
* [ ] Have you added your name to the list of [contributors.adoc](https://github.com/docToolchain/docToolchain/blob/master/src/docs/manual/05_contributors.adoc)?

### New Feature Submissions:

1. [ ] Did you create new tests for your submission?
2. [ ] Does your submission pass all tests? (see travis check)

### Changes to Core Features:

* [ ] Have you written new tests for your core changes, as applicable?
* [ ] Have you successfully ran tests with your changes locally?


inspiration: https://github.com/stevemao/github-issue-templates
