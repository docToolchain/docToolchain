# Bundled MathJax

These files are vendored from [MathJax](https://www.mathjax.org/) **3.2.2** so the
generated microsite renders STEM (math) content fully **offline / self-contained**,
without loading anything from a CDN.

| File | Purpose |
|------|---------|
| `tex-mml-svg.js` | Combined component: TeX (`latexmath`) + MathML input, **SVG** output. SVG output embeds glyph paths, so no web-font files are fetched at runtime. |
| `input/asciimath.js` | AsciiMath (`asciimath`) input processor, loaded on demand by `tex-mml-svg.js`. |

The theme footer (`src/site/templates/footer.gsp`) loads `tex-mml-svg.js` only on
pages that actually contain math, and configures the AsciiMath delimiter (`\$…\$`)
to match what AsciiDoctor emits.

To update: download the same two files from
`https://cdn.jsdelivr.net/npm/mathjax@<version>/es5/` and keep the directory layout
(`input/asciimath.js` must stay one level below `tex-mml-svg.js`, since MathJax's
loader resolves it relative to the main script).

MathJax is licensed under the Apache License 2.0.
