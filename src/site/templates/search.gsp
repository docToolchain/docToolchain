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
				<div class="td-sidebar__inner" id="td-sidebar-menu" >
					<!--%include "submenu.gsp"%-->
				</div>
			</div>
			<div class="d-none d-xl-block col-xl-2 td-toc d-print-none">
				<div class="td-page-meta ml-2 pb-1 pt-2 mb-0">
					<!--%include "rightcolumn.gsp" %-->
				</div>
			</div>
			<main class="col-12 col-md-9 col-xl-8 pl-md-5" role="main">
                <!--nav aria-label="breadcrumb" class="d-none d-md-block d-print-none">
                    <ol class="breadcrumb spb-1">
                        <li aria-current="page" class="breadcrumb-item active">
                <a href="">Search</a>
            </li>

                    </ol>
                </nav-->


                <div class="td-content">
                    <p>
                        <h3>Search Results</h3>
                <script src="${content.rootpath}js/lunr.js"></script>
                <script src="${content.rootpath}lunrjsindex.js"></script>
                <script src="${content.rootpath}js/lunrsearch.js"></script>
                <script src="${content.rootpath}js/lunrdosearch.js"></script>
                <input type="text" name="q" id="lunrsrc" onkeyup="dosearch(this);" onchange="dosearch(this);" />
                <div id="results"></div>
                <script>
                    var input = document.querySelector("#lunrsrc");
                    input.focus();
                    var params = new URLSearchParams(window.location.search);
                    input.value = params.get('q');
                    dosearch(input);
                </script>
            </p>


                <br>

                <!--div class="text-muted mt-5 pt-3 border-top">Last modified July 3, 2019: <a
                href="https://github.com/google/docsy-example/commit/d6aa89c8b24089d7e6741030864eff209465e896">Added
            links to user guide repo (d6aa89c)</a>
        </div-->
        </div>
			</main>

		</div>
	</div>


	<%include "footer.gsp"%>

</body>
</html>
