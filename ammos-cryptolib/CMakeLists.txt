#  Copyright (C) 2009 - 2017 National Aeronautics and Space Administration. All Foreign Rights are Reserved to the U.S. Government.
#
#   This software is provided "as is" without any warranty of any, kind either express, implied, or statutory, including, but not
#   limited to, any warranty that the software will conform to, specifications any implied warranties of merchantability, fitness
#   for a particular purpose, and freedom from infringement, and any warranty that the documentation will conform to the program, or
#   any warranty that the software will be error free.
#
#   In no event shall NASA be liable for any damages, including, but not limited to direct, indirect, special or consequential damages,
#   arising out of, resulting from, or in any way connected with the software or its documentation.  Whether or not based upon warranty,
#   contract, tort or otherwise, and whether or not loss was sustained from, or arose out of the results of, or use of, the software,
#   documentation or services provided hereunder
#
#   ITC Team
#   NASA IV&V
#   ivv-itc@lists.nasa.gov

cmake_minimum_required(VERSION 3.14.0)
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

set(CMAKE_INSTALL_RPATH "$ORIGIN/../lib")
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

if(CMAKE_PROJECT_NAME STREQUAL PROJECT_NAME)
    include(CTest)
    enable_testing()
endif()


#Include cmake install module - todo
#include(GNUInstallDirs)

set(PROJECT_BINARY_DIR ${CMAKE_CURRENT_SOURCE_DIR}/build)

add_subdirectory(CryptoLib)
add_subdirectory(kmc_sdls)


#Useful Commands:
#ctest --tests-regex 'Kmc.*'
#ctest --tests-regex 'Kmc.*' --verbose