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

-- CREATE USER IF NOT EXISTS sadb_user PASSWORD 'sadb_test' ADMIN;

CREATE SCHEMA IF NOT EXISTS sadb;

USE sadb;

-- IV_LEN should probably not have that default -- to be reviewed.

CREATE TABLE IF NOT EXISTS security_associations
(
    spi        INT             NOT NULL,
    ekid       VARCHAR(100)             DEFAULT NULL                                        -- 'EG, for KMC Crypto KeyRef, 'kmc/test/KEY130', for libgcrypt '130'
    ,
    akid       VARCHAR(100)             DEFAULT NULL                                        -- Same as ekid
    ,
    sa_state   SMALLINT        NOT NULL DEFAULT 0,
    tfvn       TINYINT         NOT NULL,
    scid       SMALLINT        NOT NULL,
    vcid       TINYINT         NOT NULL,
    mapid      TINYINT         NOT NULL DEFAULT 0,
    lpid       SMALLINT,
    est        SMALLINT        NOT NULL DEFAULT 0,
    ast        SMALLINT        NOT NULL DEFAULT 0,
    shivf_len  SMALLINT        NOT NULL DEFAULT 12,
    shsnf_len  SMALLINT        NOT NULL DEFAULT 0,
    shplf_len  SMALLINT        NOT NULL DEFAULT 0,
    stmacf_len SMALLINT        NOT NULL DEFAULT 0,
    ecs_len    SMALLINT        NOT NULL DEFAULT 1,
    ecs        VARBINARY(4)    NOT NULL DEFAULT X'01'                                       -- ECS_SIZE=4
    ,
    iv_len     SMALLINT                 NOT NULL DEFAULT 0,
    iv         VARBINARY(20)            DEFAULT NULL               -- IV_SIZE=12
    ,
    acs_len    SMALLINT        NOT NULL DEFAULT 0,
    acs        VARBINARY(4)    NOT NULL DEFAULT X'00',
    abm_len    MEDIUMINT,
    abm        VARBINARY(1786) NOT NULL DEFAULT X'0000FC0000FFFF000000000000000000000000'   -- ABM_SIZE=1024
    ,
    arsn_len   SMALLINT        NOT NULL DEFAULT 0,
    arsn       VARBINARY(20)   NOT NULL DEFAULT X'0000000000000000000000000000000000000000' -- ARSN_SIZE=20 , TBD why so large...
    ,
    arsnw      SMALLINT        NOT NULL DEFAULT 0                                           -- ARSNW_SIZE=1
);

create unique index if not exists main_spi on security_associations (spi, scid, vcid, tfvn, mapid);

-- IV_LEN should probably not have that default -- to be reviewed.

CREATE TABLE IF NOT EXISTS security_associations_aos
(
    spi        INT             NOT NULL,
    ekid       VARCHAR(100)             DEFAULT NULL                                        -- 'EG, for KMC Crypto KeyRef, 'kmc/test/KEY130', for libgcrypt '130'
    ,
    akid       VARCHAR(100)             DEFAULT NULL                                        -- Same as ekid
    ,
    sa_state   SMALLINT        NOT NULL DEFAULT 0,
    tfvn       TINYINT         NOT NULL,
    scid       SMALLINT        NOT NULL,
    vcid       TINYINT         NOT NULL,
    mapid      TINYINT         NOT NULL DEFAULT 0,
    lpid       SMALLINT,
    est        SMALLINT        NOT NULL DEFAULT 0,
    ast        SMALLINT        NOT NULL DEFAULT 0,
    shivf_len  SMALLINT        NOT NULL DEFAULT 12,
    shsnf_len  SMALLINT        NOT NULL DEFAULT 0,
    shplf_len  SMALLINT        NOT NULL DEFAULT 0,
    stmacf_len SMALLINT        NOT NULL DEFAULT 0,
    ecs_len    SMALLINT        NOT NULL DEFAULT 1,
    ecs        VARBINARY(4)    NOT NULL DEFAULT X'01'                                       -- ECS_SIZE=4
    ,
    iv_len     SMALLINT                 NOT NULL DEFAULT 0,
    iv         VARBINARY(20)            DEFAULT NULL               -- IV_SIZE=12
    ,
    acs_len    SMALLINT        NOT NULL DEFAULT 0,
    acs        VARBINARY(4)    NOT NULL DEFAULT X'00',
    abm_len    MEDIUMINT,
    abm        VARBINARY(1786) NOT NULL DEFAULT X'0000FC0000FFFF000000000000000000000000'   -- ABM_SIZE=1024
    ,
    arsn_len   SMALLINT        NOT NULL DEFAULT 0,
    arsn       VARBINARY(20)   NOT NULL DEFAULT X'0000000000000000000000000000000000000000' -- ARSN_SIZE=20 , TBD why so large...
    ,
    arsnw      SMALLINT        NOT NULL DEFAULT 0                                           -- ARSNW_SIZE=1
    );

create unique index if not exists main_spi on security_associations_aos (spi, scid, vcid, tfvn, mapid);

CREATE TABLE IF NOT EXISTS security_associations_tm
(
    spi        INT             NOT NULL,
    ekid       VARCHAR(100)             DEFAULT NULL                                        -- 'EG, for KMC Crypto KeyRef, 'kmc/test/KEY130', for libgcrypt '130'
    ,
    akid       VARCHAR(100)             DEFAULT NULL                                        -- Same as ekid
    ,
    sa_state   SMALLINT        NOT NULL DEFAULT 0,
    tfvn       TINYINT         NOT NULL,
    scid       SMALLINT        NOT NULL,
    vcid       TINYINT         NOT NULL,
    mapid      TINYINT         NOT NULL DEFAULT 0,
    lpid       SMALLINT,
    est        SMALLINT        NOT NULL DEFAULT 0,
    ast        SMALLINT        NOT NULL DEFAULT 0,
    shivf_len  SMALLINT        NOT NULL DEFAULT 12,
    shsnf_len  SMALLINT        NOT NULL DEFAULT 0,
    shplf_len  SMALLINT        NOT NULL DEFAULT 0,
    stmacf_len SMALLINT        NOT NULL DEFAULT 0,
    ecs_len    SMALLINT        NOT NULL DEFAULT 1,
    ecs        VARBINARY(4)    NOT NULL DEFAULT X'01'                                       -- ECS_SIZE=4
    ,
    iv_len     SMALLINT                 NOT NULL DEFAULT 0,
    iv         VARBINARY(20)            DEFAULT NULL               -- IV_SIZE=12
    ,
    acs_len    SMALLINT        NOT NULL DEFAULT 0,
    acs        VARBINARY(4)    NOT NULL DEFAULT X'00',
    abm_len    MEDIUMINT,
    abm        VARBINARY(1786) NOT NULL DEFAULT X'0000FC0000FFFF000000000000000000000000'   -- ABM_SIZE=1024
    ,
    arsn_len   SMALLINT        NOT NULL DEFAULT 0,
    arsn       VARBINARY(20)   NOT NULL DEFAULT X'0000000000000000000000000000000000000000' -- ARSN_SIZE=20 , TBD why so large...
    ,
    arsnw      SMALLINT        NOT NULL DEFAULT 0                                           -- ARSNW_SIZE=1
    );

create unique index if not exists main_spi on security_associations_tm (spi, scid, vcid, tfvn, mapid);
