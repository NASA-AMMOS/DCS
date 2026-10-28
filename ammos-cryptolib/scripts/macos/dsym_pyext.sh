#!/bin/sh
# Generate .dSYM bundles for the cffi extension(s) installed in the given site-packages directory.
for f in "$1"/kmc_python_c_sdls_interface*.so; do dsymutil "$f" -o "$f.dSYM"; done
