FROM ubuntu:noble-20250127
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential cmake git swig \
    libcurl4-openssl-dev libgcrypt20-dev \
    libmariadb-dev libmariadb-dev-compat \
    libffi-dev python3 python3-dev python3-pip python3-setuptools python3-cffi \
    openjdk-17-jdk maven \
    && rm -rf /var/lib/apt/lists/*
# kmc-resources/scripts/setenv.sh defaults JAVA_HOME to the RHEL path
RUN ln -s "$(ls -d /usr/lib/jvm/java-17-openjdk-* | head -1)" /usr/lib/jvm/java-17-openjdk
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
# the repo is bind-mounted with the host user's ownership
RUN git config --system --add safe.directory '*'
WORKDIR /workspace
