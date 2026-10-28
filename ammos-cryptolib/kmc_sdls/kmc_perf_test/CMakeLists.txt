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

include_directories(include)
include_directories(../crypto/public_inc)
include_directories(../crypto/util/include)
include_directories(../kmc_sdls/public_inc)
include_directories(../kmc_sdls/include/)

aux_source_directory(src UTIL_SRC_FILES)
aux_source_directory(app APP_SRC_FILES)

file( GLOB SOURCE_FILES src/*.c )

SET(CMAKE_SKIP_BUILD_RPATH FALSE)
SET(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
SET(CMAKE_INSTALL_RPATH $ORIGIN/../../lib:$ENV{LIBPATH})

add_executable(performance_test src/performance_test.c)

target_sources(performance_test PRIVATE src/performance_test.c src/shared_util.c)
target_link_libraries(performance_test LINK_PUBLIC kmc_sdls)

add_custom_command(TARGET performance_test POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:performance_test> ${PROJECT_BINARY_DIR}/bin/performance_test
    COMMENT "Created ${PROJECT_BINARY_DIR}/bin/performance_test"
    )

target_include_directories (crypto PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

install(TARGETS performance_test
    DESTINATION ${CMAKE_INSTALL_PREFIX}/test/bin)

