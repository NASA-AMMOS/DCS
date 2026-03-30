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
from os import path, listdir
from setuptools import setup, find_packages

#https://packaging.python.org/guides/distributing-packages-using-setuptools/#setup-py

description = "KmcSdlsClient provides the necessary python interfaces to the KMC Python C SDLS Interface. " \
              "It enables missions control systems to apply and reverse security on telecommand transfer frames."

# Get the long description from the README file
here = path.abspath(path.dirname(__file__))
with io.open(path.join(here, 'README.md'), encoding='utf-8') as f:
    long_description = f.read()

setup(
    name = 'KmcSdlsClient',
    version = '0.2.0',
    description  = description,
    long_description = long_description,
    long_description_content_type = 'text/markdown',
    url = 'https://github.com/NASA-AMMOS/DCS',
    packages = find_packages(where='src', include=['gov*'],exclude=['tests']),
    package_dir={"": "src"},
    data_files = [],
    author = 'DCS Development Team',
    author_email='no-reply@jpl.nasa.gov',

    install_requires = [],

    extras_require = {},

    entry_points = {}
)
