<div class="row flex-xl-nowrap">
    <main class="col-12 col-md-12 col-xl-12 pl-md-12" role="main">
        <div class="bg-light p-5 rounded jumbotron">
            <img src="images/doctoolchain-logo-blue.png" alt="docToolchain" style="float: left" />
            <h1>docToolchain <span class="badge bg-primary">v4</span></h1>
            <p class="lead">
                Docs-as-code. Fast. No build tool at runtime.
            </p>
            <p>docToolchain v4 runs directly on the JVM &mdash; no Gradle, no daemon, startup in 2&ndash;3 seconds.
                One wrapper script, one config file, powerful output.
            </p>
            <a class="btn btn-primary btn-lg" href="010_manual/20_install.html" role="button">Get Started</a>
        </div>

        <div class="row row-cols-1 row-cols-md-3 mb-3 text-center mt-4">
            <div class="col">
                <div class="card mb-4 shadow-sm">
                    <div class="card-header">
                        <h4 class="my-0 fw-normal">Fast Startup</h4>
                    </div>
                    <div class="card-body">
                        Direct JVM execution &mdash; no Gradle, no daemon process. Tasks start in 2&ndash;3 seconds instead of 15&ndash;30.
                    </div>
                </div>
            </div>
            <div class="col">
                <div class="card mb-4 shadow-sm">
                    <div class="card-header">
                        <h4 class="my-0 fw-normal">One Script Setup</h4>
                    </div>
                    <div class="card-body">
                        Just <code>dtcw4</code> in your project. It installs docToolchain and runs all tasks. No other files needed in your repo.
                    </div>
                </div>
            </div>
            <div class="col">
                <div class="card mb-4 shadow-sm">
                    <div class="card-header">
                        <h4 class="my-0 fw-normal">Cross-Platform</h4>
                    </div>
                    <div class="card-body">
                        Runs on Linux, macOS, and Windows (WSL2). No Ruby, no Python, no native dependencies &mdash; just Java 17.
                    </div>
                </div>
            </div>
        </div>

        <div class="row row-cols-1 row-cols-md-3 mb-3 text-center">
            <div class="col">
                <div class="card mb-4 shadow-sm">
                    <div class="card-header">
                        <h4 class="my-0 fw-normal">Fully Open Source</h4>
                    </div>
                    <div class="card-body">
                        No vendor lock-in. No contracts. Modify docToolchain to meet your unique needs.<br /><br />
                        <a class="btn btn-outline-primary" href="https://github.com/docToolchain/docToolchain" role="button">GitHub</a>
                    </div>
                </div>
            </div>
            <div class="col">
                <div class="card mb-4 shadow-sm">
                    <div class="card-header">
                        <h4 class="my-0 fw-normal">Publish to Confluence</h4>
                    </div>
                    <div class="card-body">
                        Docs-as-code but your team wants a wiki? No problem &mdash; publish directly to Confluence.<br /><br />
                        <a class="btn btn-outline-primary" href="015_tasks/03_task_publishToConfluence.html" role="button">Learn more</a>
                    </div>
                </div>
            </div>
            <div class="col">
                <div class="card mb-4 shadow-sm">
                    <div class="card-header">
                        <h4 class="my-0 fw-normal">Microsites with jBake</h4>
                    </div>
                    <div class="card-body">
                        Generate full documentation websites with landing page, navigation, search, and edit links.<br /><br />
                        <a class="btn btn-outline-primary" href="015_tasks/03_task_generateSite.html" role="button">Learn more</a>
                    </div>
                </div>
            </div>
        </div>

        <div class="row row-cols-1 mb-3">
            <div class="col">
                <h2 class="mt-4">Quick Start</h2>
            </div>
        </div>
        <div class="row row-cols-1 mb-3">
            <div class="col">
                <div class="card shadow-sm">
                    <div class="card-body">
<pre class="mb-0"><code>curl -Lo dtcw4 https://doctoolchain.org/dtcw4
chmod +x dtcw4
./dtcw4 local install doctoolchain
./dtcw4 local generateSite</code></pre>
                    </div>
                </div>
            </div>
        </div>

        <div class="row row-cols-1 mb-3">
            <div class="col">
                <h2 class="mt-4">Sub-Projects &amp; Ecosystem</h2>
                <p>docToolchain is more than a single tool &mdash; it's a growing ecosystem of related projects to support your docs-as-code journey.</p>
            </div>
        </div>
        <div class="row row-cols-1 row-cols-md-3 mb-3 text-center">
            <div class="col">
                <div class="card mb-4 shadow-sm">
                    <div class="card-header">
                        <h4 class="my-0 fw-normal">AsciiDoc Linter</h4>
                    </div>
                    <div class="card-body">
                        A linter for AsciiDoc files to help you keep your documentation clean and consistent.<br /><br />
                        <a class="btn btn-primary" href="https://doctoolchain.org/asciidoc-linter" role="button">learn more</a>
                    </div>
                </div>
            </div>
            <div class="col">
                <div class="card mb-4 shadow-sm">
                    <div class="card-header">
                        <h4 class="my-0 fw-normal">dacli</h4>
                    </div>
                    <div class="card-body">
                        A command-line interface to simplify and streamline your docs-as-code workflow with docToolchain.<br /><br />
                        <a class="btn btn-primary" href="https://doctoolchain.org/dacli" role="button">learn more</a>
                    </div>
                </div>
            </div>
            <div class="col">
                <div class="card mb-4 shadow-sm">
                    <div class="card-header">
                        <h4 class="my-0 fw-normal">Bausteinsicht</h4>
                    </div>
                    <div class="card-body">
                        Architecture-as-code: define building blocks in JSONC, sync bidirectionally with draw.io diagrams.<br /><br />
                        <a class="btn btn-primary" href="https://doctoolchain.org/Bausteinsicht/" role="button">learn more</a>
                    </div>
                </div>
            </div>
            <div class="col">
                <div class="card mb-4 shadow-sm">
                    <div class="card-header">
                        <h4 class="my-0 fw-normal">diagrams.net IntelliJ Plugin</h4>
                    </div>
                    <div class="card-body">
                        Edit diagrams.net (draw.io) diagrams directly inside IntelliJ IDEA without leaving your IDE.<br /><br />
                        <a class="btn btn-primary" href="https://github.com/docToolchain/diagrams.net-intellij-plugin" role="button">learn more</a>
                    </div>
                </div>
            </div>
            <div class="col">
                <div class="card mb-4 shadow-sm">
                    <div class="card-header">
                        <h4 class="my-0 fw-normal">iSAQB Template</h4>
                    </div>
                    <div class="card-body">
                        A ready-to-use documentation template following the iSAQB arc42 software architecture documentation standard.<br /><br />
                        <a class="btn btn-primary" href="https://github.com/docToolchain/iSAQB-Template" role="button">learn more</a>
                    </div>
                </div>
            </div>
            <div class="col">
                <div class="card mb-4 shadow-sm">
                    <div class="card-header">
                        <h4 class="my-0 fw-normal">CI/CD Demo</h4>
                    </div>
                    <div class="card-body">
                        A demo project showing how to integrate docToolchain into your CI/CD pipeline for automated documentation builds.<br /><br />
                        <a class="btn btn-primary" href="https://github.com/docToolchain/ci-cd-demo" role="button">learn more</a>
                    </div>
                </div>
            </div>
        </div>
    </main>

</div>
