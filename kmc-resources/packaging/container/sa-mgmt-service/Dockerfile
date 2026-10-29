# Dockerfile for AMMOS DCS SA Management Service

FROM redhat/ubi9

LABEL architecture="x86_64"  \
      version="${VERSION}"  \
      gov.nasa.jpl.ammos.component="kmc-sa-mgmt-service" \
      gov.nasa.jpl.ammos.license="Apache 2.0" \
      description="AMMOS KMC SA Management Service provides a web-based service for managing SDLS Security Associations, which are stored in a Security Association Database." \
      io.k8s.description="AMMOS KMC SA Management Service provides a web-based service for managing SDLS Security Associations, which are stored in a Security Association Database." \
      distribution-scope="public"  \
      maintainer="NASA AMMOS - Application Security Team <mgss.css.requests@jpl.nasa.gov>" \
      name="kmc-sa-mgmt-service" \
      io.k8s.display-name="kmc-sa-mgmt-service" \
      io.openshift.tags="rhel9 kmc sa-mgmt-service" \
      summary="Provides AMMOS KMC SA Management Service ${VERSION}" \
      url="https://github.com/NASA-AMMOS/KMC" \
      vcs-ref="${GIT_COMMIT}" \
      vendor="NASA AMMOS"

# Software Install
RUN /bin/dnf -y upgrade
RUN /bin/dnf -y install --allowerasing --skip-broken java-17-openjdk-headless apr mariadb-connector-c openssl net-tools bind-utils iputils curl procps-ng mariadb
COPY kmcroot/opt /opt/
COPY bin/*.sh /opt/ammos/kmc/services/sa-mgmt/bin/
RUN /opt/ammos/kmc/lib/deploy/finish_deploy.sh --sa-mgmt-service

ENTRYPOINT /opt/ammos/kmc/services/sa-mgmt/bin/sa_mgmt_service_setup.sh

