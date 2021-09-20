    <nav aria-label="breadcrumb" class="d-none d-md-block d-print-none">
        <ol class="breadcrumb spb-1">
            <!--li aria-current="page" class="breadcrumb-item active">
                <a href="https://example.docsy.dev/docs/">Documentation</a>
            </li-->

        </ol>
    </nav>


    <div class="td-content">
        <p>
            <%
                def splitBody = content.body
                if (splitBody.contains("<!-- endtoc -->")) {
                    splitBody = splitBody.split("(?ms)<!-- endtoc -->", 2)[1]
                }
                out << splitBody
            %>
        </p>

        <%include "feedback.gsp" %>

        <br>

        <!--div class="text-muted mt-5 pt-3 border-top">Last modified July 3, 2019: <a
                href="https://github.com/google/docsy-example/commit/d6aa89c8b24089d7e6741030864eff209465e896">Added
            links to user guide repo (d6aa89c)</a>
        </div-->
    </div>

