<nav class="navbar navbar-expand-md navbar-dark fixed-top bg-dark">
    <div class="container-fluid">
        <a class="navbar-brand" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>">arc42</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarCollapse" aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarCollapse">
            <ul class="navbar-nav me-auto mb-2 mb-md-0">
                <li><a class="nav-link" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>index.html">Home</a></li>

                <%
                    def menu = [:]
                    published_content.each {page ->
                        if (page['jbake-menu']) {
                            if (menu[page['jbake-menu']] == null) {
                                menu[page['jbake-menu']] = []

                            }
                            menu[page['jbake-menu']] << [
                                title: page['jbake-title'],
                                order: page['jbake-order'],
                                filename: page['filename'],
                                uri: page['uri']
                            ]
                        }
                    }

                    menu.eachWithIndex { title, entries, i ->
                        out << """<li class="nav-item dropdown">"""
                        out << """<a class="nav-link dropdown-toggle" href="#" id="dropdown$i" data-bs-toggle="dropdown" aria-expanded="false">$title</a>"""
                        out << """<ul class="dropdown-menu" aria-labelledby="dropdown$i">"""
                        entries.sort{it.order as Integer}.each {entry ->
                %>
                <li><a class="dropdown-item" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>${entry.uri}">${entry.title}</a></li>
                <%
                            }
                            out << """</ul></li>"""
                    }
                %>

                <li><a class="nav-link" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>blog/index.html">Blog</a></li>
                <li><a class="nav-link" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>about.html">About</a></li>
                <li><a class="nav-link" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>${config.feed_file}">RSS-Feed</a></li>
            </ul>
            <form class="d-flex">
                <input class="form-control me-2" type="search" placeholder="Search" aria-label="Search">
                <button class="btn btn-outline-success" type="submit">Search</button>
            </form>
        </div>
    </div>
</nav>
