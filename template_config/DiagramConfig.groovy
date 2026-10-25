// Enhanced docToolchainConfig.groovy template with diagram support
// This configuration shows how to enable enhanced diagram support for Structurizr CLI

// ... existing configuration ...

//******************************************************************************
// Enhanced Diagram Configuration
//******************************************************************************

// To enable enhanced diagram support with Structurizr CLI:
// 1. Ensure DIAGRAMS_STRUCTURIZRCLI_HOME environment variable is set
// 2. Include DiagramSupport.gradle in your build
// 3. Use -Ddiagram.debug=true for verbose logging

// Example Docker setup for Structurizr CLI:
/*
ENV DIAGRAMS_STRUCTURIZRCLI_HOME=/opt/structurizr-cli
ENV PATH="$PATH:/opt/structurizr-cli"
*/

// Example usage in AsciiDoc documents:
/*
[structurizr,format=svg,target=example-diagram]
----
workspace {
    model {
        user = person "User" "A user of the system"
        system = softwareSystem "System" "The software system"
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
*/

// Debugging commands:
// ./dtcw checkDiagramEnvironment          # Check diagram tool setup
// ./dtcw generateHTML -Ddiagram.debug=true  # Generate with verbose logging
// ./dtcw cleanDiagramCache                # Clean diagram cache

//******************************************************************************
// Existing configuration continues below...
//******************************************************************************
