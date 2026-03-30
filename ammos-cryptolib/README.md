# AMMOS-CryptoLib

# CryptoLib
Provides a software implementation of the CCSDS Space Data Link Security Protocol (SDLS) to secure communications between a ground system and a spacecraft.

In order to build crypto the following must be installed assuming Ubuntu 18.04 LTS:
* `sudo apt install libgpg-error-dev:i386 libgcrypt20-dev:i386`

The following Python 3 modules must be installed to build the AMMOS-CryptoLib Python bindings:
`cffi`
`invoke`

AMMOS-CryptoLib will automatically build python bindings for all python 3 versions in your PATH. If you have multiple versions of python 3 installed, you must install the invoke and CFFI modules for each install, eg:
```
pip3.9 install --user invoke cffi
pip3.8 install --user invoke cffi
```

## Checkout and Build the AMMOS-CryptoLib and Nasa CryptoLib libraries
```
git clone git@github.com:NASA-AMMOS/DCS
cd ammos-cryptoLib
git submodule init
git submodule update
cmake .
make
```
### Troubleshooting the Build
To build a version of AMMOS-CryptoLib with debug symbols and debug print output on, replace the `cmake` command above as follows:
`cmake -DDEBUG=ON .`
To turn on verbose output of the build, replace the `make` command above as follows:
`make VERBOSE=1`

## Running a Local AMMOS-CryptoLib Build
Once you build AMMOS-CryptoLib, to run the binary test utilities, all you need to do is execute them directly, EG:
`./build/bin/process_security fsw/crypto_tests/data/tc4.1.dat`
Note: Unless you built with the DEBUG flag, there will be no output from this command.

To run the Python and execute a basic CryptoLib function, you'll need to set PYTHONPATH and import the module. A simple test use case looks as follows:
```
export PYTHONPATH=`pwd`/build/lib/python<py_version>/site-packages
python3
from gov.nasa.jpl.ammos.kmc.sdlsclient import KmcSdlsClient
import binascii
kmc_sdls = KmcSdlsClient.KmcSdlsClient([])
tc = bytearray(binascii.unhexlify("2003009E00FF000100001880D037008C197F0B000100840000344892BBC54F5395297D4C37172F2A3C46F6A81C1349E9E26AC80985D8BBD55A5814C662E49FBA52F99BA09558CD21CF268B8E50B2184137E80F76122034C580464E2F06D2659A50508BDFE9E9A55990BA4148AF896D8A6EEBE8B5D2258685D4CE217A20174FDD4F0EFAC62758C51B04E55710A47209C923B641D19A39001F9E986166F5FFD95555"))
kmc_sdls.process_security_tc(tc)
```
