<%
    if (config.site_menu=="") {
        config.site_menu=[:]
    }
    def menu = content.menu[content['jbake-menu']]

    def printTitle(entry) {
        def title

        if (entry instanceof String) {
            title  = entry
            }
        else {
            title = entry.title
        }
        if (config.site_menuType?.toLowerCase()?.startsWith("config")) {
            // do nothing
        } else {
            if (config.site_menu[title]) {
                title = config.site_menu[title]
            }
        }
        return title
    }

    def printMenu(def c, int index, def entries) {
        String result = ''
        if(entries) {
            String htmlClass = (index == 0) ? 'td-sidebar-nav__section ' : '';
            result = result + """
                        <ul class="${htmlClass}ul-$index">"""
            entries?.sort{a, b ->a.order <=> b.order ?: a.title <=> b.title }.eachWithIndex { entry, index2 ->

                def hasChild = (entry.children) ? 'with-child' : 'without-child'
                def isActive = (c.uri==entry.uri) ? 'active' : ''
                if (entry.uri) {
                    // file belongs to a document, so it can't be a folder
                    def is00file = entry.uri.split("/")[-1] ==~ /^0+[-_].*/
                    def render = true
                    if (is00file && config.site_render00=='false') {
                        render = false
                    }
                    if (render) {
                        result = result + """
                            <li class="td-sidebar-nav__section-title td-sidebar-nav__section $hasChild">
                                <a class="align-left pl-0 pr-2 pt-2 td-sidebar-link td-sidebar-link__section $isActive"
                                   href="${c.rootpath}${entry.uri}" title="${printTitle(entry)}">${printTitle(entry)}</a>
                                """
                    }
                } else {
                    def has00file = entry.children.find{ page ->
                        if (page?.uri==null) return false
                        if (page.uri.contains("/")==false) return false
                        if (page.uri.split("/")[-1] ==~ /^0+[-_].*/) return true
                    }
                    if (entry.children) {
                        result += """\n<details id="d${printTitle(entry).md5()}">"""
                        if (has00file) {
                        result = result + """<summary><span class="label"><a style="margin-left: 0px;" class="$isActive" href="${c.rootpath}${has00file.uri}" title="${printTitle(has00file)}">${printTitle(has00file)}</a></span></summary>"""
                        } else {
                        result = result + """<summary><span class="label">${printTitle(entry)}</span></summary>"""
                        }
                    } else {
                        result += """<li class="td-sidebar-nav__section-title td-sidebar-nav__section $hasChild">"""
                        if (has00file) {
                        result = result + """<span class="label"><a style="margin-left: 0px;" class="$isActive" href="${c.rootpath}${has00file.uri}" title="${printTitle(has00file)}">${printTitle(has00file)}</a></span>"""
                        } else {
                        result = result + """<span class="label">${printTitle(entry)}</span>"""
                        }
                    }
                }
                if (entry.children) {
                    result = result + printMenu(c, index + 1, entry.children)
                    result += """</details>"""
                } else {
                    result = result + '''
                            </li>'''
                }
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

        <nav aria-label="Submenu" class="collapse td-sidebar-nav" id="td-section-nav" >

            <ul class="td-sidebar-nav__section">
                <li class="td-sidebar-nav__section-title">
                    <span class="align-left pl-0 pr-2 pt-2 active td-sidebar-link td-sidebar-link__section"><%= printTitle(content['jbake-menu']) %></span>
                </li>
                <li>
                  <ul>
                      <li class="collapse show" id="docs">
                          <%= printMenu(content, 0, menu) %>
                      </li>
                  </ul>
                <li>
        </nav>
