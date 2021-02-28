<!doctype html>
<html lang="de">
<%include "header.gsp"%>
<body onload="prettyPrint()" class="d-flex flex-column h-100">

<%include "menu.gsp"%>

<main class="container">
    <div class="bg-light p-5 rounded">
        <h1>Blog</h1>
        <%published_posts.each {post ->%>
        <p class="lead">
        <a href="../${post.uri}"><h3>${post.title}</h3></a>
        <p>${new java.text.SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(post.date)}</p>
        </p>
        <%}%>

    </div>
</main>

<%include "footer.gsp"%>

</body>
</html>
