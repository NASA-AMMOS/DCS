#Copyright 2026, by the California Institute of Technology.
#ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
#Any commercial use must be negotiated with the Office of Technology
#Transfer at the California Institute of Technology.
#

include_directories(include)
include_directories(../crypto/public_inc)
include_directories(../kmc_sdls/public_inc)

aux_source_directory(src UTIL_SRC_FILES)
aux_source_directory(app APP_SRC_FILES)



file( GLOB SOURCE_FILES app/*.c )
foreach(SOURCE_PATH ${SOURCE_FILES})

    get_filename_component(EXECUTABLE_NAME ${SOURCE_PATH} NAME_WE)

    add_executable(${EXECUTABLE_NAME} ${SOURCE_PATH})

    target_sources(${EXECUTABLE_NAME} PRIVATE src/kmc_shared_util.c)
    target_link_libraries(${EXECUTABLE_NAME} LINK_PUBLIC kmc_sdls)

    add_custom_command(TARGET ${EXECUTABLE_NAME} POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:${EXECUTABLE_NAME}> ${PROJECT_BINARY_DIR}/bin/${EXECUTABLE_NAME}
            COMMAND ${CMAKE_COMMAND} -E rm $<TARGET_FILE:${EXECUTABLE_NAME}>
            COMMENT "Created ${PROJECT_BINARY_DIR}/bin/${EXECUTABLE_NAME}"
            )
endforeach(SOURCE_PATH ${SOURCE_FILES})

target_include_directories (crypto PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

