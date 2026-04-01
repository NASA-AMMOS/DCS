# KMC Security Association Management CLI

## Requirements

- Java
- Maven

This module depends on it's two sister modules, `kmc-sa-db-api` and `kmc-sa-db-lib`.

The unit tests in this module depend on the unit test resources from `kmc-sa-db-lib`; they are copied
into `./target/test-classes` at build time in the `generate-test-resources` phase. If you run into trouble with the unit
tests, try regenerating the test resources:

```shell
mvn generate-test-resources
```

## Building

```shell
mvn clean install
```

The application will be generated under `./target/kmc-cli`

# Configuring

KMC CLI has two methods of configuration: configuration files and environment variable overrides.

The configuration file is called `config.properties` and is located under the `./conf` dir of the application root.

The configuration properties are:

```shell
# database user name
db.auth.user=testuser
# database password
db.auth.pass=changeit!
# database connection string. not recommended for use. if provided, the db.host, db.port, and db.schema keys will be ignored
db.conn.string=none
# database host
db.host=localhost
# database port
db.port=3306
# database schema
db.schema=sadb
# database TLS keystore
db.keystore=none
# database TLS keystore password
db.keystore.pass=none
# database TLS truststore
db.truststore=none
# database TLS truststore password
db.truststore.pass=none
# use TLS for database connections
# the provided truststore must contain the certificate chain for connecting to the database.
db.tls=false
# use mutual TLS for database connections. when mTLS is true, the db.auth.pass key is ignored. 
# the provided keystore must be trusted by the database server and database user. 
db.mtls=false
```

The following environment variables are available to configure the application:

```shell
DB_USER=testuser
DB_PASS=changeit!
DB_HOST=fqdn.kmc.db.example.com
DB_PORT=3306
DB_SCHEMA=sadb
DB_KEYSTORE=/path/to/keystore.p12
DB_KEYSTORE_PASS=changeit!
DB_TRUSTSTORE=/path/to/truststore.p12
DB_TRUSTSTORE_PASS=changeit!
DB_TLS=false
DB_MTLS=false
```

## TLS and Mutual TLS

To use TLS to connect to the SADB, you need to have a truststore that contains the correct certificate chain for
trusting the database server connection.

To use mutual TLS, you need to have a keystore that contains the correct client certificate and key for the configured
database user.

For more information and sample commands for generating the key and trust stores, please
see https://mariadb.com/kb/en/using-tls-ssl-with-mariadb-java-connector/
