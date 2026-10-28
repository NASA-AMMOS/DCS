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

cmake_minimum_required(VERSION 3.14.0)

# On macOS the default compiler is AppleClang, which CryptoLib doesn't build with (see the GCC check below).
# Pick Homebrew's newest GCC unless the caller already chose a compiler (-DCMAKE_C_COMPILER=... or CC=...).
# This must run before project(), and only takes effect on a fresh build directory (the compiler is cached).
if(APPLE AND NOT CMAKE_C_COMPILER AND NOT DEFINED ENV{CC})
    find_program(KMC_HOMEBREW_GCC NAMES gcc-20 gcc-19 gcc-18 gcc-17 gcc-16 gcc-15 gcc-14 gcc-13 gcc-12 gcc-11
                 PATHS /opt/homebrew/bin /usr/local/bin)
    if(KMC_HOMEBREW_GCC)
        set(CMAKE_C_COMPILER ${KMC_HOMEBREW_GCC} CACHE FILEPATH "C compiler")
    endif()
endif()

project(CRYPTO C)

#Build Options:
# CryptoLib/support/scripts/build_kmc.sh
# cmake $BASE_DIR -DCODECOV=1 -DDEBUG=1 -DCRYPTO_KMC=1 -DKEY_KMC=1 -DSA_MARIADB=1 -DTEST=1 -DTEST_ENC=1
OPTION(DEBUG "Debug" OFF) # Disabled by default
MESSAGE(STATUS "DEBUG: ${DEBUG}")
OPTION(MYSQL "Mysql" ON) # Disabled by default, enable with: -DMYSQL=ON
MESSAGE(STATUS "MYSQL: ${MYSQL}")
OPTION(LIBGCRYPT "Libgcrypt" OFF) # Enabled by default, disable with: -DLIBGCRYPT=OFF
MESSAGE(STATUS "LIBGCRYPT: ${LIBGCRYPT}")
OPTION(KMCCRYPTO "KmcCrypto" ON) # Disabled by default, enable with: -DKMCCRYPTO=ON
MESSAGE(STATUS "KMCCRYPTO: ${KMCCRYPTO}")
OPTION(ENCTEST "Encryption-Tests" OFF) # Disabled by default, enable with: -DENCTEST=ON
MESSAGE(STATUS "ENCTEST: ${ENCTEST}")
OPTION(CODECOV "Code Coverage" OFF)
MESSAGE(STATUS "CODECOV: ${CODECOV}")
OPTION(CRYPTO_KMC "Cryptography Module - KMC" ON)
MESSAGE(STATUS "CRYPTO_KMC: ${CRYPTO_KMC}")
OPTION(KEY_KMC "Key Module - KMC" ON)
MESSAGE(STATUS "KEY_KMC: ${KEY_KMC}")
OPTION(SA_MARIADB "Security Association - MariaDB" ON)
MESSAGE(STATUS "SA_MARIADB: ${SA_MARIADB}")
OPTION(TEST "Test" ON)
MESSAGE(STATUS "TEST: ${TEST}")
OPTION(TEST_ENC "Tests - Encryption" ON)
MESSAGE(STATUS "TEST_ENC: ${TEST_ENC}")
OPTION(SYSTEM_INSTALL "SystemInstall" OFF) #Disabled by default, enable with: -DSYSTEM_INSTALL=ON
MESSAGE(STATUS "SYSTEM_INSTALL: ${SYSTEM_INSTALL}")
OPTION(BUILD_JAVA "Build Java Library" ON) #Enabled by default, disable with: -DBUILD_JAVA=OFF
MESSAGE(STATUS "Build Java: ${BUILD_JAVA}")
OPTION(BUILD_PYTHON "Build Python Library" ON) #Enabled by default, disable with: -DBUILD_PYTHON=OFF
MESSAGE(STATUS "Build Python: ${BUILD_PYTHON}")

if(APPLE)
    set(CMAKE_INSTALL_RPATH "@loader_path/../lib")
else()
    set(CMAKE_INSTALL_RPATH "$ORIGIN/../lib")
endif()
set(CMAKE_INSTALL_PREFIX ${PROJECT_SOURCE_DIR}/install) #install to root project dir


