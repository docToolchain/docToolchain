# Enhanced Diagram Support for docToolchain

This enhancement addresses [GitHub Issue #1480](https://github.com/docToolchain/docToolchain/issues/1480): "AsciiDoc Diagrams can't leverage Structurizr CLI"

## Problem

When using asciidoctor-diagram with external CLI tools like Structurizr CLI through docToolchain, the diagram tools were not accessible due to:

1. **Environment Variable Isolation**: The `DIAGRAMS_STRUCTURIZRCLI_HOME` environment variable was not properly propagated to the AsciidoctorJ subprocess
2. **Process Execution Context**: AsciidoctorJ runs in a forked JVM process that doesn't inherit the full environment context
3. **Extension Loading Timing**: The asciidoctor-diagram extension may load before environment variables are properly set

## Solution

This enhancement provides:

### 🔧 Core Features

- **Environment Variable Propagation**: Ensures diagram tool environment variables reach the AsciidoctorJ subprocess
- **Enhanced Fork Configuration**: Proper JVM arguments and system properties for external tools
- **Debug Support**: Comprehensive debugging and environment checking capabilities
- **Memory Optimization**: Increased default memory allocation for complex diagram processing

### 📁 Files Added

1. **`scripts/DiagramSupport.gradle`** - Main enhancement script
2. **`template_config/DiagramConfig.groovy`** - Configuration template
3. **`src/docs/015_tasks/03_task_enhanced_diagrams.adoc`** - Documentation
4. **`src/docs/015_tasks/03_task_structurizr_test.adoc`** - Test document with examples

### 🚀 New Tasks

```bash
# Check diagram environment setup
./dtcw checkDiagramEnvironment

# Clean diagram cache
./dtcw cleanDiagramCache

# Generate with verbose diagram logging
./dtcw generateHTML -Ddiagram.debug=true
```

## Quick Start

### 1. Environment Setup

Set the required environment variable:

```bash
export DIAGRAMS_STRUCTURIZRCLI_HOME=/path/to/structurizr-cli
```

### 2. Docker Setup (if using Docker)

```dockerfile
ENV DIAGRAMS_STRUCTURIZRCLI_HOME=/opt/structurizr-cli
ENV PATH="$PATH:/opt/structurizr-cli"
```

### 3. Usage in AsciiDoc

```asciidoc
[structurizr,format=svg,target=example]
----
workspace {
    model {
        user = person "User"
        system = softwareSystem "System"
        user -> system "Uses"
    }
    views {
        systemContext system {
            include *
            autoLayout lr
        }
    }
}
----
```

### 4. Verification

```bash
# Check environment
./dtcw checkDiagramEnvironment

# Generate with debug
./dtcw generateHTML -Ddiagram.debug=true
```

## Supported Tools

| Tool | Environment Variable | Purpose |
|------|---------------------|---------|
| Structurizr CLI | `DIAGRAMS_STRUCTURIZRCLI_HOME` | Software architecture diagrams |
| PlantUML | `PLANTUML_JAR` | UML and other diagrams |
| Graphviz | `GRAPHVIZ_DOT` | Graph visualizations |

## Debug Output

When using `-Ddiagram.debug=true`, expect output like:

```
docToolchain> Propagating DIAGRAMS_STRUCTURIZRCLI_HOME=/opt/structurizr-cli
docToolchain> Document attribute set: structurizr-cli-path=/opt/structurizr-cli
docToolchain> Created diagram cache directory: /project/build/diagrams-cache

=== docToolchain Diagram Environment Debug ===
Working directory: /project
Build directory: /project/build
Cache directory: /project/build/diagrams-cache
DIAGRAMS_STRUCTURIZRCLI_HOME: /opt/structurizr-cli
PATH: /usr/local/bin:/usr/bin:/bin:/opt/structurizr-cli
Structurizr directory exists: true
Structurizr CLI JARs found: [structurizr-cli-1.30.0.jar]
===============================================
```

## CI/CD Integration

### GitHub Actions Example

```yaml
- name: Setup Structurizr CLI
  run: |
    wget -O /tmp/structurizr-cli.zip https://github.com/structurizr/cli/releases/latest/download/structurizr-cli.zip
    unzip /tmp/structurizr-cli.zip -d /opt/structurizr-cli
    echo "DIAGRAMS_STRUCTURIZRCLI_HOME=/opt/structurizr-cli" >> $GITHUB_ENV

- name: Generate Documentation
  run: ./dtcw generateHTML
```

### Docker Compose Example

```yaml
version: '3.8'
services:
  doctoolchain:
    image: my-doctoolchain-structurizr
    environment:
      - DIAGRAMS_STRUCTURIZRCLI_HOME=/opt/structurizr-cli
    volumes:
      - .:/project
```

## Troubleshooting

### Common Issues

1. **Diagrams render as text instead of images**
   - Check: `./dtcw checkDiagramEnvironment`
   - Ensure Structurizr CLI is properly installed
   - Verify environment variable is set

2. **"Tool not found" errors**
   - Use absolute paths in environment variables
   - Check PATH configuration
   - Verify tool permissions

3. **Cache issues**
   - Run: `./dtcw cleanDiagramCache`
   - Check cache directory permissions

4. **Memory issues**
   - Increase JVM memory: `-Xmx2048m`
   - Monitor diagram complexity

### Debug Steps

1. **Environment Check**
   ```bash
   ./dtcw checkDiagramEnvironment
   ```

2. **Verbose Generation**
   ```bash
   ./dtcw generateHTML -Ddiagram.debug=true
   ```

3. **Manual Tool Test**
   ```bash
   java -jar $DIAGRAMS_STRUCTURIZRCLI_HOME/structurizr-cli-*.jar --version
   ```

## Backward Compatibility

This enhancement is designed to be backward compatible:

- Existing diagram configurations continue to work
- No breaking changes to existing APIs
- Optional environment variables (graceful degradation)
- Debug features are opt-in

## Testing

The enhancement includes comprehensive testing:

- Environment verification tests
- Tool accessibility checks  
- Diagram generation tests
- Error condition handling
- Performance regression tests

## Related Issues

- Fixes #1480: AsciiDoc Diagrams can't leverage Structurizr CLI
- Improves general external CLI tool integration
- Enhances debugging capabilities for diagram issues

## Contributing

To test this enhancement:

1. Clone the repository
2. Switch to branch: `fix/structurizr-cli-integration`
3. Set up Structurizr CLI environment
4. Run the test document: `./dtcw generateHTML -Ddiagram.debug=true`
5. Verify diagrams render correctly

## Future Enhancements

Potential future improvements:

- Support for additional diagram tools
- Automatic tool discovery and installation
- Enhanced caching strategies
- Performance monitoring and optimization
- Integration with diagram-as-code tools
