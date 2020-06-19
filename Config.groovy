outputPath = 'build/docs'

// Path where the docToolchain will search for the input files.
// This path is appended to the docDir property specified in gradle.properties
// or in the command line, and therefore must be relative to it.

inputPath = 'src/docs';


inputFiles = [
        [file: 'manual.adoc',       formats: ['html','pdf']],
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

    // username:password of an account which has the right permissions to create and edit
    // confluence pages in the given space.
    // if you want to store it securely, fetch it from some external storage or leave it empty to fallback to gradle variables
    // set through gradle properties files or environment variables. The fallback uses the 'confluenceUser' and 'confluencePassword' keys.
    // you might even want to prompt the user for the password like in this example

    credentials = "user:pass_or_token".bytes.encodeBase64().toString()

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
}
//end::confluenceConfig[]
//*****************************************************************************************
//tag::exportEAConfig[]
//Configuration for the export script 'exportEA.vbs'.
// The following parameters can be used to change the default behaviour of 'exportEA'.
// All parameter are optionally.
// Parameter 'connection' allows to select a certain database connection by using the ConnectionString as used for
// directly connecting to the project database instead of looking for EAP/EAPX files inside and below the 'src' folder.
// Parameter 'packageFilter' is an array of package GUID's to be used for export. All images inside and in all packages below the package represented by its GUID are exported.
// A packageGUID, that is not found in the currently opened project, is silently skipped.
// PackageGUID of multiple project files can be mixed in case multiple projects have to be opened.

exportEA.with {
// OPTIONAL: Set the connection to a certain project or comment it out to use all project files inside the src folder or its child folder.
// connection = "DBType=1;Connect=Provider=SQLOLEDB.1;Integrated Security=SSPI;Persist Security Info=False;Initial Catalog=[THE_DB_NAME_OF_THE_PROJECT];Data Source=[server_hosting_database.com];LazyLoad=1;"
// OPTIONAL: Add one or multiple packageGUIDs to be used for export. All packages are analysed, if no packageFilter is set.
// packageFilter = [
//                    "{A237ECDE-5419-4d47-AECC-B836999E7AE0}",
//                    "{B73FA2FB-267D-4bcd-3D37-5014AD8806D6}"
//                  ]
// OPTIONAL: relative path to base 'docDir' to which the diagrams and notes are to be exported
// exportPath = "src/docs/"
// OPTIONAL: relative path to base 'docDir', in which Enterprise Architect project files are searched
// searchPath = "src/docs/"
 
}
//end::exportEAConfig[]
