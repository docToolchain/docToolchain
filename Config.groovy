outputPath = 'build/docs'

// Path where the docToolchain will search for the input files.
// This path is appended to the docDir property specified in gradle.properties
// or in the command line, and therefore must be relative to it.

inputPath = 'src/docs';


inputFiles = [
        [file: 'manual.adoc',       formats: ['html','pdf']],
        /** inputFiles **/
]

//folders in which asciidoc will find images.
//these will be copied as resources to ./images
//folders are relative to inputPath
imageDirs = [
        /** imageDirs **/
]

taskInputsDirs = ["${inputPath}/images"]

taskInputsFiles = []

//*****************************************************************************************

//Configuration for exportChangelog

exportChangelog = [:]

changelog.with {

    // Directory of which the exportChangelog task will export the changelog.
    // It should be relative to the docDir directory provided in the
    // gradle.properties file.
    dir = 'src/docs'

    // Command used to fetch the list of changes.
    // It should be a single command taking a directory as a parameter.
    // You cannot use multiple commands with pipe between.
    // This command will be executed in the directory specified by changelogDir
    // it the environment inherited from the parent process.
    // This command should produce asciidoc text directly. The exportChangelog
    // task does not do any post-processing
    // of the output of that command.
    //
    // See also https://git-scm.com/docs/pretty-formats
    cmd = 'git log --pretty=format:%x7c%x20%ad%x20%n%x7c%x20%an%x20%n%x7c%x20%s%x20%n --date=short'

}

//*****************************************************************************************

//tag::confluenceConfig[]
//Configureation for publishToConfluence

confluence = [:]

// 'input' is an array of files to upload to Confluence with the ability
//          to configure a different parent page for each file.
//
// Attributes
// - 'file': absolute or relative path to the asciidoc generated html file to be exported
// - 'url': absolute URL to an asciidoc generated html file to be exported
// - 'ancestorName' (optional): the name of the parent page in Confluence as string;
//                             this attribute has priority over ancestorId, but if page with given name doesn't exist,
//                             ancestorId will be used as a fallback
// - 'ancestorId' (optional): the id of the parent page in Confluence as string; leave this empty
//                            if a new parent shall be created in the space
// - 'preambleTitle' (optional): the title of the page containing the preamble (everything
//                            before the first second level heading). Default is 'arc42'
//
// The following four keys can also be used in the global section below
// - 'spaceKey' (optional): page specific variable for the key of the confluence space to write to
// - 'createSubpages' (optional): page specific variable to determine whether ".sect2" sections shall be split from the current page into subpages
// - 'pagePrefix' (optional): page specific variable, the pagePrefix will be a prefix for the page title and it's sub-pages
//                            use this if you only have access to one confluence space but need to store several
//                            pages with the same title - a different pagePrefix will make them unique
// - 'pageSuffix' (optional): same usage as prefix but appended to the title and it's subpages
// only 'file' or 'url' is allowed. If both are given, 'url' is ignored
confluence.with {
    input = [
            [ file: "build/docs/html5/arc42-template-de.html" ],
    ]

    // endpoint of the confluenceAPI (REST) to be used
    // verfiy that you got the correct endpoint by browsing to 
    // https://[yourServer]/[context]/rest/api/user/current
    // you should get a valid json which describes your current user
    // a working example is https://arc42-template.atlassian.net/wiki/rest/api/user/current
    api = 'https://[yourServer]/[context]/rest/api/'

//    Additionally, spaceKey, createSubpages, pagePrefix and pageSuffix can be globally defined here. The assignment in the input array has precedence

    // the key of the confluence space to write to
    spaceKey = 'asciidoc'

    // the title of the page containing the preamble (everything the first second level heading). Default is 'arc42'
    preambleTitle = ''

    // variable to determine whether ".sect2" sections shall be split from the current page into subpages
    createSubpages = false

    // the pagePrefix will be a prefix for each page title
    // use this if you only have access to one confluence space but need to store several
    // pages with the same title - a different pagePrefix will make them unique
    pagePrefix = ''

    pageSuffix = ''

    /*
    WARNING: It is strongly recommended to store credentials securely instead of commiting plain text values to your git repository!!!

    Tool expects credentials that belong to an account which has the right permissions to to create and edit confluence pages in the given space.
    Credentials can be used in a form of:
     - passed parameters when calling script (-PconfluenceUser=myUsername -PconfluencePass=myPassword) which can be fetched as a secrets on CI/CD or
     - gradle variables set through gradle properties (uses the 'confluenceUser' and 'confluencePass' keys)
    Often, same credentials are used for Jira & Confluence, in which case it is recommended to pass CLI parameters for both entities as
    -Pusername=myUser -Ppassword=myPassword
    */

    //optional API-token to be added in case the credentials are needed for user and password exchange.
    //apikey = "[API-token]"

    // HTML Content that will be included with every page published
    // directly after the TOC. If left empty no additional content will be
    // added
    // extraPageContent = '<ac:structured-macro ac:name="warning"><ac:parameter ac:name="title" /><ac:rich-text-body>This is a generated page, do not edit!</ac:rich-text-body></ac:structured-macro>
    extraPageContent = ''

    // enable or disable attachment uploads for local file references
    enableAttachments = false

    // default attachmentPrefix = attachment - All files to attach will require to be linked inside the document.
    // attachmentPrefix = "attachment"


    // Optional proxy configuration, only used to access Confluence
    // schema supports http and https
    // proxy = [host: 'my.proxy.com', port: 1234, schema: 'http']

    // Optional: specify which Confluence OpenAPI Macro should be used to render OpenAPI definitions
    // possible values: ["confluence-open-api", "open-api", "swagger-open-api", true]. true is the same as "confluence-open-api" for backward compatibility
    // useOpenapiMacro = "confluence-open-api"
}
//end::confluenceConfig[]
//*****************************************************************************************
//tag::exportEAConfig[]
//Configuration for the export script 'exportEA.vbs'.
// The following parameters can be used to change the default behaviour of 'exportEA'.
// All parameter are optionally.
// -  connection: Parameter allows to select a certain database connection by
//    using the ConnectionString as used for directly connecting to the project
//    database instead of looking for EAP/EAPX files inside and below the 'src' folder.
// - 'packageFilter' is an array of package GUID's to be used for export. All
//    images inside and in all packages below the package represented by its GUID
//    are exported. A packageGUID, that is not found in the currently opened
//    project, is silently skipped. PackageGUID of multiple project files can
//    be mixed in case multiple projects have to be opened.
// -  exportPath: relative path to base 'docDir' to which the diagrams and notes are to be exported
// -  searchPath: relative path to base 'docDir', in which Enterprise Architect project files are searched
// -  absoluteSearchPath: absolute path in which Enterprise Architect project files are searched
// -  glossaryAsciiDocFormat: if set, the EA glossary is exported into exportPath as 'glossary.ad'
// -  glossaryTypes: if set and glossary is exported, used to filter for certain types.
//    Not set or empty list will cause no filtered glossary.
// -  diagramAttributes: if set, the diagram attributes are exported and formatted as specified

