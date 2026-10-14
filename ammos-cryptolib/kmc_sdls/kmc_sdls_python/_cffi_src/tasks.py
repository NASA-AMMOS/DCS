#kmc_sdls_client.py
import subprocess

import cffi
import invoke
import pathlib
import os
import filecmp
import shutil
...

def print_banner(msg):
    print("==================================================")
    print("= {} ".format(msg))

@invoke.task()
def build_kmc_python_c_interface(c):
    """ Build the CFFI Python bindings """
    print_banner("Building CFFI Module")
    ffi = cffi.FFI()

    curdir = pathlib.Path().absolute()

    rpath = ""
    if os.environ.get("CMAKE_BUILD_RPATH") is not None:
        rpath = os.environ.get("CMAKE_BUILD_RPATH")
    else:
        rpath = "$ORIGIN/../../../lib"

    gen_directory = "filtered_headers"
    curr_def_file = "cffi_definitions.i"
    script_path = "./gen_cffi_def.sh"
    print(f"Using script '{script_path}' to generate CFFI definition file and compare against current definition file '{curr_def_file}'")
    result = subprocess.run([script_path], capture_output=True, text=True, check=True)
    if result.stderr:
        print(result.stderr)


    gen_def_file = f"{gen_directory}/gen_cffi_definitions.i"
    if os.path.exists(gen_def_file):
        if not filecmp.cmp(curr_def_file, gen_def_file, shallow=False):
            raise Exception(f"Generated CFFI definition file '{gen_def_file}' and current definition file '{curr_def_file}' are different !!\n{curr_def_file} may need to be updated.")
        else:
            print(f"Generated CFFI definition file '{gen_def_file}' and current definition file '{curr_def_file}' are the same. Removing directory '{gen_directory}' and its contents.")
            shutil.rmtree("filtered_headers")

    with open(curr_def_file, 'r') as cffi_def_file:
        data = cffi_def_file.read()

    ffi.cdef(data, packed=True)
    ffi.set_source("kmc_python_c_sdls_interface"
                   ,"""
                   #include "kmc_sdls.h"
                   """
                   ,libraries=['kmc_sdls']
                   ,define_macros=[("MAC_SIZE", "64")]
                   ,library_dirs=[(curdir / "../../../build/lib").as_posix()]
                   ,extra_link_args=["-Wl,-rpath,%s" % rpath]
                   ,include_dirs=[(curdir / "../../public_inc").as_posix(),
                                  (curdir / "../../../CryptoLib/include").as_posix()])

    ffi.compile(verbose=1, debug=True)
    print("* Complete")

@invoke.task(
    build_kmc_python_c_interface,
)
def all(c):
    """Build and run all tests"""
    pass
