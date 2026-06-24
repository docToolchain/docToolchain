
    <footer class="bg-dark py-5 row d-print-none">
        <div class="container-fluid mx-sm-5">
            <div class="row">
                <div class="col-6 col-sm-4 text-xs-center order-sm-2">


                    <ul class="list-inline mb-0">
                        <% if (config.site_footerMail) { %>
                        <li aria-label="User mailing list" class="list-inline-item mx-2 h3" data-original-title="User mailing list" data-placement="top"
                            data-toggle="tooltip" title="">
                            <a class="text-white" href="mailto:${config.site_footerMail}" rel="noopener noreferrer"
                               target="_blank">
                                <i class="fa fa-envelope"></i>
                            </a>
                        </li>
                        <% } %>
                        <% if (config.site_footerTwitter) { %>
                        <li aria-label="Twitter" class="list-inline-item mx-2 h3" data-original-title="Twitter" data-placement="top"
                            data-toggle="tooltip" title="">
                            <a class="text-white" href="${config.site_footerTwitter}" rel="noopener noreferrer"
                               target="_blank">
                                <i class="fab fa-twitter"></i>
                            </a>
                        </li>
                        <% } %>
                        <% if (config.site_footerSO) { %>
                        <li aria-label="Stack Overflow" class="list-inline-item mx-2 h3" data-original-title="Stack Overflow" data-placement="top"
                            data-toggle="tooltip" title="">
                            <a class="text-white" href="${config.site_footerSO}" rel="noopener noreferrer"
                               target="_blank">
                                <i class="fab fa-stack-overflow"></i>
                            </a>
                        </li>
                        <% } %>
                    </ul>


                </div>
                <div class="col-6 col-sm-4 text-right text-xs-center order-sm-3">


                    <ul class="list-inline mb-0">

                        <% if (config.site_footerGithub) { %>
                        <li aria-label="GitHub" class="list-inline-item mx-2 h3" data-original-title="GitHub" data-placement="top"
                            data-toggle="tooltip" title="">
                            <a class="text-white" href="${config.site_footerGithub}" rel="noopener noreferrer"
                               target="_blank">
                                <i class="fab fa-github"></i>
                            </a>
                        </li>
                        <% } %>

                        <% if (config.site_footerSlack) { %>
                        <li aria-label="Slack" class="list-inline-item mx-2 h3" data-original-title="Slack" data-placement="top"
                            data-toggle="tooltip" title="">
                            <a class="text-white" href="${config.site_footerSlack}" rel="noopener noreferrer"
                               target="_blank">
                                <i class="fab fa-slack"></i>
                            </a>
                        </li>
                        <% } %>


                    </ul>


                </div>
                <div class="col-12 col-sm-4 text-center py-2 order-sm-2">

                        ${config.site_footerText}

                </div>
            </div>
        </div>
    </footer>

    <script src="${content.rootpath}js/popper.min.js"></script>
    <script src="${content.rootpath}js/bootstrap.min.js"></script>


    <script src="${content.rootpath}js/main.min.b5fc1b29d2465835844254bd4d804738eaf24c0cec53009b4c6899989be55a47.js"></script>
    <script src="${content.rootpath}js/blocks.js" ></script>
    <script src="${content.rootpath}js/prettify.js"></script>
    <script src="${content.rootpath}js/copy-n-paste.js"></script>
    <script src="${content.rootpath}js/submenucollapse.js"></script>

    <!-- dtc:mathjax:start -->
    <%
        // STEM (math) rendering: only load MathJax on pages that actually carry
        // math, so math-free pages pay nothing. AsciiDoctor emits latexmath as
        // \( … \) / \[ … \] and asciimath as \$ … \$, so those delimiters (or a
        // stem block) are the signal.
        def __body = (content.body ?: '')
        def __hasStem = __body.contains('\\(') || __body.contains('\\[') ||
                        __body.contains('\\$') || __body.contains('class="stem')
        if (__hasStem) {
    %>
    <!-- docToolchain — STEM/math via bundled MathJax (SVG output: self-contained, no CDN, works offline) -->
    <script>
        // Build the asciimath delimiter (backslash followed by dollar, the form
        // AsciiDoctor emits) from char codes, to avoid GSP template escaping.
        var dtcAm = String.fromCharCode(92) + String.fromCharCode(36);
        window.MathJax = {
            loader: { load: ['input/asciimath'] },
            asciimath: { delimiters: [[dtcAm, dtcAm]] }
        };
    </script>
    <script id="MathJax-script" src="${content.rootpath}js/mathjax/tex-mml-svg.js"></script>
    <% } %>
    <!-- dtc:mathjax:end -->

    <!-- docToolchain v4 — colour-scheme toggle -->
    <script>
        function dtcToggleTheme() {
            var root = document.documentElement;
            var next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
            root.setAttribute('data-theme', next);
            try { localStorage.setItem('dtc-theme', next); } catch (e) {}
            document.querySelectorAll('.dtc-theme-toggle').forEach(function (b) {
                b.textContent = next === 'dark' ? '☀️' : '🌙';
            });
        }
        // sync icon with the theme applied in <head>
        document.addEventListener('DOMContentLoaded', function () {
            var dark = document.documentElement.getAttribute('data-theme') === 'dark';
            document.querySelectorAll('.dtc-theme-toggle').forEach(function (b) {
                b.textContent = dark ? '☀️' : '🌙';
            });
        });
    </script>
