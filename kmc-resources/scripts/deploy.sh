#!/bin/bash 
################################################################################
#
# deploy.sh - direct filesystem deployment for KMC
#
# Options:
#   -a | --all-services - deploy all KMC services 
#          (implies --kmc-crypto-service --kmc-sdls-service 
#           --kmc-sa-mgmt-service)
#   -c | --kmc-crypto-service  - Deploy KMC Crypto Service
#   -h | --help - Display deploy.sh script usage
#   -i | --img - Produce OCI Container Images for the services
#   -s | --kmc-sdls-service - Deploy KMC SDLS Service
#   -m | --kmc-sa-mgmt-service - Deploy KMC SA Management Service
#   -p DIR | --pkg DIR - Deploy local copy for packaging in DIR
#   -r | --rpm - Deploy and build RPM packages via spec in 
#           kmc-resources/packaging/rpm
#   -t DIR | --tar DIR - deploy/build tar.bz2 archive in DIR
#   -z DIR | --zip DIR - deploy/build zip archive in DIR
#
################################################################################
# Setup execution environment
source $(dirname "$0")/setenv.sh

LOCAL_DEPLOY=1
DEPLOY_FINISH=0
BUILD_RPM=0
BUILD_IMG=0

usage() {
>&2 cat << EOF
Usage: $0
  [ -a | --all-services ] - deploy ALL KMC services
  [ -c | --crypto-service ] - deploy KMC Crypto Service
  [ -h | --help ] - display deploy.sh script usage
  [ -i | --img ] - produce OCI container images for KMC Services
  [ -m | --sa-mgmt-service ] - deploy KMC SA Management Service
  [ -s | --sdls-service ] - deploy KMC SDLS REST API
  [ -p DIR | --pkg DIR ] - deploy for package building in DIR
  [ -r | --rpm ] - deploy and build RPM packages in kmc-resources/packaging/rpm
  [ -t DIR | --tar DIR ] - deploy/build tar.bz2 archive in DIR
  [ -z DIR | --zip DIR ] - deploy/build zip archive in DIR
EOF
exit 1
}

# Parse Command line arguments
args=$(getopt -a -o achimrsp:t:z: --long help,img,crypto-service,sdls-service,sa-mgmt-service,all-services,rpm,pkg:,tar:,zip: -- "$@")
if [[ $? -ne 0 ]]; then
    usage
fi

eval set -- "$args"
while [ : ]; do
  case $1 in
    -h | --help)
      usage;;
    -c | --crypto-service)
        DEPLOY_KMC_CRYPTO_SERVICE=1
        shift
        ;;
    -s | --sdls-service)
        DEPLOY_KMC_SDLS_SERVICE=1
        shift
        ;;
    -m | --sa-mgmt-service)
        DEPLOY_KMC_SA_MGMT_SERVICE=1
        shift 
        ;;
    -a | --all-services)
        DEPLOY_KMC_CRYPTO_SERVICE=1
        DEPLOY_KMC_SDLS_SERVICE=1
        DEPLOY_KMC_SA_MGMT_SERVICE=1
        shift 
        ;;
    -p | --pkg)
        DEPLOY_KMC_CRYPTO_SERVICE=1
        DEPLOY_KMC_SDLS_SERVICE=1
        DEPLOY_KMC_SA_MGMT_SERVICE=1
        PKGROOT=$2
        LOCAL_DEPLOY=0
        shift; shift;
        ;;
    -r | --rpm)
        LOCAL_DEPLOY=0
        DEPLOY_KMC_CRYPTO_SERVICE=1
        DEPLOY_KMC_SDLS_SERVICE=1
        DEPLOY_KMC_SA_MGMT_SERVICE=1
        BUILD_RPM=1
        PKGROOT="${DIST}/kmc-resources/packaging/rpm/BUILD/kmc-${VERSION}"
        [ -d "${PKGROOT}" ] && /bin/rm -rf "${PKGROOT}"
        /bin/mkdir -p "${PKGROOT}"
        shift
        ;;
    -t | --tar)
        PKGROOT=$2
        LOCAL_DEPLOY=0
	DEPLOY_FINISH=1
        ARCHIVE="tar"
        shift; shift;
        ;;
    -z | --zip)
        PKGROOT=$2
        LOCAL_DEPLOY=0
	DEPLOY_FINISH=1
        ARCHIVE="zip"
        shift; shift;
        ;;
    -i | --img)
        LOCAL_DEPLOY=0
        DEPLOY_KMC_CRYPTO_SERVICE=1
        DEPLOY_KMC_SDLS_SERVICE=1
        DEPLOY_KMC_SA_MGMT_SERVICE=1
        BUILD_IMG=1
	DEPLOY_FINISH=1
        PKGROOT="${DIST}/kmc-resources/packaging/container/kmcroot"
        [ -d "${PKGROOT}" ] && /bin/rm -rf "${PKGROOT}"
        /bin/mkdir -p "${PKGROOT}"
        shift
        ;;
    --) shift; break;;
    *)  >&2 echo Unsupported option: "$1"
        usage ;;
  esac
done

echo '-------------------------------'
echo 'KMC Deployment'
echo '-------------------------------'

