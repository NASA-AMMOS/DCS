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

 * Open Source License Approved by Caltech/JPL
 * APACHE LICENSE, VERSION 2.0
 *  • Text version: https://www.apache.org/licenses/LICENSE-2.0.txt
 *  • SPDX short identifier: Apache-2.0
 *  • OSI Approved License: https://opensource.org/licenses/Apache-2.0
 */

/*
 *  Simple apply security program that reads a file into memory and calls the Crypto_TC_ApplySecurity function on the data.
 */
#include <stdlib.h>

#include <crypto_error.h>
#include "kmc_crypto_sequence.h"

int main(int argc, char *argv[]) {
    char *buffer;
    char const *filename;
    long buffer_size;
    char *security_type;

    if (argc < 3 || argc % 2 == 0) {
        fprintf(stderr,"Command line usage: \n"\
               "\t%s [<tc|tm|aos> <filename>]+\n"\
               "specify as many [<tc_a|tm_a|aos_a|tc_p|tm_p|aos_p> <filename>] pairs as necessary to complete your crypto sequence test. Each file will be loaded and processed in sequence. \n"\
               "<tc_a|tm_a|aos_a> : Apply TeleCommand (tc_a) | Telemetry (tm_a) | Advanced Orbiting Systems (aos_a) Security T\n"\
               "<tc_p|tm_p|aos_p> : Process TeleCommand (tc_p) | Telemetry (tm_p) | Advanced Orbiting Systems (aos_p) Security T\n"\
               "<filename> : binary file with telecommand transfer frame bits\n",argv[0]);

        return CRYPTO_LIB_ERROR;
    }
    //Setup & Initialize CryptoLib
    sdls_init_unit_test();

    int arg_index = 0;
    uint8_t * ptr_enc_frame = NULL;
    uint16_t enc_frame_len;

    while(arg_index != argc-1){
        security_type = argv[++arg_index];
        debug_printf("Security Type: %s\n",security_type);
        filename = argv[++arg_index];
        debug_printf("Filename: %s\n",filename);
        buffer = c_read_file(filename,&buffer_size);
        debug_printf("File buffer size:%lu\n",buffer_size);
        int buffer_size_i = (int) buffer_size;
        debug_printf("File buffer size int:%d\n",buffer_size_i);
        debug_printf("File content: \n");
        debug_hexprintf(buffer,buffer_size_i);

        //Call Apply/ProcessSecurity on buffer contents depending on type.
        if (strcmp(security_type,"tc_a")==0){
            apply_security_tc(buffer, buffer_size_i,&ptr_enc_frame,&enc_frame_len);
        } else if (strcmp(security_type,"tm_a")==0){
            fprintf(stderr,"Functionality not yet implemented.\n");
        } else if (strcmp(security_type,"aos_a")==0){
            fprintf(stderr,"Functionality not yet implemented.\n");
        } else if (strcmp(security_type,"tc_p")==0){
            TC_t* tc_sdls_processed_frame = malloc(sizeof(TC_t));
            process_security_tc(buffer, &buffer_size_i,tc_sdls_processed_frame);
        } else if (strcmp(security_type,"tm_p")==0){
            fprintf(stderr,"Functionality not yet implemented.\n");
        } else if (strcmp(security_type,"aos_p")==0){
            fprintf(stderr,"Functionality not yet implemented.\n");
        }
        free(buffer);
    }
}
