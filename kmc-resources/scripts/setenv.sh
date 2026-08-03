VERSION=4.0.0

# Deployment Settings
CRYPTO_SERVICE_FQDN="crypto.example.com"
SADB_FQDN="sadb.example.com"

PREFIX="/opt/ammos/kmc"
BINPATH="${PREFIX}/bin"
INCPATH="${PREFIX}/include"
export LIBPATH="${PREFIX}/lib"
CFGPATH="${PREFIX}/etc"
LOGPATH="${PREFIX}/logs"
TESTPATH="${PREFIX}/test"
SVCPATH="${PREFIX}/services"
DOCPATH="${PREFIX}/docs"

CRYPTOSVC_PREFIX="${SVCPATH}/crypto-service"
CRYTPOSVC_LIBPATH="${CRYPTOSVC_PREFIX}/lib"
CRYPTOSVC_CFGPATH="${CRYPTOSVC_PREFIX}/etc"
CRYPTOSVC_LOGPATH="${CRYPTOSVC_PREFIX}/logs"

SDLSSVC_PREFIX="${SVCPATH}/sdls-service"
SDLSSVC_LIBPATH="${SDLSSVC_PREFIX}/lib"
SDLSSVC_CFGPATH="${SDLSSVC_PREFIX}/etc"
SDLSSVC_LOGPATH="${SDLSSVC_PREFIX}/logs"

SAMGMTSVC_PREFIX="${SVCPATH}/sa-mgmt"
SAMGMTSVC_LIBPATH="${SAMGMTSVC_PREFIX}/lib"
SAMGMTSVC_CFGPATH="${SAMGMTSVC_PREFIX}/etc"
SAMGMTSVC_LOGPATH="${SAMGMTSVC_PREFIX}/logs"

SYSCONFIG_PATH="/etc/sysconfig"
SYSTEMD_PATH="/lib/systemd/system"
FIREWALLD_PATH="/etc/firewalld/services"

DEPLOY_KMC_CRYPTO_SERVICE=0
DEPLOY_KMC_SDLS_SERVICE=0
DEPLOY_KMC_SA_MGMT_SERVICE=0

SERVICE_JRE=/usr/lib/jvm/jre-17-openjdk

KMC_CRYPTO_SERVICE_PORT=8443
KMC_SDLS_SERVICE_PORT=8445
KMC_SAMGMT_SERVICE_PORT=8447

# Deployment Users/Groups
  # Crypto Group - group controls access to KMC Service logs and sensitive files
  CRYPTOGRP_NAME="crypto"
  CRYPTOGRP_GID=59111
  # cryptogrp_members is a comma separated list of users to verify membership 
  CRYPTOGRP_MEMBERS=""

  # Crypto User - KMC service run-as user
  CRYPTOUSR_NAME="crypto"
  CRYPTOUSR_UID=59111
  CRYPTOUSR_HOME="${PREFIX}"
  CRYPTOUSR_SHELL="/sbin/nologin"
  CRYPTOUSR_COMMENT="KMC Service User"

  # Config Group - group for KMC configuration files
  CFGGRP_NAME="mgsscm"
  CFGGRP_GID=59105
  # cfggrp_members is a comma separated list of users to verify membership 
  CFGGRP_MEMBERS=""

  # Config User - owns KMC configuration files
  CFGUSR_NAME="mgsscm"
  CFGUSR_UID=59105
  CFGUSR_HOME="${PREFIX}"
  CFGUSR_SHELL="/bin/bash"
  CFGUSR_COMMENT="KMC_Configuration_User"

  # Users Group - for CLI logging
  USERSGRP_NAME="users"
  USERSGRP_GID=100
  # usersgrp_members is a comma separated list of users to verify membership 
  USERSGRP_MEMBERS=""

# Build Settings
if [ -z "${JAVA_HOME}" ]; then
  export JAVA_HOME=/lib/jvm/java-17-openjdk
fi
# PYTHON_INT should point to a Python 3.x interpreter.  
# Python 3.9 is the default Python version on RHEL 9.
PYTHON_INT=/usr/bin/python
if [ -r ${PYTHON_INT} ]; then
  PYTHON_VER=`${PYTHON_INT} -V | /bin/cut -f2 -d' ' | /bin/cut -f1-2 -d'.'`
  PYTHONPATH="${LIBPATH}/python${PYTHON_VER}/site-packages"
fi


# Container settings
# CONTAINER_EXEC should point to the binary to produce container images locally
# Docker is also functional
CONTAINER_EXEC="/bin/podman"

# Do not edit settings below -- needed for build steps
pushd $(dirname "$0") >/dev/null 2>&1
if [ -x /usr/bin/git ]; then
  ROOT=$(git rev-parse --show-toplevel)
  DIST=$ROOT
  BUILD_DIR=$DIST/build$BUILD/build
  RAW_DIR=$DIST/build$BUILD/raw
  SRC_KMC=$DIST
  SRC_KMIP=$DIST/kmip-client
  SRC_CRYPTO_LIB=$DIST/ammos-cryptolib
  SRC_RSC=$SRC_KMC/kmc-resources
fi

