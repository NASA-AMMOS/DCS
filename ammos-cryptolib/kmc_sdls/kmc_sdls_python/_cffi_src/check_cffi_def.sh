#!/bin/bash
# Regenerates the cffi cdef from headers and fails if it differs from the checked-in copy.
# Not part of the build -- run via ctest or CI. If it fails, review the diff and, if the
# header change is intended, copy filtered_headers/gen_cffi_definitions.i over cffi_definitions.i.
set -euo pipefail
cd "$(dirname "$0")"
./gen_cffi_def.sh
trap 'rm -rf filtered_headers' EXIT
diff -u cffi_definitions.i filtered_headers/gen_cffi_definitions.i
