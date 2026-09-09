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
import * as utilities from './utilities.js'
import {expect, test} from "vitest";

test('test state lookup', () => {
    expect(utilities.stateLookupInt(1)).toBe('Unkeyed | Expired')
    expect(utilities.stateLookupInt(2)).toBe('Keyed')
    expect(utilities.stateLookupInt(3)).toBe('Operational')
    expect(utilities.stateLookupStr('Unkeyed | Expired')).toBe(1)
    expect(utilities.stateLookupStr('Keyed')).toBe(2)
    expect(utilities.stateLookupStr('Operational')).toBe(3)
})

test('test service type lookup', () => {
    expect(utilities.serviceTypeLookupInt(0)).toBe('PLAINTEXT')
    expect(utilities.serviceTypeLookupInt(1)).toBe('AUTHENTICATION')
    expect(utilities.serviceTypeLookupInt(2)).toBe('ENCRYPTION')
    expect(utilities.serviceTypeLookupInt(3)).toBe('AUTHENTICATED_ENCRYPTION')
    expect(utilities.serviceTypeLookupStr('PLAINTEXT')).toBe(0)
    expect(utilities.serviceTypeLookupStr('AUTHENTICATION')).toBe(1)
    expect(utilities.serviceTypeLookupStr('ENCRYPTION')).toBe(2)
    expect(utilities.serviceTypeLookupStr('AUTHENTICATED_ENCRYPTION')).toBe(3)
})

test('test enc cipher hex lookup', () => {
    expect(utilities.hexEncLookup()).toBe('')
    expect(utilities.hexEncLookup('')).toBe('')
    expect(utilities.hexEncLookup('0x00').name).toBe('None')
    expect(utilities.hexEncLookup('0x01').name).toBe('AES256-GCM')
    expect(utilities.hexEncLookup('0x02').name).toBe('AES256-CBC')
    expect(utilities.hexEncLookup('0x')).toBe('0x')
    expect(utilities.hexEncLookup('0x00').name).toBe('None')
    expect(utilities.hexEncLookup('0x01').name).toBe('AES256-GCM')
    expect(utilities.hexEncLookup('0x02').name).toBe('AES256-CBC')
    expect(utilities.hexAuthLookup('hi there')).toBe('')
})

test('test auth cipher hex lookup', () => {
    expect(utilities.hexAuthLookup()).toBe('')
    expect(utilities.hexAuthLookup('')).toBe('')
    expect(utilities.hexAuthLookup('00').name).toBe('None')
    expect(utilities.hexAuthLookup('01').name).toBe('AES256-CMAC')
    expect(utilities.hexAuthLookup('02').name).toBe('HMAC-SHA256')
    expect(utilities.hexAuthLookup('03').name).toBe('HMAC-SHA512')
    expect(utilities.hexAuthLookup('04')).toBe('04')
    expect(utilities.hexAuthLookup('0x')).toBe('0x')
    expect(utilities.hexAuthLookup('0x00').name).toBe('None')
    expect(utilities.hexAuthLookup('0x01').name).toBe('AES256-CMAC')
    expect(utilities.hexAuthLookup('0x02').name).toBe('HMAC-SHA256')
    expect(utilities.hexAuthLookup('0x03').name).toBe('HMAC-SHA512')
    expect(utilities.hexAuthLookup('0x04')).toBe('0x04')
    expect(utilities.hexAuthLookup('hi there')).toBe('')
})