<!doctype html>
<html lang="de">
<%include "header.gsp"%>
<body onload="prettyPrint()" class="d-flex flex-column h-100">

<%include "menu.gsp"%>

<main class="container">
	<div class="bg-light p-5 rounded">
		<h1>Solution Architecture Documentation</h1>
		<p class="lead">
			This Microsite contains the documentation for system X
		</p>
		<p>Insert an introduction here</p>
	</div>

    <div class="row row-cols-1 row-cols-md-3 mb-3 text-center">
        <div class="col">
            <div class="card mb-4 shadow-sm">
                <div class="card-header">
                    <h4 class="my-0 fw-normal">Feature One</h4>
                </div>
                <div class="card-body">
                    Write a teaser for this feature here.
                </div>
            </div>
        </div>
        <div class="col">
            <div class="card mb-4 shadow-sm">
                <div class="card-header">
                    <h4 class="my-0 fw-normal">Feature Two</h4>
                </div>
                <div class="card-body">
                    Write a teaser for this feature here.
                </div>
            </div>
        </div>
        <div class="col">
            <div class="card mb-4 shadow-sm">
                <div class="card-header">
                    <h4 class="my-0 fw-normal">Feature Three</h4>
                </div>
                <div class="card-body">
                    Write a teaser for this feature here.
                </div>
            </div>
        </div>
    </div>
</main>

<%include "footer.gsp"%>

</body>
</html>
