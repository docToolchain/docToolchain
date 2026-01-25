# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**docToolchain** is an open-source documentation generation tool implementing the "docs-as-code" approach. It's built with Groovy/Java using Gradle and targets cross-platform deployment (Linux, macOS, Windows).

- **Current Version**: 3.4.2 (ng branch)
- **Primary Language**: Groovy with Java 17
- **Build System**: Gradle 8.1.1
- **Primary Interface**: `dtcw` wrapper script (NOT direct Gradle commands)

## Essential Commands

### Build and Test

```bash
# Build the project (expect ~11 test failures out of 53 - this is normal)
./gradlew clean build

# Run core tests only (should always pass - 66 tests, 3 skipped)
./gradlew core:test

# Run all tests (some failures expected if external tools missing)
./gradlew test

# Check for dependency updates
./gradlew dependencyUpdates
```

### Using dtcw Wrapper (Primary Interface)

**CRITICAL**: Always use `./dtcw` for docToolchain operations, not direct Gradle commands.

```bash
# Check version and status
./dtcw --version

# List all docToolchain tasks
./dtcw tasks --group doctoolchain

# Install Java 17 locally
./dtcw local install java

# Install docToolchain locally
./dtcw local install doctoolchain

# Generate documentation (basic functionality test)
./dtcw local generateHTML    # Output: build/html5/manual_test_script.html (~37KB)
./dtcw local generatePDF     # Output: build/pdf/manual_test_script.pdf (~62KB)

# Generate complete microsite
./dtcw local generateSite

# Preview microsite
./dtcw local previewSite
```

### Development Workflow

```bash
# 1. Validate shell scripts
find . -name "*.sh" -exec shellcheck {} \;

# 2. Run full CI pipeline locally
./.ci.sh

# 3. Build core module and create distribution
./gradlew core:jar
./gradlew prepareDist
./gradlew createDist
```

### Running Tests

```bash
# Unit tests (Spock framework in core module)
./gradlew core:test --info

# Integration tests (BATS - requires bats-core installed)
cd test && bats *.bats

# Test specific environment
bats test/local_environment.bats
bats test/docker_environment.bats
bats test/sdk_installation.bats
```

## Architecture and Code Organization

### Directory Structure

```
docToolchain/
├── core/                          # Core Java/Groovy library
│   ├── src/main/groovy/org/docToolchain/
│   │   ├── tasks/                 # Gradle task implementations
│   │   ├── atlassian/             # Jira & Confluence integration
│   │   │   ├── jira/              # Jira client, converters, utilities
│   │   │   └── confluence/        # Confluence publishing
│   │   ├── configuration/         # Configuration handling
│   │   └── scripts/               # Core script utilities
│   └── src/test/groovy/           # Spock unit tests
├── scripts/                       # Gradle task scripts (plugins)
│   ├── AsciiDocBasics.gradle      # Core document generation
│   ├── publishToConfluence.gradle # Confluence publishing
│   ├── exportJiraIssues.gradle    # Jira integration
│   ├── exportEA.gradle            # Enterprise Architect export
│   ├── generateSite.gradle        # Microsite generation (jBake)
│   └── [20+ other task scripts]
├── src/
│   ├── docs/                      # Documentation sources (AsciiDoc)
│   │   ├── 010_manual/            # User manual
│   │   ├── 015_tasks/             # Task documentation
│   │   ├── 020_tutorial/          # Tutorials
│   │   └── images/                # Image assets
│   ├── site/                      # Microsite templates (jBake)
│   └── test/                      # Test resources
├── test/                          # BATS integration tests
├── bin/                           # Shell scripts (doctoolchain, autobuildSite.bash)
├── template_config/               # Default configuration templates
├── dtcw, dtcw.ps1, dtcw.bat      # Cross-platform wrapper scripts
├── build.gradle                   # Main build configuration
├── docToolchainConfig.groovy      # Project configuration
├── Config.groovy                  # Alternative configuration file
└── libs.versions.toml             # Dependency version catalog
```

### Core Architecture

#### 1. Multi-Layer Build System
- **Wrapper Layer**: `dtcw` scripts abstract environment complexity (local/docker/sdk)
- **Gradle Layer**: Main build orchestration in `build.gradle`
- **Core Module**: Standalone library in `/core` with task implementations
- **Script Plugins**: Modular Gradle scripts in `/scripts` directory

#### 2. Task Organization
All tasks are implemented in `/scripts/*.gradle` files and applied in `build.gradle`:
- Each script file represents a feature/capability (e.g., `exportJiraIssues.gradle`)
- Tasks use core library classes from `org.docToolchain.tasks.*`
- Configuration-driven via `docToolchainConfig.groovy` or `Config.groovy`

