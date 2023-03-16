# SPDX-License-Identifier: MIT
# Copyright 2023, Max Hofer and the docToolchain contributors

# Helper functions used by more  than one test suite

disable_command() {
    local cmd=$1
    local abs_path
    if abs_path=$(command -v $cmd); then
        mv -n "${abs_path}" "${abs_path}.disabled"
    else
        return 0
    fi
}

enable_command() {
    local cmd=$1
    local abs_path
    if abs_path=$(command -v "$cmd.disabled"); then
        mv "${abs_path}" "${abs_path%.disabled}"
    else
        return 0
    fi
}

function mock_create() {
    local file_name=/usr/local/bin/$1
    cat << EOF >"${file_name}"
echo "docker mock called: \$@"
EOF
    chmod +x "${file_name}"
}

function mock_delete() {
    rm -f "/usr/local/bin/$1"
}
