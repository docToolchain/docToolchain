<!doctype html>
<html lang="de">
<%include "header.gsp"%>
<body onload="prettyPrint()" class="d-flex flex-column h-100">

<%include "menu.gsp"%>

<main class="container">
	<div class="bg-light p-5 rounded">
		<h1>${content.title}</h1>
		<p><em>${new java.text.SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(content.date)}</em></p>
		<p>${content.body}</p>
	</div>
</main>

<%include "footer.gsp"%>

</body>
</html>
