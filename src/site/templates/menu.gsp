<nav class="navbar navbar-expand-md navbar-dark fixed-top bg-dark">
  <div class="container-fluid">
    <a class="navbar-brand" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>">arc42</a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarCollapse" aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarCollapse">
      <ul class="navbar-nav me-auto mb-2 mb-md-0">
        <li><a class="nav-link" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>index.html">Home</a></li>
        <li class="nav-item dropdown">
          <a class="nav-link dropdown-toggle" href="#" id="dropdown01" data-bs-toggle="dropdown" aria-expanded="false">arc42</a>
          <ul class="dropdown-menu" aria-labelledby="dropdown01">
            <li><a class="dropdown-item" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>arc42/chap01.html">01 Einführung und Ziele</a></li>
            <li><a class="dropdown-item" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>arc42/chap02.html">02 Randbedingungen</a></li>
            <li><a class="dropdown-item" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>arc42/chap03.html">03 Kontextabgrenzung</a></li>
            <li><a class="dropdown-item" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>arc42/chap04.html">04 Lösungsstrategie</a></li>
            <li><a class="dropdown-item" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>arc42/chap05.html">05 Bausteinsicht</a></li>
            <li><a class="dropdown-item" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>arc42/chap06.html">06 Laufzeitsicht</a></li>
            <li><a class="dropdown-item" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>arc42/chap07.html">07 Verteilungssicht</a></li>
            <li><a class="dropdown-item" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>arc42/chap08.html">08 Querschnittliche Konzepte</a></li>
            <li><a class="dropdown-item" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>arc42/chap09.html">09 Entwurfsentscheidungen</a></li>
            <li><a class="dropdown-item" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>arc42/chap10.html">10 Qualitätsanforderungen</a></li>
            <li><a class="dropdown-item" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>arc42/chap11.html">11 Risiken und technische Schulden</a></li>
            <li><a class="dropdown-item" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>arc42/chap12.html">12 Glossar</a></li>
          </ul>
        </li>
        <li><a class="nav-link" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>blog/index.html">Blog</a></li>
        <li><a class="nav-link" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>about.html">About</a></li>
        <li><a class="nav-link" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>${config.feed_file}">RSS-Feed</a></li>
      </ul>
      <form class="d-flex">
        <input class="form-control me-2" type="search" placeholder="Search" aria-label="Search">
        <button class="btn btn-outline-success" type="submit">Search</button>
      </form>
    </div>
  </div>
</nav>
