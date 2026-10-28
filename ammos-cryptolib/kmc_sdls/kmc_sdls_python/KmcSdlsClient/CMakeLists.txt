# Copyright (c) 2014-26 California Institute of Technology (“Caltech”). U.S. Government sponsorship acknowledged.
#
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#  • Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#  • Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#  • Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
# THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
# BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
# GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
# STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

# Open Source License Approved by Caltech/JPL
# APACHE LICENSE, VERSION 2.0
#  • Text version: https://www.apache.org/licenses/LICENSE-2.0.txt
#  • SPDX short identifier: Apache-2.0
#  • OSI Approved License: https://opensource.org/licenses/Apache-2.0

set(_prev_target "")
foreach(PY_VER IN LISTS KMC_PYTHON_USABLE)
    set(_site_build   ${PROJECT_BINARY_DIR}/lib/python${PY_VER}/site-packages)
    set(_site_install ${CMAKE_INSTALL_PREFIX}/lib/python${PY_VER}/site-packages)
    set(_stamp        ${PROJECT_BINARY_DIR}/KmcSdlsClient_${PY_VER}.stamp)
    set(_dbg_env "")
    if(DEBUG_SYMBOLS)
        set(_dbg_env "CFLAGS=-g -O0")
    endif()
    set(CL_SCRIPTS_MACOS ${PROJECT_SOURCE_DIR}/scripts/macos)
    set(_dsym_cmd "")
    if(APPLE AND DEBUG_SYMBOLS)
        set(_dsym_cmd COMMAND ${CL_SCRIPTS_MACOS}/dsym_pyext.sh ${_site_build})
    endif()
    set(_pip ${PYTHON_${PY_VER}} -m pip install --no-deps --upgrade
             --no-warn-script-location --disable-pip-version-check)

    # pip builds in an isolated environment with the pinned build requirements from pyproject.toml. That environment
    # holds compiled extensions (cffi), which cannot be loaded from a /tmp mounted noexec, so TMPDIR points into the
    # build tree. Pip before 21.3 builds from a temporary copy of this directory, so the paths kmc_cffi_build.py needs
    # are passed explicitly rather than derived from its own location.
    add_custom_command(
            OUTPUT ${_stamp}
            COMMAND ${CMAKE_COMMAND} -E make_directory ${PROJECT_BINARY_DIR}/pip-tmp
            COMMAND ${CMAKE_COMMAND} -E env KMC_SDLS_LIB_DIR=${PROJECT_BINARY_DIR}/lib MAC_SIZE=${MAC_SIZE} ${_dbg_env}
                    TMPDIR=${PROJECT_BINARY_DIR}/pip-tmp
                    KMC_CFFI_CDEF=${CMAKE_CURRENT_SOURCE_DIR}/../_cffi_src/cffi_definitions.i
                    "KMC_SDLS_INCLUDE_DIRS=${PROJECT_SOURCE_DIR}/kmc_sdls/public_inc:${PROJECT_SOURCE_DIR}/CryptoLib/include"
                    ${_pip} --target ${_site_build} ${CMAKE_CURRENT_SOURCE_DIR}
            ${_dsym_cmd}
            COMMAND ${PYTHON_${PY_VER}} -c "import os, sys; d = sys.argv[1]; os.path.isdir(os.path.join(d, 'gov', 'nasa')) or sys.exit('ERROR: KmcSdlsClient was not installed into ' + d + ' (the pip build produced no package, see the pip output above)')" ${_site_build}
            COMMAND ${CMAKE_COMMAND} -E touch ${_stamp}
            DEPENDS kmc_sdls
                    ${CMAKE_CURRENT_SOURCE_DIR}/pyproject.toml
                    ${CMAKE_CURRENT_SOURCE_DIR}/setup.py
                    ${CMAKE_CURRENT_SOURCE_DIR}/kmc_cffi_build.py
                    ${CMAKE_CURRENT_SOURCE_DIR}/../_cffi_src/cffi_definitions.i
                    ${CMAKE_CURRENT_SOURCE_DIR}/src/gov/nasa/jpl/ammos/kmc/sdlsclient/KmcSdlsClient.py
            WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
            VERBATIM)
    add_custom_target(KmcSdlsClient_${PY_VER} ALL DEPENDS ${_stamp})

    # setuptools builds in-tree (KmcSdlsClient/build, src/*.egg-info); serialize so
    # parallel make (-j) doesn't race on the shared egg-info dir.
    if(_prev_target)
        add_dependencies(KmcSdlsClient_${PY_VER} ${_prev_target})
    endif()
    set(_prev_target KmcSdlsClient_${PY_VER})

    install(DIRECTORY ${_site_build}/ DESTINATION ${_site_install}
            PATTERN "__pycache__" EXCLUDE)
endforeach()

set_property(DIRECTORY APPEND PROPERTY ADDITIONAL_CLEAN_FILES
        ${CMAKE_CURRENT_SOURCE_DIR}/build ${CMAKE_CURRENT_SOURCE_DIR}/src/KmcSdlsClient.egg-info)
