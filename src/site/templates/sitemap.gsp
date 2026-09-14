<?xml version="1.0" encoding="UTF-8"?>
<%
    // A sitemap promises crawlers that these addresses exist. Two things used to
    // break that promise silently:
    //
    //  - site.host still held a placeholder, because nothing ever asked for it.
    //    Every entry then pointed at jbake.org or localhost.
    //  - every published page was listed with its source extension, so a page
    //    rendered to another extension (the lunr.js search index) was listed as
    //    .html and answered 404.
    //
    // Both are now caught here. An empty sitemap is better than a wrong one.
    def host = (config.site_host ?: '').trim().replaceAll('/+$', '')
    def placeholders = ['http://jbake.org', 'http://www.jbake.org', 'https://jbake.org']
    def unset = !host || host in placeholders || host ==~ '(?i)https?://localhost(:\\d+)?'
    if (unset) {
        System.err.println """
        WARNING  site.host is '${config.site_host}' — a placeholder, not this site's address.
                 The sitemap is written empty, because every entry would point at
                 a host that does not serve this site.
                 Set microsite.host in your docToolchain config.
        """.stripIndent()
    }

    /** The address a page is really published under — its own extension, not the source one. */
    def uriOf = { content ->
        def extension = config."template_${content.type}_extension" ?: '.html'
        content.uri.replaceAll('[.]html$', extension)
    }

    /** Only pages belong in a sitemap. A search index or a feed is not a page. */
    def isPage = { content ->
        (config."template_${content.type}_extension" ?: '.html') == '.html'
    }
%>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd">
<% if (!unset) { published_content.findAll(isPage).each {content -> %>
    <url>
    	<loc>${host}/${uriOf(content)}</loc>
    	<lastmod>${content.date.format("yyyy-MM-dd")}</lastmod>
    </url>
<%  } }%>
</urlset>
