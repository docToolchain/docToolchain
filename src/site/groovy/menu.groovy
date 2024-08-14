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
                order: page['jbake-order'] as Integer,
                filename: page['filename'],
                uri: page['uri'],
                children: []
            ]
        }
    }
    menu = menu.collectEntries {k, v -> 
        [k, makeHierachical(v) ]
    }
    // first, use all menu codes which are defined in the config
    if (config.site_menu==null) {
        config.site_menu = [:]
        System.out.println("""
warning: No menu defined in your config
example: menu = [code1: 'title1', code2: 'title2']
see http://doctoolchain.org/docToolchain/v2.0.x/015_tasks/03_task_generateSite.html#config
for more details
""")
    }
    if (config.site_menu=="") {
        config.site_menu = [:]
    }
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
                def firstEntryUri = findFirstUri(entries)
                def url = "${content.rootpath}$firstEntryUri"
                def basePath = url.replaceAll('[^/]*$', '')
                def isActive = ""
                if ((content.rootpath + content.uri)?.startsWith(basePath)) {
                    isActive = "active"
                }
                //System.out.println "   $title"
                newEntries << [isActive: isActive, href: url, title: title]
            }
        }
    }
} catch (Exception e) {
                System.out.println """

>>> menu.gsp: (2) ${e.message}

"""
}

def makeHierachical(def entries) {
    def tree = asTree(entries.collect { it.uri.split('/') })
    def map = tree
    def prefix = ''
    while(map.size() == 1 && !map.values().first().isEmpty()) {
        def key = map.keySet().first()
        prefix = prefix + key + '/'
        map = map[key]
    }

    result = processMap(entries, prefix, map, true)
    return result
}

def processMap(def originalEntries, String prefix, def map, boolean skipIndex) {
    return map.entrySet()
        .findAll { entry -> skipIndex || entry.key != 'index.html' }
        .collect { entry ->
            def candidate;
            if(entry.key.endsWith(".html")) {
                candidate = originalEntries.find {it.uri == prefix + entry.key }
            } else {
                def index = originalEntries.find {it.uri == prefix + entry.key + '/index.html'}
                if(index != null) {
                    candidate = index;
                    if(candidate.order == -987654321) {
                        String t = entry.key;
                        def matcher = t =~ /^([0-9]+)[_-](.*)$/
                        if(matcher.matches()) {
                            Integer o = matcher.group(1) as Integer
                            candidate.order = o
                        }
                    }
                } else {
                    String t = entry.key;
                    Integer o = -1
                    def matcher = t =~ /^([0-9]+)[_-](.*)$/
                    if(matcher.matches()) {
                        o = matcher.group(1) as Integer
                        t = matcher.group(2)
                    }
                    candidate = [
                        title: t,
                        order: o,
                        filename: null,
                        uri: null,
                        children: []
                    ]
                }
            }
            if(!entry.value.isEmpty()) {
                candidate['children'] = processMap(originalEntries, prefix + entry.key + '/', entry.value, false)
            }
            return candidate
        }
}

def Map<String, ?> asTree(List<List<String>> list) {
    if(list == [[]]) {
        return [:]
    } else {
        return list
                .groupBy { it.head() }
                .collectEntries { k, v -> [k, asTree(v.collect {it.tail()}) ] }
    }
}

def String findFirstUri(def entries) {
    def indexEntry = entries.find { it.order == -987654321 }
    if(indexEntry && indexEntry.uri) {
        return indexEntry.uri
    }
    def firstEntry = entries.sort { a, b -> a.order <=> b.order ?: a.title <=> b.title }[0]
    if (firstEntry) {
        if(firstEntry.uri) {
            return firstEntry.uri
        }
        return findFirstUri(firstEntry.children)
    }
    return ''
}

//store results to be used in other templates
content.menu = menu
content.entriesMap = entriesMap
content.newEntries = newEntries
