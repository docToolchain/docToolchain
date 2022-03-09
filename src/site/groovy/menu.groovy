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
def newEntries = []
try {
    entriesMap.eachWithIndex { code, data, i ->
        def (title, entries) = data
        if (entries[0]) {
            if (title!="-" ) {
                def firstEntry = entries.sort { a, b -> a.order <=> b.order ?: a.title <=> b.title }[0]
                def url = "${content.rootpath}${firstEntry.uri}"
                def basePath = url.replaceAll('[^/]*$', '')
                def isActive = ""
                if ((content.rootpath + content.uri)?.startsWith(basePath)) {
                    isActive = "active"
                }
                //System.out.println "   $title"
                newEntries << [isActive: isActive, href: "${content.rootpath}${entries.find { it.order == 0 }?.uri ?: entries[0].uri}", title: title]
            }
        }
    }
} catch (Exception e) {
                System.out.println """

>>> menu.gsp: (2) ${e.message}

"""
}

//store results to be used in other templates
content.menu = menu
content.entriesMap = entriesMap
content.newEntries = newEntries
