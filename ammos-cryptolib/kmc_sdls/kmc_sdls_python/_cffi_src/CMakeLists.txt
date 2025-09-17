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

list(APPEND PYTHON_VERSIONS 3.6 3.7 3.8 3.9)

foreach(PY_VER IN LISTS PYTHON_VERSIONS)
	find_program(PYTHON_${PY_VER} python${PY_VER})
	if(PYTHON_${PY_VER})
		execute_process (
				COMMAND bash "-c" "${PYTHON_${PY_VER}} -c 'import sysconfig;print(sysconfig.get_config_var(\"EXT_SUFFIX\"))'"
				OUTPUT_STRIP_TRAILING_WHITESPACE
				OUTPUT_VARIABLE PythonExtSuffix_${PY_VER}
		)
		execute_process (
				COMMAND bash "-c" "${PYTHON_${PY_VER}} -c 'import sys;print(\"%d.%d\"%(sys.version_info[0],sys.version_info[1]))'"
				OUTPUT_STRIP_TRAILING_WHITESPACE
				OUTPUT_VARIABLE PythonVersionDot_${PY_VER}
		)


		add_custom_command(#OUTPUT ${CMAKE_CURRENT_SOURCE_DIR}/kmc_python_c_sdls_interface.c ${CMAKE_CURRENT_SOURCE_DIR}/kmc_python_c_sdls_interface.o
				OUTPUT kmc_python_c_sdls_interface${PythonExtSuffix_${PY_VER}}
				COMMAND ${PYTHON_${PY_VER}} -m invoke build-kmc-python-c-interface
				WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})

		add_custom_target(kmc_python_c_sdls_interface_${PY_VER} ALL
				DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/kmc_python_c_sdls_interface${PythonExtSuffix_${PY_VER}})
		#DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/kmc_python_c_sdls_interface.c ${CMAKE_CURRENT_SOURCE_DIR}/kmc_python_c_sdls_interface.o)

		add_custom_command(
				TARGET kmc_python_c_sdls_interface_${PY_VER} POST_BUILD
				COMMAND ${CMAKE_COMMAND} -E copy
				${CMAKE_CURRENT_SOURCE_DIR}/kmc_python_c_sdls_interface${PythonExtSuffix_${PY_VER}}
				${PROJECT_BINARY_DIR}/lib/python${PythonVersionDot_${PY_VER}}/site-packages/kmc_python_c_sdls_interface${PythonExtSuffix_${PY_VER}}
				COMMAND ${CMAKE_COMMAND} -E copy
				${CMAKE_CURRENT_SOURCE_DIR}/kmc_python_c_sdls_interface${PythonExtSuffix_${PY_VER}}
				${PROJECT_BINARY_DIR}/lib/python${PythonVersionDot_${PY_VER}}/site-packages/kmc_python_c_sdls_interface.so
				COMMENT "Created ${PROJECT_BINARY_DIR}/lib/python${PythonVersionDot_${PY_VER}}/site-packages/kmc_python_c_sdls_interface${PythonExtSuffix_${PY_VER}}")

		install(FILES ${CMAKE_CURRENT_SOURCE_DIR}/kmc_python_c_sdls_interface${PythonExtSuffix_${PY_VER}}
				DESTINATION ${CMAKE_INSTALL_PREFIX}/lib/python${PythonVersionDot_${PY_VER}}/site-packages/)

		file(GLOB built_library ./*.so)

		set_property(
				TARGET kmc_python_c_sdls_interface_${PY_VER}
				APPEND
				PROPERTY ADDITIONAL_CLEAN_FILES ${built_library} kmc_python_c_sdls_interface.c  kmc_python_c_sdls_interface.o
		)

	endif()
endforeach()
