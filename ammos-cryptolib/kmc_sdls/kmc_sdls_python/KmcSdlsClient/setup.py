# Data Cryptography Services (DSC)
# Space Data Link Security (SDLS) Client
#
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
#  • OSI Approved License: https://opensource.org/licenses/Apache-2.0

import io
from os import path
from setuptools import setup, find_packages

# The package metadata lives here rather than in pyproject.toml's [project] table, which only setuptools >= 61 reads.
# pip normally builds with the setuptools pinned in pyproject.toml, but on Debian/Ubuntu the system setuptools (59.6 on
# Ubuntu 22.04) can shadow it inside pip's isolated build environment and would then produce an "UNKNOWN" package.

description = "KmcSdlsClient provides the necessary python interfaces to the KMC Python C SDLS Interface. " \
              "It enables missions control systems to apply and reverse security on telecommand transfer frames."

here = path.abspath(path.dirname(__file__))
with io.open(path.join(here, "README.md"), encoding="utf-8") as f:
    long_description = f.read()

setup(
    name="KmcSdlsClient",
    version="0.2.0",
    description=description,
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/NASA-AMMOS/DCS",
    author="DCS Development Team",
    author_email="no-reply@jpl.nasa.gov",
    package_dir={"": "src"},
    packages=find_packages(where="src", include=["gov*"]),
    python_requires=">=3.9",
    # cffi 1.14.5 is what RHEL 9 ships; Python 3.12, 3.13 and 3.14 need cffi 1.16, 1.17.1 and 2.0.
    install_requires=[
        "cffi>=1.14.5; python_version < '3.12'",
        "cffi>=1.16; python_version == '3.12'",
        "cffi>=1.17.1; python_version == '3.13'",
        "cffi>=2.0; python_version >= '3.14'",
    ],
    cffi_modules=["kmc_cffi_build.py:ffi"],
)
