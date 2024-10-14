<%
    //let's build some urls.
    //what's the correct source file name with path?
    def sourceFileName = content?.uri?.replaceAll("[.]html", (content.file =~ /[.][^.]+$/)[0])
    def subject = java.net.URLEncoder.encode("Docs: Feedback for '${content?.title}'", "UTF-8")
%>
<% if (config.site_gitRepoUrl) { %>
        <a href="${config.site_gitRepoUrl}/${sourceFileName}"
           target="_blank"><i class="fa fa-edit fa-fw"></i> Improve this doc</a>
<% } %>
<% if (config.site_issueUrl) {%>
        <a href="${config.site_issueUrl}?title=${subject}&body=%0A%0A%5BEnter%20feedback%20here%5D%0A%0A%0A---%0A%23page:%20${config.site_gitRepoUrl}/${sourceFileName}%0A%23branch:%20${config.site_branch}" target="_blank"><i
                class="fab fa-github fa-fw"></i> Create an issue</a>
<% } %>
        ${content?.rightcolumnhtml?.replaceAll("&lt;","<")?.replaceAll("&gt;",">")?:''}
<% if (config.site_issuesBaseUrl) {%>        
        <hr />
<script>
const issuesBaseUrl = "${config.site_issuesBaseUrl}";
const searchTerm = "${sourceFileName}";
</script>

<script src="${content.rootpath}js/issues.js"></script>

<style>
        #issues-container {
            background-color: white;
            padding: 0px;
            margin: 0;
        }
        .issue {
            display: flex;
            align-items: flex-start;
            margin-bottom: 5px;
            padding-bottom: 5px;
            border-bottom: 1px solid #eee;
        }
        .issue:last-child {
            border-bottom: none;
        }
        .user-avatar {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            margin-right: -10px;
        }
        .issue-content {
            flex-grow: 1;
            background-color: rgba(255,255,255,0.6);
        }
        .issue-title {
            font-weight: bold;
            color: #0366d6;
        }
        .issue-date {
            font-size: 0.9em;
            color: #586069;
        }

</style>

<div id="issues-container">
searching issues...
</div>
<% } %>
        <hr />
<% if (content?.body.contains('<!-- endtoc -->')) { %>
        ${content?.body?.split("(?ms)<!-- endtoc -->",2)[0]}
<% } %>
<% if (config.site_rightColumnExtra) {
    out << new File(config.site_rightColumnExtra).text
} %>
