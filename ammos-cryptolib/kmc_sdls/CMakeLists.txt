#Copyright 2021, by the California Institute of Technology.
#ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
#Any commercial use must be negotiated with the Office of Technology
#Transfer at the California Institute of Technology.
#
#This software may be subject to U.S. export control laws. By accepting
#this software, the user agrees to comply with all applicable U.S.
#export laws and regulations. User has the responsibility to obtain
#export licenses, or other export authority as may be required before
#exporting such information to foreign countries or providing access to
#foreign persons.

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