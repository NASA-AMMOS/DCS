# AMMOS-CryptoLib

Provides a software implementation of the CCSDS Space Data Link Security Protocol (SDLS) to secure communications
between a ground system and a spacecraft. This directory wraps the NASA CryptoLib submodule (`CryptoLib/`) and builds:

* the C libraries (`libcryptolib`, `libkmc_sdls`) and CLI tools,
* the Java JNI wrapper (`KmcSdlsJNI.jar` + `libkmc_sdls_java`),
* the Python bindings (`KmcSdlsClient`, one build per Python 3 interpreter).

## Prerequisites

Common to all platforms:

* CMake, SWIG, libgcrypt, libcurl/OpenSSL, and the MariaDB client development package (provides `mysql_config`).
* JDK 17 on `JAVA_HOME` and `mvn` on `PATH` for the Java bindings (needs network access or a populated `~/.m2` on
  the first build).
* Python 3.9 - 3.14 for the Python bindings. Each `python3.X` found on `PATH` needs `setuptools`, `cffi` and `pip`;
  interpreters missing them are skipped with a CMake warning. Install them per interpreter:

  ```bash
  python3.12 -m pip install -r kmc_sdls/kmc_sdls_python/requirements.txt
  ```

  or use the OS packages (`python3-setuptools python3-cffi`).

### Linux (Ubuntu example)

```bash
sudo apt install build-essential cmake swig git libcurl4-openssl-dev libgcrypt20-dev \
    libmariadb-dev libmariadb-dev-compat libffi-dev python3 python3-dev python3-pip \
    python3-setuptools python3-cffi openjdk-17-jdk maven
```

### macOS (Apple Silicon, Homebrew)

```bash
brew install cmake swig libgcrypt mariadb openssl@3 gcc maven
```

* **GCC is required.** Apple's `cc`/`gcc` is clang, and configuration fails on macOS unless the C compiler is GNU.
  On a fresh build directory CMake picks the newest Homebrew `gcc-<version>` by itself; pass
  `-DCMAKE_C_COMPILER=...` or set `CC` to override.
