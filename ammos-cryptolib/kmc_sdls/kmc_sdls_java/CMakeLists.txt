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

find_package(SWIG REQUIRED)
find_package(Java REQUIRED)
find_package(JNI REQUIRED)
include(UseSWIG)
include_directories(${JNI_INCLUDE_DIRS})

set(SWIGJAVADIR gov/nasa/jpl/ammos/asec/kmc)
set(SWIGJAVAPKG gov.nasa.jpl.ammos.asec.kmc)
set(SWIG_JAVA_ROOT ${CMAKE_CURRENT_BINARY_DIR}/swig-java)          # package root
set(CMAKE_SWIG_OUTDIR ${SWIG_JAVA_ROOT}/${SWIGJAVADIR})
set(CMAKE_SWIG_FLAGS -package ${SWIGJAVAPKG})
#SWIGJAVASRCDIR = ./$(SWIGJAVADIR)
#SWIGJAVACLASSDIR = $(JAVABUILDDIR)/$(SWIGJAVADIR)

# Maven compiles everything under swig-java/, so a type removed from kmc_sdls.i leaves a stale
# .java behind. After removing a type from kmc_sdls.i, run `make clean`.
set_property(SOURCE kmc_sdls.i PROPERTY DEPENDS
    ${CMAKE_CURRENT_SOURCE_DIR}/../../CryptoLib/include/crypto_config_structs.h
    ${CMAKE_CURRENT_SOURCE_DIR}/../../CryptoLib/include/crypto_structs.h)
swig_add_library(kmc_sdls_java
                    LANGUAGE java
                    SOURCES kmc_sdls.i
                    OUTPUT_DIR ${CMAKE_SWIG_OUTDIR})

target_compile_definitions(kmc_sdls_java PRIVATE MAC_SIZE=${MAC_SIZE})

swig_link_libraries(kmc_sdls_java kmc_sdls)

# For convenience we copy the dynamic library to the current build folder
add_custom_command(
        TARGET kmc_sdls_java
        POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different $<TARGET_FILE:kmc_sdls_java> ${PROJECT_BINARY_DIR}/lib
)

install(TARGETS kmc_sdls_java
        DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)

find_program(MVN_EXECUTABLE mvn REQUIRED)
set(KMC_MVN_ARGS "-B;-ntp" CACHE STRING "Extra args passed to every mvn invocation (;-separated)")

set(KMC_JNI_POM   ${CMAKE_CURRENT_SOURCE_DIR}/pom.xml)
set(KMC_JNI_JAR   ${CMAKE_CURRENT_SOURCE_DIR}/target/KmcSdlsJNI.jar)
set(KMC_JNI_MVN   ${MVN_EXECUTABLE} ${KMC_MVN_ARGS} -f ${KMC_JNI_POM}
                  -Dswig.generated.dir=${SWIG_JAVA_ROOT}
                  -Dnative.lib.dir=${PROJECT_BINARY_DIR}/lib)

file(GLOB_RECURSE KMC_JNI_HANDWRITTEN CONFIGURE_DEPENDS
     ${CMAKE_CURRENT_SOURCE_DIR}/src/main/java/*.java)

add_custom_command(
        OUTPUT ${KMC_JNI_JAR}
        COMMAND ${KMC_JNI_MVN} -DskipTests package
        COMMAND ${CMAKE_COMMAND} -E copy_if_different ${KMC_JNI_JAR} ${PROJECT_BINARY_DIR}/lib/KmcSdlsJNI.jar
        DEPENDS kmc_sdls_java ${KMC_JNI_POM} ${KMC_JNI_HANDWRITTEN}
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        COMMENT "Building KmcSdlsJNI.jar with Maven"
        VERBATIM)
add_custom_target(KmcSdlsJNI ALL DEPENDS ${KMC_JNI_JAR})

install(FILES ${PROJECT_BINARY_DIR}/lib/KmcSdlsJNI.jar DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)

# Publish to ~/.m2 only when explicitly asked (build.sh does this). Not part of ALL.
add_custom_target(kmc_sdls_java_m2_install
        COMMAND ${KMC_JNI_MVN} -DskipTests install
        DEPENDS KmcSdlsJNI
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        VERBATIM)

# Runs the KmcSdlsJNI unit tests against the freshly built native libs. Not part of ALL; build.sh invokes it.
add_custom_target(kmc_sdls_java_test
        COMMAND ${KMC_JNI_MVN} -DskipTests=false test
        DEPENDS KmcSdlsJNI
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        VERBATIM)

if(BUILD_TESTING)
    add_test(NAME TestKmcSdlsJavaTest
             COMMAND ${KMC_JNI_MVN} -DskipTests=false test
             WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})
endif()

set_property(DIRECTORY APPEND PROPERTY ADDITIONAL_CLEAN_FILES
             ${CMAKE_CURRENT_SOURCE_DIR}/target ${SWIG_JAVA_ROOT})
