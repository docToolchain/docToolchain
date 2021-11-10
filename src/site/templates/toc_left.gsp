<%
    def menu = content.menu[content['jbake-menu']]

%>
        <form class="td-sidebar__search d-flex align-items-center d-lg-none" action="${content.rootpath}search.html">

            <input aria-label="Search this site…" name="q" autocomplete="off" class="form-control td-search-input"
                   placeholder=" Search this site…" type="search">


            <button aria-controls="td-docs-nav" aria-expanded="false"
                    aria-label="Toggle section navigation" class="btn btn-link td-sidebar__toggle d-md-none p-0 ml-3 fas fa-bars" data-target="#td-section-nav"
                    data-toggle="collapse" type="button">
            </button>
        </form>

        <nav class="collapse td-sidebar-nav" id="td-section-nav" >

            <ul class="td-sidebar-nav__section pr-md-3">
                <li class="td-sidebar-nav__section-title">
                    <span class="align-left pl-0 pr-2 active td-sidebar-link td-sidebar-link__section">${config.site_menu[content['jbake-menu']]?:content['jbake-menu']}</span>
                </li>
                <ul>
                    <li class="collapse show" id="docs">
                        <ul class="td-sidebar-nav__section pr-md-3">
                            <% menu?.sort{a, b ->a.order <=> b.order ?: a.title <=> b.title }.each { entry -> %>
                            <%
                                        def isActive = ""
                                        if (content.uri==entry.uri) {
                                            isActive = "active"
                                        }
                            %>
                            <li class="td-sidebar-nav__section-title">
                                <a class="align-left pl-0 pr-2 td-sidebar-link td-sidebar-link__section $isActive"
                                   href="${content.rootpath}${entry.uri}">${entry.title?:entry}</a>
                            </li>
                            <% } %>
                            <!--li class="td-sidebar-nav__section-title">
                                <a  href="/docs/overview/" class="align-left pl-0 pr-2 td-sidebar-link td-sidebar-link__section">Overview</a>
                            </li>
                            <ul>
                                <li class="collapse show">
                                    <a class="td-sidebar-link td-sidebar-link__page " id="m-docsgetting-startedexample-page" href="/docs/getting-started/example-page/">Example Page</a>
                                </li>
                            </ul-->
                        </ul>

        </nav>
