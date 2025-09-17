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

find_package(SWIG REQUIRED)
find_package(Java REQUIRED)
find_package(JNI REQUIRED)
include(UseJava)
include(UseSWIG)
include_directories(${JNI_INCLUDE_DIRS})

set(SWIGJAVADIR gov/nasa/jpl/ammos/asec/kmc)
set(SWIGJAVAPKG gov.nasa.jpl.ammos.asec.kmc)
set(CMAKE_SWIG_OUTDIR ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR})
set(CMAKE_SWIG_FLAGS -package ${SWIGJAVAPKG})
#SWIGJAVASRCDIR = ./$(SWIGJAVADIR)
#SWIGJAVACLASSDIR = $(JAVABUILDDIR)/$(SWIGJAVADIR)

swig_add_library(kmc_sdls_java
                    LANGUAGE java
                    SOURCES kmc_sdls.i
                    OUTPUT_DIR ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR})

swig_link_libraries(kmc_sdls_java kmc_sdls)

# For convenience we copy the dynamic library to the current build folder
add_custom_command(
        TARGET kmc_sdls_java
        POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different $<TARGET_FILE:kmc_sdls_java> ${PROJECT_BINARY_DIR}/lib
)

install(TARGETS kmc_sdls_java
        DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)

# Doesn't work because this happens at cmake time, but swig command generated files at build time...
# file(GLOB_RECURSE kmc_sdls_jni_sources ${CMAKE_SWIG_OUTDIR}/*.java)

#You must manually update this when new source files are generated! -- cmake can't get them at build time!
##I think the only way to have this be automated is a cmake sub project with add_custom_target -- TBD.
set(SWIG_GENERATED_SRCS
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/TcUniqueSaPerMapId.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/TcSegmentHdrsPresent.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/TcPusHdrPresent.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/TcProcessSdlsPdus.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/TcIgnoreSaState.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/TcIgnoreAntiReplay.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/FecfPresent.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/CreateFecfBool.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/CheckFecfBool.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SadbType.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/CamConfig_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/CamEnabledBool.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/CamLoginMethod.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SaIncrementNonTransmittedIvPortion.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/CryptographyType.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/EncCipherSuite.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/AuthCipherSuite.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/CryptoConfig_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/_GvcidManagedParameters_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SadbMariaDBConfig_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/CryptographyKmcCryptoServiceConfig_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/crypto_gvcid_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SecurityAssociation_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_FSR_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_TLV_Hdr_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_TLV_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_OTAR_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_EKB_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_KEY_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_KEY_INVENTORY_CMD_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_KEY_INVENTORY_RPLY_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_KEY_BLK_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_KEYV_CMD_BLK_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_KEYV_RPLY_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_KEYV_RPLY_BLK_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_KEYV_CMD_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_KEYDB_RPLY_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_KEYDB_CMD_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_SA_STATUS_RPLY_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_SA_READ_ARSN_RPLY_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_MC_LOG_RPLY_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_MC_ST_RPLY_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_MC_SN_RPLY_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_MC_DUMP_RPLY_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_MC_LOG_RPLY_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_MC_DUMP_BLK_RPLY_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/TC_FramePrimaryHeader_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/TC_FrameSecurityTrailer_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/TC_FrameSecurityHeader_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/TC_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/CCSDS_SPP_HDR_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/ECSS_PUS_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/CCSDS_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/TM_FrameSecurityHeader_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/TM_FramePrimaryHeader_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/TM_FrameSecurityTrailer_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/TM_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/kmc_sdlsJNI.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/kmc_sdlsConstants.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/kmc_sdls.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SWIGTYPE_p_unsigned_short.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SWIGTYPE_p_unsigned_char.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SWIGTYPE_p_p_unsigned_char.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SWIGTYPE_p_int.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/AOS_FramePrimaryHeader_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/AOS_FrameSecurityHeader_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/AOS_FrameSecurityTrailer_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/AOS_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/AosFhecPresent.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/AosInsertZonePresent.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/InitStatus.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/IvType.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/KeyType.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/McType.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/OcfPresent.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/SDLS_MC_LOG_RPLY_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/Telemetry_Frame_Ocf_Clcw_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/Telemetry_Frame_Ocf_Fsr_t.java
        ${CMAKE_CURRENT_BINARY_DIR}/${SWIGJAVADIR}/TmSecondaryHdrPresent.java
        )

set(KMC_SDLS_ENGINE_SRCS
        ${CMAKE_CURRENT_SOURCE_DIR}/${SWIGJAVADIR}/KmcSdlsEngine.java
        ${CMAKE_CURRENT_SOURCE_DIR}/${SWIGJAVADIR}/SDLS_TC_TransferFrame.java
        )

add_jar(KmcSdlsJNI
        SOURCES ${SWIG_GENERATED_SRCS} ${KMC_SDLS_ENGINE_SRCS}
        OUTPUT_DIR ${PROJECT_BINARY_DIR}/lib)


add_dependencies(KmcSdlsJNI kmc_sdls_java)

install_jar(KmcSdlsJNI
            DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)

add_subdirectory(kmc_sdls_java_test)
