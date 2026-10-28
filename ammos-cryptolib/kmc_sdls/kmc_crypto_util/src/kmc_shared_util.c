/*
 * Copyright (c) 2014-26 California Institute of Technology (“Caltech”). U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *  • Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *  • Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *  • Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Open Source License Approved by Caltech/JPL
 * APACHE LICENSE, VERSION 2.0
 *  • Text version: https://www.apache.org/licenses/LICENSE-2.0.txt
 *  • SPDX short identifier: Apache-2.0
 *  • OSI Approved License: https://opensource.org/licenses/Apache-2.0
 */

//temp debug, remove later.
#include <string.h>
#include <stdlib.h>

#include "kmc_shared_util.h"


/*
* Function:  c_read_file
* --------------------
* Reads a file from disk into a char * buffer.
*
*
*  const char* f_name: file name & path to be read
*  long* f_size:
*
*  returns: a malloc'd char* containing the contents of the buffer.
*      Note that this buffer is NOT null terminated and must be free()'d.
*/
char * c_read_file(const char * f_name, long * f_size) {
    char* buffer=0;
    long length;
    FILE* f = fopen(f_name,"rb");
    if (f){
        fseek (f, 0, SEEK_END);
        length = ftell (f);
        fseek (f, 0, SEEK_SET);
        buffer = malloc (length);
        if (buffer) {
            fread (buffer, 1, length, f);
        }
        fclose (f);
    }
    if (buffer){
        *f_size = length;
        debug_printf("Buffer Length:%lu\n",length);
        return buffer;
    } else{
        return NULL;
    }

}

#ifdef DEBUG
void debug_printf(const char *format, ...)
{
    va_list args;
    fprintf(stderr, "DEBUG - ");
    va_start(args, format);
    vfprintf(stderr,format, args);
    va_end(args);
}
#else
void debug_printf(const char* format, ...) {
    //Do nothing, DEBUG preprocessor disabled.
}
#endif

#ifdef DEBUG
void debug_hexprintf(const char *bin_data, int size_bin_data)
{
    //https://stackoverflow.com/questions/6357031/how-do-you-convert-a-byte-array-to-a-hexadecimal-string-in-c
    //https://stackoverflow.com/questions/5040920/converting-from-signed-char-to-unsigned-char-and-back-again
    unsigned char* u_bin_data = (unsigned char*)bin_data;
    unsigned char output[(size_bin_data*2)+1];
    char *ptr = &output[0];
    int i;
    for(i=0; i < size_bin_data; i++){
        ptr += sprintf(ptr,"%02X",u_bin_data[i]);
    }
    debug_printf("%s\n",output);
}
#else
void debug_hexprintf(const char* bin_data, int size_bin_data) {
    //Do nothing, DEBUG preprocessor disabled.
}
#endif