if [ ${LOCAL_DEPLOY} -eq 1 ]; then
  echo '-------------------------------'
  echo 'User/Group Configuration'
  echo '-------------------------------'
  # Make sure effective UID is 0 (elevated privileges) -- local deploy requires
  # privileges to create users/groups and install files outside of 
  # user-writeable space
  if [ "${EUID}" != "0" ]; then
    echo "ERROR: KMC local deployment requires elevated (root) privileges to be able to write necessary files & directories.  Please execute ${0} with elevated privileges." >&2
    exit 1
  fi

  # Set hostnames
  CRYPTO_SERVICE_FQDN=$(/bin/hostname -f)
  SADB_FQDN="${CRYPTO_SERVICE_FQDN}"

  # Define users/groups as needed
  # Users Group
  USERSGRP="$(/bin/getent group "${USERSGRP_NAME}")"
  if [ $? -ne 0 ]; then
    # group name doesn't already exist, check gid
    USERSGRP="$(/bin/getent group "${USERSGRP_GID}")"
    if [ $? -ne 0 ]; then
      # group doesn't exist, create locally
      if [ -n "${USERSGRP_MEMBERS}" ]; then
        /bin/groupadd -g "${USERSGRP_GID}" -U "$( echo "${USERSGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' )" "${USERSGRP_NAME}"
      else
        /bin/groupadd -g "${USERSGRP_GID}" "${USERSGRP_NAME}"
      fi
    else
      # gid already exists, but with a different name
      NAME=$(echo "${USERSGRP}" | /bin/cut -f1 -d':')
      echo "WARNING: GID ${USERSGRP_GID} already exists, but is named ${NAME}.  Changing deployment config to use ${NAME} instead of ${USERSGRP_NAME} for the users group." 1>&2
      echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
      sleep 5
      USERSGRP_NAME=${NAME}
      # Make sure USERSGRP_MEMBERS are in the group membership list.  
      # Only add if group is local (in /etc/group), otherwise warn.
      if [ -n "${USERSGRP_MEMBERS}" ]; then
        # Iterate over defined members list, determine necessary additions
        CUR_MEMBERS=$(echo "${USERSGRP}" | /bin/cut -f3 -d':')
        GROUP_MEMBERS_ADD=""
        for USER in $( echo "${USERSGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' ); do
          echo "${CUR_MEMBERS}" | grep "${USER}" >/dev/null 2>&1
          if [ $? -ne 0 ]; then
            GROUP_MEMBERS_ADD+="${USER} "
          fi
        done
        /bin/grep -E "^${USERSGRP_NAME}:" /etc/group  >/dev/null 2>&1
        if [ $? -eq 0 ]; then
          # Group is locally defined.  
          /sbin/groupmod -a -U "${GROUP_MEMBERS_ADD}" "${USERSGRP_NAME}"
        else
          # Group is network defined
          echo "WARNING: Users Group (${USERSGRP_NAME}) is provided by a network-based directory (LDAP, Active Directory, etc.).  KMC Deployment cannot change group membership lists.  Users that need to be added: ${GROUP_MEMBERS_ADD}" 1>&2
        fi
      fi
    fi
  else
    # named group already exists, check to make sure the GID matches
    GID=$(echo "${USERSGRP}" | /bin/cut -f3 -d':')
    if [ "${GID}" != "${USERSGRP_GID}" ]; then
      echo "WARNING: ${USERSGRP_NAME} already exists, but is has GID ${GID}.  Changing deployment config to use ${GID} instead of ${USERSGRP_GID} for the users group GID." 1>&2
      echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
      sleep 5
      USERSGRP_GID=${GID}
      # Make sure USERSGRP_MEMBERS are in the group membership list.  
      # Only add if group is local (in /etc/group), otherwise warn.
      if [ -n "${USERSGRP_MEMBERS}" ]; then
        # Iterate over defined members list, determine necessary additions
        CUR_MEMBERS=$(echo "${USERSGRP}" | /bin/cut -f3 -d':')
        GROUP_MEMBERS_ADD=""
        for USER in $( echo "${USERSGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' ); do
          echo "${CUR_MEMBERS}" | grep "${USER}" >/dev/null 2>&1
          if [ $? -ne 0 ]; then
            GROUP_MEMBERS_ADD+="${USER} "
          fi
        done
        /bin/grep -E "^${USERSGRP_NAME}:" /etc/group  >/dev/null 2>&1
        if [ $? -eq 0 ]; then
          # Group is locally defined.  
          /sbin/groupmod -a -U "${GROUP_MEMBERS_ADD}" "${USERSGRP_NAME}"
        else
          # Group is network defined
          echo "WARNING: Users Group (${USERSGRP_NAME}) is provided by a network-based directory (LDAP, Active Directory, etc.).  KMC Deployment cannot change group membership lists.  Users that need to be added: ${GROUP_MEMBERS_ADD}" 1>&2
        fi
      fi
    fi
  fi

  # Crypto Group
  CRYPTOGRP=$(/bin/getent group "${CRYPTOGRP_NAME}")
  if [ $? -ne 0 ]; then
    # group name doesn't already exist, check gid
    CRYPTOGRP=$(/bin/getent group "${CRYPTOGRP_GID}")
    if [ $? -ne 0 ]; then
      # group doesn't exist, create locally
      if [ -n "${CRYPTOGRP_MEMBERS}" ]; then
        /bin/groupadd -g "${CRYPTOGRP_GID}" -U "$( echo "${CRYPTOGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' )" "${CRYPTOGRP_NAME}"
      else
        /bin/groupadd -g "${CRYPTOGRP_GID}" "${CRYPTOGRP_NAME}"
      fi
    else
      # gid already exists, but with a different name
      NAME=$(echo "${CRYPTOGRP}" | /bin/cut -f1 -d':')
      echo "WARNING: GID ${CRYPTOGRP_GID} already exists, but is named ${NAME}.  Changing deployment config to use ${NAME} instead of ${CRYPTOGRP_NAME} for the crypto group." 1>&2
      echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
      sleep 5
      CRYPTOGRP_NAME=${NAME}
      # Make sure CRYPTOGRP_MEMBERS are in the group membership list.  
      # Only add if group is local (in /etc/group), otherwise warn.
      if [ -n "${CRYPTOGRP_MEMBERS}" ]; then
        # Iterate over defined members list, determine necessary additions
        CUR_MEMBERS=$(echo "${CRYPTOGRP}" | /bin/cut -f3 -d':')
        GROUP_MEMBERS_ADD=""
        for USER in $( echo "${CRYPTOGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' ); do
          echo "${CUR_MEMBERS}" | grep "${USER}"  >/dev/null 2>&1
          if [ $? -ne 0 ]; then
            GROUP_MEMBERS_ADD+="${USER} "
          fi
        done
        /bin/grep -E "^${CRYPTOGRP_NAME}:" /etc/group  >/dev/null 2>&1
        if [ $? -eq 0 ]; then
          # Group is locally defined.  
          /sbin/groupmod -a -U "${GROUP_MEMBERS_ADD}" "${CRYPTOGRP_NAME}"
        else
          # Group is network defined
          echo "WARNING: Crypto Group (${CRYPTOGRP_NAME}) is provided by a network-based directory (LDAP, Active Directory, etc.).  KMC Deployment cannot change group membership lists.  Users that need to be added: ${GROUP_MEMBERS_ADD}" 1>&2
        fi
      fi
    fi
  else
    # named group already exists, check to make sure the GID matches
    GID=$(echo "${CRYPTOGRP}" | /bin/cut -f3 -d':')
    if [ "${GID}" != "${CRYPTOGRP_GID}" ]; then
      echo "WARNING: ${CRYPTOGRP_NAME} already exists, but is has GID ${GID}.  Changing deployment config to use ${GID} instead of ${CRYPTOGRP_GID} for the crypto group GID." 1>&2
      echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
      sleep 5
      CRYPTOGRP_GID=${GID}
      # Make sure CRYPTOGRP_MEMBERS are in the group membership list.  
      # Only add if group is local (in /etc/group), otherwise warn.
      if [ -n "${CRYPTOGRP_MEMBERS}" ]; then
        # Iterate over defined members list, determine necessary additions
        CUR_MEMBERS=$(echo "${CRYPTOGRP}" | /bin/cut -f3 -d':')
        GROUP_MEMBERS_ADD=""
        for USER in $( echo "${CRYPTOGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' ); do
          echo "${CUR_MEMBERS}" | grep "${USER}" >/dev/null 2>&1
          if [ $? -ne 0 ]; then
            GROUP_MEMBERS_ADD+="${USER} "
          fi
        done
        /bin/grep -E "^${CRYPTOGRP_NAME}:" /etc/group  >/dev/null 2>&1
        if [ $? -eq 0 ]; then
          # Group is locally defined.  
          /sbin/groupmod -a -U "${GROUP_MEMBERS_ADD}" "${CRYPTOGRP_NAME}"
        else
          # Group is network defined
          echo "WARNING: Crypto Group (${CRYPTOGRP_NAME}) is provided by a network-based directory (LDAP, Active Directory, etc.).  KMC Deployment cannot change group membership lists.  Users that need to be added: ${GROUP_MEMBERS_ADD}" 1>&2
        fi
      fi
    fi
  fi

  # Config Group
  CFGGROUP=$(/bin/getent group "${CFGGRP_NAME}")
  if [ $? -ne 0 ]; then
    # group name doesn't already exist, check gid
    CFGGROUP=$(/bin/getent group "${CFGGRP_GID}")
    if [ $? -ne 0 ]; then
      # group doesn't exist, create locally
      if [ -n "${CFGGRP_MEMBERS}" ]; then
        /bin/groupadd -g "${CFGGRP_GID}" -U "$( echo "${CFGGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' )" "${CFGGRP_NAME}"
      else
        /bin/groupadd -g "${CFGGRP_GID}" "${CFGGRP_NAME}"
      fi
    else
      # gid already exists, but with a different name
      NAME=$(echo "${CFGGROUP}" | /bin/cut -f1 -d':')
      echo "WARNING: GID ${CFGGRP_GID} already exists, but is named ${NAME}.  Changing deployment config to use ${NAME} instead of ${CFGGRP_NAME} for the config group." 1>&2
      echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
      sleep 5
      CFGGRP_NAME=${NAME}
      # Make sure CFGGRP_MEMBERS are in the group membership list.  
      # Only add if group is local (in /etc/group), otherwise warn.
      if [ -n "${CFGGRP_MEMBERS}" ]; then
        # Iterate over defined members list, determine necessary additions
        CUR_MEMBERS=$(echo "${CFGGROUP}" | /bin/cut -f3 -d':')
        GROUP_MEMBERS_ADD=""
        for USER in $( echo "${CFGGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' ); do
          echo "${CUR_MEMBERS}" | grep "${USER}" >/dev/null 2>&1
          if [ $? -ne 0 ]; then
            GROUP_MEMBERS_ADD+="${USER} "
          fi
        done
        /bin/grep -E "^${CFGGRP_NAME}:" /etc/group  >/dev/null 2>&1
        if [ $? -eq 0 ]; then
          # Group is locally defined.  
          /sbin/groupmod -a -U "${GROUP_MEMBERS_ADD}" "${CFGGRP_NAME}"
        else
          # Group is network defined
          echo "WARNING: Config Group (${CFGGRP_NAME}) is provided by a network-based directory (LDAP, Active Directory, etc.).  KMC Deployment cannot change group membership lists.  Users that need to be added: ${GROUP_MEMBERS_ADD}" 1>&2
        fi
      fi
    fi
  else
    # named group already exists, check to make sure the GID matches
    GID=$(echo "${CFGGROUP}" | /bin/cut -f3 -d':')
    if [ "${GID}" != "${CFGGRP_GID}" ]; then
      echo "WARNING: ${CFGGRP_NAME} already exists, but is has GID ${GID}.  Changing deployment config to use ${GID} instead of ${CFGGRP_GID} for the config group GID." 1>&2
      echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
      sleep 5
      CFGGRP_GID=${GID}
      # Make sure CFGGRP_MEMBERS are in the group membership list.  
      # Only add if group is local (in /etc/group), otherwise warn.
      if [ -n "${CFGGRP_MEMBERS}" ]; then
        # Iterate over defined members list, determine necessary additions
        CUR_MEMBERS=$(echo "${CFGGRP}" | /bin/cut -f3 -d':')
        GROUP_MEMBERS_ADD=""
        for USER in $( echo "${CFGGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' ); do
          echo "${CUR_MEMBERS}" | grep "${USER}" >/dev/null 2>&1
          if [ $? -ne 0 ]; then
            GROUP_MEMBERS_ADD+="${USER} "
          fi
        done
        /bin/grep -E "^${CFGGRP_NAME}:" /etc/group  >/dev/null 2>&1
        if [ $? -eq 0 ]; then
          # Group is locally defined.  
          /sbin/groupmod -a -U "${GROUP_MEMBERS_ADD}" "${CFGGRP_NAME}"
        else
          # Group is network defined
          echo "WARNING: Config Group (${CFGGRP_NAME}) is provided by a network-based directory (LDAP, Active Directory, etc.).  KMC Deployment cannot change group membership lists.  Users that need to be added: ${GROUP_MEMBERS_ADD}" 1>&2
        fi
      fi
    fi
  fi

  # Crypto User
  CRYPTOUSER=$(/bin/getent passwd "${CRYPTOUSR_NAME}")
  if [ $? -ne 0 ]; then
    # username does not exist, check UID
    CRYPTOUSER=$(/bin/getent passwd "${CRYPTOUSR_UID}")
    if [ $? -ne 0 ]; then
      # UID is not defined, create local user
      /bin/useradd -u "${CRYPTOUSR_UID}" -g "${CRYPTOGRP_NAME}" -d "${CRYPTOUSR_HOME}" -M -c "${CRYPTOUSR_COMMENT}" -s "${CRYPTOUSR_SHELL}" "${CRYPTOUSR_NAME}"
    else
      # UID already exists, check to see if username matches
      NAME=$(echo "$CRYPTOUSER" | /bin/cut -f1 -d':')
      echo "WARNING: UID ${CRYPTOUSR_UID} already exists, but is assigned to user ${NAME}.  Changing deployment config to use ${NAME} instead of ${CRYPTOUSR_NAME} for the service (crypto) user." 1>&2
      echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
      sleep 5
      CRYPTOUSR_NAME=${NAME}
    fi
  else
    # username already exists, check to see if UID matches
    C_UID=$(echo "${CRYPTOUSER}" | cut -f3 -d':')
    if [ "${C_UID}" != "${CRYPTOUSR_UID}" ]; then
      echo "WARNING: Username ${CRYPTOUSR_NAME} already exists, but is assigned to UID ${UID}.  Changing deployment config to use ${UID} instead of ${CRYPTOUSR_UID} for the service (crypto) user UID." 1>&2
      echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
      sleep 5
      CRYPTOUSR_UID=${C_UID}
   fi
  fi

  # Config User
  CFGUSER=$(/bin/getent passwd "${CFGUSR_NAME}")
  if [ $? -ne 0 ]; then
    # username does not exist, check UID
    CFGUSER=$(/bin/getent passwd "${CFGUSR_UID}")
    if [ $? -ne 0 ]; then
      # UID is not defined, create local user
      /bin/useradd -u "${CFGUSR_UID}" -g "${CFGGRP_NAME}" -d "${CFGUSR_HOME}" -M -c "${CFGUSR_COMMENT}" -s "${CFGUSR_SHELL}" "${CFGUSR_NAME}"
    else
      # UID already exists, check to see if username matches
      NAME=$(echo "$CFGUSER" | /bin/cut -f1 -d':')
      echo "WARNING: UID ${CFGUSR_UID} already exists, but is assigned to user ${NAME}.  Changing deployment config to use ${NAME} instead of ${CFGUSR_NAME} for the config user." 1>&2
      echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
      sleep 5
      CFGUSR_NAME=${NAME}
    fi
  else
    # username already exists, check to see if UID matches
    C_UID=$(echo "${CFGUSER}" | cut -f3 -d':')
    if [ "${C_UID}" != "${CFGUSR_UID}" ]; then
      echo "WARNING: Username ${CFGUSR_NAME} already exists, but is assigned to UID ${UID}.  Changing deployment config to use ${UID} instead of ${CFGUSR_UID} for the config user UID." 1>&2
      echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
      sleep 5
      CFGUSR_UID=${C_UID}
    fi
  fi

  INST_ROOTUSR="0"
  INST_ROOTGRP="0"
  INST_CFGUSR_UID="${CFGUSR_UID}"
  INST_USERSGRP_GID="${USERSGRP_GID}"
  INST_CRYPTOGRP_GID="${CRYPTOGRP_GID}"

  INST_PREFIX="${PREFIX}"
  INST_BINPATH="${BINPATH}"
  INST_INCPATH="${INCPATH}"
  INST_LIBPATH="${LIBPATH}"
  INST_CFGPATH="${CFGPATH}"
  INST_LOGPATH="${LOGPATH}"
  INST_SVCPATH="${SVCPATH}"
  INST_DOCPATH="${DOCPATH}"
  INST_TESTPATH="${PREFIX}/test"

  INST_CRYPTOSVC_PREFIX="${CRYPTOSVC_PREFIX}"
  INST_CRYPTOSVC_LIBPATH="${CRYTPOSVC_LIBPATH}"
  INST_CRYPTOSVC_CFGPATH="${CRYPTOSVC_CFGPATH}"
  INST_CRYPTOSVC_LOGPATH="${CRYPTOSVC_LOGPATH}"

  INST_SDLSSVC_PREFIX="${SDLSSVC_PREFIX}"
  INST_SDLSSVC_LIBPATH="${SDLSSVC_LIBPATH}"
  INST_SDLSSVC_CFGPATH="${SDLSSVC_CFGPATH}"
  INST_SDLSSVC_LOGPATH="${SDLSSVC_LOGPATH}"

  INST_SAMGMTSVC_PREFIX="${SAMGMTSVC_PREFIX}"
  INST_SAMGMTSVC_LIBPATH="${SAMGMTSVC_LIBPATH}"
  INST_SAMGMTSVC_CFGPATH="${SAMGMTSVC_CFGPATH}"
  INST_SAMGMTSVC_LOGPATH="${SAMGMTSVC_LOGPATH}"

  INST_SYSCONFIG_PATH="${SYSCONFIG_PATH}"
  INST_SYSTEMD_PATH="${SYSTEMD_PATH}"
  INST_FIREWALLD_PATH="${FIREWALLD_PATH}"
else
  # Package install -- does not require elevated privileges, just use the EUID/GUID or executing user's uid and primary gid

  if [ "${EUID}" != "0" ]; then
    INST_ROOTUSR=$(/bin/id -u)
    INST_ROOTGRP=$(/bin/id -g)
    INST_CFGUSR_UID="${INST_ROOTUSR}"
    INST_USERSGRP_GID="${INST_ROOTGRP}"
    INST_CRYPTOGRP_GID="${INST_ROOTGRP}"
  else
    INST_ROOTUSR="0"
    INST_ROOTGRP="0"
    INST_CFGUSR_UID="${CFGUSR_UID}"
    INST_USERSGRP_GID="${USERSGRP_GID}"
    INST_CRYPTOGRP_GID="${CRYPTOGRP_GID}"
  fi

  INST_PREFIX="${PKGROOT}${PREFIX}"
  INST_BINPATH="${PKGROOT}${BINPATH}"
  INST_INCPATH="${PKGROOT}${INCPATH}"
  INST_LIBPATH="${PKGROOT}${LIBPATH}"
  INST_CFGPATH="${PKGROOT}${CFGPATH}"
  INST_LOGPATH="${PKGROOT}${LOGPATH}"
  INST_SVCPATH="${PKGROOT}${SVCPATH}"
  INST_DOCPATH="${PKGROOT}${DOCPATH}"
  INST_TESTPATH="${PKGROOT}${PREFIX}/test"

  INST_CRYPTOSVC_PREFIX="${PKGROOT}${CRYPTOSVC_PREFIX}"
  INST_CRYPTOSVC_LIBPATH="${PKGROOT}${CRYTPOSVC_LIBPATH}"
  INST_CRYPTOSVC_CFGPATH="${PKGROOT}${CRYPTOSVC_CFGPATH}"
  INST_CRYPTOSVC_LOGPATH="${PKGROOT}${CRYPTOSVC_LOGPATH}"

  INST_SDLSSVC_PREFIX="${PKGROOT}${SDLSSVC_PREFIX}"
  INST_SDLSSVC_LIBPATH="${PKGROOT}${SDLSSVC_LIBPATH}"
  INST_SDLSSVC_CFGPATH="${PKGROOT}${SDLSSVC_CFGPATH}"
  INST_SDLSSVC_LOGPATH="${PKGROOT}${SDLSSVC_LOGPATH}"

  INST_SAMGMTSVC_PREFIX="${PKGROOT}${SAMGMTSVC_PREFIX}"
  INST_SAMGMTSVC_LIBPATH="${PKGROOT}${SAMGMTSVC_LIBPATH}"
  INST_SAMGMTSVC_CFGPATH="${PKGROOT}${SAMGMTSVC_CFGPATH}"
  INST_SAMGMTSVC_LOGPATH="${PKGROOT}${SAMGMTSVC_LOGPATH}"

  INST_SYSCONFIG_PATH="${PKGROOT}${SYSCONFIG_PATH}"
  INST_SYSTEMD_PATH="${PKGROOT}${SYSTEMD_PATH}"
  INST_FIREWALLD_PATH="${PKGROOT}${FIREWALLD_PATH}"

  echo '-------------------------------'
  echo 'KMC File Installation'
  echo '-------------------------------'
  /bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_SYSCONFIG_PATH}"
  /bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_SYSTEMD_PATH}"
  /bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_FIREWALLD_PATH}"
fi

# Create directory structure
/bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_PREFIX}"
/bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_BINPATH}"
/bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_INCPATH}"
/bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_LIBPATH}"
/bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_CFGUSR_UID}" "${INST_CFGPATH}"
/bin/install -d -m 0770 -g "${INST_USERSGRP_GID}" -o "${INST_ROOTUSR}" "${INST_LOGPATH}"
/bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_DOCPATH}"
/bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_PREFIX}"/test
/bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_SVCPATH}"

