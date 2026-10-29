# Dockerfile for AMMOS DCS Crypto Service

FROM redhat/ubi9

LABEL architecture="x86_64"  \
      version="${VERSION}"  \
      gov.nasa.jpl.ammos.component="kmc-crypto-service" \
      gov.nasa.jpl.ammos.license="Apache 2.0" \
      description="AMMOS KMC Crypto Service provides data-level encryption/decryption and integrity check value (ICV) creation/verification capabilities for securing data at-rest and in-transit, as an additional layer of protection over secure connections and storage." \
      io.k8s.description="AMMOS KMC Crypto Service provides data-level encryption/decryption and integrity check value (ICV) creation/verification capabilities for securing data at-rest and in-transit, as an additional layer of protection over secure connections and storage." \
      distribution-scope="public"  \
      maintainer="NASA AMMOS - Application Security Team <mgss.css.requests@jpl.nasa.gov>" \
      name="kmc-crypto-service" \
      io.k8s.display-name="kmc-crypto-service" \
      io.openshift.tags="rhel9 kmc crypto-service" \
      summary="Provides AMMOS KMC Crypto Service ${VERSION}" \
      url="https://github.com/NASA-AMMOS/KMC" \
      vcs-ref="${GIT_COMMIT}" \
      vendor="NASA AMMOS"

# Software Install
RUN /bin/dnf -y upgrade
RUN /bin/dnf -y install --allowerasing --skip-broken java-17-openjdk-headless apr mariadb-connector-c openssl net-tools bind-utils iputils curl procps-ng
COPY kmcroot/opt /opt/
COPY bin/*.sh /opt/ammos/kmc/services/crypto-service/bin/
RUN /opt/ammos/kmc/lib/deploy/finish_deploy.sh --img --crypto-service

ENTRYPOINT /opt/ammos/kmc/services/crypto-service/bin/crypto_service_setup.sh

