/*
 * Copyright 2022, by the California Institute of Technology.
 * ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology
 * Transfer at the California Institute of Technology.
 *
 * This software may be subject to U.S. export control laws. By accepting
 * this software, the user agrees to comply with all applicable U.S.
 * export laws and regulations. User has the responsibility to obtain
 * export licenses, or other export authority as may be required before
 * exporting such information to foreign countries or providing access to
 * foreign persons.
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

    let parsed = parseInt(value, 16)
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
    return parseInt(value, 16)
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