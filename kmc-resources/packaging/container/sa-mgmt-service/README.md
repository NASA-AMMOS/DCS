# DCS Security Association Management Service Container

The DCS Security Association Management Service Container provides a self-contained DCS Security Association Management Service offering a graphical tool for managing the Security Association database. DCS, including the Security Association Management Service Container, is available under the Apache 2.0 software license, but this container is based on the Red Hat Enterprise Linux (RHEL) 9 Universal Basic Image (UBI), which is subject to their licensing terms.  See https://www.redhat.com/en/about/red-hat-end-user-license-agreements#UBI for specifics about the RHEL 9 UBI licensing.

## Building the Container

Follow the instructions in the root level README.md to configure and build DCS.  In particular, make sure to configure the CONTAINER_EXEC variable in setenv.sh to match your local container engine. CONTAINER_EXEC defaults to /bin/podman, since that is the default container virtualzation package offered on Red Hat Enterprise Linux 9.  Once the build is complete, use the deploy.sh script to execute the container builds.

From the root of the DCS repository, run the following command once the build and tests are complete:
```kmc-resources/scripts/deploy.sh --img```

That command will deploy all of the DCS files into a local directory (kmc-resources/packaging/container/kmcroot) and then execute the container build process using the Dockerfile in each of the service container trees.  The container build process requires access to the Internet for two things: 
* Retrieval of the RHEL9 UBI image
* Download of the BouncyCastle FIPS Java library from Maven Central, which is used to support FIPS-compliant keystores.

At the end of the container build process, there should be three DCS containers in your local image repository (examples shown using podman):

```
$ podman image ls
REPOSITORY                                  TAG         IMAGE ID      CREATED            SIZE
localhost/kmc-sa-mgmt-service               4.0.0       aebf0191473b  About an hour ago  890 MB
localhost/kmc-sdls-service                  4.0.0       cfb068d65d92  About an hour ago  890 MB
localhost/kmc-crypto-service                4.0.0       2e1285394471  About an hour ago  890 MB
```

The image can be saved to transferrable image files with the following commands:
```
$ podman image save -o kmc-sa-mgmt-service-4.0.0.tar kmc-sa-mgmt-service:4.0.0
```

## Security Association Management Service Container Configuration Options
There are a large number of configurable options that control the behavior and operation of the DCS Security Association Management Service Container.  The full list is documented here.

Some of the configuration options are "sensitive" data -- TLS keys, keystores, passwords, etc.  Sensitive configuration options are flagged with the [SENSITIVE] flag.  It is *STRONGLY RECOMMENDED* that secure methods be used to provide these configuration items to the container.  Podman/docker secrets, Amazon Secrets Manager, and similar systems can and should be used for any configuration options marked "Sensitive."  If multiple DCS services are being deployed on the same host system, they can share the secrets defined.

In the list of parameters below, [REQUIRED] denotes configuration parameters that are required for DCS Security Association Management Service container deployment.  Many parameters may be provided via multiple configuration paths -- [SECRET] denotes a hosting platform secret (docker/podman secrets, AWS Secrets Manager, etc.), [ENV] denotes an Environment variable passed to the container.  Options that are *not required* for operation have a default if a reasonable value is available -- default values are noted in the Default: field.

### TLS Configuration Options
- Disable MTLS Flag
  Boolean (1/0) option whether to disable mutual TLS authentication for the Crypto Service.
  Default: 0 (mTLS enabled by default)
  Configuration Paths:
    [ENV]    - DISABLE_MTLS

- TLS Host Certificate [REQUIRED] [SENSITIVE]
  X.509 PEM-formatted Certificate.  
  Required for TLS operation to identify the server host.
  Configuration Paths:
    [SECRET] - tls_host_cert
    [ENV]    - TLS_HOST_CERT

- TLS Host Key [REQUIRED] [SENSITIVE]
  X.509 PEM-formatted Private Key (for the TLS Host Certificate)
  Required for TLS operation to identify the server host.
  Configuration paths:
    [SECRET] - tls_host_key
    [ENV]    - TLS_HOST_KEY