echo "${INST_PREFIX}"  >> "${INST_CFGPATH}/install_manifest.txt"
echo "${INST_BINPATH}" >> "${INST_CFGPATH}/install_manifest.txt"
echo "${INST_INCPATH}" >> "${INST_CFGPATH}/install_manifest.txt"
echo "${INST_LIBPATH}" >> "${INST_CFGPATH}/install_manifest.txt"
echo "${INST_CFGPATH}" >> "${INST_CFGPATH}/install_manifest.txt"
echo "${INST_LOGPATH}" >> "${INST_CFGPATH}/install_manifest.txt"
echo "${INST_DOCPATH}" >> "${INST_CFGPATH}/install_manifest.txt"
echo "${INST_PREFIX}/test" >> "${INST_CFGPATH}/install_manifest.txt"
echo "${INST_SVCPATH}" >> "${INST_CFGPATH}/install_manifest.txt"

# Deploy AMMOS-Cryptolib
if [ ! -d "${SRC_CRYPTO_LIB}/install" ]; then
  echo "WARNING: AMMOS-Cryptolib build is not complete.  Aborting deployment..." 1>&2
  exit 1
fi

/bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_CFGPATH}/sa_mariadb_sql"
/bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${SRC_CRYPTO_LIB}"/install/etc/sa_mariadb_sql/* "${INST_CFGPATH}/sa_mariadb_sql/"
/bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${SRC_CRYPTO_LIB}"/install/include/* "${INST_INCPATH}/"

