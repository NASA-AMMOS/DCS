# Copyright (c) 2014-26 California Institute of Technology (“Caltech”). U.S. Government sponsorship acknowledged.
#
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#  • Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#  • Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#  • Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
# THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
# BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
# GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
# STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

# Open Source License Approved by Caltech/JPL
# APACHE LICENSE, VERSION 2.0
#  • Text version: https://www.apache.org/licenses/LICENSE-2.0.txt
#  • SPDX short identifier: Apache-2.0

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
