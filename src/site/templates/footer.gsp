
    <!--footer class="footer mt-auto py-3 bg-light">
        <div class="container">
            <span class="text-muted">&copy; 2020 | based on <a href="https://arc42.org">arc42 7.0</a> | mixed with <a href="http://getbootstrap.com/">Bootstrap v5.0.0-beta</a> | Baked with <a href="http://jbake.org">JBake ${version}</a></span>
        </div>
    </footer-->

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

    <script crossorigin="anonymous"
            integrity="sha384-ZMP7rVo3mIykV+2+9J3UJ46jBk0WLaUAdn689aCwoqbBJiSnjAK/l8WvCWPIPm49"
            src="${content.rootpath}js/popper.min.js"></script>
    <script crossorigin="anonymous"
            integrity="sha384-ChfqqxuZUCnJSK3+MXmPNIyE6ZbWh2IMqE241rYiqJxyMiZ6OW/JmZQ5stwEULTy"
            src="${content.rootpath}js/bootstrap.min.js"></script>


    <script crossorigin="anonymous"
            integrity="sha256-tfwbKdJGWDWEQlS9TYBHOOryTAzsUwCbTGiZmJvlWkc=" src="${content.rootpath}/js/main.min.b5fc1b29d2465835844254bd4d804738eaf24c0cec53009b4c6899989be55a47.js"></script>
    <script src="${content.rootpath}js/bootstrap.min.js"></script>
    <script src="${content.rootpath}js/blocks.js" ></script>
    <script src="${content.rootpath}js/prettify.js"></script>