- TLS Keystore Format
  Defines the format used for TLS Keystores.  Available settings are BCFKS
  and PKCS12.
  Default: BCFKS
  Configuration Paths:
    [ENV]    - KEYSTORE_FORMAT

- TLS Host Keystore Passphrase [SENSITIVE]
  String containing passphrase for TLS Host Keystore.  Input strings *must be 
  encoded with base64 to avoid shell interpretation of special characters.*
  Default: changeit
  Configuration Paths:
    [SECRET] - tls_host_keystore_pass
    [ENV]    - TLS_HOST_KEYSTORE_PASS

- TLS Host Keystore [SENSITIVE]
  Keystore file formatted as specified with the TLS Keystore Format option that 
  contains the TLS Host Certificate and TLS Host Key.
  Default: Keystore built on container creation from TLS Host Certificate and 
           TLS Host Key.
  Configuration Paths:
    [SECRET] - tls_host_keystore
    [ENV]    - TLS_HOST_KEYSTORE  (*MUST be base64 encoded*)

- TLS CA Certificate Bundle [REQUIRED]
  PEM-formatted file containing relevant TLS Certificate Authority certificates.
  Configuration Paths:
    [SECRET] - tls_ca_bundle
    [ENV]    - TLS_CA_BUNDLE

- TLS Truststore Passphrase [SENSITIVE]
  String containing passphrase for the Java TLS Truststore. Input strings 
  *must be encoded with base64 to avoid shell interpretation of special 
  characters.*
  Default: changeit
  Configuration Paths:
    [SECRET] - tls_truststore_pass
    [ENV]    - TLS_TRUSTSTORE_PASS

- TLS Truststore
  Java Key Store (JKS) formatted truststore containing the CA Certificates in
  the TLS CA Bundle.
  Default: Truststore built on container creation from TLS CA Bundle
  Configuration Paths:
    [SECRET] - tls_truststore
    [ENV]    - TLS_TRUSTSTORE  (*MUST be base64 encoded*)

- MTLS Truststore Passphrase [SENSITIVE]
  String containing the passphrase for the MTLS Java Truststore. Not necessary if DISABLE_MTLS is true (1).
  Default: changeit
  Configuration Paths:
    [SECRET] - tls_mtls_truststore_pass
    [ENV]    - TLS_MTLS_TRUSTSTORE_PASS (*MUST be base64 encoded*)

- MTLS Truststore [REQUIRED] [SENSITIVE]
  Java Key Store (JKS) formatted truststore containing the certificates to be 
  allowed to access the DCS Security Association Management Service via mutual TLS authentication. Not required if DISABLE_MTLS is true (1).
  Configuration Paths:
    [SECRET] - tls_mtls_truststore
    [ENV]    - TLS_MTLS_TRUSTSTOR (*MUST be base64 encoded*)E

### Security Association Management Service Configuration Options
- Security Association Database Fully-Qualified Domain Name [REQUIRED]
  String containing the fully-qualified domain name of the host where the DCS
  SADB is running (port tcp/3306)
  Configuration Paths:
    [ENV]    - SADB_FQDN

- SA Database Mutual TLS Authentication Flag
  Boolean flag (true/false)that controls whether or not to use mutual TLS to 
  authenticate to the SA Database.
  Default: true
  Configuraton Paths:
    [ENV]   - DB_MTLS

- SA Database User
  String containing the database username for connections to the SADB
  Default: cryptosvc
  Configuration Paths:
    [ENV]    - DB_USER

- SA Database User Password
  String containing the password for the database user for connections to the 
  SADB. The default is to use mutual TLS authentication, rather than a password
  for the SA database user.
  Configuraton Paths:
    [SECRET] - db_user_pass
    [ENV]    - DB_USER_PASS (*MUST be base64 encoded*)

### Java Configuration Options
- Java Maximum Memory Heap Size
  String containing Java Maximum Memory Heap allocated to the Java Virtual 
  Machine
  Default: 2G
  Configuration Paths
    [ENV]    - JAVA_MAX_HEAP

- Java Minimum Memory Heap Size
  String containing Java Minimum Memory Heap allocated to the Java Virtual 
  Machine
  Default: 1G
  Configuration Paths
    [ENV]    - JAVA_MIN_HEAP
  
