import {expect, test as base} from '@playwright/test'

export {expect}

export const FRAME_TYPES = ['TC', 'TM', 'AOS']

// MUI's default palette warning.main in each theme
export const WARNING_MAIN = {light: 'rgb(237, 108, 2)', dark: 'rgb(255, 167, 38)'}

/**
 * An SA as the REST API accepts it. Defaults to an unkeyed AES-GCM authenticated-encryption SA with a real
 * (non-hex) key ID; vcid 2 is valid for every frame type.
 */
export const sa = (spi, scid, overrides = {}) => ({
    id: {spi, scid}, spi, scid, type: 'TC',
    tfvn: 0, vcid: 2, mapid: 0, saState: 1, lpid: null,
    serviceType: 'AUTHENTICATED_ENCRYPTION', est: 1, ast: 1,
    ekid: 'kmc/test/key128', akid: null,
    ecsLen: 1, ecs: '01', acsLen: 1, acs: '00',
    ivLen: 12, iv: '000000000000000000000001',
    shivfLen: 12, shsnfLen: 0, shplfLen: 0, stmacfLen: 16,
    abmLen: 19, abm: 'ffffffffffffff000000000000000000000000',
    arsnLen: 0, arsn: '', arsnw: 5,
    ...overrides,
})

/** Thin client for the SA management REST API, used to seed and check state around the UI under test. */
export class Sadb {
    constructor(request) {
        this.request = request
    }

    async list(type) {
        const r = await this.request.get(`api/sa/${type}`)
        expect(r.ok()).toBeTruthy()
        return r.json()
    }

    /** The SA, or null if it does not exist. */
    async get(type, spi, scid) {
        const r = await this.request.get(`api/sa/${type}?spi=${spi}&scid=${scid}`)
        expect(r.ok()).toBeTruthy()
        return (await r.json())[0] ?? null
    }

    async create(...sas) {
        for (const s of sas) {
            const r = await this.request.put('api/sa', {data: s})
            expect(r.ok(), `seed ${s.type} ${s.spi}/${s.scid}: ${await r.text()}`).toBeTruthy()
        }
    }

    async clear() {
        for (const type of FRAME_TYPES) {
            const ids = (await this.list(type)).map(s => s.id)
            if (ids.length) {
                const r = await this.request.delete(`api/sa/${type}`, {data: ids})
                expect(r.ok()).toBeTruthy()
            }
        }
    }
}

export const test = base.extend({
    // every test starts from an empty SADB
    sadb: async ({request}, use) => {
        const sadb = new Sadb(request)
        await sadb.clear()
        await use(sadb)
    },
    // fail the test on any uncaught error in the page
    page: async ({page, sadb}, use) => {
        const errors = []
        page.on('pageerror', e => errors.push(e.message))
        await use(page)
        expect(errors, 'uncaught page errors').toEqual([])
    },
})

/** Toast (notistack snackbar) containing the given text. */
export const toast = (page, text) => page.locator('#notistack-snackbar').filter({hasText: text})

/** Open the Edit SA dialog for the row with the given SPI on the current tab. */
export const openSa = async (page, spi) => {
    await page.getByRole('cell', {name: String(spi), exact: true}).first().click()
    await expect(page.getByText('Edit SA')).toBeVisible()
}

/** A dialog by its title text. */
export const dialog = (page, title) => page.getByRole('dialog').filter({hasText: title}).last()

/** Computed font weight, family and color of every leaf "0x" adornment inside a container. */
export const hexPrefixStyles = (container) => container.evaluate(d =>
    [...d.querySelectorAll('p,span')].filter(e => e.textContent === '0x' && e.children.length === 0)
        .map(e => {
            const c = getComputedStyle(e)
            return {weight: c.fontWeight, family: c.fontFamily, color: c.color}
        }))
