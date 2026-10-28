import os
import pathlib
import sys

import cffi

HERE = pathlib.Path(__file__).resolve().parent          # .../kmc_sdls_python/KmcSdlsClient
KMC_SDLS = HERE.parent.parent                             # .../kmc_sdls
CL_ROOT = KMC_SDLS.parent                                 # .../ammos-cryptolib

cdef_file = pathlib.Path(os.environ.get("KMC_CFFI_CDEF", HERE.parent / "_cffi_src" / "cffi_definitions.i"))
lib_dir = os.environ.get("KMC_SDLS_LIB_DIR", (CL_ROOT / "build" / "lib").as_posix())
include_dirs = os.environ.get(
    "KMC_SDLS_INCLUDE_DIRS",
    os.pathsep.join([(KMC_SDLS / "public_inc").as_posix(), (CL_ROOT / "CryptoLib" / "include").as_posix()]),
).split(os.pathsep)
_origin = "@loader_path" if sys.platform == "darwin" else "$ORIGIN"
rpath = os.environ.get("CMAKE_BUILD_RPATH", f"{_origin}/../../../lib")

ffi = cffi.FFI()
ffi.cdef(cdef_file.read_text(), packed=True)
ffi.set_source(
    "kmc_python_c_sdls_interface",
    '#include "kmc_sdls.h"',
    libraries=["kmc_sdls"],
    library_dirs=[lib_dir],
    include_dirs=include_dirs,
    define_macros=[("MAC_SIZE", os.environ.get("MAC_SIZE", "64"))],
    extra_link_args=["-Wl,-rpath,%s" % rpath],
    py_limited_api=False,
)
