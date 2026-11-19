import {dialog, expect, hexPrefixStyles, openSa, sa, test, toast, WARNING_MAIN} from './fixtures'

const openCreate = async (page) => {
    await page.getByRole('button', {name: 'Create', exact: true}).click()
    const d = dialog(page, 'Create SA')
    await expect(d).toBeVisible()
    return d
}

test('creates an encrypted SA from the form', async ({page, sadb}) => {
    await page.goto('./')
    const d = await openCreate(page)
    await d.locator('#spi').fill('300')
    await d.locator('#scid').fill('46')
    await d.locator('#vcid').fill('3')
    await d.getByLabel('Service Type').click()
    await page.getByRole('option', {name: 'ENCRYPTION', exact: true}).click()
    await d.locator('#ekid').fill('kmc/test/key256')
    await d.locator('#iv').fill('000000000000000000000001')
    await d.getByRole('button', {name: 'Create', exact: true}).click()
    await expect(toast(page, 'SADB 300/46 has been created')).toBeVisible()
    await expect.poll(() => sadb.get('TC', 300, 46))
        .toMatchObject({vcid: 3, est: 1, ast: 0, ekid: 'kmc/test/key256', serviceType: 'ENCRYPTION'})
    await expect(page.getByRole('cell', {name: '300', exact: true})).toBeVisible()
})

test('enforces the 3-bit VCID width for TM in the form', async ({page, sadb}) => {
    await page.goto('./')
    await page.getByRole('tab', {name: 'TM'}).click()
    const d = await openCreate(page)
    await d.locator('#vcid').fill('9')
    await d.locator('#vcid').blur()
    await expect(d.getByText('VCID must be at most 7 for TM frames')).toBeVisible()
})

for (const theme of ['light', 'dark']) {
    test(`hex fields use bold warning-colored 0x prefixes and Roboto Mono (${theme} theme)`, async ({page, sadb}) => {
        await sadb.create(sa(100, 46))
        await page.addInitScript(dark => localStorage.setItem('theme', JSON.stringify(dark)), theme === 'dark')
        await page.goto('./')
        const expected = {weight: '700', family: '"Roboto Mono"', color: WARNING_MAIN[theme]}

        const create = await openCreate(page)
        expect(await hexPrefixStyles(create)).toEqual([expected, expected, expected])
        await expect(create.locator('#iv')).toHaveCSS('font-family', '"Roboto Mono"')
        await page.keyboard.press('Escape')
        await expect(create).toBeHidden()

        await openSa(page, 100)
        await page.getByRole('button', {name: 'IV/ARSN'}).click()
        expect(await hexPrefixStyles(dialog(page, 'Reset IV and/or ARSN'))).toEqual([expected, expected])
    })
}
