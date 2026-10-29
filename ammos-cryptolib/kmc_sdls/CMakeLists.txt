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
#

include_directories(../CryptoLib/include)
include_directories(public_inc)

aux_source_directory(src LIB_SRC_FILES)

# --as-needed is a GNU ld option; Apple's linker doesn't support it and errors on it ("unknown options:
# --as-needed").
if(NOT APPLE)
    set(CMAKE_EXE_LINKER_FLAGS    "-Wl,--as-needed ${CMAKE_EXE_LINKER_FLAGS}")
    set(CMAKE_SHARED_LINKER_FLAGS "-Wl,--as-needed ${CMAKE_SHARED_LINKER_FLAGS}")
endif()

if(CMAKE_PROJECT_NAME STREQUAL PROJECT_NAME)
    include(CTest)
    enable_testing()
endif()


# Create the app module
set(APPTYPE "SHARED")
add_library(kmc_sdls ${APPTYPE} ${LIB_SRC_FILES})
if(KEY_INTERNAL)
    # Lets sdls_config_cryptolib_ex() accept KEY_TYPE_INTERNAL (CryptoLib's built-in test key ring).
    # Off in shipping builds; enable with -DKEY_INTERNAL=ON for the hermetic pytest suite.
    target_compile_definitions(kmc_sdls PRIVATE KMC_SDLS_KEY_INTERNAL)
endif()

# Add cryptolib
#target_link_options(kmc_sdls INTERFACE)
target_link_libraries(kmc_sdls PUBLIC crypto)


set_target_properties(kmc_sdls PROPERTIES PUBLIC_HEADER "public_inc/kmc_sdls.h")

add_custom_command(TARGET kmc_sdls POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:kmc_sdls> ${PROJECT_BINARY_DIR}/lib/libkmc_sdls${CMAKE_SHARED_LIBRARY_SUFFIX}
        COMMENT "Created ${PROJECT_BINARY_DIR}/lib/libkmc_sdls${CMAKE_SHARED_LIBRARY_SUFFIX}"
        )

# Copy libcryptolib to build/lib so kmc_sdls can find it via RPATH
add_custom_command(TARGET kmc_sdls POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
                $<TARGET_FILE:crypto>
                ${PROJECT_BINARY_DIR}/lib/$<TARGET_FILE_NAME:crypto>
        COMMENT "Copying libcryptolib to build/lib for RPATH"
        VERBATIM)

install(TARGETS kmc_sdls
        DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
        PUBLIC_HEADER DESTINATION ${CMAKE_INSTALL_PREFIX}/include)


add_subdirectory(kmc_crypto_util)
add_subdirectory(kmc_perf_test)

if((CMAKE_PROJECT_NAME STREQUAL PROJECT_NAME OR MYPROJECT_BUILD_TESTING) AND BUILD_TESTING)
    add_subdirectory(kmc_sdls_tests)
endif()

if(BUILD_JAVA)
    add_subdirectory(kmc_sdls_java)
endif()

if(BUILD_PYTHON)
    add_subdirectory(kmc_sdls_python)
endif()
