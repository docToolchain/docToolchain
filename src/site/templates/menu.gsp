<%
    // execute externalized script to get the menu
    new GroovyShell(getClass().getClassLoader(),
        new Binding([
            published_content: published_content,
            content: content,
            config: config
        ])
    ).evaluate(
        new File(config.sourceFolder,"groovy/menu.groovy").text
    )
%>
<nav class="js-navbar-scroll navbar navbar-expand navbar-dark flex-column flex-md-row td-navbar">
    <a class="navbar-brand" href="${content.rootpath}index.html">
        <span class="navbar-logo"><img src="${content.rootpath}images/doctoolchain-logo-blue.png" alt="docToolchain" width="32px" /></span><span
            class="font-weight-bold">${config.site_title}</span>
    </a>
    <div class="td-navbar-nav-scroll ml-md-auto" id="main_navbar">
        <ul class="navbar-nav mt-2 mt-lg-0">
    <li class="nav-item mr-4 mb-2 mb-lg-0"><!--img src="${content.rootpath}images/status.png" alt="status" width="16" height="16" onerror="this.style.display='none'"--></li>
<%
        content.newEntries.each { entry ->
%>
            <li class="nav-item mr-4 mb-2 mb-lg-0">
                <a class="nav-link ${entry.isActive}" href="${entry.href}"><span>${entry.title}</span></a>
            </li>
<%
        }
%>
        </ul>
    </div>
    <div class="navbar-nav d-none d-lg-block" >
        <form action="${content.rootpath}search.html">
        <input aria-label="Search this site…" autocomplete="off" class="form-control td-search-input"
               placeholder=" Search this site…" type="search" name="q">
        </form>
    </div>
</nav>