#### 3. Core Package Structure
```
org.docToolchain/
├── tasks/                         # Task base classes
│   ├── DocToolchainTask.groovy    # Base task
│   ├── AbstractConfluenceTask     # Confluence operations
│   ├── ExportJiraIssuesTask       # Jira export
│   └── [other task implementations]
├── atlassian/                     # External integrations
│   ├── jira/
│   │   ├── JiraService            # Main Jira service
│   │   ├── clients/               # REST clients
│   │   ├── converter/             # Format converters (AsciiDoc, Excel)
│   │   └── utils/                 # Date utilities, etc.
│   └── confluence/                # Confluence integration
├── configuration/                 # Config handling
└── scripts/                       # Utility scripts
```

#### 4. Configuration System
Two configuration files supported:
- **docToolchainConfig.groovy**: Primary config (Groovy DSL)
- **Config.groovy**: Alternative/legacy config
- **gradle.properties**: Build-time settings (Java version, memory, paths)

Key configuration sections:
- `inputPath`: Where to find source documents
- `outputPath`: Where to write generated docs
- `inputFiles`: List of files to process with output formats
- `imageDirs`: Image resource directories
- Task-specific configs: `jira`, `confluence`, `exportChangelog`, etc.

#### 5. Execution Environments
docToolchain supports three execution modes:
1. **local**: Installed in `$HOME/.doctoolchain`
2. **docker**: Container-based execution
3. **sdk**: SDKMAN-based installation

The `dtcw` wrapper automatically handles environment selection and setup.

## Key Dependencies and Technologies

### Primary Dependencies (libs.versions.toml)
- **Groovy 3.0.13**: Core language
- **AsciiDoctor 4.0.4**: Document processing
- **jBake 5.5.0**: Static site generation
- **Apache POI 5.3.0**: Excel integration
- **Jsoup 1.18.1**: HTML parsing
- **Apache HttpClient 5.3**: REST client for Jira/Confluence
- **Spock 2.3**: Testing framework
- **PlantUML/Graphviz**: Diagram generation

### Gradle Plugins
- `org.jbake.site`: Static site generation
- `org.aim42.htmlSanityCheck`: HTML validation
- `com.github.ben-manes.versions`: Dependency updates
- `org.openapi.generator`: OpenAPI integration
- `com.github.johnrengelman.shadow`: Fat JAR creation (core module)

## Testing Strategy

### Unit Tests (Spock)
- Located in `core/src/test/groovy/`
- Use Spock framework with Groovy
- **Expected behavior**: Core tests should always pass (66 tests, 3 skipped)
- Run with: `./gradlew core:test`

### Integration Tests (Project-level)
- Located in root test directory (Spock)
- **Expected behavior**: ~11 failures out of 53 tests is NORMAL
- Failures typically relate to missing external tools (Pandoc, Enterprise Architect)
- Run with: `./gradlew test`

### End-to-End Tests (BATS)
- Located in `/test/*.bats`
- Bash Automated Testing System
- Test wrapper functionality across environments
- Examples: `local_environment.bats`, `docker_environment.bats`, `sdk_installation.bats`
- Requires `bats-core` to be installed

## CI/CD Pipeline

### GitHub Actions Workflows (`.github/workflows/`)
- **default-build.yml**: Main CI (Java 17, Ubuntu, runs `.ci.sh`)
- **dtcw-tests.yaml**: Tests dtcw wrapper (bash)
- **dtcw.ps1-tests.yaml**: Tests PowerShell wrapper
- **codeql.yml**: Security analysis
- **sdkman.yml**: SDKMAN release automation

### CI Script (`.ci.sh`)
Execution phases:
1. **cleaning**: `./gradlew clean`
2. **dependency_info**: Check for updates
3. **unit_tests**: Run core and project tests
4. **create_doc**: Generate documentation (on ng/main-2.x branches with Java 17)
5. **publish_doc**: Publish to GitHub Pages (specific branches only)

### CI Dependencies
```bash
sudo apt-get install -y graphviz shellcheck pandoc powershell
```

## Important Timing Expectations

**CRITICAL**: Set long timeouts (600+ seconds) for builds and tests. Do NOT cancel long-running tasks.

- **First-time task listing**: ~95 seconds (downloads Gradle wrapper)
- **Core tests**: ~11 seconds (should always pass)
- **Full build with tests**: 3-5 minutes (11 failures expected)
- **generateHTML**: 13-19 seconds
- **generatePDF**: 19-20 seconds
- **Full CI pipeline**: 3-4 minutes

## Common Development Tasks

### Adding a New Export Task
1. Create new Gradle script in `/scripts/exportMyFeature.gradle`
2. Implement task class in `core/src/main/groovy/org/docToolchain/tasks/`
3. Add `apply from: 'scripts/exportMyFeature.gradle'` to `build.gradle`
4. Add configuration section to `docToolchainConfig.groovy`
5. Add tests in `core/src/test/groovy/`
6. Document in `/src/docs/015_tasks/`

