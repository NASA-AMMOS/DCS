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


set(KMC_PYTHON_VERSIONS 3.9 3.10 3.11 3.12 3.13 3.14
    CACHE STRING "Python versions to build bindings for (each found on PATH as pythonX.Y)")

set(KMC_PYTHON_USABLE "")
foreach(PY_VER IN LISTS KMC_PYTHON_VERSIONS)
    find_program(PYTHON_${PY_VER} python${PY_VER})
    if(PYTHON_${PY_VER})
        execute_process(COMMAND ${PYTHON_${PY_VER}} -c "import setuptools, cffi, pip"
                        RESULT_VARIABLE _ok OUTPUT_QUIET ERROR_QUIET)
        if(_ok EQUAL 0)
            list(APPEND KMC_PYTHON_USABLE ${PY_VER})
        else()
            message(WARNING "python${PY_VER} found but missing setuptools/cffi/pip -- skipping. "
                            "Fix: python${PY_VER} -m pip install -r ${CMAKE_CURRENT_SOURCE_DIR}/requirements.txt")
        endif()
    endif()
endforeach()
message(STATUS "Building Python bindings for: ${KMC_PYTHON_USABLE}")

add_subdirectory(_cffi_src)
add_subdirectory(KmcSdlsClient)
add_subdirectory(kmc_sdls_python_scripts)
add_subdirectory(kmc_python_tests)
