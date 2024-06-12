<!doctype html>
<html lang="de">
<%include "header.gsp"%>
<body class="td-section">
<header>
	<%include "menu.gsp"%>
</header>
<div class="container-fluid td-outer">
	<div class="td-main">
		<div class="row flex-xl-nowrap">
			<div class="col-12 col-md-3 col-xl-2 td-sidebar d-print-none">
				<div class="td-sidebar__inner" id="td-sidebar-menu">
					<%include "submenu.gsp"%>
				</div>
			</div>
			<div class="d-none d-xl-block col-xl-2 td-toc d-print-none">
				<div class="td-page-meta ml-2 pb-1 pt-2 mb-0">
					<%include "rightcolumn.gsp" %>
				</div>
			</div>
			<main class="col-12 col-md-9 col-xl-8 pl-md-5" role="main">
				<p><em>${new java.text.SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(content.date)}, ${content.author?:''}</em></p>
				<p>
                    <%
                        def splitBody = content.body
                        if (splitBody.contains("<!-- endtoc -->")) {
                            splitBody = splitBody.split("(?ms)<!-- endtoc -->", 2)[1]
                        }
                        out << splitBody
                    %>

                </p>
			</main>

		</div>
	</div>


	<%include "footer.gsp"%>

</body>
</html>

