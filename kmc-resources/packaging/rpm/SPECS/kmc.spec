Name:           kmc
Version:        %{kmc_version}
Release:        1%{?dist}
Summary:        AMMOS Data Cryptography Services (DCS)

AutoReqProv:    no
Provides:       ammos(kmc) = %{version}-%{release}

License:        Apache 2.0
URL:            https://github.com/NASA-AMMOS/DCS

%description
AMMOS DCS provides data-level encryption/decryption and integrity check value (ICV) creation/ verification capabilities for securing data at-rest (and in-transit, as an additional layer of protection over secure connections), and implements the CCSDS Space Data Link Security (SDLS) protocol for doing transfer frame encryption.

%package        crypto-service
Summary:        AMMOS Data Cryptography Services (DCS)
Version:        %{version}
License:        Apache 2.0
AutoReqProv:    no
Provides:       ammos(kmc)(crypto-service) = %{version}-%{release}
Requires:       ammos(kmc) = %{version}-%{release}

%description    crypto-service
AMMOS Key Management & Cryptography Crypto Service

%package        sdls-service
Summary:        AMMOS Key Management & Cryptography SDLS Service
Version:        %{version}
License:        Apache 2.0
AutoReqProv:    no
Provides:       ammos(kmc)(sdls-service) = %{version}-%{release}
Requires:       ammos(kmc) = %{version}-%{release}

%description    sdls-service
AMMOS Key Management & Cryptography SDLS Service

%package        sa-mgmt-service
Summary:        AMMOS Key Management & Cryptography SA Management Service
Version:        %{version}
License:        Apache 2.0
AutoReqProv:    no
Provides:       ammos(kmc)(sa-mgmt-service) = %{version}-%{release}
Requires:       ammos(kmc) = %{version}-%{release}

%description    sa-mgmt-service
AMMOS Key Management & Cryptography Security Association (SA) Management Service

%prep

%build

