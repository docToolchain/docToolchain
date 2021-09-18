<head>
  <%
      //let's fix the context root
      if (content.rootpath) {

      } else {
        //if we are in the main folder, we need no rootpath
        content.rootpath = ''
        //but if we are deeper in the folder structure...
        if (content.sourceuri) {
          content.rootpath = '../' * (content.sourceuri?.split('/')?.size()-1)
        }
      }
      //this is mainly a fix for the imagesdir which is set to /images
      content.body = content.body?.replaceAll('src="/','src="'+content.rootpath)
  %>
  <!-- ${content.sourceuri} -->
  <meta content="text/html; charset=UTF-8" http-equiv="Content-Type">

  <meta content="width=device-width, initial-scale=1, shrink-to-fit=no" name="viewport">
  <meta content="jbake" name="generator">
  <meta content="INDEX, FOLLOW" name="ROBOTS">

  <title><%if (content.title) {%>${content.title}<% } else { %>JBake<% }%></title>
  <meta content="Documentation" property="og:title">
  <meta content="A Docsy example site" property="og:description">
  <meta content="website" property="og:type">
  <meta content="/docs/" property="og:url">
  <meta content="Goldydocs" property="og:site_name">
  <meta content="Documentation" itemprop="name">
  <meta content="A Docsy example site" itemprop="description">
  <meta content="summary" name="twitter:card">
  <meta content="Documentation" name="twitter:title">
  <meta content="A Docsy example site" name="twitter:description">

  <link as="style" href="${content.rootpath}css/main.min.881fe5f7b53609f55ebfb496c7097c3b30b2e8ceb20a54bc6a48350ded67224f.css"
        rel="preload">
  <link href="${content.rootpath}css/main.min.881fe5f7b53609f55ebfb496c7097c3b30b2e8ceb20a54bc6a48350ded67224f.css" integrity=""
        rel="stylesheet">
  <link href="${content.rootpath}css/asciidoctor.css" rel="stylesheet">
  <link href="${content.rootpath}css/prettify.css" rel="stylesheet">

    <!-- favicon generated with https://www.favicon-generator.org/ -->
    <link rel="shortcut icon" href="${content.rootpath}favicon.ico">
    <link rel="apple-touch-icon" sizes="57x57" href="${content.rootpath}apple-icon-57x57.png">
    <link rel="apple-touch-icon" sizes="60x60" href="${content.rootpath}apple-icon-60x60.png">
    <link rel="apple-touch-icon" sizes="72x72" href="${content.rootpath}apple-icon-72x72.png">
    <link rel="apple-touch-icon" sizes="76x76" href="${content.rootpath}apple-icon-76x76.png">
    <link rel="apple-touch-icon" sizes="114x114" href="${content.rootpath}apple-icon-114x114.png">
    <link rel="apple-touch-icon" sizes="120x120" href="${content.rootpath}apple-icon-120x120.png">
    <link rel="apple-touch-icon" sizes="144x144" href="${content.rootpath}apple-icon-144x144.png">
    <link rel="apple-touch-icon" sizes="152x152" href="${content.rootpath}apple-icon-152x152.png">
    <link rel="apple-touch-icon" sizes="180x180" href="${content.rootpath}apple-icon-180x180.png">
    <link rel="icon" type="image/png" sizes="192x192"  href="${content.rootpath}android-icon-192x192.png">
    <link rel="icon" type="image/png" sizes="32x32" href="${content.rootpath}favicon-32x32.png">
    <link rel="icon" type="image/png" sizes="96x96" href="${content.rootpath}favicon-96x96.png">
    <link rel="icon" type="image/png" sizes="16x16" href="${content.rootpath}favicon-16x16.png">
    <link rel="manifest" href="${content.rootpath}manifest.json">
    <meta name="msapplication-TileColor" content="#ffffff">
    <meta name="msapplication-TileImage" content="${content.rootpath}ms-icon-144x144.png">
    <meta name="theme-color" content="#ffffff">

  <script crossorigin="anonymous" integrity="sha256-9/aliU8dGd2tb6OSsuzixeV4y/faTqgFtohetphbbj0="
          src="${content.rootpath}js/jquery-3.5.1.min.js"></script>
  <style>
  span.tag {
      border-radius: 10px;
      background-color: #30638E;
      color: white;
      padding: 5px 10px;
  }
  span.tag a {
      color: white;
      padding: 0
  }
  span.tag span {
      margin-left: 10px;
      border-radius: 10px;
      background-color: white;
      color: #30638E;
  }
  div.td-toc {
    height: calc(100vh - 5rem) !important;
  }
  div#toctitle {
    display: none
  }
#td-section-nav {
    max-height: calc(100vh - 4rem);
  }
  #td-section-nav ul.td-sidebar-nav__section  {
    padding-left:0px;
  }
  </style>

</head>