* `mariadb` (not `mariadb-connector-c`) is needed for `-DSA_MARIADB=ON`. CMake puts the
  `scripts/macos/mysql_config` shim ahead of the real `mysql_config` on `PATH` during configuration (it works around
  a CryptoLib bug where `--cflags`/`--libs` output isn't turned into a CMake list), so no shell setup is needed.
* Python interpreters can come from `uv`, for example:

  ```bash
  uv venv --seed ~/venvs/py3.12 --python 3.12
  uv pip install --python ~/venvs/py3.12/bin/python -r kmc_sdls/kmc_sdls_python/requirements.txt
  export PATH="$HOME/venvs/py3.12/bin:$PATH"   # so `python3.12` resolves to the venv
  ```

## Checkout

```bash
git clone git@github.com:NASA-AMMOS/DCS
cd DCS/ammos-cryptolib
git submodule update --init
```

## Build variables

Pass these to CMake as `-D<NAME>=<value>`.

| Variable | Default | Meaning |
|---|---|---|
| `CRYPTO_KMC` | `ON` | Use the KMC crypto service as the cryptography module. |
| `CRYPTO_LIBGCRYPT` | `OFF` | Use libgcrypt as the cryptography module. |
| `KEY_KMC` | `ON` | Use the KMC key module. |
| `MC_DISABLED` | `OFF` | Disable the monitoring-and-control module. |
| `SA_MARIADB` | `ON` | Store security associations in MariaDB. |
| `SA_INTERNAL` | `OFF` | Use the in-memory security association database. |
| `SA_FILE` | `OFF` | Save security associations to a file. |
| `KMC_MDB_DB` | `OFF` | Debian-flavoured MariaDB integration settings. |
| `TEST` | `ON` | Build CryptoLib's C unit tests. |
| `CODECOV` | `OFF` | Build with code coverage instrumentation. |
| `DEBUG` | `OFF` | Debug symbols and debug print output. |
| `DEBUG_SYMBOLS` | `OFF` | `-g -O0` for the C libraries and the Python extension without DEBUG's print output; on macOS also writes `.dSYM` bundles. |
| `BUILD_JAVA` | `ON` | Build `KmcSdlsJNI.jar` and `libkmc_sdls_java` (needs `mvn`). |
| `BUILD_PYTHON` | `ON` | Build the Python bindings for every usable interpreter. |
| `SYSTEM_INSTALL` | `OFF` | Install to `/usr/local` instead of `ammos-cryptolib/install`. |
| `KMC_PYTHON_VERSIONS` | `3.9 3.10 3.11 3.12 3.13 3.14` | Interpreters to build bindings for (each must be on `PATH` as `pythonX.Y`). |
| `KMC_MVN_ARGS` | `-B;-ntp` | Extra arguments for every `mvn` invocation (`;`-separated), e.g. `-DKMC_MVN_ARGS="-B;-ntp;-o"`. |
| `CMAKE_C_COMPILER` | auto | C compiler; on macOS defaults to the newest Homebrew GCC. |

The flag set used by `kmc-resources/scripts/build.sh` is:

```bash
-DDEBUG=OFF -DCRYPTO_KMC=ON -DCRYPTO_LIBGCRYPT=ON -DKEY_KMC=ON -DMC_DISABLED=ON -DSA_MARIADB=ON \
-DSA_INTERNAL=ON -DTEST=OFF -DSA_FILE=OFF -DKMC_MDB_DB=ON -DCODECOV=OFF
```

`MAC_SIZE` is fixed at 64 by the top-level `CMakeLists.txt` for every component; don't override it.

## Build

Products always land in `ammos-cryptolib/build/` and `ammos-cryptolib/install/`, whichever build directory you use.

### Linux (in-source, as `build.sh` does)

```bash
cd ammos-cryptolib
cmake -DDEBUG=OFF -DCRYPTO_KMC=ON -DCRYPTO_LIBGCRYPT=ON -DKEY_KMC=ON -DMC_DISABLED=ON -DSA_MARIADB=ON \
      -DSA_INTERNAL=ON -DTEST=OFF -DSA_FILE=OFF -DKMC_MDB_DB=ON -DCODECOV=OFF .
make
make install
```

### macOS (out-of-source)

```bash
cd ammos-cryptolib
cmake -S . -B build-native \
  -DCRYPTO_KMC=ON -DCRYPTO_LIBGCRYPT=ON -DKEY_KMC=ON -DMC_DISABLED=ON \
  -DSA_MARIADB=ON -DSA_INTERNAL=ON -DTEST=OFF -DSA_FILE=OFF -DKMC_MDB_DB=ON -DCODECOV=OFF
cmake --build build-native
cmake --install build-native
```

`-B build-native` only controls where `CMakeCache.txt` and the generated Makefiles live. Everywhere below,
`make <target>` (in-source) is `cmake --build build-native --target <target>` (out-of-source).

For a debug build add `-DDEBUG=ON`; for verbose build output use `make VERBOSE=1`.

## Make targets

| Target | What it does |
|---|---|
| `all` (default) | Builds the C libraries, CLI tools, `KmcSdlsJNI.jar` and the Python bindings. Never writes outside `ammos-cryptolib/`. |
| `install` | Copies the products to `ammos-cryptolib/install/` (`lib/`, `include/`, `etc/`, `test/`). `deploy.sh` ships `install/lib`. |
| `clean` | Resets everything the build produces: CMake objects, `kmc_sdls_java/swig-java/` and `target/`, the Python build trees, all of `build/` and `install/` (only when `install/` is the default in-tree prefix, never `/usr/local`), `install_manifest.txt` and the `sa_save_file.bin`/`log.txt` files the tests write. It leaves `CMakeCache.txt`; remove the build directory too when you change compilers. |
| `KmcSdlsJNI` | Builds `build/lib/KmcSdlsJNI.jar` with Maven from the SWIG output (part of `all`). |
| `kmc_sdls_java_test` | Runs the KmcSdlsJNI unit tests with Maven against the built native libs. Not part of `all`; `build.sh` runs it. |
| `kmc_sdls_java_m2_install` | Publishes `KmcSdlsJNI` to `~/.m2` (needed by `kmc-sdls-service`). Never part of `all`. |
| `KmcSdlsClient_<X.Y>` | Installs the Python bindings for one interpreter into `build/lib/pythonX.Y/site-packages` (part of `all`). |
| `crypto`, `kmc_sdls`, `kmc_sdls_java` | The native libraries. |
| `kmc_process_security`, `kmc_apply_security`, `kmc_crypto_sequence`, `performance_test` | The CLI tools (in `build/bin`). |
| `test` | Runs ctest (see below). |

Run `make help` (or `cmake --build <dir> --target help`) for the full list for your configuration.

### Build outputs

* `build/lib`: `libcryptolib`, `libkmc_sdls`, `libkmc_sdls_java` (`.so` on Linux, `.dylib`/`.jnilib` on macOS),
  `KmcSdlsJNI.jar`, and `python3.X/site-packages/`.
* `build/bin`: the CLI tools.
* `install/lib`: the same libraries, jar and Python trees as installed, plus `install/test` with the test
  utilities.

## Tests

```bash
ctest --output-on-failure        # in the build directory; run serially, tests write sa_save_file.bin
```

Registered tests: `Kmc_Process_Security`, `TestKmcSdlsJavaTest` (Maven + JNI), `Kmc_Cffi_Cdef_Drift`, and
`Kmc_Python_{Tests,Mock}_PY<X.Y>` for each usable interpreter. Useful filter:
`ctest -R 'Kmc_Python|Kmc_Cffi|TestKmcSdlsJavaTest'`.

* `-DTEST=ON` additionally builds CryptoLib's own C unit tests. They need CryptoLib's test configuration (for
  example `-DCRYPTO_LIBGCRYPT=ON -DMC_INTERNAL=ON -DSA_INTERNAL=ON -DKEY_INTERNAL=ON -DTEST=ON`) rather than the
  `build.sh` flags, otherwise most of them segfault on the key-interface stub. The `*_KMC` C tests also need a
  MariaDB with mTLS, the DCS crypto service, and developer-specific certificate paths, so they are not expected to
  pass elsewhere.
* The Python cffi definitions are checked in (`kmc_sdls_python/_cffi_src/cffi_definitions.i`).
  `Kmc_Cffi_Cdef_Drift` regenerates them from the headers and fails if they differ. If a header change is
  intended, copy `_cffi_src/filtered_headers/gen_cffi_definitions.i` over `cffi_definitions.i`.

## Running a local build

To run the CLI utilities, execute them directly, e.g.:
`./build/bin/kmc_process_security fsw/crypto_tests/data/tc4.1.dat`
(unless you built with `DEBUG=ON`, there is no output from this command).

To use the Python bindings, set `PYTHONPATH` and import the module:

```
export PYTHONPATH=`pwd`/build/lib/python<py_version>/site-packages
python3
from gov.nasa.jpl.ammos.kmc.sdlsclient import KmcSdlsClient
import binascii
kmc_sdls = KmcSdlsClient.KmcSdlsClient([])
tc = bytearray(binascii.unhexlify("2003009E00FF000100001880D037008C197F0B000100840000344892BBC54F5395297D4C37172F2A3C46F6A81C1349E9E26AC80985D8BBD55A5814C662E49FBA52F99BA09558CD21CF268B8E50B2184137E80F76122034C580464E2F06D2659A50508BDFE9E9A55990BA4148AF896D8A6EEBE8B5D2258685D4CE217A20174FDD4F0EFAC62758C51B04E55710A47209C923B641D19A39001F9E986166F5FFD95555"))
kmc_sdls.process_security_tc(tc)
```

## Known gaps (macOS)

* `libkmc_sdls.dylib` references `@rpath/libcryptolib.dylib`, which is not copied to `build/lib` or `install/lib`.
  The build tree works as is; the install tree only imports the Python bindings with
  `DYLD_LIBRARY_PATH=build-native/CryptoLib/src`.
* Python bindings were tested natively with 3.12, 3.13 and 3.14 only.
* Running `kmc-sdls-service` natively against the JNI library is unverified. The Linux container below can run it.

## Linux container build

`.devcontainer/` at the repository root defines an Ubuntu 24.04 container with the Linux prerequisites above:

* `docker-compose.yml`: service `cryptolib-build`. It bind-mounts the repository at `/workspace` and the host's `~/.m2` at
  `/root/.m2`, so `make kmc_sdls_java_m2_install` publishes straight to the host's `~/.m2`. It exposes 8445
  (kmc-sdls-service) and 5005 (JDWP).
* `docker-compose.mariadb.yml`: optional overlay that joins the external `kmcdb_default` network, for SA lookups
  against a MariaDB container reachable as `mariadb`.
* `devcontainer.json`: lets VS Code or JetBrains Gateway attach to the same service with `/workspace` as the project
  root. In CLion, load `ammos-cryptolib/CMakeLists.txt` as the CMake project; don't set `workspaceFolder` to a
  subdirectory, as that mounts the repository twice.

```bash
docker compose -f .devcontainer/docker-compose.yml up -d --build   # add -f .devcontainer/docker-compose.mariadb.yml for MariaDB
docker compose -f .devcontainer/docker-compose.yml exec cryptolib-build bash
cd /workspace/ammos-cryptolib                                       # then build as in "Linux (in-source)" above
export PYTHONPATH=/workspace/ammos-cryptolib/build/lib/python3.12/site-packages
```
