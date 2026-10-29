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

set(PROJECT_TEST_DIR ${CMAKE_CURRENT_SOURCE_DIR})

# kmc_process_security only works with CryptoLib's internal modules; with the build.sh flags (MC_DISABLED, KEY_KMC) it
# loads the stub interfaces and segfaults, so register it only for a build with all three internal modules.
if(MC_INTERNAL AND SA_INTERNAL AND KEY_INTERNAL)
    add_test(NAME Kmc_Process_Security
             COMMAND ${PROJECT_BINARY_DIR}/bin/kmc_process_security tc ${PROJECT_TEST_DIR}/data/tc4.1.dat
             WORKING_DIRECTORY ${PROJECT_TEST_DIR})
endif()
