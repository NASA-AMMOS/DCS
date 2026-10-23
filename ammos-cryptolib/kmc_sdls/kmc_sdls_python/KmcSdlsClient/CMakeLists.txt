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
    set(_pip ${PYTHON_${PY_VER}} -m pip install --no-deps --no-build-isolation --upgrade
             --no-warn-script-location --disable-pip-version-check)

    add_custom_command(
            OUTPUT ${_stamp}
            COMMAND ${CMAKE_COMMAND} -E env KMC_SDLS_LIB_DIR=${PROJECT_BINARY_DIR}/lib ${_dbg_env}
                    ${_pip} --target ${_site_build} ${CMAKE_CURRENT_SOURCE_DIR}
            ${_dsym_cmd}
            COMMAND ${CMAKE_COMMAND} -E touch ${_stamp}
            DEPENDS kmc_sdls
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
