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
        <span class="navbar-logo"><img src="${content.rootpath}images/doctoolchain-logo-blue.png" width="32px" /></span><span
            class="font-weight-bold">${config.site_title}</span>
    </a>
    <div class="td-navbar-nav-scroll ml-md-auto" id="main_navbar">
        <ul class="navbar-nav mt-2 mt-lg-0">
    <li class="nav-item mr-4 mb-2 mb-lg-0"><!--img src="${content.rootpath}images/status.png" alt="status" width="16" height="16" onerror="this.style.display='none'"--></li>

<li>
</li>

<%
    import static java.util.stream.Collectors.groupingBy
    import static java.util.stream.Collectors.mapping
    import static java.util.stream.Collectors.toList

    //in the following we assume that `content.newEntries` is an ArrayList of `LinkedHashMap`s
    assert content.newEntries instanceof java.util.ArrayList
    assert (content.newEntries.size() == 0) || content.newEntries[0] instanceof java.util.LinkedHashMap

    //a custom class for drop down items
    // I could not make records work:
    //   - `record Point(int x, int y, String color) { }` gives `Unexpected input: '('`
    //   - `@RecordType class` ... gives `SimpleTemplateScript ... unable to resolve annotation RecordType`
    class Item {
        boolean isActive
        String href
        String title
        Item(isActive, href, title) {
            this.isActive = isActive
            this.href = href
            this.title = title
        }
    };

    //Transform a LinkedHashMap to an Item.
    //iff the title is a combination of menu item and dropdown item, only the dropdown item is kept.
    def transform(e) {
        var title = e.title.contains(": ")
                  ? e.title.split(": ")[1]
                  : e.title
        new Item(e.isActive, e.href, title)
    }

    MARKER_DROPDOWN = "dropdownmenuitem_"

    //get the menu item from a combined title, and the full title from a regular title.
    def getMenuItem(e) {
        e.title.contains(": ")
                  ? MARKER_DROPDOWN + e.title.split(": ")[0]
                  :                   e.title
    }

    //group by menu item. Key is the menu item, value a list of dropdown items under the menu item.
    //if there is no separator the full title is taken as prefix and the value is a list with one element
    var LinkedHashMap<String, List<Item>> itemGroups
    itemGroups = content.newEntries.stream()
                                   .collect(
                                       groupingBy(
                                           this::getMenuItem,
                                           LinkedHashMap::new,
                                           mapping(this::transform, toList())
                                       )
                                   )

    itemGroups.each { key, val ->
        if (key.startsWith(MARKER_DROPDOWN)) {
            //drop down
%>
            <li class="nav-item mr-4 mb-2 mb-lg-0">
                <div class="dropdown">
                    <button class="dropbtn"><span>${key.minus(MARKER_DROPDOWN)}</span></button>
                    <div class="dropdown-content">
<%
            val.each { item ->
%>
                        <a href="${item.href}">${item.title}</a>
<%
            }
%>
                    </div>
                </div>
            </li>
<%
        } else {
            //there is no drop down, take the only element
            entry = val.get(0)
%>
            <li class="nav-item mr-4 mb-2 mb-lg-0">
                <a class="nav-link ${entry.isActive}" href="${entry.href}"><span>${entry.title}</span></a>
            </li>
<%
        }
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
