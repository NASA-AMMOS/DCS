#!/bin/bash

# uncomment for debug
#set -x

echo "Compile and build DCS"

source $(dirname "$0")/setenv.sh

SKIP_TESTS="-Dmaven.test.skip=true"
MVN_COMMON_ARGS="-B -ntp"
MVN="mvn ${MVN_COMMON_ARGS} ${SKIP_TESTS}"

echo "----------------------------------------"
echo "Java Version used in this build"
echo "----------------------------------------"
if [ -n "${JAVA_HOME}" ]; then
  "${JAVA_HOME}"/bin/java -version
else
  java -version
fi
echo "----------------------------------------"

# Initialize variables to track build failures
BUILD_FAILURES=()
BUILD_FLAG=0

echo "----------------------------------------"
echo "AMMOS CryptoLib"
echo "----------------------------------------"
if [ -x /usr/bin/cmake ]; then
  cd $SRC_CRYPTO_LIB
  if [ -f CMakeCache.txt ]; then
    make clean
    rm -f CMakeCache.txt make.out
  fi
  cmake -DDEBUG=OFF -DCRYPTO_KMC=ON -DCRYPTO_LIBGCRYPT=ON -DKEY_KMC=ON -DMC_DISABLED=ON -DSA_MARIADB=ON -DSA_INTERNAL=ON -DTEST=OFF -DSA_FILE=OFF -DKMC_MDB_DB=ON -DCODECOV=OFF . > make.out 2>&1
  if [ $? -ne 0 ]; then
    echo "ERROR: Failed cmake in $SRC_CRYPTO_LIB"
    BUILD_FAILURES+=("AMMOS CryptoLib cmake")
    BUILD_FLAG=1
  else
    make >> make.out 2>&1
    if [ $? -ne 0 ]; then
      echo "ERROR: Failed to build $SRC_CRYPTO_LIB"
      BUILD_FAILURES+=("AMMOS CryptoLib make")
      BUILD_FLAG=1
    else
      make install >> make.out 2>&1
      if [ $? -ne 0 ]; then
        echo "ERROR: Failed to install $SRC_CRYPTO_LIB"
        BUILD_FAILURES+=("AMMOS CryptoLib make install")
        BUILD_FLAG=1
      fi
    fi
  fi
else
  echo "Skip building CryptoLib as cmake not found"
fi
echo "----------------------------------------"

cd $SRC_KMC; mvn $MVN_COMMON_ARGS clean
cd $SRC_KMC; $MVN install -N -DDEFAULT_PREFIX="${PREFIX}" -DDEFAULT_BINPATH="${BINPATH}" -DDEFAULT_LIBPATH="${LIBPATH}" -DDEFAULT_CFGPATH="${CFGPATH}" -DDEFAULT_LOGPATH="${LOGPATH}"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to build $SRC_KMC"
  BUILD_FAILURES+=("Root level 'mvn install' failed")
  BUILD_FLAG=1
fi
echo "----------------------------------------"
echo "KMIP Client Library"
echo "----------------------------------------"
cd $SRC_KMIP; mvn $MVN_COMMON_ARGS clean
cd $SRC_KMIP; $MVN install -DDEFAULT_PREFIX="${PREFIX}" -DDEFAULT_BINPATH="${BINPATH}" -DDEFAULT_LIBPATH="${LIBPATH}" -DDEFAULT_CFGPATH="${CFGPATH}" -DDEFAULT_LOGPATH="${LOGPATH}"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to build KMIP Client Library"
  BUILD_FAILURES+=("KMIP Client Library")
  BUILD_FLAG=1
fi
echo "----------------------------------------"
echo "DCS Key Client Library"
echo "----------------------------------------"
cd $SRC_KMC/kmc-key-client ; $MVN install -DDEFAULT_PREFIX="${PREFIX}" -DDEFAULT_BINPATH="${BINPATH}" -DDEFAULT_LIBPATH="${LIBPATH}" -DDEFAULT_CFGPATH="${CFGPATH}" -DDEFAULT_LOGPATH="${LOGPATH}"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to build DCS Key Client Library"
  BUILD_FAILURES+=("DCS Key Client Library")
  BUILD_FLAG=1
fi
echo "----------------------------------------"
echo "DCS Crypto Interface"
echo "----------------------------------------"
cd $SRC_KMC/kmc-crypto ; $MVN install -DDEFAULT_PREFIX="${PREFIX}" -DDEFAULT_BINPATH="${BINPATH}" -DDEFAULT_LIBPATH="${LIBPATH}" -DDEFAULT_CFGPATH="${CFGPATH}" -DDEFAULT_LOGPATH="${LOGPATH}"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to build DCS Crypto Interface"
  BUILD_FAILURES+=("DCS Crypto Interface")
  BUILD_FLAG=1
fi
echo "----------------------------------------"
echo "DCS Crypto Library"
echo "----------------------------------------"
cd $SRC_KMC/kmc-crypto-library ; $MVN install -DDEFAULT_PREFIX="${PREFIX}" -DDEFAULT_BINPATH="${BINPATH}" -DDEFAULT_LIBPATH="${LIBPATH}" -DDEFAULT_CFGPATH="${CFGPATH}" -DDEFAULT_LOGPATH="${LOGPATH}"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to build DCS Crypto Library"
  BUILD_FAILURES+=("DCS Crypto Library")
  BUILD_FLAG=1