# 2025-04-16 - aruty
# Override crypto_config.h defaults here
#
# We encountered a memory corruption issue due to buffer overflow and identified MAC_SIZE definition as the root cause
# CryptoLib defines MAC_SIZE to 16 bytes by default and we wanted to override it to 64 bytes
#
# CryptoLib/CMakeLists.txt exposes this as a build option (option(MAC_SIZE ...)), but its add_compile_definitions()
# only applies inside the CryptoLib directory. TC_t/TM_t/AOS_t embed `uint8_t mac[MAC_SIZE]` in packed structs, so
# kmc_sdls, the JNI wrapper and the Python extension must be compiled with the same value as libcryptolib or they
# read fields after the MAC (e.g. tc_sec_trailer.fecf) at the wrong offset. Define it for every directory below.
# (This replaces an in-place `sed -i` on the vendored crypto_config.h, a GNU-sed-only construct that fails on
# macOS's BSD sed and left a permanently-dirty file in the CryptoLib submodule.)
set(MAC_SIZE 64)
add_compile_definitions(MAC_SIZE=${MAC_SIZE})

IF(SYSTEM_INSTALL)
    set(CMAKE_INSTALL_PREFIX /usr/local)
ENDIF()

set(CRYPTO_SUBMODULE_INSTALL ${PROJECT_SOURCE_DIR}/install) #CryptoLib is a submodule of this build

IF(DEBUG)
    ADD_DEFINITIONS(-DDEBUG -DOCF_DEBUG -DFECF_DEBUG -DSA_DEBUG -DPDU_DEBUG -DCCSDS_DEBUG -DTC_DEBUG -DMAC_DEBUG -DTM_DEBUG)
    add_compile_options(-ggdb)
ENDIF(DEBUG)

# Debugger-friendly build (lldb/gdb) without the debug print output enabled by DEBUG: full symbols and no optimisation
# for the C libraries and the Python cffi extension (pip builds it with CFLAGS from the environment).
OPTION(DEBUG_SYMBOLS "Build with -g -O0 for debugger use" OFF)
MESSAGE(STATUS "DEBUG_SYMBOLS: ${DEBUG_SYMBOLS}")
IF(DEBUG_SYMBOLS)
    add_compile_options(-g -O0 -fno-omit-frame-pointer)
ENDIF(DEBUG_SYMBOLS)

if(CMAKE_PROJECT_NAME STREQUAL PROJECT_NAME)
    include(CTest)
    enable_testing()
endif()


#Include cmake install module - todo
#include(GNUInstallDirs)

set(PROJECT_BINARY_DIR ${CMAKE_CURRENT_SOURCE_DIR}/build)

# CryptoLib/src/CMakeLists.txt links these by bare name (target_link_libraries(crypto gcrypt) /
# target_link_libraries(crypto curl)) and relies on them being on the linker's default search path. That's
# true on Linux (Homebrew there installs to /usr) but not on Apple Silicon macOS, where Homebrew installs to
# /opt/homebrew, which isn't part of the default search path. Resolving the packages here and adding their
# include/lib dirs via include_directories()/link_directories() -- which are inherited by the CryptoLib
# submodule's directory scope via add_subdirectory() below -- fixes this without touching the submodule.
if(CRYPTO_LIBGCRYPT)
    find_path(LIBGCRYPT_INCLUDE_DIR gcrypt.h HINTS /opt/homebrew/include /usr/local/include)
    find_library(LIBGCRYPT_LIBRARY gcrypt HINTS /opt/homebrew/lib /usr/local/lib)
    if(LIBGCRYPT_INCLUDE_DIR AND LIBGCRYPT_LIBRARY)
        include_directories(${LIBGCRYPT_INCLUDE_DIR})
        get_filename_component(LIBGCRYPT_LIBRARY_DIR "${LIBGCRYPT_LIBRARY}" DIRECTORY)
        link_directories(${LIBGCRYPT_LIBRARY_DIR})
    endif()
endif()

if(CRYPTO_KMC)
    find_package(CURL REQUIRED)
    include_directories(${CURL_INCLUDE_DIRS})
    list(GET CURL_LIBRARIES 0 _CURL_FIRST_LIB)
    get_filename_component(CURL_LIBRARY_DIR "${_CURL_FIRST_LIB}" DIRECTORY)
    link_directories(${CURL_LIBRARY_DIR})
endif()

if(SA_MARIADB AND APPLE)
    # `mysql_config --libs` (used by CryptoLib/src/CMakeLists.txt for SA_MARIADB) pulls in -lssl -lcrypto,
    # since Homebrew's mariadb links against Homebrew's OpenSSL. OpenSSL is keg-only on Homebrew (not
    # symlinked into /opt/homebrew/lib) specifically to avoid clashing with macOS's own libssl, so those
    # libs aren't on the default linker search path. Adding the lib dir here (inherited by CryptoLib's
    # directory scope) fixes the resulting "library 'ssl' not found" link error without touching the
    # submodule.
    find_package(OpenSSL REQUIRED)
    get_filename_component(OPENSSL_LIBRARY_DIR "${OPENSSL_SSL_LIBRARY}" DIRECTORY)
    link_directories(${OPENSSL_LIBRARY_DIR})
