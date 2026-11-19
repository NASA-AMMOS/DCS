import fs from 'node:fs'
import {dialog, expect, sa, test, toast} from './fixtures'

/** Export the current SADB as CSV through the API and save it for upload. */
const exportCsv = async (request, testInfo) => {
    const r = await request.get('api/sa/csv')
    expect(r.ok()).toBeTruthy()
    const path = testInfo.outputPath('sas.csv')
    fs.writeFileSync(path, await r.text())
    return path
}

const upload = async (page, file, {replace = false} = {}) => {
    await page.getByRole('button', {name: 'Bulk'}).click()
    const d = dialog(page, 'Upload CSV')
    await d.locator('#contained-button-file').setInputFiles(file)
    if (replace) {
        await d.getByLabel('Replace existing').check()
    }
    // exact: the dialog's <label> wraps the file input and the buttons, so the input is also named "Continue Cancel"
    await d.getByRole('button', {name: 'Continue', exact: true}).click()
}

test('restricts the file picker to CSV files', async ({page}) => {
    await page.goto('./')
    await page.getByRole('button', {name: 'Bulk'}).click()
    await expect(page.locator('#contained-button-file')).toHaveAttribute('accept', '.csv')
})

test('creates SAs from an uploaded CSV', async ({page, request, sadb}, testInfo) => {
    await sadb.create(sa(100, 46), sa(7, 46, {type: 'TM'}))
    const csv = await exportCsv(request, testInfo)
    await sadb.clear()
    await page.goto('./')
    await upload(page, csv)
    await expect(toast(page, 'Uploaded CSV')).toBeVisible()
    await expect(page.getByRole('cell', {name: '100', exact: true})).toBeVisible()
    expect(await sadb.get('TC', 100, 46)).toMatchObject({ekid: 'kmc/test/key128', iv: '000000000000000000000001'})
    expect(await sadb.get('TM', 7, 46)).not.toBeNull()
})

test('replaces existing SAs when asked', async ({page, request, sadb}, testInfo) => {
    await sadb.create(sa(100, 46))
    const csv = await exportCsv(request, testInfo)
    const r = await request.post('api/sa/iv/TC', {data: {id: {spi: 100, scid: 46}, iv: '0000000000000000000000ee', ivLen: 12}})
    expect(r.ok()).toBeTruthy()
    await page.goto('./')
    await upload(page, csv, {replace: true})
    await expect(toast(page, 'Uploaded CSV')).toBeVisible()
    await expect.poll(async () => (await sadb.get('TC', 100, 46)).iv).toBe('000000000000000000000001')
})

test('reports a conflict instead of overwriting by default', async ({page, request, sadb}, testInfo) => {
    await sadb.create(sa(100, 46))
    const csv = await exportCsv(request, testInfo)
    await page.goto('./')
    await upload(page, csv)
    await expect(toast(page, 'an SA with the SPI/SCID combination 100/46 already exists')).toBeVisible()
})
