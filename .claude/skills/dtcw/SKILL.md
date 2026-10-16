---
name: dtcw
description: "docToolchain Wrapper — generate HTML/PDF, publish to Confluence, export from Jira, and manage docs-as-code workflows via docToolchain."
user-invocable: true
---

# dtcw — docToolchain Wrapper

Use the `dtcw` command to generate documentation, publish to Confluence, export from Jira, and manage docs-as-code workflows via docToolchain.

## Prerequisites

The `dtcw` wrapper auto-detects the execution environment (in priority order):

1. **compose**: `docker-compose.yml` with doctoolchain service + compose command
2. **devcontainer**: `.devcontainer/devcontainer.json` with doctoolchain image + CLI
3. **local**: docToolchain installed in `$HOME/.doctoolchain`
4. **sdk**: docToolchain installed via SDKMAN!
5. **docker**: one-shot container via `docker run`

Force a specific environment: `./dtcw <environment> <task>`

## Tasks

### Generate Documentation

```bash
# Generate HTML output
dtcw generateHTML

# Generate PDF output
dtcw generatePDF

# Generate microsite with jBake
dtcw generateSite

# Preview microsite locally
dtcw previewSite

# Generate DocBook XML
dtcw generateDocBook

# Generate reveal.js slide deck
dtcw generateDeck
```

### Publish and Integrate

```bash
# Publish documentation to Confluence
dtcw publishToConfluence

# Export Jira issues to AsciiDoc
dtcw exportJiraIssues

# Export Jira sprint changelog
dtcw exportJiraSprintChangelog

# Export Confluence pages to AsciiDoc
dtcw exportConfluence
```

### Export from External Tools

```bash
# Export from Enterprise Architect
dtcw exportEA

# Export Excel tables to AsciiDoc
dtcw exportExcel

# Export PowerPoint slides as images
dtcw exportPPT

# Export Visio diagrams
dtcw exportVisio

# Export draw.io diagrams
dtcw exportDrawIo

# Export Structurizr architecture diagrams
dtcw exportStructurizr

# Export OpenAPI spec
dtcw exportOpenApi

# Export Markdown to AsciiDoc
dtcw exportMarkdown
```

### Utilities

```bash
# Export git changelog
dtcw exportChangeLog

# Export git contributors
dtcw exportContributors

# Collect all includes into a single list
dtcw collectIncludes

# Convert to DOCX via Pandoc
dtcw convertToDocx

# Convert to EPUB
dtcw convertToEpub

# Fix file encoding issues
dtcw fixEncoding

# HTML sanity check (broken links etc.)
dtcw htmlSanityCheck

# Download arc42/req42 template
dtcw downloadTemplate

# Copy site themes
dtcw copyThemes

# List all available tasks
dtcw tasks
```

### Installation (local/project-home mode only)

```bash
# Install docToolchain locally
dtcw install doctoolchain

# Install Java 17 locally
dtcw install java
```

## Usage Patterns

### Before generating documentation

When the user asks to build or preview documentation:

```bash
# Check what tasks are available
dtcw tasks

# Generate HTML for quick preview
dtcw generateHTML
# Output: build/html5/

# Generate PDF for distribution
dtcw generatePDF
# Output: build/pdf/
```

### Working with Confluence

```bash
# Publish current docs to Confluence
dtcw publishToConfluence

# Export existing Confluence pages to AsciiDoc
dtcw exportConfluence
```

### Working with Jira

```bash
# Export issues referenced in documentation
dtcw exportJiraIssues

# Export sprint changelog
dtcw exportJiraSprintChangelog
```

### Full site generation

```bash
# Generate complete microsite
dtcw generateSite

# Preview in browser
dtcw previewSite
```

### Docker Compose workflow (validate-first)

When a docker-compose.yml with both adoc and doctoolchain services exists:

```bash
# Validate AsciiDoc first (via adcw if available)
adcw validate -i Docs/main.adoc --strict

# Then build (auto-detects compose environment)
dtcw generatePDF
dtcw generateHTML

# Or force compose explicitly
dtcw compose generateHTML
```

### Bootstrap a compose or devcontainer setup

```bash
# Generate docker-compose.yml with doctoolchain service
dtcw compose install doctoolchain

# Generate .devcontainer/devcontainer.json
dtcw devcontainer install doctoolchain
```

## Error Handling

- If `dtcw` is not found: install via Homebrew (`brew install tpo42/dtcw/dtcw`) or use `./dtcw` from project directory
- If Java is missing: suggest `dtcw install java` (local mode) or use compose/docker
- If docToolchain is not installed: suggest `dtcw install doctoolchain` or use compose/docker
- Build timeouts: docToolchain builds can take 1-5 minutes — set long timeouts (600+ seconds)

## Notes

- All paths are relative to the project root
- HTML output: `build/html5/`
- PDF output: `build/pdf/`
- Microsite output: `build/microsite/output/`
- Configuration: `docToolchainConfig.groovy` (primary) or `Config.groovy` (legacy)
- In compose mode, DTC_OPTS from the container environment are appended automatically
- The wrapper is version-agnostic: works with both v3 (Gradle) and v4 (direct Groovy)
