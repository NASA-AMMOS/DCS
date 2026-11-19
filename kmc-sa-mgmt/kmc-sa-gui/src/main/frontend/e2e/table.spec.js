import fs from 'node:fs'
import {dialog, expect, FRAME_TYPES, sa, test, toast} from './fixtures'

// Several tests here port kmc-test's SA_Management_Browser*.robot suites, which need an installed DCS.

const row = (page, spi) => page.locator('tbody tr').filter({has: page.getByRole('cell', {name: String(spi), exact: true})})

test('shows each frame type on its own tab', async ({page, sadb}) => {
    await sadb.create(sa(100, 46), sa(7, 46, {type: 'TM'}), sa(9, 46, {type: 'AOS'}))
    await page.goto('./')
    await expect(row(page, 100)).toBeVisible()
    await expect(row(page, 7)).toHaveCount(0)
    await page.getByRole('tab', {name: 'TM'}).click()
    await expect(row(page, 7)).toBeVisible()
    await expect(row(page, 100)).toHaveCount(0)
    await page.getByRole('tab', {name: 'AOS'}).click()
    await expect(row(page, 9)).toBeVisible()
})

test('filters rows by SPI', async ({page, sadb}) => {
    await sadb.create(sa(100, 46), sa(101, 46))
    await page.goto('./')
    await expect(page.locator('tbody tr')).toHaveCount(2)
    await page.locator('th').filter({hasText: /^SPI/}).getByRole('button').first().click()
    await page.getByRole('textbox').last().fill('101')
    await page.keyboard.press('Escape')
    await expect(page.locator('tbody tr')).toHaveCount(1)
    await expect(row(page, 101)).toBeVisible()
})

test('exports the SADB as CSV', async ({page, sadb}) => {
    await sadb.create(sa(100, 46), sa(7, 46, {type: 'TM'}))
    await page.goto('./')
    const [download] = await Promise.all([page.waitForEvent('download'),
        page.getByRole('button', {name: 'Export'}).click()])
    expect(download.suggestedFilename()).toMatch(/^SADB_.*\.csv$/)
    const csv = fs.readFileSync(await download.path(), 'utf8')
    expect(csv).toContain('"100","46"')
    expect(csv).toContain('"7","46"')
    await expect(toast(page, 'Downloaded CSV')).toBeVisible()
})

test('deletes the selected rows', async ({page, sadb}) => {
    await sadb.create(sa(100, 46), sa(101, 46))
    await page.goto('./')
    await row(page, 101).locator('input[type=checkbox]').check()
    await page.getByRole('button', {name: 'DELETE'}).click()
    const d = dialog(page, 'Bulk Delete SAs')
    await expect(d).toContainText('101/46')
    await d.getByRole('button', {name: 'Continue'}).click()
    await expect(toast(page, 'Deleted 1 SAs')).toBeVisible()
    await expect(row(page, 101)).toHaveCount(0)
    expect(await sadb.get('TC', 101, 46)).toBeNull()
    expect(await sadb.get('TC', 100, 46)).not.toBeNull()
})

const filterColumn = async (page, header, value) => {
    await page.locator('th').filter({hasText: new RegExp(`^${header}`)}).getByRole('button').first().click()
    await page.getByRole('textbox').last().fill(value)
    await page.keyboard.press('Escape')
}

for (const type of FRAME_TYPES) {
    test(`shows the title and table controls on the ${type} tab`, async ({page}) => {
        await page.goto('./')
        await page.getByRole('tab', {name: type}).click()
        await expect(page.getByText('AMMOS Security Association Database (SADB) Management')).toBeVisible()
        for (const name of ['Columns', 'DELETE', 'Create', 'Bulk', 'Export', 'Reload']) {
            await expect(page.getByRole('button', {name, exact: true})).toBeVisible()
        }
        await expect(page.getByRole('columnheader', {name: /^SPI/})).toBeVisible()
    })
}

test('filters rows by VCID and by AKID', async ({page, sadb}) => {
    await sadb.create(sa(100, 46, {vcid: 2}), sa(101, 46, {vcid: 20}),
        sa(102, 46, {serviceType: 'AUTHENTICATION', est: 0, ast: 1, ekid: null, akid: 'kmc/test/KEY130', acs: '02'}),
        sa(103, 46, {serviceType: 'AUTHENTICATION', est: 0, ast: 1, ekid: null, akid: 'kmc/test/NIST_CMAC_90', acs: '01'}))
    await page.goto('./')
    await expect(page.locator('tbody tr')).toHaveCount(4)
    await filterColumn(page, 'VCID', '20')
    await expect(page.locator('tbody tr')).toHaveCount(1)
    await expect(row(page, 101)).toBeVisible()

    await page.getByRole('button', {name: 'Reload'}).click()
    await expect(page.locator('tbody tr')).toHaveCount(4)
    await filterColumn(page, 'AKID', 'KEY130')
    await expect(page.locator('tbody tr')).toHaveCount(1)
    await expect(page.getByText('kmc/test/NIST_CMAC_90')).toHaveCount(0)
    await expect(row(page, 102)).toBeVisible()
})

test('Reload fetches SAs created elsewhere and clears the filters', async ({page, sadb}) => {
    await sadb.create(sa(100, 46), sa(101, 46))
    await page.goto('./')
    await filterColumn(page, 'SPI', '100')
    await expect(page.locator('tbody tr')).toHaveCount(1)
    await sadb.create(sa(102, 46))
    await page.getByRole('button', {name: 'Reload'}).click()
    await expect(page.locator('tbody tr')).toHaveCount(3)
    await expect(row(page, 102)).toBeVisible()
})

test('the Columns chooser shows hidden columns, remembers the choice and resets to the defaults', async ({page, sadb}) => {
    await sadb.create(sa(100, 46))
    await page.goto('./')
    const ivHeader = page.getByRole('columnheader', {name: /^IV Length/})
    await expect(ivHeader).toHaveCount(0)

    await page.getByRole('button', {name: 'Columns'}).click()
    const chooser = page.locator('#colSel')
    await expect(chooser.getByRole('button', {name: 'Reset'})).toBeVisible()
    await expect(chooser.getByText('ABM Length')).toBeVisible()
    await chooser.getByLabel('IV Length').check()
    await page.keyboard.press('Escape')
    await expect(ivHeader).toBeVisible()
    await expect(row(page, 100).getByRole('cell', {name: '12', exact: true}).first()).toBeVisible()

    await page.reload()
    await expect(ivHeader).toBeVisible()

    await page.getByRole('button', {name: 'Columns'}).click()
    await chooser.getByRole('button', {name: 'Reset'}).click()
    await page.keyboard.press('Escape')
    await expect(ivHeader).toHaveCount(0)
})
