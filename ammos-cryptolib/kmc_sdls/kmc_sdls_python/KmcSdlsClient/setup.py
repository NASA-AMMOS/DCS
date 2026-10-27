# Key Management & Cryptography (KMC)
# Space Data Link Security (SDLS) Client
#
# Copyright 2021, by the California Institute of Technology. ALL RIGHTS
# RESERVED. United States Government Sponsorship acknowledged. Any
# commercial use must be negotiated with the Office of Technology Transfer
# at the California Institute of Technology.
#
# This software may be subject to U.S. export control laws. By accepting
# this software, the user agrees to comply with all applicable U.S. export
# laws and regulations. User has the responsibility to obtain export licenses,
# or other export authority as may be required before exporting such
# information to foreign countries or providing access to foreign persons.

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
