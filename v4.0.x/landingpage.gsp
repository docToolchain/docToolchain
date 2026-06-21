<div class="row flex-xl-nowrap">
    <main class="col-12 col-md-12 col-xl-12 pl-md-12" role="main">

<!-- v4 landing page — adopted from the v4 design system (claude.ai/design).
     Design-system CSS vars are mapped onto the active theme tokens so the page is
     dark-mode aware; hover and responsive behaviour are pure CSS (no JS). The site
     header/footer come from the microsite template, so the mockup's own nav/footer
     are intentionally omitted. -->
<style>
.dtc-l {
  --bg:var(--dtc-bg,#f4f3ee); --band:var(--dtc-band,#efece3); --surface:var(--dtc-card,#fbfaf6);
  --ink:var(--dtc-ink,#17181c); --muted:var(--dtc-muted,#55575d); --line:var(--dtc-border,#cdc9bd);
  --accent:#1f8a5b;
  font-family:'IBM Plex Sans',sans-serif; color:var(--ink); margin:-1.5rem -15px 0;
}
.dtc-l a { text-decoration:none; }
.dtc-l .sg { font-family:'Space Grotesk',sans-serif; }
.dtc-l .mono { font-family:'IBM Plex Mono',monospace; }
.dtc-l .cta:hover { filter:brightness(1.08); transform:translateY(-1px); }
.dtc-l .ghost:hover { border-color:var(--accent) !important; color:var(--accent) !important; }
.dtc-l .ecocard:hover { transform:translateY(-3px); border-color:var(--accent) !important; }
.dtc-l .fcell:hover { background:var(--surface); }
.dtc-l .lnk:hover { opacity:.7; }
.dtc-l .navlink:hover { color:var(--ink) !important; }
@media (max-width:860px){
  .dtc-l .hero-grid, .dtc-l .qs-grid { grid-template-columns:1fr !important; }
  .dtc-l .pipe-grid { grid-template-columns:1fr 1fr !important; }
  .dtc-l .feat-grid, .dtc-l .eco-grid { grid-template-columns:1fr !important; }
}
@keyframes dtcblink { 0%,50%{opacity:1} 51%,100%{opacity:0} }
</style>

<div class="dtc-l">

  <!-- ===== HERO ===== -->
  <section id="top" style="max-width:1200px;margin:0 auto;padding:56px 24px 64px">
    <div class="hero-grid" style="display:grid;grid-template-columns:1.06fr 0.94fr;gap:48px;align-items:center">
      <div>
        <div class="mono" style="font-size:12.5px;letter-spacing:.22em;text-transform:uppercase;color:var(--accent);display:flex;align-items:center;gap:12px;margin-bottom:24px">
          <span style="width:26px;height:1px;background:var(--accent);display:inline-block"></span>docs-as-code
        </div>
        <h1 class="sg" style="font-weight:700;font-size:clamp(40px,5.4vw,66px);line-height:1.03;letter-spacing:-0.025em;margin:0 0 22px;color:var(--ink)">
          Your docs, as code &mdash;<br>from keystroke<br>to <span style="color:var(--accent)">publish.</span>
        </h1>
        <p style="font-size:18px;line-height:1.6;color:var(--muted);max-width:480px;margin:0 0 32px">
          docToolchain v4 runs directly on the JVM &mdash; no Gradle, no daemon, startup in <strong style="color:var(--ink);font-weight:600">2&ndash;3 seconds</strong>. One wrapper script, one config file, powerful output.
        </p>
        <div style="display:flex;gap:14px;flex-wrap:wrap;margin-bottom:38px">
          <a class="cta mono" href="010_manual/20_install.html" style="display:inline-flex;align-items:center;gap:9px;background:var(--accent);color:#fff;font-size:14px;font-weight:500;padding:15px 26px;border-radius:8px;transition:transform .15s,filter .2s">Get Started <span style="font-size:15px">&rarr;</span></a>
          <a class="ghost mono" href="https://github.com/docToolchain/docToolchain" target="_blank" rel="noopener" style="display:inline-flex;align-items:center;gap:9px;background:transparent;color:var(--ink);font-size:14px;font-weight:500;padding:15px 26px;border-radius:8px;border:1px solid var(--line);transition:border-color .2s,color .2s">&starf; Star on GitHub</a>
        </div>
        <div class="mono" style="display:flex;gap:30px;flex-wrap:wrap;font-size:12.5px;color:var(--muted)">
          <div><span style="color:var(--ink);font-weight:600">Java 17</span> &middot; no Ruby, no Python</div>
          <div><span style="color:var(--ink);font-weight:600">Linux</span> &middot; macOS &middot; Windows</div>
        </div>
      </div>

      <!-- terminal -->
      <div style="background:#15171d;border-radius:12px;border:1px solid #272a33;box-shadow:0 24px 60px -24px rgba(20,24,40,.5);overflow:hidden">
        <div style="display:flex;align-items:center;gap:8px;padding:14px 16px;border-bottom:1px solid #23262f">
          <span style="width:11px;height:11px;border-radius:50%;background:#ff5f57"></span>
          <span style="width:11px;height:11px;border-radius:50%;background:#febc2e"></span>
          <span style="width:11px;height:11px;border-radius:50%;background:#28c840"></span>
          <span class="mono" style="font-size:11.5px;color:#6b7080;margin-left:10px">~/my-project &mdash; dtcw4</span>
        </div>
        <div class="mono" style="padding:22px 22px 26px;font-size:13px;line-height:2;color:#cdd2dc">
          <div><span style="color:#5b8cff">&#36;</span> curl -Lo dtcw4 https://doctoolchain.org/dtcw4</div>
          <div><span style="color:#5b8cff">&#36;</span> chmod +x dtcw4</div>
          <div><span style="color:#5b8cff">&#36;</span> ./dtcw4 local install doctoolchain</div>
          <div style="color:#5a6072">  &#8627; docToolchain ready in 2.4s</div>
          <div><span style="color:#5b8cff">&#36;</span> ./dtcw4 local <span style="color:#3ddc97">generateSite</span></div>
          <div style="color:#3ddc97">  &#10003; site published &rarr; build/microsite</div>
          <div style="height:6px"></div>
          <div><span style="color:#5b8cff">&#36;</span> <span style="background:#3ddc97;width:9px;height:17px;display:inline-block;vertical-align:-3px;animation:dtcblink 1.1s steps(1) infinite"></span></div>
        </div>
      </div>
    </div>
  </section>

  <!-- ===== PIPELINE ===== -->
  <section id="pipeline" style="border-top:1px solid var(--line);background:var(--band)">
    <div style="max-width:1200px;margin:0 auto;padding:68px 24px 76px">
      <div style="display:flex;align-items:flex-end;justify-content:space-between;gap:24px;flex-wrap:wrap;margin-bottom:44px">
        <h2 class="sg" style="font-weight:600;font-size:clamp(28px,3.4vw,40px);letter-spacing:-0.02em;line-height:1.08;margin:0;max-width:560px;color:var(--ink)">One toolchain, from keystroke to publish</h2>
        <p class="mono" style="font-size:13px;color:var(--muted);max-width:300px;margin:0;line-height:1.7">The best open-source tools wired into a single, reproducible flow.</p>
      </div>
      <div class="pipe-grid" style="display:grid;grid-template-columns:repeat(4,1fr);gap:0;border:1px solid var(--line);border-radius:12px;overflow:hidden;background:var(--surface)">
        <div style="padding:30px 26px;border-right:1px solid var(--line)">
          <div class="mono" style="font-size:12px;letter-spacing:.16em;text-transform:uppercase;color:var(--accent);margin-bottom:18px">01 &middot; write</div>
          <div style="font-size:34px;margin-bottom:14px;line-height:1">&#128221;</div>
          <div class="sg" style="font-weight:600;font-size:22px;letter-spacing:-0.01em;margin-bottom:6px;color:var(--ink)">AsciiDoc</div>
          <div style="font-size:13.5px;color:var(--muted);line-height:1.55">Author in plain text, versioned in Git like the rest of your code.</div>
        </div>
        <div style="padding:30px 26px;border-right:1px solid var(--line)">
          <div class="mono" style="font-size:12px;letter-spacing:.16em;text-transform:uppercase;color:var(--accent);margin-bottom:18px">02 &middot; render</div>
          <div style="font-size:34px;margin-bottom:14px;line-height:1">&#127912;</div>
          <div class="sg" style="font-weight:600;font-size:22px;letter-spacing:-0.01em;margin-bottom:6px;color:var(--ink)">Asciidoctor</div>
          <div style="font-size:13.5px;color:var(--muted);line-height:1.55">Convert to HTML, PDF and slides with AsciidoctorJ on the JVM.</div>
        </div>
        <div style="padding:30px 26px;border-right:1px solid var(--line)">
          <div class="mono" style="font-size:12px;letter-spacing:.16em;text-transform:uppercase;color:var(--accent);margin-bottom:18px">03 &middot; diagram</div>
          <div style="font-size:34px;margin-bottom:14px;line-height:1">&#128208;</div>
          <div class="sg" style="font-weight:600;font-size:22px;letter-spacing:-0.01em;margin-bottom:6px;color:var(--ink)">PlantUML</div>
          <div style="font-size:13.5px;color:var(--muted);line-height:1.55">Generate architecture diagrams straight from text definitions.</div>
        </div>
        <div style="padding:30px 26px">
          <div class="mono" style="font-size:12px;letter-spacing:.16em;text-transform:uppercase;color:var(--accent);margin-bottom:18px">04 &middot; publish</div>
          <div style="font-size:34px;margin-bottom:14px;line-height:1">&#128640;</div>
          <div class="sg" style="font-weight:600;font-size:22px;letter-spacing:-0.01em;margin-bottom:6px;color:var(--ink)">Confluence</div>
          <div style="font-size:13.5px;color:var(--muted);line-height:1.55">Ship a microsite or push docs directly into your team wiki.</div>
        </div>
      </div>
    </div>
  </section>

  <!-- ===== FEATURES ===== -->
  <section id="features" style="max-width:1200px;margin:0 auto;padding:80px 24px">
    <div style="margin-bottom:48px;max-width:620px">
      <div class="mono" style="font-size:12.5px;letter-spacing:.22em;text-transform:uppercase;color:var(--accent);margin-bottom:18px">Rebuilt for the GenAI age</div>
      <h2 class="sg" style="font-weight:600;font-size:clamp(28px,3.6vw,42px);letter-spacing:-0.02em;line-height:1.06;margin:0;color:var(--ink)">Every task reviewed. Legacy retired. Modern ones added.</h2>
    </div>
    <div class="feat-grid" style="display:grid;grid-template-columns:repeat(3,1fr);border-top:1px solid var(--line);border-left:1px solid var(--line)">
      <div class="fcell" style="padding:34px 30px 38px;border-right:1px solid var(--line);border-bottom:1px solid var(--line);min-height:190px;transition:background .15s">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:22px"><span class="mono" style="font-size:12px;color:var(--accent);font-weight:600">01</span><span style="font-size:18px;opacity:.85">&#9889;</span></div>
        <h3 class="sg" style="font-weight:600;font-size:18.5px;letter-spacing:-0.01em;margin:0 0 10px;color:var(--ink)">Fast Startup</h3>
        <p style="font-size:14px;line-height:1.6;color:var(--muted);margin:0">Direct JVM execution &mdash; no Gradle, no daemon. Tasks start in 2&ndash;3 seconds instead of 15&ndash;30.</p>
      </div>
      <div class="fcell" style="padding:34px 30px 38px;border-right:1px solid var(--line);border-bottom:1px solid var(--line);min-height:190px;transition:background .15s">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:22px"><span class="mono" style="font-size:12px;color:var(--accent);font-weight:600">02</span><span style="font-size:18px;opacity:.85">&#129302;</span></div>
        <h3 class="sg" style="font-weight:600;font-size:18.5px;letter-spacing:-0.01em;margin:0 0 10px;color:var(--ink)">LLM-native</h3>
        <p style="font-size:14px;line-height:1.6;color:var(--muted);margin:0">Structured, section-level document access over MCP via dacli, so an AI can read and edit docs precisely.</p>
      </div>
      <div class="fcell" style="padding:34px 30px 38px;border-right:1px solid var(--line);border-bottom:1px solid var(--line);min-height:190px;transition:background .15s">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:22px"><span class="mono" style="font-size:12px;color:var(--accent);font-weight:600">03</span><span style="font-size:18px;opacity:.85">&#127822;</span></div>
        <h3 class="sg" style="font-weight:600;font-size:18.5px;letter-spacing:-0.01em;margin:0 0 10px;color:var(--ink)">Native Apple Silicon</h3>
        <p style="font-size:14px;line-height:1.6;color:var(--muted);margin:0">Runs natively on ARM64 Macs &mdash; no Rosetta, no OrientDB/JNI workarounds. Just JVM and AsciidoctorJ.</p>
      </div>
      <div class="fcell" style="padding:34px 30px 38px;border-right:1px solid var(--line);border-bottom:1px solid var(--line);min-height:190px;transition:background .15s">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:22px"><span class="mono" style="font-size:12px;color:var(--accent);font-weight:600">04</span><span style="font-size:18px;opacity:.85">&#128737;</span></div>
        <h3 class="sg" style="font-weight:600;font-size:18.5px;letter-spacing:-0.01em;margin:0 0 10px;color:var(--ink)">Modern, Secure Deps</h3>
        <p style="font-size:14px;line-height:1.6;color:var(--muted);margin:0">Every dependency upgraded to current versions &mdash; fewer known CVEs, continuously scanned with Trivy.</p>
      </div>
      <div class="fcell" style="padding:34px 30px 38px;border-right:1px solid var(--line);border-bottom:1px solid var(--line);min-height:190px;transition:background .15s">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:22px"><span class="mono" style="font-size:12px;color:var(--accent);font-weight:600">05</span><span style="font-size:18px;opacity:.85">&#128230;</span></div>
        <h3 class="sg" style="font-weight:600;font-size:18.5px;letter-spacing:-0.01em;margin:0 0 10px;color:var(--ink)">One Script Setup</h3>
        <p style="font-size:14px;line-height:1.6;color:var(--muted);margin:0">Just dtcw4 in your project. It installs docToolchain and runs all tasks &mdash; no other files needed.</p>
      </div>
      <div class="fcell" style="padding:34px 30px 38px;border-right:1px solid var(--line);border-bottom:1px solid var(--line);min-height:190px;transition:background .15s">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:22px"><span class="mono" style="font-size:12px;color:var(--accent);font-weight:600">06</span><span style="font-size:18px;opacity:.85">&#128421;</span></div>
        <h3 class="sg" style="font-weight:600;font-size:18.5px;letter-spacing:-0.01em;margin:0 0 10px;color:var(--ink)">Cross-Platform</h3>
        <p style="font-size:14px;line-height:1.6;color:var(--muted);margin:0">Linux, macOS and Windows (WSL2). No Ruby, no Python, no native deps &mdash; just Java 17.</p>
      </div>
    </div>
  </section>

  <!-- ===== QUICK START ===== -->
  <section id="start" style="border-top:1px solid var(--line);background:var(--band)">
    <div class="qs-grid" style="max-width:1200px;margin:0 auto;padding:76px 24px;display:grid;grid-template-columns:1fr 1.15fr;gap:48px;align-items:center">
      <div>
        <div class="mono" style="font-size:12.5px;letter-spacing:.22em;text-transform:uppercase;color:var(--accent);margin-bottom:18px">Quick start</div>
        <h2 class="sg" style="font-weight:600;font-size:clamp(28px,3.4vw,40px);letter-spacing:-0.02em;line-height:1.08;margin:0 0 18px;color:var(--ink)">Four lines. One file in your repo.</h2>
        <p style="font-size:16px;line-height:1.65;color:var(--muted);margin:0 0 28px;max-width:420px">Just <code class="mono" style="font-size:14px;background:var(--surface);border:1px solid var(--line);padding:1px 6px;border-radius:5px;color:var(--ink)">dtcw4</code> in your project. It installs docToolchain and runs every task &mdash; no other files needed.</p>
        <a class="lnk mono" href="010_manual/20_install.html" style="display:inline-flex;align-items:center;gap:9px;font-size:14px;font-weight:500;color:var(--accent);transition:opacity .15s">Read the install guide &rarr;</a>
      </div>
      <div style="background:#15171d;border-radius:12px;border:1px solid #272a33;overflow:hidden;box-shadow:0 24px 60px -28px rgba(20,24,40,.45)">
        <div style="display:flex;align-items:center;justify-content:space-between;padding:13px 18px;border-bottom:1px solid #23262f">
          <span class="mono" style="font-size:11.5px;color:#6b7080">install.sh</span>
        </div>
        <pre class="mono" style="margin:0;padding:24px 22px;font-size:13.5px;line-height:2.05;color:#cdd2dc;white-space:pre-wrap"><span style="color:#5a6072"># grab the wrapper</span>
<span style="color:#5b8cff">curl</span> -Lo dtcw4 https://doctoolchain.org/dtcw4
<span style="color:#5b8cff">chmod</span> +x dtcw4

<span style="color:#5a6072"># install + build your site</span>
<span style="color:#5b8cff">./dtcw4</span> local install doctoolchain
<span style="color:#5b8cff">./dtcw4</span> local <span style="color:#3ddc97">generateSite</span></pre>
      </div>
    </div>
  </section>

  <!-- ===== ECOSYSTEM ===== -->
  <section id="ecosystem" style="max-width:1200px;margin:0 auto;padding:80px 24px 86px">
    <div style="display:flex;align-items:flex-end;justify-content:space-between;gap:24px;flex-wrap:wrap;margin-bottom:42px">
      <div style="max-width:560px">
        <div class="mono" style="font-size:12.5px;letter-spacing:.22em;text-transform:uppercase;color:var(--accent);margin-bottom:16px">Ecosystem</div>
        <h2 class="sg" style="font-weight:600;font-size:clamp(28px,3.4vw,40px);letter-spacing:-0.02em;line-height:1.08;margin:0;color:var(--ink)">More than a single tool</h2>
      </div>
      <p class="mono" style="font-size:13px;color:var(--muted);max-width:280px;margin:0;line-height:1.7">A growing set of projects for your docs-as-code journey.</p>
    </div>
    <div class="eco-grid" style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px">
      <a class="ecocard" href="https://doctoolchain.org/asciidoc-linter" style="display:block;background:var(--surface);border:1px solid var(--line);border-radius:11px;padding:26px 24px 22px;transition:transform .15s ease,border-color .15s ease">
        <div class="mono" style="font-size:11.5px;letter-spacing:.04em;color:var(--accent);margin-bottom:14px">lint</div>
        <h3 class="sg" style="font-weight:600;font-size:19px;letter-spacing:-0.01em;margin:0 0 9px;color:var(--ink)">AsciiDoc Linter</h3>
        <p style="font-size:13.5px;line-height:1.55;color:var(--muted);margin:0 0 18px">Keep your documentation clean and consistent with a dedicated AsciiDoc linter.</p>
        <span class="mono" style="font-size:12.5px;color:var(--ink)">learn more &rarr;</span>
      </a>
      <a class="ecocard" href="https://doctoolchain.org/dacli" style="display:block;background:var(--surface);border:1px solid var(--line);border-radius:11px;padding:26px 24px 22px;transition:transform .15s ease,border-color .15s ease">
        <div class="mono" style="font-size:11.5px;letter-spacing:.04em;color:var(--accent);margin-bottom:14px">cli</div>
        <h3 class="sg" style="font-weight:600;font-size:19px;letter-spacing:-0.01em;margin:0 0 9px;color:var(--ink)">dacli</h3>
        <p style="font-size:13.5px;line-height:1.55;color:var(--muted);margin:0 0 18px">A command-line interface to simplify and streamline your docs-as-code workflow.</p>
        <span class="mono" style="font-size:12.5px;color:var(--ink)">learn more &rarr;</span>
      </a>
      <a class="ecocard" href="https://doctoolchain.org/Bausteinsicht/" style="display:block;background:var(--surface);border:1px solid var(--line);border-radius:11px;padding:26px 24px 22px;transition:transform .15s ease,border-color .15s ease">
        <div class="mono" style="font-size:11.5px;letter-spacing:.04em;color:var(--accent);margin-bottom:14px">arch</div>
        <h3 class="sg" style="font-weight:600;font-size:19px;letter-spacing:-0.01em;margin:0 0 9px;color:var(--ink)">Bausteinsicht</h3>
        <p style="font-size:13.5px;line-height:1.55;color:var(--muted);margin:0 0 18px">Architecture-as-code: define building blocks in JSONC, sync with draw.io diagrams.</p>
        <span class="mono" style="font-size:12.5px;color:var(--ink)">learn more &rarr;</span>
      </a>
      <a class="ecocard" href="https://github.com/docToolchain/diagrams.net-intellij-plugin" style="display:block;background:var(--surface);border:1px solid var(--line);border-radius:11px;padding:26px 24px 22px;transition:transform .15s ease,border-color .15s ease">
        <div class="mono" style="font-size:11.5px;letter-spacing:.04em;color:var(--accent);margin-bottom:14px">plugin</div>
        <h3 class="sg" style="font-weight:600;font-size:19px;letter-spacing:-0.01em;margin:0 0 9px;color:var(--ink)">diagrams.net for IntelliJ</h3>
        <p style="font-size:13.5px;line-height:1.55;color:var(--muted);margin:0 0 18px">Edit draw.io diagrams directly inside IntelliJ IDEA without leaving your IDE.</p>
        <span class="mono" style="font-size:12.5px;color:var(--ink)">learn more &rarr;</span>
      </a>
      <a class="ecocard" href="https://github.com/docToolchain/iSAQB-Template" style="display:block;background:var(--surface);border:1px solid var(--line);border-radius:11px;padding:26px 24px 22px;transition:transform .15s ease,border-color .15s ease">
        <div class="mono" style="font-size:11.5px;letter-spacing:.04em;color:var(--accent);margin-bottom:14px">template</div>
        <h3 class="sg" style="font-weight:600;font-size:19px;letter-spacing:-0.01em;margin:0 0 9px;color:var(--ink)">iSAQB Template</h3>
        <p style="font-size:13.5px;line-height:1.55;color:var(--muted);margin:0 0 18px">A ready-to-use template following the iSAQB arc42 architecture documentation standard.</p>
        <span class="mono" style="font-size:12.5px;color:var(--ink)">learn more &rarr;</span>
      </a>
      <a class="ecocard" href="https://github.com/docToolchain/ci-cd-demo" style="display:block;background:var(--surface);border:1px solid var(--line);border-radius:11px;padding:26px 24px 22px;transition:transform .15s ease,border-color .15s ease">
        <div class="mono" style="font-size:11.5px;letter-spacing:.04em;color:var(--accent);margin-bottom:14px">demo</div>
        <h3 class="sg" style="font-weight:600;font-size:19px;letter-spacing:-0.01em;margin:0 0 9px;color:var(--ink)">CI/CD Demo</h3>
        <p style="font-size:13.5px;line-height:1.55;color:var(--muted);margin:0 0 18px">A demo showing how to integrate docToolchain into your CI/CD pipeline for automated builds.</p>
        <span class="mono" style="font-size:12.5px;color:var(--ink)">learn more &rarr;</span>
      </a>
    </div>
  </section>

</div>
    </main>
</div>
