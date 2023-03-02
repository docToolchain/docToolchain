FROM gitpod/workspace-full:latest

USER root
# Install custom tools, runtime, etc.
RUN apt-get update && apt-get install -y graphviz

# Install powershell
RUN apt-get install -y wget apt-transport-https software-properties-common && \
    wget -q "https://packages.microsoft.com/config/ubuntu/$(lsb_release -rs)/packages-microsoft-prod.deb" && \
    dpkg -i packages-microsoft-prod.deb && \
    rm packages-microsoft-prod.deb && \
    apt-get update && \
    apt-get install -y powershell 