exportEA.with {
// OPTIONAL: Set the connection to a certain project or comment it out to use all project files inside the src folder or its child folder.
// connection = "DBType=1;Connect=Provider=SQLOLEDB.1;Integrated Security=SSPI;Persist Security Info=False;Initial Catalog=[THE_DB_NAME_OF_THE_PROJECT];Data Source=[server_hosting_database.com];LazyLoad=1;"
// OPTIONAL: Add one or multiple packageGUIDs to be used for export. All packages are analysed, if no packageFilter is set.
// packageFilter = [
//                    "{A237ECDE-5419-4d47-AECC-B836999E7AE0}",
//                    "{B73FA2FB-267D-4bcd-3D37-5014AD8806D6}"
//                  ]
// OPTIONAL: export diagrams, notes, etc. below folder src/docs
// exportPath = "src/docs/"
// OPTIONAL: EA project files are expected to be located in folder src/projects
// searchPath = "src/projects/"
// OPTIONAL: terms will be exported as asciidoc 'Description, single-line'
// glossaryAsciiDocFormat = "TERM:: MEANING"
// OPTIONAL: only terms of type Business and Technical will be exported.
// glossaryTypes = ["Business", "Technical"]
// OPTIONAL: Additional files will be exported containing diagram attributes in the given asciidoc format
// diagramAttributes = "Modified: %DIAGRAM_AUTHOR%, %DIAGRAM_MODIFIED%, %DIAGRAM_NAME%,
// %DIAGRAM_GUID%, %DIAGRAM_CREATED%, %DIAGRAM_NOTES%, %DIAGRAM_DIAGRAM_TYPE%, %DIAGRAM_VERSION%"
}
//end::exportEAConfig[]

//tag::htmlSanityCheckConfig[]
htmlSanityCheck.with {
    //sourceDir = "build/html5/site"

    // where to put results of sanityChecks...
    //checkingResultsDir =

    // OPTIONAL: directory where the results written to in JUnit XML format
    //junitResultsDir =

    // OPTIONAL: which statuscodes shall be interpreted as warning, error or success defaults to standard
    //httpSuccessCodes = []
    //httpWarningCodes = []
    //httpErrorCodes = []

    // fail build on errors?
    failOnErrors = false
}
//end::htmlSanityCheckConfig[]

//tag::jiraConfig[]
// Configuration for Jira related tasks
jira = [:]

