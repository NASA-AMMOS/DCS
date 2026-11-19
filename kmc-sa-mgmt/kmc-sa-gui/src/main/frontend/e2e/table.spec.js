import fs from 'node:fs'
import {dialog, expect, sa, test, toast} from './fixtures'

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
