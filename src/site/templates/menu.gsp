<%
    def menu = [:]
    def entriesMap = [:]
    try {
        published_content.each { page ->

            if (page['jbake-menu']) {
                //initialize entry if it doesn't exist yet
                if (menu[page['jbake-menu']] == null) {
                    menu[page['jbake-menu']] = []
                }
                //push all page info to the menu map
                menu[page['jbake-menu']] << [
                           title: page['jbake-title'],
                           order: page['jbake-order'],
                        filename: page['filename'],
                             uri: page['uri']
                ]
            }
        }
        // first, use all menu codes which are defined in the config
        config.site_menu.eachWithIndex { code, title, i ->
            def entries = menu[code] ?: []
            entriesMap[code] = [title, entries]
        }
        // now, add all remaining codes
        menu.each { code, entries ->
            if (config.site_menu[code]) {
                // already covered
            } else {
                entriesMap[code] = [code, entries]
            }
        }
    } catch (Exception e) {
        System.out.println """

>>> menu.gsp: (1) ${e.message}

"""
    }
    //store results to be used in other templates
    content.menu = menu
    content.entriesMap = entriesMap
%>

<nav class="js-navbar-scroll navbar navbar-expand navbar-dark flex-column flex-md-row td-navbar">
    <a class="navbar-brand" href="${content.rootpath}index.html">
        <span class="navbar-logo"></span><span
            class="font-weight-bold">${config.site_title}</span>
    </a>
    <div class="td-navbar-nav-scroll ml-md-auto" id="main_navbar">
        <ul class="navbar-nav mt-2 mt-lg-0">
    <li class="nav-item mr-4 mb-2 mb-lg-0"><!--img src="${content.rootpath}images/status.png" alt="status" width="16" height="16" onerror="this.style.display='none'"--></li>
            <!--
            ${menu}
            -->
<%
    try {
        entriesMap.eachWithIndex { code, data, i ->
            def (title, entries) = data
            if (entries[0]) {
                if (title!="-" ) {
                    def firstEntry = entries.sort{a, b ->a.order <=> b.order ?: a.title <=> b.title }[0]
                    def url = "${content.rootpath}${firstEntry.uri}"
                    def basePath = url.replaceAll('[^/]*$','')
                    def isActive = ""
                    if ((content.rootpath+content.uri)?.startsWith(basePath)) {
                        isActive = "active"
                    }
%>
            <li class="nav-item mr-4 mb-2 mb-lg-0">
                <a class="nav-link $isActive" href="${content.rootpath}${entries.find{it.order==0}?.uri?:entries[0].uri}"><span>$title</span></a>
            </li>
            <!--
            ${content.menu}

            ${content.entriesMap}
            -->
<%
                }
            } else {
                // System.out.println "> found menu entrie in config for which no page is defined: $code"
            }
        }
    } catch (Exception e) {
                System.out.println """

>>> menu.gsp: (2) ${e.message}

"""
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