endif()

# The CryptoLib submodule is only built and tested with GCC: AppleClang rejects crypto.c's deliberate self-assigns
# under -Werror (-Wself-assign) and utest.h's clang `__auto_type` path cannot take bit-fields (ut_sa_save.c).
# On macOS, install GCC (`brew install gcc`) and configure with e.g. -DCMAKE_C_COMPILER=gcc-15 on a fresh build dir.
if(APPLE AND NOT CMAKE_C_COMPILER_ID STREQUAL "GNU")
    message(FATAL_ERROR "macOS builds require GCC (found ${CMAKE_C_COMPILER_ID}: ${CMAKE_C_COMPILER}). "
                        "Run `brew install gcc`, then reconfigure a fresh build directory with "
                        "-DCMAKE_C_COMPILER=gcc-<version>.")
endif()

if(APPLE)
    # Homebrew GCC emits warnings from the macOS SDK's own headers (-Wexpansion-to-defined,
    # -Wunused-variable, -Wpacked-not-aligned, ...), which CryptoLib/CMakeLists.txt's -Wall -Wextra -Werror would
    # turn into hard errors. Directory-level compile options come after CMAKE_C_FLAGS, so this overrides -Werror.
    add_compile_options(-Wno-error)
endif()

# CryptoLib/src/CMakeLists.txt runs `mysql_config` at configure time and mishandles its whitespace-separated
# output; the shim in scripts/macos reformats it into a CMake list (see the comment in that script).
if(APPLE AND SA_MARIADB)
    set(ENV{PATH} "${CMAKE_CURRENT_SOURCE_DIR}/scripts/macos:$ENV{PATH}")
endif()


add_subdirectory(CryptoLib)
add_subdirectory(kmc_sdls)

# Apple linkers only record a debug map pointing at the .o files; dsymutil bundles the DWARF into .dSYM directories
# next to each library so the symbols survive `make clean` of the object files and lldb finds them anywhere.
if(APPLE AND DEBUG_SYMBOLS)
    find_program(DSYMUTIL dsymutil REQUIRED)
    set(_dsym_libs crypto kmc_sdls)
    if(TARGET kmc_sdls_java)
        list(APPEND _dsym_libs kmc_sdls_java)
    endif()
    set(_dsym_cmds "")
    foreach(_t IN LISTS _dsym_libs)
        list(APPEND _dsym_cmds COMMAND ${DSYMUTIL} $<TARGET_FILE:${_t}> -o $<TARGET_FILE:${_t}>.dSYM)
    endforeach()
    add_custom_target(dsym ALL ${_dsym_cmds} DEPENDS ${_dsym_libs} COMMENT "Generating .dSYM bundles" VERBATIM)
endif()

# `make clean` resets every product directory so repeated builds can't accumulate stale output: build/ (libs, jar,
# CLI tools and the pip-installed Python trees, none of which CMake tracks individually), install/ (`make install`
# only ever copies files in, and deploy.sh ships install/lib wholesale), install_manifest.txt and test state files.
# (Attached to a target: a directory-level ADDITIONAL_CLEAN_FILES has no effect in this directory, which defines no targets.)
# install/ is only removed when it is the default in-tree prefix, never for -DSYSTEM_INSTALL=ON (/usr/local).
set_property(TARGET kmc_sdls APPEND PROPERTY ADDITIONAL_CLEAN_FILES
        ${PROJECT_BINARY_DIR}
        ${CMAKE_BINARY_DIR}/install_manifest.txt
        ${CMAKE_CURRENT_SOURCE_DIR}/kmc_sdls/kmc_sdls_java/src/test/resources/sa_save_file.bin
        ${CMAKE_CURRENT_SOURCE_DIR}/kmc_sdls/kmc_sdls_python/kmc_python_tests/sa_save_file.bin
        ${CMAKE_CURRENT_SOURCE_DIR}/kmc_sdls/kmc_sdls_tests/sa_save_file.bin
        ${CMAKE_CURRENT_SOURCE_DIR}/kmc_sdls/kmc_sdls_tests/log.txt)
if(CMAKE_INSTALL_PREFIX STREQUAL "${CMAKE_CURRENT_SOURCE_DIR}/install")
    set_property(TARGET kmc_sdls APPEND PROPERTY ADDITIONAL_CLEAN_FILES ${CMAKE_INSTALL_PREFIX})
endif()

# Install libcryptolib so install tree works without DYLD_LIBRARY_PATH/LD_LIBRARY_PATH
install(FILES $<TARGET_FILE:crypto>
        DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)

#Useful Commands:
#ctest --tests-regex 'Kmc.*'
#ctest --tests-regex 'Kmc.*' --verbose
