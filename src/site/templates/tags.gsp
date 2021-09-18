    <!doctype html>
<html lang="de">
<%include "header.gsp"%>
<body onload="prettyPrint()" class="td-section">
<header>
    <%include "menu.gsp"%>
</header>
<div class="container-fluid td-outer">
    <div class="td-main">
        <div class="row flex-xl-nowrap">
            <div class="col-12 col-md-3 col-xl-2 td-sidebar d-print-none">
                <div class="td-sidebar__inner" id="td-sidebar-menu">
                    <!--%include "submenu.gsp"%-->
                </div>
            </div>
            <div class="d-none d-xl-block col-xl-2 td-toc d-print-none">
                <div class="td-page-meta ml-2 pb-1 pt-2 mb-0">
                    <h3>all tags</h3>
                    <%
                        alltags?.sort().each { thisTag ->
                            thisTag = thisTag.trim()
                            def postsCount = posts.findAll { post ->
                                post.status == "published" && post.tags?.contains(thisTag)
                            }.size()
                    %>
                    <span class="tag"><a href="${content.rootpath}tags/${tag.replace(' ', '-')}.html">${thisTag}&nbsp;<span class=""badge">${postsCount}</span></a></span>
                    <%
                        }
                    %>
                </div>
            </div>
            <main class="col-12 col-md-9 col-xl-8 pl-md-5" role="main">
                <%
                try {
                        %>
                <h1>All Posts for tag '${tag}'</h1>
                <%
                    tag_posts.each {post ->%>
                <p class="lead">
                    <a href="../${post.uri}"><h3>${post.title}</h3></a>
                <p>${new java.text.SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(post.date)}</p>
            </p>
                <%}
                } catch (Exception e) {
                    System.out.println "error 1: $e"
                }

                %>

            </main>

        </div>
    </div>


    <%include "footer.gsp"%>

</body>
</html>

