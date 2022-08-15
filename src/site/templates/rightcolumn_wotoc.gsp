<%
    //let's build some urls.
    //what's the correct source file name with path?
    def sourceFileName = content?.uri?.replaceAll("[.]html", (content.file =~ /[.][^.]+$/)[0])
    def subject = java.net.URLEncoder.encode("Docs: Feedback for '${content?.title}'", "UTF-8")
%>
        <a href="${config.site_gitRepoUrl}/${sourceFileName}"
           target="_blank"><i class="fa fa-edit fa-fw"></i> Improve this doc</a>
        <!--a href="https://github.com/google/docsy-example/new/master/content/en/docs/_index.md?filename=change-me.md&amp;value=---%0Atitle%3A+%22Long+Page+Title%22%0AlinkTitle%3A+%22Short+Nav+Title%22%0Aweight%3A+100%0Adescription%3A+%3E-%0A+++++Page+description+for+heading+and+indexes.%0A---%0A%0A%23%23+Heading%0A%0AEdit+this+template+to+create+your+new+page.%0A%0A%2A+Give+it+a+good+name%2C+ending+in+%60.md%60+-+e.g.+%60getting-started.md%60%0A%2A+Edit+the+%22front+matter%22+section+at+the+top+of+the+page+%28weight+controls+how+its+ordered+amongst+other+pages+in+the+same+directory%3B+lowest+number+first%29.%0A%2A+Add+a+good+commit+message+at+the+bottom+of+the+page+%28%3C80+characters%3B+use+the+extended+description+field+for+more+detail%29.%0A%2A+Create+a+new+branch+so+you+can+preview+your+new+file+and+request+a+review+via+Pull+Request.%0A"
           target="_blank"><i class="fa fa-edit fa-fw"></i> Create child page</a-->
        <a href="${config.site_issueUrl}?title=${subject}&body=%0A%0A%5BEnter%20feedback%20here%5D%0A%0A%0A---%0A%23page:${sourceFileName}" target="_blank"><i
                class="fab fa-github fa-fw"></i> Create an issue</a>
        <!--a href="https://github.com/google/docsy/issues/new" target="_blank"><i
                class="fas fa-tasks fa-fw"></i> Create project issue</a-->
        <!--a href="https://example.docsy.dev/docs/_print/" id="print"><i class="fa fa-print fa-fw"></i> Print
        entire section</a-->
        ${content?.rightcolumnhtml?.replaceAll("&lt;","<")?.replaceAll("&gt;",">")?:''}
