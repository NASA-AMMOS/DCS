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
/**
 * @author panjames
 */

export const PLAINTEXT = "PLAINTEXT";

export const AUTHENTICATION = "AUTHENTICATION";

export const ENCRYPTION = "ENCRYPTION";

export const AUTHENTICATED_ENCRYPTION = "AUTHENTICATED_ENCRYPTION";

export const UNKEYED_EXPIRED = 'Unkeyed | Expired';

export const KEYED = 'Keyed';

export const OPERATIONAL = 'Operational';

/**
 * Auth cipher suite enum
 * @type {Readonly<{MAC_AES256: {name: string, value: number}, MAC_SHA512: {name: string, value: number}, MAC_SHA256: {name: string, value: number}, NONE: {name: string, value: number}}>}
 */
export const AUTH_CIPHERS = Object.freeze({
    NONE: {
        value: 0,
        name: 'None',
        hex: '00'
    },
    AES256_CMAC: {
        value: 1,
        name: 'AES256-CMAC',
        hex: '01'
    },
    HMAC_SHA256: {
        value: 2,
        name: 'HMAC-SHA256',
        hex: '02'
    },
    HMAC_SHA512: {
        value: 3,
        name: 'HMAC-SHA512',
        hex: '03'
    }
})

/**
 * Enc cipher suite enum
 * @type {Readonly<{AES256_GCM: {name: string, value: number}, NONE: {name: string, value: number}}>}
 */
export const ENC_CYPHERS = Object.freeze({
    NONE: {
        value: 0,
        name: 'None',
        hex: '00'
    },
    AES256_GCM: {
        value: 1,
        name: 'AES256-GCM',
        hex: '01'
    },
    AES256_CBC: {
        value: 2,
        name: 'AES256-CBC',
        hex: '02'
    }
})

/**
 * SA Service Type lookup. Possible Values are:
 * PLAINTEXT                - 0
 * AUTHENTICATION           - 1
 * ENCRYPTION               - 2
 * AUTHENTICATED_ENCRYPTION - 3
 *
 * @param st
 * @returns {number}
 */
export function serviceTypeLookupStr(st) {
    switch (st) {
        case PLAINTEXT:
            return 0
        case AUTHENTICATION:
            return 1
        case ENCRYPTION:
            return 2
        case AUTHENTICATED_ENCRYPTION:
            return 3
        default:
            return undefined
    }
}

/**
 * Lookup hex string to Enc Cipher Suite name lookup
 * @param value
 * @returns {*}
 */
export function hexEncLookup(value) {
    return hexCipherLookup(ENC_CYPHERS, value)
}

function hexCipherLookup(obj, value) {
    if (value === null || value === undefined) {
        return ''
    } else if (value === '') {
        return value
    }

    if (!value.match(/^(0x)?([\da-fA-F]{2})*$/)) {
        return ''
    }

    let parsed = Number.parseInt(value, 16)
    let entries = Object.entries(obj).find((entry) => {
        const val = entry[1]
        return val.value === parsed
    })

    if (entries === undefined || entries.length === 0) {
        return value
    }
    return entries[1]
}

export function intEcsLookup(value) {
    return intCipherLookup(ENC_CYPHERS, value)
}

export function intAcsLookup(value) {
    return intCipherLookup(AUTH_CIPHERS, value)
}

function intCipherLookup(obj, value) {
    if (value === null) {
        return ''
    }  else if (value === '') {
        return value
    }

    if (value instanceof Number) {
        return ''
    }

    let entries = Object.entries(obj).find((entry) => {
        const val = entry[1]
        return val.value === value
    })

    if (entries === undefined || entries.length === 0) {
        return value
    }
    return entries[1]
}

/**
 * Hex string to int value
 * @param value
 * @returns {number}
 */
export function hexToInt(value) {
    if (value === null || value === '') {
        return -1
    }
    return Number.parseInt(value, 16)
}

/**
 * Lookup hex string to Auth Cipher Suite name
 * @param value hex string
 * @returns {*}
 */
export function hexAuthLookup(value) {
    return hexCipherLookup(AUTH_CIPHERS, value)
}

/**
 * SA Service Type lookup. Possible values are:
 * 0 - PLAINTEXT
 * 1 - AUTHENTICATION
 * 2 - ENCRYPTION
 * 3 - AUTHENTICATED_ENCRYPTION
 *
 * @param st integer value
 * @returns {string} string enum
 */
export function serviceTypeLookupInt(st) {
    switch (st) {
        case 0:
            return PLAINTEXT
        case 1:
            return AUTHENTICATION
        case 2:
            return ENCRYPTION
        case 3:
            return AUTHENTICATED_ENCRYPTION
        default:
            return undefined
    }
}

/**
 * SA State lookup from integer value to string. Possible values are:
 * 1 - 'Unkeyed | Expired'
 * 2 - 'Keyed'
 * 3 - 'Operational'
 *
 * @param state integer value
 * @returns {string}
 */
export function stateLookupInt(state) {
    switch (state) {
        case 1:
            return UNKEYED_EXPIRED
        case 2:
            return KEYED
        case 3:
            return OPERATIONAL
        default:
            return undefined
    }
}

/**
 * SA State lookup from string to integer value. Possible values are:
 * 'Unkeyed | Expired' - 1
 * 'Keyed'             - 2
 * 'Operational'       - 3
 *
 * @param state
 * @returns {number}
 */
export function stateLookupStr(state) {
    switch (state) {
        case UNKEYED_EXPIRED:
            return 1
        case KEYED:
            return 2
        case OPERATIONAL:
            return 3
        default:
            return undefined
    }
}