### Modifying Core Functionality
1. Edit source in `core/src/main/groovy/org/docToolchain/`
2. Add/update unit tests in `core/src/test/groovy/`
3. Build core module: `./gradlew core:jar`
4. Test changes: `./gradlew core:test`
5. Integration test: `./dtcw local generateHTML`

### Updating Documentation
- Documentation sources: `src/docs/` (AsciiDoc format)
- Generate locally: `./dtcw local generateHTML`
- Output: `build/html5/` and `build/pdf/`
- Microsite: `./dtcw local generateSite` → `build/microsite/output/`

## Critical Configuration Files

### gradle.properties
- **dtc_version**: Current version (3.4.2)
- **org.gradle.jvmargs**: JVM settings (-Xmx2048m for PDF generation)
- **docDir**: Document directory (default: `.`)
- **inputPath**: Source path (default: `.`)

### docToolchainConfig.groovy
- **outputPath**: Where to write generated docs
- **inputPath**: Relative path to source documents
- **inputFiles**: Array of `[file: 'name.adoc', formats: ['html','pdf']]`
- **imageDirs**: Image resource directories
- **microsite**: Microsite configuration (jBake)
- Task-specific sections: `jira`, `confluence`, `exportChangelog`, etc.

### libs.versions.toml
- Centralized dependency version management
- Three sections: `[versions]`, `[plugins]`, `[libraries]`
- Referenced in build files as `libs.groovy.all`, `libs.plugins.jbake.site`, etc.

## Common Pitfalls and Solutions

### 1. "Task not found" Error
- **Cause**: Using `gradle` or `./gradlew` instead of `./dtcw`
- **Fix**: Always use `./dtcw [task]` for docToolchain tasks

### 2. Java Version Errors
- **Cause**: Wrong Java version (must be exactly Java 17)
- **Fix**: `./dtcw local install java` or set JAVA_HOME to Java 17

### 3. Test Failures
- **Normal**: ~11 test failures in main project (external tools missing)
- **Problem**: Core tests failing (should never fail)
- **Fix**: Check Java version, dependencies, or investigate specific failure

### 4. Memory Issues During PDF Generation
- **Already configured**: `gradle.properties` sets `-Xmx2048m`
- **If still occurring**: Increase in `org.gradle.jvmargs`

### 5. Docker Permission/Execution Errors
- Ensure Docker is running
- Check user permissions for Docker
- Verify `dtcw_docker.env` if using proxy

### 6. Build Output Locations
- HTML: `build/html5/` (direct generation) or `build/microsite/output/` (site)
- PDF: `build/pdf/`
- DocBook: `build/docbook/`

## Validation Checklist

Before committing changes:

```bash
# 1. Validate shell scripts
find . -name "*.sh" -exec shellcheck {} \;

# 2. Run core tests (must pass)
./gradlew core:test

# 3. Test basic functionality
./dtcw --version
./dtcw local generateHTML
./dtcw local generatePDF

# 4. Verify outputs exist
ls -la build/html5/manual_test_script.html
ls -la build/pdf/manual_test_script.pdf

# 5. Run full build (11 failures expected)
./gradlew clean build
```

## External Integration Points

### Atlassian (Jira & Confluence)
- **Jira**: Export issues via REST API, convert to AsciiDoc/Excel
- **Confluence**: Publish documentation directly, supports new editor
- Configuration via environment variables or config files
- Use API tokens, not passwords

### Enterprise Tools
- **Enterprise Architect**: Export diagrams via COM automation (Windows)
- **Visio**: Export diagrams to images
- **PowerPoint**: Extract slides as images
- **Excel**: Convert tables to AsciiDoc

### Diagram Tools
- **PlantUML**: Embedded diagram generation
- **Graphviz**: Graph visualization
- **Structurizr**: Architecture diagrams (DSL support)

### Version Control
- **Git**: Export changelog, contributor lists
- Supports git-based metadata extraction

## Special Notes

### Wrapper Script (dtcw) Architecture
The `dtcw` wrapper is the primary interface and handles:
- Environment detection (local/docker/sdk)
- Java version management
- Dependency downloading
- Task delegation
- Platform-specific execution (bash, PowerShell, batch)

### Distribution Creation
```bash
# Create release distribution
./gradlew prepareDist createDist
# Output: build/docToolchain-3.4.2.zip
```

### Module Isolation
When docToolchain is used as a submodule:
- Only tasks in groups 'docToolchain', 'Documentation', 'docToolchain helper' are enabled
- Prevents interference with parent project tasks
- Requires `docDir` to point to actual documentation (not `.`)

### Headless Mode Support
Recent addition supports non-interactive operation for CI/CD pipelines.