%install
[ -d %{buildroot} ] && /bin/rm -rf %{buildroot}
/bin/mkdir -p %{buildroot}
/bin/cp -rp %{_builddir}/%{NAME}-%{VERSION}/* %{buildroot}/

%files
%defattr(0644,root,root,0755)
%license %{docpath}/LICENSE
%doc %{docpath}/README.md 
%attr(0755,root,root) %{binpath}/*

%dir %attr(0755,%{cfgusr},root) %{cfgpath}
%config %attr(0644,%{cfgusr},%{usersgrp}) %{cfgpath}/*

%{incpath}/*

%attr(0755,root,root) %{libpath}/*.so
%attr(0755,root,root) %{libpath}/python*/site-packages/kmc_python*.so
%{libpath}/*

%attr(2770,root,%{usersgrp}) %{logpath}

%dir %{_prefix}/services

%attr(0755,root,root) %{testpath}/bin/*
%attr(0644,%{cfgusr},root) %{testpath}/etc/kmc_sdls_test_app.properties
%{testpath}/input/*
%{testpath}/sql/*

%exclude %{systemd_path}
%exclude %{sysconfig_path}
%exclude %{cryptosvc_prefix}
%exclude %{sdlssvc_prefix}
%exclude %{samgmtsvc_prefix}
%exclude %{cfgpath}/install_manifest.txt

%files crypto-service
%defattr(0640,root,%{cryptogrp},0750)
%dir %attr(0750,%{cfgusr},%{cryptogrp}) %{cryptosvc_cfgpath}
%config %attr(0640,%{cfgusr},%{cryptogrp}) %{cryptosvc_cfgpath}/*

%{cryptosvc_prefix}/lib/*

%attr(2770,root,%{cryptogrp}) %{cryptosvc_logpath}

%dir %attr(0750,root,%{cryptogrp}) %{cryptosvc_prefix}/work
%dir %attr(0750,root,%{cryptogrp}) %{cryptosvc_prefix}/work/Tomcat
%dir %attr(2770,root,%{cryptogrp}) %{cryptosvc_prefix}/work/Tomcat/localhost

%{systemd_path}/kmc-crypto-service.service
%config %attr(0640,%{cfgusr},%{cryptogrp}) %{sysconfig_path}/kmc-crypto-service
%attr(0644,root,root) %{firewalld_path}/kmc-crypto-service.xml

%files sdls-service
%defattr(0640,root,%{cryptogrp},0750)
%dir %attr(0750,%{cfgusr},%{cryptogrp}) %{sdlssvc_cfgpath}
%config %attr(0640,%{cfgusr},%{cryptogrp}) %{sdlssvc_cfgpath}/*

%{sdlssvc_prefix}/lib/*

%attr(2770,root,%{cryptogrp}) %{sdlssvc_logpath}

%dir %attr(0750,root,%{cryptogrp}) %{sdlssvc_prefix}/work
%dir %attr(0750,root,%{cryptogrp}) %{sdlssvc_prefix}/work/Tomcat
%dir %attr(2770,root,%{cryptogrp}) %{sdlssvc_prefix}/work/Tomcat/localhost

%{systemd_path}/kmc-sdls-service.service
%config %attr(0640,%{cfgusr},%{cryptogrp}) %{sysconfig_path}/kmc-sdls-service
%attr(0644,root,root) %{firewalld_path}/kmc-sdls-service.xml

%files sa-mgmt-service
%defattr(0640,root,%{cryptogrp},0750)
%dir %attr(0750,%{cfgusr},%{cryptogrp}) %{samgmtsvc_cfgpath}
%config %attr(0640,%{cfgusr},%{cryptogrp}) %{samgmtsvc_cfgpath}/*

%{samgmtsvc_prefix}/lib/*

%attr(2770,root,%{cryptogrp}) %{samgmtsvc_logpath}

%dir %attr(0750,root,%{cryptogrp}) %{samgmtsvc_prefix}/work
%dir %attr(0750,root,%{cryptogrp}) %{samgmtsvc_prefix}/work/Tomcat
%dir %attr(2770,root,%{cryptogrp}) %{samgmtsvc_prefix}/work/Tomcat/localhost

%{systemd_path}/kmc-sa-mgmt.service
%config %attr(0640,%{cfgusr},%{cryptogrp}) %{sysconfig_path}/kmc-sa-mgmt-service
%attr(0644,root,root) %{firewalld_path}/kmc-sa-mgmt-service.xml

%pre
if [ $1 = 1 ]; then
  # ensure/add all groups & users listed below
  ADDGROUPS="
  %{cfggrp}:%{cfggrp_gid}:%{?cfggrp_members}
  %{usersgrp}:%{usersgrp_gid}:%{?usersgrp_members}
  "

  ADDUSERS="
  %{cfgusr}:%{cfggrp_gid}:%{cfgusr_uid}:%{cfgusr_home}:%{cfgusr_comment}:%{cfgusr_shell}
  "
  echo "${ADDGROUPS}" | while read E ; do
    [ -z "${E}" ] && continue

    ADDGRP=`echo "${E}" | /bin/cut -f1 -d':'`
    ADDGID=`echo "${E}" | /bin/cut -f2 -d':'`
    ADDMEMBERS=`echo "${E}" | /bin/cut -f3 -d':' | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g'`

    GRP=`/bin/getent group "${ADDGRP}"`
    if [ -z "${GRP}" ]; then
      # Group name undefined, check gid
      GRP=`/bin/getent group "${ADDGID}"`
      if [ -z "${GRP}" ]; then
        # Group name/gid undefined, create local group
        if [ -n "${ADDMEMBERS}" ]; then
          /bin/groupadd -g "${ADDGID}" -U "${ADDMEMBERS}" "${ADDGRP}"
        else
          /bin/groupadd -g "${ADDGID}" "${ADDGRP}"
        fi
      else
        # Group Name undefined, GID used.  Display error & exit
        echo "  ERROR: Cannot add ${ADDGRP}, requested GID ${ADDGID} already in use!."
        exit 1
      fi
    else
      # Group name already defined, check gid
      GID=`echo "${GRP}" | /bin/cut -f3 -d':'`
      if [ "${GID}" != "${ADDGID}" ]; then
        # Group exists, GID mismatch. Display warning
        echo "  WARNING: ${ADDGRP} exists, but uses GID ${GID} rather than specified ${ADDGID}.  Will use ${GID} for deployment."
      fi

      # Add group membership checking
    fi
  done

  echo "${ADDUSERS}" | while read E; do
    [ -z "${E}" ] && continue

    ADDUSER=`echo $E | cut -f1 -d:`
    ADDGROUP=`echo $E | cut -f2 -d:`
    ADDUID=`echo $E | cut -f3 -d:`
    ADDDIR=`echo $E | cut -f4 -d:`
    ADDCOMMENT=`echo $E | cut -f5 -d:`
    ADDSHELL=`echo $E | cut -f6 -d:`

    NUMU=`/bin/getent passwd "${ADDUSER}" | /bin/wc -l`
    if [ $NUMU -eq 0 ]; then
      UID_COUNT=`/bin/getent passwd "${ADDUID}" | /bin/wc -l`
      if [ $UID_COUNT -eq 0 ]; then
        echo "adding user $ADDUSER..."
        /sbin/useradd -u $ADDUID -g $ADDGROUP -d "$ADDDIR" -M -c "$ADDCOMMENT" -s "$ADDSHELL" $ADDUSER
      else
        echo "ERROR: cannot add user $ADDUSER because uid $ADDUID is already used"
        exit 1
      fi
    fi
  done
fi

%pre crypto-service
if [ $1 = 1 ]; then
  # ensure/add all groups & users listed below
  ADDGROUPS="
  %{cryptogrp}:%{cryptogrp_gid}:%{?cryptogrp_members}
  "

  ADDUSERS="
  %{cryptousr}:%{cryptogrp_gid}:%{cryptousr_uid}:%{cryptousr_home}:%{cryptousr_comment}:%{cryptousr_shell}
  "
  echo "${ADDGROUPS}" | while read E ; do
    [ -z "${E}" ] && continue

    ADDGRP=`echo "${E}" | /bin/cut -f1 -d':'`
    ADDGID=`echo "${E}" | /bin/cut -f2 -d':'`
    ADDMEMBERS=`echo "${E}" | /bin/cut -f3 -d':' | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g'`

    GRP=`/bin/getent group "${ADDGRP}"`
    if [ -z "${GRP}" ]; then
      # Group name undefined, check gid
      GRP=`/bin/getent group "${ADDGID}"`
      if [ -z "${GRP}" ]; then
        # Group name/gid undefined, create local group
        if [ -n "${ADDMEMBERS}" ]; then
          /bin/groupadd -g "${ADDGID}" -U "${ADDMEMBERS}" "${ADDGRP}"
        else
          /bin/groupadd -g "${ADDGID}" "${ADDGRP}"
        fi
      else
        # Group Name undefined, GID used.  Display error & exit
        echo "  ERROR: Cannot add ${ADDGRP}, requested GID ${ADDGID} already in use!."
        exit 1
      fi
    else
      # Group name already defined, check gid
      GID=`echo "${GRP}" | /bin/cut -f3 -d':'`
      if [ "${GID}" != "${ADDGID}" ]; then
        # Group exists, GID mismatch. Display warning
        echo "  WARNING: ${ADDGRP} exists, but uses GID ${GID} rather than specified ${ADDGID}.  Will use ${GID} for deployment."
      fi

      # Add group membership checking
    fi
  done

  echo "${ADDUSERS}" | while read E; do
    [ -z "${E}" ] && continue

    ADDUSER=`echo $E | cut -f1 -d:`
    ADDGROUP=`echo $E | cut -f2 -d:`
    ADDUID=`echo $E | cut -f3 -d:`
    ADDDIR=`echo $E | cut -f4 -d:`
    ADDCOMMENT=`echo $E | cut -f5 -d:`
    ADDSHELL=`echo $E | cut -f6 -d:`

    NUMU=`/bin/getent passwd "${ADDUSER}" | /bin/wc -l`
    if [ $NUMU -eq 0 ]; then
      UID_COUNT=`/bin/getent passwd "${ADDUID}" | /bin/wc -l`
      if [ $UID_COUNT -eq 0 ]; then
        echo "adding user $ADDUSER..."
        /sbin/useradd -u $ADDUID -g $ADDGROUP -d "$ADDDIR" -M -c "$ADDCOMMENT" -s "$ADDSHELL" $ADDUSER
      else
        echo "ERROR: cannot add user $ADDUSER because uid $ADDUID is already used"
        exit 1
      fi
    fi
  done
fi

%pre sdls-service
if [ $1 = 1 ]; then
  # ensure/add all groups & users listed below
  ADDGROUPS="
  %{cryptogrp}:%{cryptogrp_gid}:%{?cryptogrp_members}
  "

  ADDUSERS="
  %{cryptousr}:%{cryptogrp_gid}:%{cryptousr_uid}:%{cryptousr_home}:%{cryptousr_comment}:%{cryptousr_shell}
  "
  echo "${ADDGROUPS}" | while read E ; do
    [ -z "${E}" ] && continue

    ADDGRP=`echo "${E}" | /bin/cut -f1 -d':'`
    ADDGID=`echo "${E}" | /bin/cut -f2 -d':'`
    ADDMEMBERS=`echo "${E}" | /bin/cut -f3 -d':' | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g'`

    GRP=`/bin/getent group "${ADDGRP}"`
    if [ -z "${GRP}" ]; then
      # Group name undefined, check gid
      GRP=`/bin/getent group "${ADDGID}"`
      if [ -z "${GRP}" ]; then
        # Group name/gid undefined, create local group
        if [ -n "${ADDMEMBERS}" ]; then
          /bin/groupadd -g "${ADDGID}" -U "${ADDMEMBERS}" "${ADDGRP}"
        else
          /bin/groupadd -g "${ADDGID}" "${ADDGRP}"
        fi
      else
        # Group Name undefined, GID used.  Display error & exit
        echo "  ERROR: Cannot add ${ADDGRP}, requested GID ${ADDGID} already in use!."
        exit 1
      fi
    else
      # Group name already defined, check gid
      GID=`echo "${GRP}" | /bin/cut -f3 -d':'`
      if [ "${GID}" != "${ADDGID}" ]; then
        # Group exists, GID mismatch. Display warning
        echo "  WARNING: ${ADDGRP} exists, but uses GID ${GID} rather than specified ${ADDGID}.  Will use ${GID} for deployment."
      fi

      # Add group membership checking
    fi
  done

  echo "${ADDUSERS}" | while read E; do
    [ -z "${E}" ] && continue

    ADDUSER=`echo $E | cut -f1 -d:`
    ADDGROUP=`echo $E | cut -f2 -d:`
    ADDUID=`echo $E | cut -f3 -d:`
    ADDDIR=`echo $E | cut -f4 -d:`
    ADDCOMMENT=`echo $E | cut -f5 -d:`
    ADDSHELL=`echo $E | cut -f6 -d:`

    NUMU=`/bin/getent passwd "${ADDUSER}" | /bin/wc -l`
    if [ $NUMU -eq 0 ]; then
      UID_COUNT=`/bin/getent passwd "${ADDUID}" | /bin/wc -l`
      if [ $UID_COUNT -eq 0 ]; then
        echo "adding user $ADDUSER..."
        /sbin/useradd -u $ADDUID -g $ADDGROUP -d "$ADDDIR" -M -c "$ADDCOMMENT" -s "$ADDSHELL" $ADDUSER
      else
        echo "ERROR: cannot add user $ADDUSER because uid $ADDUID is already used"
        exit 1
      fi
    fi
  done
fi

%pre sa-mgmt-service
if [ $1 = 1 ]; then
  # ensure/add all groups & users listed below
  ADDGROUPS="
  %{cryptogrp}:%{cryptogrp_gid}:%{?cryptogrp_members}
  "

  ADDUSERS="
  %{cryptousr}:%{cryptogrp_gid}:%{cryptousr_uid}:%{cryptousr_home}:%{cryptousr_comment}:%{cryptousr_shell}
  "
  echo "${ADDGROUPS}" | while read E ; do
    [ -z "${E}" ] && continue

    ADDGRP=`echo "${E}" | /bin/cut -f1 -d':'`
    ADDGID=`echo "${E}" | /bin/cut -f2 -d':'`
    ADDMEMBERS=`echo "${E}" | /bin/cut -f3 -d':' | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g'`

    GRP=`/bin/getent group "${ADDGRP}"`
    if [ -z "${GRP}" ]; then
      # Group name undefined, check gid
      GRP=`/bin/getent group "${ADDGID}"`
      if [ -z "${GRP}" ]; then
        # Group name/gid undefined, create local group
        if [ -n "${ADDMEMBERS}" ]; then
          /bin/groupadd -g "${ADDGID}" -U "${ADDMEMBERS}" "${ADDGRP}"
        else
          /bin/groupadd -g "${ADDGID}" "${ADDGRP}"
        fi
      else
        # Group Name undefined, GID used.  Display error & exit
        echo "  ERROR: Cannot add ${ADDGRP}, requested GID ${ADDGID} already in use!."
        exit 1
      fi
    else
      # Group name already defined, check gid
      GID=`echo "${GRP}" | /bin/cut -f3 -d':'`
      if [ "${GID}" != "${ADDGID}" ]; then
        # Group exists, GID mismatch. Display warning
        echo "  WARNING: ${ADDGRP} exists, but uses GID ${GID} rather than specified ${ADDGID}.  Will use ${GID} for deployment."
      fi

      # Add group membership checking
    fi
  done

  echo "${ADDUSERS}" | while read E; do
    [ -z "${E}" ] && continue

    ADDUSER=`echo $E | cut -f1 -d:`
    ADDGROUP=`echo $E | cut -f2 -d:`
    ADDUID=`echo $E | cut -f3 -d:`
    ADDDIR=`echo $E | cut -f4 -d:`
    ADDCOMMENT=`echo $E | cut -f5 -d:`
    ADDSHELL=`echo $E | cut -f6 -d:`

    NUMU=`/bin/getent passwd "${ADDUSER}" | /bin/wc -l`
    if [ $NUMU -eq 0 ]; then
      UID_COUNT=`/bin/getent passwd "${ADDUID}" | /bin/wc -l`
      if [ $UID_COUNT -eq 0 ]; then
        echo "adding user $ADDUSER..."
        /sbin/useradd -u $ADDUID -g $ADDGROUP -d "$ADDDIR" -M -c "$ADDCOMMENT" -s "$ADDSHELL" $ADDUSER
      else
        echo "ERROR: cannot add user $ADDUSER because uid $ADDUID is already used"
        exit 1
      fi
    fi
  done
fi

%post crypto-service
%systemd_post kmc-crypto-service
%firewalld_reload

%post sdls-service
%systemd_post kmc-sdls-service
%firewalld_reload

%post sa-mgmt-service
%systemd_post kmc-sa-mgmt
%firewalld_reload

%postun crypto-service
%systemd_postun kmc-crypto-service
%firewalld_reload

%postun sdls-service
%systemd_postun kmc-sdls-service
%firewalld_reload

%postun sa-mgmt-service
%systemd_postun kmc-sa-mgmt
%firewalld_reload

%changelog
* Thu Nov 21 2024 MGSS ASEC Team <mgss.css.requests@jpl.nasa.gov> - 3.7.0-1
- Initial RPM spec for KMC 3.7.0
- 