- DEBUG Flag
  Boolean option that allows debugging of running containers.  Accepted values: 
  0,1
  Default: 0
  Configuraton Paths:
    [ENV]    - DEBUG

## Deploying the Security Association Management Service Container

The DCS Security Association Management Service Container can be deployed via a wide variety of tools and processes.  Examples are provided for running the bare container with podman and using podman-compose.  Releases of the Security Association Management Service Container are also tested in the Amazon Elastic Container Service (ECS), and can be run there.  The DCS Security Association Management Service Container should be deployable on any container virtualization system that supports OCI-compliant images and processes.

There are a large number of configurable options that control the behavior and operation of the DCS Security Association Management Service Container.  For the full list, see "Security Association Management Service Container Configuration Options" above. These examples are minimalist configurations that utilize the default settings as much as possible.  

Both examples below use podman secrets for sensitive configuration information, as well as for a simple method to provide some non-sensitive options (like the TLS CA Certificate Bundle).

### Deploying with Podman

1. Import the Image (optional, if image is available in a configured repository)
```bash
$ podman image load -i kmc-sa-mgmt-service-4.0.0.tar.gz
```

1. Configure Secrets
```bash
$ podman secret create tls_host_key /etc/pki/tls/private/ammos-server-key.pem
$ podman secret create tls_host_cert /etc/pki/tls/certs/ammos-server-cert.pem
$ podman secret create tls_ca_bundle /etc/pki/tls/certs/ammos-ca-bundle.crt
$ podman secret create crypto_keystore /msn_data/crypto/crypto_keystore.bcfks
$ podman secret create tls_mtls_truststore /etc/pki/tls/private/ammos-mtls-truststore.jks
$ echo "changeit" | podman secret create tls_mtls_truststore_pass -
```

1. Create DCS Security Association Management Service Container
```bash
podman container create -p 8447:8447 --name kmc-sa-mgmt-service \
  --secret tls_host_key --secret tls_host_cert --secret tls_ca_bundle \
  --secret tls_mtls_truststore -e SADB_FQDN=sadb.example.com \
  kmc-sa-mgmt-service:4.0.0
```

1. Create SystemD Unit file for container (if desired)
```bash
podman generate systemd -n -f --container-prefix='' kmc-sa-mgmt-service
sudo mv kmc-sa-mgmt-service.service /lib/systemd/system/
sudo chown root:root /lib/systemd/system/kmc-sa-mgmt-service.service
sudo chmod 0644 /lib/systemd/system/kmc-sa-mgmt-service.service
sudo systemctl daemon-reload
```

1. Start the container
Either:
```bash
sudo systemctl start kmc-sa-mgmt-service
```

*OR*

```bash
podman start kmc-sa-mgmt-service
```

### Deploying with podman-compose
Podman-compose uses YAML-formatted files to configure one or more services.  An example podman-compose file for the DCS Security Association Management Service Container can be found in kmc-resources/packaging/container/sa-mgmt-service/podman-compose-example.yml.  This example compose file uses podman secrets like the previous example to manage sensitive inputs.

1. Import the Image (optional, if image is available in a configured repository)
```bash
$ podman image load -i kmc-sa-mgmt-service-4.0.0.tar.gz
```

1. Configure Secrets
```bash
$ podman secret create tls_host_key /etc/pki/tls/private/ammos-server-key.pem
$ podman secret create tls_host_cert /etc/pki/tls/certs/ammos-server-cert.pem
$ podman secret create tls_ca_bundle /etc/pki/tls/certs/ammos-ca-bundle.crt
$ podman secret create tls_mtls_truststore /etc/pki/tls/private/ammos-mtls-truststore.jks
$ echo "changeit" | podman secret create tls_mtls_truststore_pass -
```

1. Edit podman-compose-example.yml as needed for desired configuration.  
See the Security Association Management Service Container Configuration Options section above for details.

1. Use podman-compose to bring up the container:
```bash
podman-compose -f podman-compose-example.yml up -d
```

1. To shut down the container using podman-compose:
```bash
podman-compose -f podman-compose-example.yml down
```