jira.with {

    // endpoint of the JiraAPI (REST) to be used
    api = 'https://your-jira-instance'

    /*
    WARNING: It is strongly recommended to store credentials securely instead of commiting plain text values to your git repository!!!

    Tool expects credentials that belong to an account which has the right permissions to read the JIRA issues for a given project.
    Credentials can be used in a form of:
     - passed parameters when calling script (-PjiraUser=myUsername -PjiraPass=myPassword) which can be fetched as a secrets on CI/CD or
     - gradle variables set through gradle properties (uses the 'jiraUser' and 'jiraPass' keys)
    Often, Jira & Confluence credentials are the same, in which case it is recommended to pass CLI parameters for both entities as
    -Pusername=myUser -Ppassword=myPassword
    */

    // the key of the Jira project
    project = 'PROJECTKEY'

    // the format of the received date time values to parse
    dateTimeFormatParse = "yyyy-MM-dd'T'H:m:s.SSSz" // i.e. 2020-07-24'T'9:12:40.999 CEST

    // the format in which the date time should be saved to output
    dateTimeFormatOutput = "dd.MM.yyyy HH:mm:ss z" // i.e. 24.07.2020 09:02:40 CEST

    // the label to restrict search to
    label = 'label1'

    // Legacy settings for Jira query. This setting is deprecated & support for it will soon be completely removed. Please use JiraRequests settings
    jql = "project='%jiraProject%' AND labels='%jiraLabel%' ORDER BY priority DESC, duedate ASC"

    // Base filename in which Jira query results should be stored
    resultsFilename = 'JiraTicketsContent'

    saveAsciidoc = true // if true, asciidoc file will be created with *.adoc extension
    saveExcel = true // if true, Excel file will be created with *.xlsx extension

    // Output folder for this task inside main outputPath
    resultsFolder = 'JiraRequests'

    /*
    List of requests to Jira API:
    These are basically JQL expressions bundled with a filename in which results will be saved.
    User can configure custom fields IDs and name those for column header,
    i.e. customfield_10026:'Story Points' for Jira instance that has custom field with that name and will be saved in a coloumn named "Story Points"
    */
    requests = [
        new JiraRequest(
            filename:"File1_Done_issues",
            jql:"project='%jiraProject%' AND status='Done' ORDER BY duedate ASC",
            customfields: [customfield_10026:'Story Points']
        ),
        new JiraRequest(
            filename:'CurrentSprint',
            jql:"project='%jiraProject%' AND Sprint in openSprints() ORDER BY priority DESC, duedate ASC",
            customfields: [customfield_10026:'Story Points']
        ),
    ]
}

@groovy.transform.Immutable
class JiraRequest {
    String filename  //filename (without extension) of the file in which JQL results will be saved. Extension will be determined automatically for Asciidoc or Excel file
    String jql // Jira Query Language syntax
    Map<String,String> customfields // map of customFieldId:displayName values for Jira fields which don't have default names, i.e. customfield_10026:StoryPoints
}
//end::jiraConfig[]

//tag::openApiConfig[]
// Configuration for OpenAPI related task
openApi = [:]

// 'specFile' is the name of OpenAPI specification yaml file. Tool expects this file inside working dir (as a filename or relative path with filename)
// 'infoUrl' and 'infoEmail' are specification metadata about further info related to the API. By default this values would be filled by openapi-generator plugin placeholders
//

openApi.with {
    specFile = 'src/docs/petstore-v2.0.yaml' // i.e. 'petstore.yaml', 'src/doc/petstore.yaml'
    infoUrl = 'https://my-api.company.com'
    infoEmail = 'info@company.com'
}
//end::openApiConfig[]

//tag::sprintChangelogConfig[]
// Sprint changelog configuration generate changelog lists based on tickets in sprints of an Jira instance.
// This feature requires at least Jira API & credentials to be properly set in Jira section of this configuration
sprintChangelog = [:]
sprintChangelog.with {
    sprintState = 'closed' // it is possible to define multiple states, i.e. 'closed, active, future'
    ticketStatus = "Done, Closed" // it is possible to define multiple ticket statuses, i.e. "Done, Closed, 'in Progress'"

    showAssignee = false
    showTicketStatus = false
    showTicketType = true
    sprintBoardId = 12345  // Jira instance probably have multiple boards; here it can be defined which board should be used

    // Output folder for this task inside main outputPath
    resultsFolder = 'Sprints'

    // if sprintName is not defined or sprint with that name isn't found, release notes will be created on for all sprints that match sprint state configuration
    sprintName = 'PRJ Sprint 1' // if sprint with a given sprintName is found, release notes will be created just for that sprint
    allSprintsFilename = 'Sprints_Changelogs' // Extension will be automatically added.
}
//end::sprintChangelogConfig[]
