# AMMOS Data Cryptography Services (DCS)

AMMOS DCS provides cryptographic capabilities, which include encryption/decryption of files and creation/verification of integrity check values (ICVs) for files.  KMC also includes command encryption capabilities via application of the Consultative Committee for Space Data Systems (CCSDS) “Blue Book” Space Data Link Security (SDLS) specification.

**NOTE** AMMOS DCS was formerly known as AMMOS Key Management & Cryptography (KMC), and the name change is ongoing.  The references to KMC in the code and documentation will be updated to DCS in future releases.

## Building DCS
1. Clone Git repository (already done if you're reading this README locally!)
```git clone https://github.com/NASA-AMMOS/DCS```
1. Change directory into the KMC repository clone:
```cd KMC```
1. Initialize/Update Git submodules (NASA Cryptolib):
```git submodule init; git submodule update```
1. (Optional) Edit kmc-resources/scripts/setenv.sh as needed to tailor configuration.  See Section XXXX in the Product Guide and/or the comments in kmc-resources/scripts/setenv.sh for details.
1. Run Build Script.  This step performs the full build and unit test suite.
```kmc-resources/scripts/build.sh```

### Build Dependencies
DCS requires several software dependenices in order to build and execute unit tests.  Required software can be installed with OS Vendor packages, pulled directly from public repositories, or any other method.  Make sure the required tool is available in the build environment appropriately for that particular tool (PATH for executables, LD\_LIBRARY\_PATH for shared libraries, PYTHONPATH for python modules, etc.). Tool versions listed are minimum versions, square brackets are used to denote the tool/version used for the official KMC build and testing.

Required Build Dependencies:
* C Compiler [gcc 11.4.1]
* Java Development Kit 17.x [OpenJDK 17.0.12]
* cmake [3.26.5]
* swig [4.0.2]
* MariaDB Connector/C [3.2.6]
* cffi (Python module) [1.14.5]
* invoke (Python module) [2.2.0]
* pycryptodome (Python module) [3.20.0]
* maven [3.6.3]

## Deploying DCS
DCS can be deployed in several different ways, all of which are documented in section 4 of the DCS Product Guide.:
1. Direct local deployment: local users/directories/files on the current system.  This will deploy into the directories set in kmc-resources/scripts/setenv.sh.

Deploying the KMC Client:
```kmc-resources/scripts/deploy.sh```  

Deploying the KMC Client & all KMC Services:
```kmc-resources/script/deploy.sh --all-services```

See the product guide for additional details about the deploy.sh script.
1. System Packages: the deploy.sh script can be used to build system-specific package types.  Currently, the script supports a generic package directory, suitable for tarball or follow-on packaging, and Red Hat-stype RPM packages using the rpmspec in kmc-resources/packaging/rpm/SPECS/kmc.spec.

Generic packaging deployment:
```kmc-resources/scripts/deploy.sh --pkg /var/tmp/kmc-pkg```

RPM Packaging (builds RPMs in kmc-resources/packaging/rpm/RPMS):
```kmc-resources/scripts/deploy.sh --rpm```

1. OCI Container Images: The deploy.sh script can also build OCI-compliant container images based on the RHEL 9 Universal Basic Image with the KMC Client and individual KMC Services (KMC Crypto Service, KMC SDLS Service, KMC SA Management Service).  Verify that the local container hosting software is configured in kmc-resources/scripts/setenv.sh (CONTAINER_EXEC).

Building KMC Service Containers:
```kmc-resources/scripts/deploy.sh --img```

See the individual KMC Service Container READMEs in kmc-resources/packaging/container/{crypto-service,sdls-service/sa-mgmt} for details on how to configure & launch the KMC Service Containers.

Copyright (c) 2023-2025 California Institute of Technology (“Caltech”). U.S. Government sponsorship acknowledged.  See LICENSE for additional information.

