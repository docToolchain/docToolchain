<head>
    <%
        //let's fix the context root
        if (!content.rootpath) {
            //if we are in the main folder, we need no rootpath
            content.rootpath = ''
            //but if we are deeper in the folder structure...
            if (content.sourceuri) {
                content.rootpath = '../' * (content.sourceuri?.split('/')?.size() - 1)
            }
        }
        //this is mainly a fix for the imagesdir which is set to /images
        content.body = content.body?.replaceAll('src="/', 'src="' + content.rootpath)
    %>
    <!-- sourceuri: ${content.sourceuri} -->
    <meta content="text/html; charset=UTF-8" http-equiv="Content-Type">

    <meta content="width=device-width, initial-scale=1, shrink-to-fit=no" name="viewport">
    <meta content="jbake" name="generator">
    <meta content="INDEX, FOLLOW" name="ROBOTS">

    <title>${content.title ? content.title : config.site_title}</title>
    <meta content="docToolchain" property="og:title">
    <meta content="build your dev docs the easy way..." property="og:description">
    <meta content="website" property="og:type">
    <meta content="docToolchain" property="og:site_name">
    <meta content="docToolchain" itemprop="name">
    <meta content="build your dev docs the easy way..." itemprop="description">
    <meta content="summary" name="twitter:card">
    <meta content="docToolchain" name="twitter:title">
    <meta content="build your dev docs the easy way..." name="twitter:description">

    <link as="style"
        href="${content.rootpath}css/main.min.881fe5f7b53609f55ebfb496c7097c3b30b2e8ceb20a54bc6a48350ded67224f.css"
        rel="preload">
    <link href="${content.rootpath}css/main.min.881fe5f7b53609f55ebfb496c7097c3b30b2e8ceb20a54bc6a48350ded67224f.css"
        integrity=""
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
    <link rel="icon" type="image/png" sizes="192x192" href="${content.rootpath}android-icon-192x192.png">
    <link rel="icon" type="image/png" sizes="32x32" href="${content.rootpath}favicon-32x32.png">
    <link rel="icon" type="image/png" sizes="96x96" href="${content.rootpath}favicon-96x96.png">
    <link rel="icon" type="image/png" sizes="16x16" href="${content.rootpath}favicon-16x16.png">
    <link rel="manifest" href="${content.rootpath}manifest.json">
    <meta name="msapplication-TileColor" content="#ffffff">
    <meta name="msapplication-TileImage" content="${content.rootpath}ms-icon-144x144.png">
    <meta name="theme-color" content="#ffffff">

    <script src="${content.rootpath}js/jquery-3.5.1.min.js"></script>
    <style>
    div.bg-light {
        background-color: rgba(127, 180, 224, 0.3) !important;
    }

    /* for blog posts */
    span.blogtag {
        border-radius: 10px;
        background-color: #30638E;
        color: white;
        padding: 5px 10px;
    }

    span.blogblogtag a {
        color: white;
        padding: 0
    }

    span.blogtag span {
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
        font-size: smaller;
        line-height: 1.1;
    }

    .td-page-meta {
        font-size: smaller;
        line-height: 1.1;
    }

    #td-section-nav ul.td-sidebar-nav__section {
        padding-left: 0;
    }

    span.navbar-logo {
        padding-right: 1.5em;
    }

    .match {
        font-size: smaller;
    }

    .admonitionblock td.icon .fa:before {
        font-size: xxx-large;
    }
    </style>

    <style>
    div.openblock.primary div.content, div.openblock.secondary div.content {
        border: 1px solid #7a2518;
        padding: 5px;
    }

    div.openblock.primary div.content div.content, div.openblock.secondary div.content div.content {
        border: 0 solid #7a2518;
        padding: 0;
    }

    .hidden {
        display: none;
    }

    .switch {
        border-width: 1px 1px 0 1px;
        border-style: solid;
        border-color: #7a2518;
        display: inline-block;
    }

    .switch--item {
        padding: 2px 10px;
        background-color: #ffffff;
        color: #7a2518;
        display: inline-block;
        cursor: pointer;
    }

    .switch--item:not(:first-child) {
        border-width: 0 0 0 1px;
        border-style: solid;
        border-color: #7a2518;
    }

    .switch--item.selected {
        background-color: #7a2519;
        color: #ffffff;
    }
    </style>

    <!-- copy-n-paste.js -->
    <style>
        div.listingblock {
            position: relative;
        }
        div.listingblock > span.icon {
            position: absolute;
            top: 2px;
            right: 6px;
            visibility: hidden;
            z-index: 2;
            cursor: pointer;
        }
        div.listingblock:hover > span.icon {
            visibility: visible;
        }
        div.listingblock div.content pre {
            margin: 0;
        }
    </style>
    <!-- drop down menu -->
    <style>
         /* Dropdown Button */
         .dropbtn {
           background-color: #30638E;
           color: white;
           padding: 10px;
           font-size: 16px;
           border: none;
         }

         /* The container <div> - needed to position the dropdown content */
         .dropdown {
           position: relative;
           display: inline-block;
         }

         /* Dropdown Content (Hidden by Default) */
         .dropdown-content {
           display: none;
           position: absolute;
           background-color: #f1f1f1;
           min-width: 160px;
           box-shadow: 0px 8px 16px 0px rgba(0,0,0,0.2);
           z-index: 1;
         }

         /* Links inside the dropdown */
         .dropdown-content a {
           color: black;
           padding: 12px 16px;
           text-decoration: none;
           display: block;
         }

         /* analogous to the `.td-navbar .nav-link` block*/
         .td-navbar .dropbtn {
           text-transform:none;
           font-weight:700;
         }

         /* Change color of dropdown links on hover */
         .dropdown-content a:hover {background-color: #ddd;}

         /* Show the dropdown menu on hover */
         .dropdown:hover .dropdown-content {display: block;}

         /* Change the background color of the dropdown button when the dropdown content is shown */
         .dropdown:hover .dropbtn {background-color: #5579ae;}

    </style>
</head>
