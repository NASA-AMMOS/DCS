# DCS SA Management GUI

Author: [JP Pan (393C)](mailto:panjames@jpl.nasa.gov?subject=DCS SA Management GUI)

- Spring Boot web service
- React frontend (CRA)

## Requirements

- Java 17

## Deployment

Deploy on preferred servlet container, eg Apache Tomcat.

### Expected files

The following configuration files are expected to be present and configured on the host:

```
/opt/ammos/kmc/services/crypto-service/etc/kmc-sa-mgmt-server.properties
/opt/ammos/kmc/services/crypto-service/etc/kmc-sa-mgmt-server-log4j2.xml
```

## Build

```
mvn clean install
```

Maven will build both the server backend and frontend code. The frontend code is copied into the WAR file
under `WEB-INF/classes/static`, where Spring Boot will find the `index.html` welcome file and other static assets.

By default, the `ammos` maven profile is active, which will strip out all 3rd party dependencies from the WAR file,
as they are expected to be included in the Common Software Environment (CSE). To generate a WAR file with dependencies
included for standalone deployment, disable the `ammos` profile:

```
mvn clean install -P'!ammos'
```

## Development

Use your IDE of choice. IntelliJ IDEA (for Java) and WebStorm (for React) are good candidates, and are available through
CAE.

### Server

#### Spring Dev Profile

Activate the `dev` Spring profile. Currently only changes logging to use console-only.

#### Config Overrides

Set the environment variable `KMC_OVERRIDE_CONFIG` to the absolute path of an override config file.

### Frontend

The frontend code is located in `/src/main/frontend`. Use `yarn` to build and run.

#### Build

```
yarn install
```

#### Run

```
yarn run start
```

The development server will start by default on `localhost:3001`, and the backend will likely be running on `localhost:8080`. To properly route backend calls to `localhost:3001/api` to `localhost:8080/api`, a development proxy is configured in `frontend/src/setupProxy.js`.