echo "${INST_CFGPATH}/sa_mariadb_sql" >> "${INST_CFGPATH}/install_manifest.txt"
for FILE in `/bin/ls -1 "${INST_CFGPATH}/sa_mariadb_sql"`; do
  echo "${INST_CFGPATH}/sa_mariadb_sql/${FILE}" >> "${INST_CFGPATH}/install_manifest.txt"
done

/bin/cp -r "${SRC_CRYPTO_LIB}"/install/libcryptolib* "${INST_LIBPATH}/"
/bin/cp -r "${SRC_CRYPTO_LIB}"/install/lib/* "${INST_LIBPATH}/"
/bin/chown -R "${INST_ROOTUSR}":"${INST_ROOTGRP}" "${INST_LIBPATH}"
/bin/find "${INST_LIBPATH}" -type f -exec /bin/chmod 0644 {} \;
/bin/find "${INST_LIBPATH}" -type d -exec /bin/chmod 0755 {} \;
/bin/find "${INST_LIBPATH}" -name \*.so -exec /bin/chmod 0755 {} \;
for FILE in `/bin/ls -1 "${INST_LIBPATH}/"`; do
  echo "${INST_LIBPATH}/${FILE}" >> "${INST_CFGPATH}/install_manifest.txt"
done

/bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_TESTPATH}/bin"
echo "${INST_TESTPATH}/bin" >> "${INST_CFGPATH}/install_manifest.txt"
/bin/install -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${SRC_CRYPTO_LIB}/install/test/bin/performance_test" "${INST_TESTPATH}/bin/"
echo "${INST_TESTPATH}/bin/performance_test" >> "${INST_CFGPATH}/install_manifest.txt"
/bin/install -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${SRC_CRYPTO_LIB}/install/test/kmc_sdls_test_app.py" "${INST_TESTPATH}/bin/"
echo "${INST_TESTPATH}/bin/kmc_sdls_test_app.py" >> "${INST_CFGPATH}/install_manifest.txt"

/bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_TESTPATH}/etc"
echo "${INST_TESTPATH}/etc" >> "${INST_CFGPATH}/install_manifest.txt"
/bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_CFGUSR_UID}" "${SRC_CRYPTO_LIB}/install/test/kmc_sdls_test_app.properties" "${INST_TESTPATH}/etc/"
echo "${INST_TESTPATH}/etc/kmc_sdls_test_app.properties" >> "${INST_CFGPATH}/install_manifest.txt"

/bin/install -d -m 0755 -g "${INST_CRYPTOGRP_GID}" -o "${INST_ROOTUSR}" "${INST_TESTPATH}/sql"
echo "${INST_TESTPATH}/sql" >> "${INST_CFGPATH}/install_manifest.txt"
/bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${SRC_CRYPTO_LIB}"/install/test/test_sa_mariadb_sql/* "${INST_TESTPATH}/sql/"
for FILE in `/bin/ls -1 "${INST_TESTPATH}/sql/"`; do
  echo "${INST_TESTPATH}/sql/${FILE}" >> "${INST_CFGPATH}/install_manifest.txt"
done

# Deploy KMC Key Client
/bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${DIST}/kmc-key-client/target/kmc-key-client-${VERSION}.jar" "${INST_LIBPATH}/kmc-key-client.jar"
echo "${INST_LIBPATH}/kmc-key-client.jar" >> "${INST_CFGPATH}/install_manifest.txt"

# Deploy KMIP Client
/bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${DIST}/kmip-client/target/kmip-client-${VERSION}.jar" "${INST_LIBPATH}/kmip-client.jar"
echo "${INST_LIBPATH}/kmip-client.jar" >> "${INST_CFGPATH}/install_manifest.txt"

# Deploy KMC Crypto Library
/bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${DIST}/kmc-crypto/target/kmc-crypto-${VERSION}.jar" "${INST_LIBPATH}/kmc-crypto.jar"
/bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${DIST}/kmc-crypto-library/target/kmc-crypto-library-${VERSION}.jar" "${INST_LIBPATH}/kmc-crypto-library.jar"
echo "${INST_LIBPATH}/kmc-crypto.jar" >> "${INST_CFGPATH}/install_manifest.txt"
echo "${INST_LIBPATH}/kmc-crypto-library.jar" >> "${INST_CFGPATH}/install_manifest.txt"

# Deploy KMC SA Mgmt CLI
/bin/install -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${DIST}/kmc-sa-mgmt/kmc-sa-cli/target/appassembler/bin/kmc-sa-mgmt" "${INST_BINPATH}/"
echo "${INST_BINPATH}/kmc-sa-mgmt" >> "${INST_CFGPATH}/install_manifest.txt"
/bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${DIST}"/kmc-sa-mgmt/kmc-sa-cli/target/appassembler/lib/*.jar "${INST_LIBPATH}/"
for FILE in `ls -1 "${DIST}"/kmc-sa-mgmt/kmc-sa-cli/target/appassembler/lib/*.jar`; do
  echo "${INST_LIBPATH}/$( basename $FILE )" >> "${INST_CFGPATH}/install_manifest.txt"
done
/bin/install -m 0644 -g "${INST_USERSGRP_GID}" -o "${INST_CFGUSR_UID}" "${DIST}"/kmc-sa-mgmt/kmc-sa-cli/target/appassembler/etc/kmc-sa-mgmt-log4j2.xml "${DIST}"/kmc-sa-mgmt/kmc-sa-cli/target/appassembler/etc/kmc-sa-mgmt.properties "${INST_CFGPATH}/"
echo "${INST_CFGPATH}/kmc-sa-mgmt-log4j2.xml" >> "${INST_CFGPATH}/install_manifest.txt"
echo "${INST_CFGPATH}/kmc-sa-mgmt.properties" >> "${INST_CFGPATH}/install_manifest.txt"

/bin/install -m 0755 -g "${INST_USERSGRP_GID}" -o "${INST_ROOTUSR}" "${DIST}/kmc-resources/crypto-client/bin/create-crypto-keystore.sh" "${INST_BINPATH}/"
/bin/sed -i -e "s|__LIBPATH__|${LIBPATH}|g" -e "s|__BCFIPS_VER__|${BCFIPS_VER}|g" "${INST_BINPATH}/create-crypto-keystore.sh"
echo "${INST_BINPATH}/create-crypto-keystore.sh" >> "${INST_CFGPATH}/install_manifest.txt"
/bin/install -m 0755 -g "${INST_USERSGRP_GID}" -o "${INST_ROOTUSR}" "${DIST}/kmc-resources/crypto-client/bin/kmc-cli-common.sh" "${INST_BINPATH}/"
echo "${INST_BINPATH}/kmc-cli-common.sh" >> "${INST_CFGPATH}/install_manifest.txt"
/bin/sed -i -e "s|__CFGPATH__|${CFGPATH}|g" "${INST_BINPATH}/kmc-cli-common.sh"
/bin/install -m 0755 -g "${INST_USERSGRP_GID}" -o "${INST_ROOTUSR}" "${DIST}"/kmc-resources/crypto-client/bin/kmc-data-*.sh "${DIST}"/kmc-resources/crypto-client/bin/kmc-icv-*.sh "${INST_BINPATH}/"
echo "${INST_BINPATH}/kmc-data-decrypt.sh" >> "${INST_CFGPATH}/install_manifest.txt"
echo "${INST_BINPATH}/kmc-data-encrypt.sh" >> "${INST_CFGPATH}/install_manifest.txt"
echo "${INST_BINPATH}/kmc-icv-create.sh" >> "${INST_CFGPATH}/install_manifest.txt"
echo "${INST_BINPATH}/kmc-icv-verify.sh" >> "${INST_CFGPATH}/install_manifest.txt"

/bin/install -m 0644 -g "${INST_USERSGRP_GID}" -o "${INST_CFGUSR_UID}" "${DIST}"/kmc-resources/crypto-client/etc/* "${INST_CFGPATH}/"
for FILE in `/bin/ls "${DIST}"/kmc-resources/crypto-client/etc/`; do
  echo "${INST_CFGPATH}/${FILE}" >> "${INST_CFGPATH}/install_manifest.txt"
done

# Test Support Files
/bin/install -d -m 0755 -g "${INST_USERSGRP_GID}" -o "${INST_ROOTUSR}" "${INST_TESTPATH}/input"
echo "${INST_TESTPATH}/input" >> "${INST_CFGPATH}/install_manifest.txt"
/bin/install -m 0644 -g "${INST_USERSGRP_GID}" -o "${INST_ROOTUSR}" "${DIST}"/kmc-resources/kmc-test/input/* "${INST_TESTPATH}/input/"
for FILE in `/bin/ls -1 "${INST_TESTPATH}/input/"`; do
  echo "${INST_TESTPATH}/input/${FILE}" >> "${INST_CFGPATH}/install_manifest.txt"
done

/bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${DIST}"/kmc-resources/kmc-test/sql/* "${INST_TESTPATH}/sql/"
for FILE in `/bin/ls -1 "${DIST}"/kmc-resources/kmc-test/sql`; do
  echo "${INST_TESTPATH}/sql/${FILE}" >> "${INST_CFGPATH}/install_manifest.txt"
done

# Docs
/bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${DIST}"/README.md "${DIST}"/LICENSE "${INST_DOCPATH}/"
echo "${INST_DOCPATH}/README.md" >> "${INST_CFGPATH}/install_manifest.txt"
echo "${INST_DOCPATH}/LICENSE" >> "${INST_CFGPATH}/install_manifest.txt"
# enable when docs added
#/bin/install -m 0644 "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${DIST}"/docs/* "${INST_DOCPATH}/"
#for FILE in `/bin/ls -1 "${DIST}/docs/"`; do
#  echo "${INST_DOCPATH}/${FILE}" >> "${INST_CFGPATH}/install_manifest.txt"
#done

# Services
# Determine service management style
/bin/ls /usr/bin/systemctl  >/dev/null 2>&1
if [ $? -eq 0 ]; then
  SYSTEMD=1
else
  SYSTEMD=0

  /bin/ls /usr/sbin/chkconfig  >/dev/null 2>&1
  if [ $? -eq 0 ]; then
    CHKCONFIG=1
  else
    CHKCONFIG=0
    INITD=1
  fi
fi

# Deploy KMC Crypto Service
if [ $DEPLOY_KMC_CRYPTO_SERVICE -eq 1 ]; then
  echo '-------------------------------'
  echo 'Crypto Service Installation'
  echo '-------------------------------'
  /bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_CRYPTOSVC_PREFIX}"
  /bin/install -d -m 0750 -g "${INST_CRYPTOGRP_GID}" -o "${INST_CFGUSR_UID}" "${INST_CRYPTOSVC_CFGPATH}"
  /bin/install -d -m 0750 -g "${INST_CRYPTOGRP_GID}" -o "${INST_ROOTUSR}" "${INST_CRYPTOSVC_LIBPATH}"
  /bin/install -d -m 2770 -g "${INST_CRYPTOGRP_GID}" -o "${INST_ROOTUSR}" "${INST_CRYPTOSVC_LOGPATH}"
  /bin/install -d -m 2770 -g "${INST_CRYPTOGRP_GID}" -o "${INST_ROOTUSR}" "${INST_CRYPTOSVC_PREFIX}/work/Tomcat/localhost"
  echo "${INST_CRYPTOSVC_PREFIX}" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_CRYPTOSVC_CFGPATH}" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_CRYPTOSVC_LIBPATH}" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_CRYPTOSVC_LOGPATH}" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_CRYPTOSVC_PREFIX}/work/Tomcat/localhost" >> "${INST_CFGPATH}/install_manifest.txt"

  /bin/install -m 0640 -g "${INST_CRYPTOGRP_GID}" -o "${INST_ROOTUSR}" "${DIST}/kmc-crypto-service/target/crypto-service.jar" "${INST_CRYPTOSVC_LIBPATH}/"
  echo "${INST_CRYPTOSVC_LIBPATH}/crypto-service.jar" >> "${INST_CFGPATH}/install_manifest.txt"

  /bin/install -m 0640 -g "${INST_CRYPTOGRP_GID}" -o "${INST_CFGUSR_UID}" "${DIST}/kmc-resources/crypto-service/kmc-crypto.cfg" "${DIST}/kmc-resources/crypto-service/kmc-crypto-service.properties" "${DIST}/kmc-resources/crypto-service/kmc-crypto-service-log4j2.xml" "${INST_CRYPTOSVC_CFGPATH}/"
  echo "${INST_CRYPTOSVC_CFGPATH}/kmc-crypto.cfg" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_CRYPTOSVC_CFGPATH}/kmc-crypto-service.properties" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_CRYPTOSVC_CFGPATH}/kmc-crypto-service-log4j2.xml" >> "${INST_CFGPATH}/install_manifest.txt"
  /bin/sed -i -e "s|/ammos/kmc-crypto-service/logs|${CRYPTOSVC_LOGPATH}|g" "${INST_CRYPTOSVC_CFGPATH}/kmc-crypto-service-log4j2.xml"

  if [ -d "${INST_SYSCONFIG_PATH}" ]; then
    /bin/install -m 0640 -g "${INST_CRYPTOGRP_GID}" -o "${INST_CFGUSR_UID}" "${DIST}/kmc-resources/crypto-service/kmc-crypto-service.sysconfig" "${INST_SYSCONFIG_PATH}/kmc-crypto-service"
    echo "${INST_SYSCONFIG_PATH}/kmc-crypto-service" >> "${INST_CFGPATH}/install_manifest.txt"
    /bin/sed -i -e "s|__LOGPATH__|${CRYPTOSVC_LOGPATH}|g" -e "s|__LIBPATH__|${LIBPATH}|g" -e "s|__CRYPTOUSR__|${CRYPTOUSR_NAME}|g" -e "s|__CRYPTOGRP__|${CRYPTOGRP_NAME}|g" "${INST_SYSCONFIG_PATH}/kmc-crypto-service"
  fi

  if [ ${SYSTEMD} -eq 1 ]; then
    /bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${DIST}/kmc-resources/crypto-service/kmc-crypto-service.service" "${INST_SYSTEMD_PATH}/kmc-crypto-service.service"
    echo "${INST_SYSTEMD_PATH}/kmc-crypto-service.service" >> "${INST_CFGPATH}/install_manifest.txt"
    /bin/sed -i -e "s|__SVCPATH__|${SVCPATH}|g" -e "s|__CFGPATH__|${CRYPTOSVC_CFGPATH}|g" -e "s|__LOGPATH__|${CRYPTOSVC_LOGPATH}|g" -e "s|__CRYPTOUSR__|${CRYPTOUSR_NAME}|g" -e "s|__CRYPTOGRP__|${CRYPTOGRP_NAME}|g" "${INST_SYSTEMD_PATH}/kmc-crypto-service.service"
    [ $LOCAL_DEPLOY -eq 1 ] && /bin/systemctl daemon-reload
  elif [ ${CHKCONFIG} -eq 1 ]; then
    # Add chkconfig-style init script
    echo 'noop - chkconfig'
  else
    # Add basic init.d init script
    echo 'noop - init.d'
  fi

  if [ -d "${INST_FIREWALLD_PATH}" ]; then
    /bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${SRC_RSC}/crypto-service/kmc-crypto-service.xml" "${INST_FIREWALLD_PATH}/kmc-crypto-service.xml"
    /bin/sed -i -e "s|__CS_PORT__|${KMC_CRYPTO_SERVICE_PORT}|g" "${INST_FIREWALLD_PATH}/kmc-crypto-service.xml"
    echo "${INST_FIREWALLD_PATH}/kmc-crypto-service.xml" >> "${INST_CFGPATH}/install_manifest.txt"
    [ $LOCAL_DEPLOY -eq 1 ] && /bin/firewall-cmd --daemon-reload
  fi
fi

# Deploy KMC SDLS Service
if [ $DEPLOY_KMC_SDLS_SERVICE -eq 1 ]; then
  echo '-------------------------------'
  echo 'SDLS Service Installation'
  echo '-------------------------------'
  /bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_SDLSSVC_PREFIX}"
  /bin/install -d -m 2750 -g "${INST_CRYPTOGRP_GID}" -o "${INST_CFGUSR_UID}" "${INST_SDLSSVC_CFGPATH}"
  /bin/install -d -m 0750 -g "${INST_CRYPTOGRP_GID}" -o "${INST_ROOTUSR}" "${INST_SDLSSVC_LIBPATH}"
  /bin/install -d -m 2770 -g "${INST_CRYPTOGRP_GID}" -o "${INST_ROOTUSR}" "${INST_SDLSSVC_LOGPATH}"
  /bin/install -d -m 2770 -g "${INST_CRYPTOGRP_GID}" -o "${INST_ROOTUSR}" "${INST_SDLSSVC_PREFIX}/work/Tomcat/localhost/ROOT"
  
  echo "${INST_SDLSSVC_PREFIX}" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_SDLSSVC_CFGPATH}" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_SDLSSVC_LIBPATH}" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_SDLSSVC_LOGPATH}" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_SDLSSVC_PREFIX}/work/Tomcat/localhost/ROOT" >> "${INST_CFGPATH}/install_manifest.txt"
  
  /bin/install -m 0640 -g "${INST_CRYPTOGRP_GID}" -o "${INST_ROOTUSR}" "${DIST}/kmc-sdls-service/target/sdls-service.jar" "${INST_SDLSSVC_LIBPATH}/"
  echo "${INST_SDLSSVC_LIBPATH}/sdls-service.jar" >> "${INST_CFGPATH}/install_manifest.txt"
  /bin/install -m 0640 -g "${INST_CRYPTOGRP_GID}" -o "${INST_CFGUSR_UID}" "${DIST}/kmc-resources/sdls-service/kmc-sdls-service.properties" "${DIST}/kmc-resources/sdls-service/kmc-sdls-service-log4j2.xml" "${INST_SDLSSVC_CFGPATH}/"
  echo "${INST_SDLSSVC_CFGPATH}/kmc-sdls-service.properties" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_SDLSSVC_CFGPATH}/kmc-sdls-service-log4j2.xml" >> "${INST_CFGPATH}/install_manifest.txt"
  [ ${LOCAL_DEPLOY} -eq 1 ] && /bin/sed -i -e "s|#cryptolib.sadb.mariadb.fqdn=localhost|cryptolib.sadb.mariadb.fqdn=${SADB_FQDN}|g" -e "s|#cryptolib.crypto.kmccryptoservice.fqdn=crypto123.example.com|cryptolib.crypto.kmccryptoservice.fqdn=${CRYPTO_SERVICE_FQDN}|g" "${INST_SDLSSVC_CFGPATH}/kmc-sdls-service.properties"
  /bin/sed -i -e "s|/var/ammos/kmc/log/services/sdls-service|${SDLSSVC_LOGPATH}|g" "${INST_SDLSSVC_CFGPATH}/kmc-sdls-service-log4j2.xml"

  if [ -d "${INST_SYSCONFIG_PATH}" ]; then
    /bin/install -m 0640 -g "${INST_CRYPTOGRP_GID}" -o "${INST_CFGUSR_UID}" "${DIST}/kmc-resources/sdls-service/kmc-sdls-service.sysconfig" "${INST_SYSCONFIG_PATH}/kmc-sdls-service"
    echo "${INST_SYSCONFIG_PATH}/kmc-sdls-service" >> "${INST_CFGPATH}/install_manifest.txt"
    /bin/sed -i -e "s|__LOGPATH__|${SDLSSVC_LOGPATH}|g" -e "s|__LIBPATH__|${LIBPATH}|g" -e "s|__CFGPATH__|${SDLSSVC_CFGPATH}|g" -e "s|__CRYPTOUSR__|${CRYPTOUSR_NAME}|g" -e "s|__CRYPTOGRP__|${CRYPTOGRP_NAME}|g" "${INST_SYSCONFIG_PATH}/kmc-sdls-service"
  fi

  if [ ${SYSTEMD} -eq 1 ]; then
    /bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${DIST}/kmc-resources/sdls-service/kmc-sdls-service.service" "${INST_SYSTEMD_PATH}/kmc-sdls-service.service"
    echo "${INST_SYSTEMD_PATH}/kmc-sdls-service.service" >> "${INST_CFGPATH}/install_manifest.txt"
    /bin/sed -i -e "s|__SVCPATH__|${SVCPATH}|g" -e "s|__CFGPATH__|${SDLSSVC_CFGPATH}|g" -e "s|__LOGPATH__|${SDLSSVC_LOGPATH}|g" -e "s|__CRYPTOUSR__|${CRYPTOUSR_NAME}|g" -e "s|__CRYPTOGRP__|${CRYPTOGRP_NAME}|g" "${INST_SYSTEMD_PATH}/kmc-sdls-service.service"
    [ $LOCAL_DEPLOY -eq 1 ] && /bin/systemctl daemon-reload
  elif [ ${CHKCONFIG} -eq 1 ]; then
    # Add chkconfig-style init script
    echo 'noop - chkconfig'
  else
    # Add basic init.d init script
    echo 'noop - init.d'
  fi

  if [ -d "${INST_FIREWALLD_PATH}" ]; then
    /bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${SRC_RSC}/sdls-service/kmc-sdls-service.xml" "${INST_FIREWALLD_PATH}/kmc-sdls-service.xml"
    /bin/sed -i -e "s|__S_PORT__|${KMC_SDLS_SERVICE_PORT}|g" "${INST_FIREWALLD_PATH}/kmc-sdls-service.xml"
    echo "${INST_FIREWALLD_PATH}/kmc-sdls-service.xml" >> "${INST_CFGPATH}/install_manifest.txt"
    [ $LOCAL_DEPLOY -eq 1 ] && /bin/firewall-cmd --daemon-reload
  fi
fi

# Deploy KMC SA Mgmt Service
if [ $DEPLOY_KMC_SA_MGMT_SERVICE -eq 1 ]; then
  echo '-------------------------------'
  echo 'SA Management Service Installation'
  echo '-------------------------------'
  /bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_SAMGMTSVC_PREFIX}"
  /bin/install -d -m 2750 -g "${INST_CRYPTOGRP_GID}" -o "${INST_CFGUSR_UID}" "${INST_SAMGMTSVC_CFGPATH}"
  /bin/install -d -m 0750 -g "${INST_CRYPTOGRP_GID}" -o "${INST_ROOTUSR}" "${INST_SAMGMTSVC_LIBPATH}"
  /bin/install -d -m 2770 -g "${INST_CRYPTOGRP_GID}" -o "${INST_ROOTUSR}" "${INST_SAMGMTSVC_LOGPATH}"
  /bin/install -d -m 2770 -g "${INST_CRYPTOGRP_GID}" -o "${INST_ROOTUSR}" "${INST_SAMGMTSVC_PREFIX}/work/Tomcat/localhost/ROOT"
  
  echo "${INST_SAMGMTSVC_PREFIX}" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_SAMGMTSVC_CFGPATH}" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_SAMGMTSVC_LIBPATH}" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_SAMGMTSVC_LOGPATH}" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_SAMGMTSVC_PREFIX}/work/Tomcat/localhost/ROOT" >> "${INST_CFGPATH}/install_manifest.txt"

  /bin/install -m 0640 -g "${INST_CRYPTOGRP_GID}" -o "${INST_ROOTUSR}" "${DIST}/kmc-sa-mgmt/kmc-sa-gui/target/kmc-sa-mgmt.jar" "${INST_SAMGMTSVC_LIBPATH}/"
  echo "${INST_SAMGMTSVC_LIBPATH}/kmc-sa-mgmt.jar" >> "${INST_CFGPATH}/install_manifest.txt"
  /bin/install -m 0640 -g "${INST_CRYPTOGRP_GID}" -o "${INST_CFGUSR_UID}" "${DIST}/kmc-resources/sa-mgmt-service/kmc-sa-mgmt-service.properties" "${DIST}/kmc-resources/sa-mgmt-service/kmc-sa-mgmt-service-log4j2.xml" "${INST_SAMGMTSVC_CFGPATH}/"
  echo "${INST_SAMGMTSVC_CFGPATH}/kmc-sa-mgmt-service.properties" >> "${INST_CFGPATH}/install_manifest.txt"
  echo "${INST_SAMGMTSVC_CFGPATH}/kmc-sa-mgmt-service-log4j2.xml" >> "${INST_CFGPATH}/install_manifest.txt"
  [ ${LOCAL_DEPLOY} -eq 1 ] && /bin/sed -i -e "s|#db.host=localhost|db.host=${SADB_FQDN}|g" "${INST_SAMGMTSVC_CFGPATH}/kmc-sa-mgmt-service.properties"
  /bin/sed -i -e "s|/var/ammos/kmc/log/services/kmc-sa-mgmt|${SAMGMTSVC_LOGPATH}|g" "${INST_SAMGMTSVC_CFGPATH}/kmc-sa-mgmt-service-log4j2.xml"

  if [ -d "${INST_SYSCONFIG_PATH}" ]; then
    /bin/install -m 0640 -g "${INST_CRYPTOGRP_GID}" -o "${INST_CFGUSR_UID}" "${DIST}/kmc-resources/sa-mgmt-service/kmc-sa-mgmt-service.sysconfig" "${INST_SYSCONFIG_PATH}/kmc-sa-mgmt-service"
    echo "${INST_SYSCONFIG_PATH}/kmc-sa-mgmt-service" >> "${INST_CFGPATH}/install_manifest.txt"
    /bin/sed -i -e "s|__LOGPATH__|${SAMGMTSVC_LOGPATH}|g" -e "s|__LIBPATH__|${LIBPATH}|g" -e "s|__CFGPATH__|${SAMGMTSVC_CFGPATH}|g" -e "s|__CRYPTOUSR__|${CRYPTOUSR_NAME}|g" -e "s|__CRYPTOGRP__|${CRYPTOGRP_NAME}|g" "${INST_SYSCONFIG_PATH}/kmc-sa-mgmt-service"
  fi

  if [ ${SYSTEMD} -eq 1 ]; then
    /bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${DIST}/kmc-resources/sa-mgmt-service/kmc-sa-mgmt-service.service" "${INST_SYSTEMD_PATH}/kmc-sa-mgmt.service"
    echo "${INST_SYSTEMD_PATH}/kmc-sa-mgmt-service.service" >> "${INST_CFGPATH}/install_manifest.txt"
    /bin/sed -i -e "s|__SVCPATH__|${SAMGMTSVC_LIBPATH}|g"  -e "s|__LOGPATH__|${SAMGMTSVC_LOGPATH}|g" -e "s|__CRYPTOUSR__|${CRYPTOUSR_NAME}|g" -e "s|__CRYPTOGRP__|${CRYPTOGRP_NAME}|g" -e "s|__CFGPATH__|${SAMGMTSVC_CFGPATH}|g" "${INST_SYSTEMD_PATH}/kmc-sa-mgmt.service"
    [ $LOCAL_DEPLOY -eq 1 ] && /bin/systemctl daemon-reload
  elif [ ${CHKCONFIG} -eq 1 ]; then
    # Add chkconfig-style init script
    echo 'noop - chkconfig'
  else
    # Add basic init.d init script
    echo 'noop - init.d'
  fi

  if [ -d "${INST_FIREWALLD_PATH}" ]; then
    /bin/install -m 0644 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${SRC_RSC}/sa-mgmt-service/kmc-sa-mgmt-service.xml" "${INST_FIREWALLD_PATH}/kmc-sa-mgmt-service.xml"
    /bin/sed -i -e "s|__SA_PORT__|${KMC_SAMGMT_SERVICE_PORT}|g" "${INST_FIREWALLD_PATH}/kmc-sa-mgmt-service.xml"
    echo "${INST_FIREWALLD_PATH}/kmc-sa-mgmt-service.xml" >> "${INST_CFGPATH}/install_manifest.txt"
    [ $LOCAL_DEPLOY -eq 1 ] && /bin/firewall-cmd --daemon-reload
  fi
fi

if [ ${DEPLOY_FINISH} -eq 1 ]; then
  /bin/install -d -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${INST_LIBPATH}/deploy"
  /bin/install -m 0755 -g "${INST_ROOTGRP}" -o "${INST_ROOTUSR}" "${SRC_RSC}/scripts/finish_deploy.sh" "${SRC_RSC}/scripts/setenv.sh" "${INST_LIBPATH}/deploy/"
fi

if [ -n "${ARCHIVE}" ]; then
  # Archive deployment selected.  Build archive from deployed files in PKGROOT
  echo '-------------------------------'
  echo 'Archive Build'
  echo '-------------------------------'
  pushd ${PKGROOT} >/dev/null 2>&1
  case "${ARCHIVE}" in
    tar)
      ARCHIVE_FILE="kmc-${VERSION}.tar.bz2"
      /bin/tar -cj --exclude "${ARCHIVE_FILE}" -f "${ARCHIVE_FILE}" * >/dev/null 2>&1
      ;;
    zip)
      ARCHIVE_FILE="kmc-${VERSION}.zip"
      /bin/zip --exclude "${ARCHIVE_FILE}" "${ARCHIVE_FILE}" -r *
      ;;
    *)
      echo "ARCHIVE Deployment: Invalid Archive Type selected (${ARCHIVE})" 1>2
      exit 1
      ;;
  esac
  if [ $? -eq 0 ]; then
    echo "ARCHIVE Deployment: Complete.  Archive File: ${PWD}/${ARCHIVE_FILE}"
    CLEANUP=$(ls -1 | grep -v "${ARCHIVE_FILE}")
    /bin/rm -rf "${CLEANUP}"
    exit 0
  else
    echo "ARCHIVE Deployment: Incomplete.  Some error prevented full creation of the archive: ${ARCHIVE_FILE}."
    exit 1
  fi
fi

if [ ${BUILD_RPM} -eq 1 ]; then
  # Build RPM Packages
  echo '-------------------------------'
  echo 'RPM Package Build'
  echo '-------------------------------'
  pushd ${SRC_RSC}/packaging/rpm/SPECS >/dev/null 2>&1
  GRPMEMBERS=""
  [ -n "${CRYPTOGRP_MEMBERS}" ] && GRPMEMBERS="${GRPMEMBERS} --define \"cryptogrp_members '${CRYPTOGRP_MEMBERS}'\""
  [ -n "${CFGGRP_MEMBERS}" ] && GRPMEMBERS="${GRPMEMBERS} --define \"cfggrp_members '${CFGGRP_MEMBERS}'\""
  [ -n "${USERSGRP_MEMBERS}" ] && GRPMEMBERS="${GRPMEMBERS} --define \"usersgrp_members '${USERSGRP_MEMBERS}'\""
  QA_RPATHS=$(( 0x0001|0x0002 )) /bin/rpmbuild -ba \
  --define "_topdir ${SRC_RSC}/packaging/rpm" \
  --define "buildroot %{_buildrootdir}/%{NAME}-%{VERSION}" \
  --define "kmc_version ${VERSION}" \
  --define "cryptogrp ${CRYPTOGRP_NAME}" \
  --define "cryptogrp_gid ${CRYPTOGRP_GID}" \
  --define "cryptogrp_members '${CRYPTOGRP_MEMBERS}'" \
  --define "cfggrp ${CFGGRP_NAME}" \
  --define "cfggrp_gid ${CFGGRP_GID}" \
  --define "cfggrp_members '${CFGGRP_MEMBERS}'" \
  --define "usersgrp ${USERSGRP_NAME}" \
  --define "usersgrp_gid ${USERSGRP_GID}" \
  --define "usersgrp_members '${USERSGRP_MEMBERS}'" \
  --define "cfgusr ${CFGUSR_NAME}" \
  --define "cfgusr_uid ${CFGUSR_UID}" \
  --define "cfgusr_home ${CFGUSR_HOME}" \
  --define "cfgusr_shell ${CFGUSR_SHELL}" \
  --define "cfgusr_comment '${CFGUSR_COMMENT}'" \
  --define "cryptousr ${CRYPTOUSR_NAME}" \
  --define "cryptousr_uid ${CRYPTOUSR_UID}" \
  --define "cryptousr_home ${CRYPTOUSR_HOME}" \
  --define "cryptousr_shell ${CRYPTOUSR_SHELL}" \
  --define "cryptousr_comment '${CRYPTOUSR_COMMENT}'" \
  --define "_prefix ${PREFIX}" \
  --define "cfgpath ${CFGPATH}" \
  --define "binpath ${BINPATH}" \
  --define "libpath ${LIBPATH}" \
  --define "logpath ${LOGPATH}" \
  --define "testpath ${TESTPATH}" \
  --define "systemd_path ${SYSTEMD_PATH}" \
  --define "sysconfig_path ${SYSCONFIG_PATH}" \
  --define "firewalld_path ${FIREWALLD_PATH}" \
  --define "docpath ${DOCPATH}" \
  --define "incpath ${INCPATH}"  \
  --define "cryptosvc_prefix ${CRYPTOSVC_PREFIX}" \
  --define "cryptosvc_cfgpath ${CRYPTOSVC_CFGPATH}" \
  --define "cryptosvc_logpath ${CRYPTOSVC_LOGPATH}" \
  --define "sdlssvc_prefix ${SDLSSVC_PREFIX}" \
  --define "sdlssvc_cfgpath ${SDLSSVC_CFGPATH}" \
  --define "sdlssvc_logpath ${SDLSSVC_LOGPATH}" \
  --define "samgmtsvc_prefix ${SAMGMTSVC_PREFIX}" \
  --define "samgmtsvc_cfgpath ${SAMGMTSVC_CFGPATH}" \
  --define "samgmtsvc_logpath ${SAMGMTSVC_LOGPATH}" \
  ${GRPMEMBERS} kmc.spec

  if [ $? -eq 0 ]; then
    echo "RPM Build successful.  RPM packages located in ${SRC_RSC}/packaging/rpm/RPMS/x86_64."
  fi
  popd >/dev/null 2>&1
  exit 0
fi

if [ ${BUILD_IMG} -eq 1 ]; then
  # Build Container images for KMC Services
  BCFIPS_VER=$(/bin/grep -A1 bc-fips "${DIST}/kmc-sdls-service/pom.xml" | /bin/grep version | /bin/cut -f2 -d'>' | /bin/cut -f1 -d'<')
  /bin/sed -e "s|__BCFIPS_VER__|${BCFIPS_VER}|g" -e "s|__CRYPTO_UID__|${CRYPTOUSR_UID}|g" -e "s|__CRYPTO_GID__|${CRYPTOGRP_GID}|g" ${SRC_RSC}/packaging/container/crypto-service/bin/crypto_service_setup.sh.tmp > ${SRC_RSC}/packaging/container/crypto-service/bin/crypto_service_setup.sh
  /bin/sed -e "s|__BCFIPS_VER__|${BCFIPS_VER}|g" -e "s|__CRYPTO_UID__|${CRYPTOUSR_UID}|g" -e "s|__CRYPTO_GID__|${CRYPTOGRP_GID}|g" ${SRC_RSC}/packaging/container/sdls-service/bin/sdls_service_setup.sh.tmp > ${SRC_RSC}/packaging/container/sdls-service/bin/sdls_service_setup.sh
  /bin/sed -e "s|__BCFIPS_VER__|${BCFIPS_VER}|g" -e "s|__CRYPTO_UID__|${CRYPTOUSR_UID}|g" -e "s|__CRYPTO_GID__|${CRYPTOGRP_GID}|g" ${SRC_RSC}/packaging/container/sa-mgmt-service/bin/sa_mgmt_service_setup.sh.tmp > ${SRC_RSC}/packaging/container/sa-mgmt-service/bin/sa_mgmt_service_setup.sh

  GIT_COMMIT=$(/bin/git log | /bin/head -1 | /bin/cut -f2 -d' ')
  echo '-------------------------------'
  echo 'Crypto Service Container'
  echo '-------------------------------'
  pushd ${SRC_RSC}/packaging/container/crypto-service >/dev/null 2>&1
  /bin/cp -r ../kmcroot .
  CRYPTO_BUILD=$("${CONTAINER_EXEC}" build -t "kmc-crypto-service:${VERSION}" \
    --env VERSION="${VERSION}" --env GIT_COMMIT="${GIT_COMMIT}" . ) 

  CRYPTO_IMG=$("${CONTAINER_EXEC}" image list -n --digests "kmc-crypto-service:${VERSION}")
  if [ -n "${CRYPTO_IMG}" ]; then
    echo "Crypto Service Container build complete:"
    echo "${CRYPTO_IMG}"
    /bin/rm -rf kmcroot
  else
     echo "Crypto Service Container build FAILED."
     echo "Build Log:"
     echo "${CRYPTO_BUILD}"
     exit 1
  fi
  popd >/dev/null 2>&1
  
  echo '-------------------------------'
  echo 'SDLS Service Container'
  echo '-------------------------------'
  pushd ${SRC_RSC}/packaging/container/sdls-service >/dev/null 2>&1
  /bin/cp -r ../kmcroot .
  SDLS_BUILD=$("${CONTAINER_EXEC}" build -t "kmc-sdls-service:${VERSION}" \
    --env VERSION="${VERSION}" --env GIT_COMMIT="${GIT_COMMIT}" . )

  SDLS_IMG=$("${CONTAINER_EXEC}" image list -n --digests "kmc-sdls-service:${VERSION}")
  if [ -n "${SDLS_IMG}" ]; then
    echo "SDLS Service Container build complete:"
    echo "${SDLS_IMG}"
    /bin/rm -rf kmcroot
  else
     echo "SDLS Service Container build FAILED."
     echo "Build Log:"
     echo "${SDLS_BUILD}"
     exit 1
  fi
  popd >/dev/null 2>&1
  
  echo '-------------------------------'
  echo 'SA Management Container'
  echo '-------------------------------'
  pushd ${SRC_RSC}/packaging/container/sa-mgmt-service >/dev/null 2>&1
  /bin/cp -r ../kmcroot .
  SAMGMT_BUILD=$("${CONTAINER_EXEC}" build -t "kmc-sa-mgmt-service:${VERSION}" \
    --env VERSION="${VERSION}" --env GIT_COMMIT="${GIT_COMMIT}" . ) 

  SAMGMT_IMG=$("${CONTAINER_EXEC}" image list -n --digests "kmc-sa-mgmt-service:${VERSION}")
  if [ -n "${SDLS_IMG}" ]; then
    echo "SA Management Service Container build complete:"
    echo "${SAMGMT_IMG}"
    /bin/rm -rf kmcroot
  else
     echo "SA Management Service Container build FAILED."
     echo "Build Log:"
     echo "${SAMGMT_BUILD}"
     exit 1
  fi
  [ -d ${SRC_RSC}/packaging/container ] && /bin/rm -rf ${SRC_RSC}/packaging/container/kmcroot
  popd >/dev/null 2>&1
  exit 0
fi
