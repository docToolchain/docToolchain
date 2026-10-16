# Bash completion for dtcw
# Source this file in .bashrc after sourcing dtcw-function.bash

_dtcw_completions() {
    local cur="${COMP_WORDS[COMP_CWORD]}"
    local prev="${COMP_WORDS[COMP_CWORD-1]}"

    # Tasks grouped by category
    local generate_tasks="generateHTML generatePDF generateSite generateDeck generateDocBook generateContent"
    local export_tasks="exportChangeLog exportConfluence exportContributors exportDrawIo exportEA exportExcel exportJiraIssues exportJiraSprintChangelog exportMarkdown exportMetrics exportOpenApi exportPPT exportStructurizr exportVisio"
    local publish_tasks="publishToConfluence wipeConfluenceSpace"
    local util_tasks="collectIncludes convertToDocx convertToEpub copyThemes createReferenceDoc downloadTemplate fixEncoding htmlSanityCheck prependFilename previewSite"
    local all_tasks="${generate_tasks} ${export_tasks} ${publish_tasks} ${util_tasks}"
    local commands="${all_tasks} tasks install shell --version help"

    # After -f: show files and directories
    if [[ "${prev}" == "-f" ]]; then
        mapfile -t COMPREPLY < <(compgen -f -- "${cur}")
        return
    fi

    # After install: show components
    if [[ "${prev}" == "install" ]]; then
        mapfile -t COMPREPLY < <(compgen -W "doctoolchain java" -- "${cur}")
        return
    fi

    # First argument: commands or -f
    if [[ ${COMP_CWORD} -eq 1 ]]; then
        mapfile -t COMPREPLY < <(compgen -W "${commands} -f" -- "${cur}")
        return
    fi

    # After -f <file>: commands
    if [[ "${COMP_WORDS[1]}" == "-f" && ${COMP_CWORD} -eq 3 ]]; then
        mapfile -t COMPREPLY < <(compgen -W "${commands}" -- "${cur}")
        return
    fi
}

complete -o filenames -F _dtcw_completions dtcw
