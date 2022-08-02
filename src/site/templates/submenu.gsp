<%
    if (config.site_menu=="") {
        config.site_menu=[:]
    }
    def menu = content.menu[content['jbake-menu']]

    def printMenu(def c, int index, def entries) {
        String result = ''
        if(entries) {
            String htmlClass = (index == 0) ? 'td-sidebar-nav__section pr-md-3 ' : '';
            result = result + """
                        <ul class="${htmlClass}ul-$index">"""
            entries?.sort{a, b ->a.order <=> b.order ?: a.title <=> b.title }.each { entry ->
                def hasChild = (entry.children) ? 'with-child' : 'without-child'
                def isActive = (c.uri==entry.uri) ? 'active' : ''
                result = result + """
                            <li class="td-sidebar-nav__section-title td-sidebar-nav__section $hasChild">"""
                if (entry.uri) {
                    result = result + """
                                <a class="align-left pl-0 pr-2 td-sidebar-link td-sidebar-link__section $isActive"
                                   href="${c.rootpath}${entry.uri}">${entry.title?:entry}</a>"""
                } else {
                    result = result + """
                                ${entry.title?:entry}"""
                }
                if (entry.children) {
                    result = result + printMenu(c, index + 1, entry.children)
                }
                result = result + '''
                            </li>'''
            }
            result = result + '''
                        </ul>'''
        }
        return result
    }
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
                        <%= printMenu(content, 0, menu) %>
                    </li>
                </ul>
        </nav>