fi
echo "----------------------------------------"
echo "DCS Crypto Service"
echo "----------------------------------------"
cd $SRC_KMC/kmc-crypto-service ; $MVN package -DDEFAULT_PREFIX="${CRYPTOSVC_PREFIX}" -DDEFAULT_BINPATH="${CRYPTOSVC_PREFIX}/bin" -DDEFAULT_LIBPATH="${CRYTPOSVC_LIBPATH}" -DDEFAULT_CFGPATH="${CRYPTOSVC_CFGPATH}" -DDEFAULT_LOGPATH="${CRYPTOSVC_LOGPATH}"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to build DCS Crypto Service"
  BUILD_FAILURES+=("DCS Crypto Service")
  BUILD_FLAG=1
fi

echo "----------------------------------------"
echo "DCS SDLS Service"
echo "----------------------------------------"
cd $SRC_KMC/kmc-sdls-service
export LD_LIBRARY_PATH=${SRC_CRYPTO_LIB}/build/lib
rm -fr local-maven-repo
rm -fr ~/.m2/repository/gov/nasa/jpl/ammos/asec/kmc/KmcSdlsJNI
mvn $MVN_COMMON_ARGS deploy:deploy-file -DgroupId=gov.nasa.jpl.ammos.asec.kmc -DartifactId=KmcSdlsJNI \
  -Dversion=$VERSION -Durl=file:./local-maven-repo/ -DrepositoryId=local-maven-repo \
  -DupdateReleaseInfo=true -Dfile=${SRC_CRYPTO_LIB}/build/lib/KmcSdlsJNI.jar
if [ $? -ne 0 ]; then
  echo "ERROR: Failed mvn deploy:deploy-file for DCS SDLS Service"
  BUILD_FAILURES+=("DCS SDLS Service deploy:deploy-file")
  BUILD_FLAG=1
fi
mvn $MVN_COMMON_ARGS eclipse:eclipse
mvn $MVN_COMMON_ARGS package -DDEFAULT_PREFIX="${SDLSSVC_PREFIX}" -DDEFAULT_BINPATH="${SDLSSVC_PREFIX}/bin" -DDEFAULT_LIBPATH="${SDLSSVC_LIBPATH}" -DDEFAULT_CFGPATH="${SDLSSVC_CFGPATH}" -DDEFAULT_LOGPATH="${SDLSSVC_LOGPATH}"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed mvn package for DCS SDLS Service"
  BUILD_FAILURES+=("DCS SDLS Service package")
  BUILD_FLAG=1
fi

echo "----------------------------------------"
echo "DCS SA Management"
echo "----------------------------------------"
cd $SRC_KMC/kmc-sa-mgmt ; mvn $MVN_COMMON_ARGS package -DDEFAULT_PREFIX="${SAMGMTSVC_PREFIX}" -DDEFAULT_BINPATH="${SAMGMTSVC_PREFIX}/bin" -DDEFAULT_LIBPATH="${SAMGMTSVC_LIBPATH}" -DDEFAULT_CFGPATH="${SAMGMTSVC_CFGPATH}" -DDEFAULT_LOGPATH="${SAMGMTSVC_LOGPATH}" # will run the tests
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to build DCS SA Management"
  BUILD_FAILURES+=("DCS SA Management")
  BUILD_FLAG=1
fi

if [ "$1" == "skip-test" ]; then
  echo "Skipping tests..."
  cd $SRC_KMC/kmc-crypto-library
  mvn $MVN_COMMON_ARGS jar:test-jar
  if [ $? -ne 0 ]; then
    echo "ERROR: Failed to create test jar for DCS Crypto Library"
    BUILD_FAILURES+=("DCS Crypto Library jar:test-jar")
    BUILD_FLAG=1
  fi
else
  if [ $BUILD_FLAG -eq 0 ]; then # only run tests if no build failures
    echo "----------------------------------------"
    echo "DCS Crypto Library Tests"
    echo "----------------------------------------"
    cd $SRC_KMC/kmc-crypto-library
    mvn $MVN_COMMON_ARGS test
    if [ $? -ne 0 ]; then
      echo "ERROR: Failed tests for DCS Crypto Library"
      BUILD_FAILURES+=("DCS Crypto Library Tests")
      BUILD_FLAG=1
    else
      mvn $MVN_COMMON_ARGS jar:test-jar
      if [ $? -ne 0 ]; then
        echo "ERROR: Failed to create test jar for DCS Crypto Library"
        BUILD_FAILURES+=("DCS Crypto Library jar:test-jar")
        BUILD_FLAG=1
      fi
    fi
  fi
fi

# At the end, report if any build failed and exit accordingly
echo "----------------------------------------"
if [ $BUILD_FLAG -ne 0 ]; then
  echo "The following component(s) failed to build, please see the logs for more detail:"
  for failure in "${BUILD_FAILURES[@]}"; do
    echo " - $failure"
  done
  exit 1
else
  echo "All components built successfully."
  exit 0
fi