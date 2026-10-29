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
