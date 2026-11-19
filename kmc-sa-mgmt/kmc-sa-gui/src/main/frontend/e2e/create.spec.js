import {dialog, expect, FRAME_TYPES, hexPrefixStyles, openSa, sa, test, toast, WARNING_MAIN} from './fixtures'

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

// ports of kmc-test SA_Management_Browser*.robot "Create Display"/"Delete Display" and "Create General SA"
for (const type of FRAME_TYPES) {
    test(`creates an SA from the defaults and deletes it on the ${type} tab`, async ({page, sadb}) => {
        await page.goto('./')
        await page.getByRole('tab', {name: type}).click()
        const d = await openCreate(page)
        await d.locator('#spi').fill('137')
        await d.getByRole('button', {name: 'Create', exact: true}).click()
        await expect(toast(page, 'SADB 137/0 has been created')).toBeVisible()
        expect(await sadb.get(type, 137, 0)).toMatchObject({serviceType: 'PLAINTEXT', saState: 1})

        await page.locator('tbody tr').filter({has: page.getByRole('cell', {name: '137', exact: true})})
            .locator('input[type=checkbox]').check()
        await page.getByRole('button', {name: 'DELETE'}).click()
        const del = dialog(page, 'Bulk Delete SAs')
        await expect(del).toContainText('This will permanently delete the following SAs (SPI/SCID):')
        await del.getByRole('button', {name: 'Continue'}).click()
        await expect(toast(page, 'Deleted 1 SAs')).toBeVisible()
        await expect.poll(() => sadb.get(type, 137, 0)).toBeNull()
    })

    test(`creates a fully specified operational SA on the ${type} tab`, async ({page, sadb}) => {
        await page.goto('./')
        await page.getByRole('tab', {name: type}).click()
        const d = await openCreate(page)
        const fields = {spi: '139', scid: '77', vcid: '5', ekid: 'kmc/test/KEY130', shivfLen: '12', stmacfLen: '16',
            ivLen: '12', iv: '000000000000000000002c20', abmLen: '19', abm: 'ffffffffffffff000000000000000000000000',
            arsnLen: '0', arsnw: '5'}
        for (const [id, value] of Object.entries(fields)) {
            await d.locator(`#${id}`).fill(value)
        }
        await d.getByLabel('State').click()
        await page.getByRole('option', {name: 'Operational', exact: true}).click()
        await d.getByLabel('Service Type').click()
        await page.getByRole('option', {name: 'AUTHENTICATED_ENCRYPTION', exact: true}).click()
        await d.getByRole('button', {name: 'Create', exact: true}).click()
        await expect(toast(page, 'SADB 139/77 has been created')).toBeVisible()
        await expect.poll(() => sadb.get(type, 139, 77)).toMatchObject({
            vcid: 5, saState: 3, serviceType: 'AUTHENTICATED_ENCRYPTION', est: 1, ast: 1, ekid: 'kmc/test/KEY130',
            shivfLen: 12, stmacfLen: 16, ivLen: 12, iv: '000000000000000000002c20', abmLen: 19,
            abm: 'ffffffffffffff000000000000000000000000', arsnLen: 0, arsnw: 5})
    })
}
