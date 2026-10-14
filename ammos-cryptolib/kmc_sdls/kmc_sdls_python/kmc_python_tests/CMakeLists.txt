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

#ctest --tests-regex 'Kmc.*'

set(PROJECT_PYTHON_TEST_DIR ${CMAKE_CURRENT_SOURCE_DIR})

foreach(PY_VER IN LISTS KMC_PYTHON_USABLE)
	foreach(_t IN ITEMS Tests:kmc_python_test Mock:mock_hex_tests Native:kmc_python_native_regression_test)
		string(REPLACE ":" ";" _p ${_t})
		list(GET _p 0 _name)
		list(GET _p 1 _file)
		add_test(NAME Kmc_Python_${_name}_PY${PY_VER}
				COMMAND ${PYTHON_${PY_VER}} ${PROJECT_PYTHON_TEST_DIR}/${_file}.py --verbose
				WORKING_DIRECTORY ${PROJECT_PYTHON_TEST_DIR})
		set_tests_properties(Kmc_Python_${_name}_PY${PY_VER} PROPERTIES ENVIRONMENT
			"PYTHONPATH=${PROJECT_BINARY_DIR}/lib/python${PY_VER}/site-packages;LD_LIBRARY_PATH=${PROJECT_BINARY_DIR}/lib")
	endforeach()

	# pytest suite (hermetic gcrypt backend). Needs pytest and a build with -DKEY_INTERNAL=ON, otherwise those
	# tests skip themselves. Run manually with --backend=kmc for MariaDB + KMC crypto service.
	execute_process(COMMAND ${PYTHON_${PY_VER}} -c "import pytest" RESULT_VARIABLE _pytest_rc OUTPUT_QUIET ERROR_QUIET)
	if(_pytest_rc EQUAL 0)
		add_test(NAME Kmc_Python_Pytest_PY${PY_VER}
				COMMAND ${PYTHON_${PY_VER}} -m pytest -q ${PROJECT_PYTHON_TEST_DIR}/pytest_suite
				WORKING_DIRECTORY ${PROJECT_PYTHON_TEST_DIR}/pytest_suite)
		set_tests_properties(Kmc_Python_Pytest_PY${PY_VER} PROPERTIES ENVIRONMENT
			"PYTHONPATH=${PROJECT_BINARY_DIR}/lib/python${PY_VER}/site-packages:${PROJECT_PYTHON_TEST_DIR}/pytest_suite;DYLD_LIBRARY_PATH=${PROJECT_BINARY_DIR}/lib;LD_LIBRARY_PATH=${PROJECT_BINARY_DIR}/lib")
	endif()
endforeach()
