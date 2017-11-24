// This config is for the exportEA task
// it is only needed if you want to sync jira issues to EA

// endpoint of the jiraAPI (REST) to be used
jiraAPI = 'https://[yourServer]/[context]/rest/api/2/'
// username:password of an account which has the right permissions to create and edit
// confluence pages in the given space.
// if you want to store it securely, fetch it from some external storage.
// you might even want to prompt the user for the password like in this example
jiraCredentials = "user:${System.console().readPassword('confluence password: ')}".bytes.encodeBase64().toString()
