import {expect, test} from "vitest";
import {maxScid, maxVcid, validationSchemaFor} from "./SaModalForm.jsx";

const vcidError = async (type, vcid) => {
    try {
        await validationSchemaFor(type).validateAt("vcid", {vcid})
        return null
    } catch (e) {
        return e.message
    }
}

test("VCID limits follow the CCSDS field width per frame type", () => {
    expect(maxVcid("TC")).toBe(63)
    expect(maxVcid("AOS")).toBe(63)
    expect(maxVcid("TM")).toBe(7)
})

test("TC and AOS accept VCID 0-63 and reject 64", async () => {
    for (const type of ["TC", "AOS"]) {
        expect(await vcidError(type, 0)).toBeNull()
        expect(await vcidError(type, 63)).toBeNull()
        expect(await vcidError(type, 64)).toMatch(/at most 63/)
    }
})

test("TM accepts VCID 0-7 and rejects 8", async () => {
    expect(await vcidError("TM", 7)).toBeNull()
    expect(await vcidError("TM", 8)).toMatch(/at most 7/)
})

test("VCID still rejects negatives and non-integers", async () => {
    expect(await vcidError("TC", -1)).not.toBeNull()
    expect(await vcidError("TC", 1.5)).not.toBeNull()
})

const scidError = async (type, scid) => {
    try {
        await validationSchemaFor(type).validateAt("scid", {scid})
        return null
    } catch (e) {
        return e.message
    }
}

test("SCID limits follow the CCSDS field width per frame type", () => {
    expect(maxScid("TC")).toBe(1023)
    expect(maxScid("TM")).toBe(1023)
    expect(maxScid("AOS")).toBe(255)
})

test("TC and TM accept SCID up to 1023 and reject 1024", async () => {
    for (const type of ["TC", "TM"]) {
        expect(await scidError(type, 1023)).toBeNull()
        expect(await scidError(type, 1024)).toMatch(/at most 1023/)
    }
})

test("AOS accepts SCID up to 255 and rejects 256", async () => {
    expect(await scidError("AOS", 255)).toBeNull()
    expect(await scidError("AOS", 256)).toMatch(/at most 255/)
})
