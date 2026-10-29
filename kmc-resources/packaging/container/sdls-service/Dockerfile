# Dockerfile for AMMOS DCS SDLS Service

FROM redhat/ubi9

LABEL architecture="x86_64"  \
      version="${VERSION}"  \
      gov.nasa.jpl.ammos.component="kmc-sdls-service" \
      gov.nasa.jpl.ammos.license="Apache 2.0" \
      description="AMMOS KMC SDLS Service implements the CCSDS Space Data Link Security (SDLS) protocol for doing transfer frame encryption." \
      io.k8s.description="AMMOS KMC SDLS Service implements the CCSDS Space Data Link Security (SDLS) protocol for doing transfer frame encryption." \
      distribution-scope="public"  \
      maintainer="NASA AMMOS - Application Security Team <mgss.css.requests@jpl.nasa.gov>" \
      name="kmc-sdls-service" \
      io.k8s.display-name="kmc-sdls-service" \
      io.openshift.tags="rhel9 kmc sdls-service" \
      summary="Provides AMMOS KMC SDLS Service ${VERSION}" \
      url="https://github.com/NASA-AMMOS/KMC" \
      vcs-ref="${GIT_COMMIT}" \
      vendor="NASA AMMOS"

# Software Install
RUN /bin/dnf -y upgrade
RUN /bin/dnf -y install --allowerasing --skip-broken java-17-openjdk-headless apr mariadb-connector-c openssl net-tools bind-utils iputils curl procps-ng
COPY kmcroot/opt /opt/
COPY bin/*.sh /opt/ammos/kmc/services/sdls-service/bin/
RUN /opt/ammos/kmc/lib/deploy/finish_deploy.sh --img --sdls-service

ENTRYPOINT /opt/ammos/kmc/services/sdls-service/bin/sdls_service_setup.sh

