import {dialog, expect, openSa, sa, test, toast} from './fixtures'

test.beforeEach(async ({page, sadb}) => {
    await sadb.create(sa(100, 46))
    await page.goto('./')
    await openSa(page, 100)
})

const resetDialog = async (page) => {
    await page.getByRole('button', {name: 'IV/ARSN'}).click()
    return dialog(page, 'Reset IV and/or ARSN')
}

test('resets IV and ARSN', async ({page, sadb}) => {
    const d = await resetDialog(page)
    await d.locator('#iv').fill('0000000000000000000000ab')
    await d.locator('#ivLen').fill('12')
    await d.locator('#arsn').fill('0102')
    await d.locator('#arsnLen').fill('2')
    await d.locator('#arsnw').fill('4')
    await d.getByRole('button', {name: 'Continue'}).click()
    await expect(d).toBeHidden()
    await expect(toast(page, 'Updated IV on 100/46')).toBeVisible()
    await expect(toast(page, 'Updated ARSN on 100/46')).toBeVisible()
    await expect.poll(() => sadb.get('TC', 100, 46)).toMatchObject({iv: '0000000000000000000000ab', arsn: '0102', arsnw: 4})
})

test('shows the server reason when an IV reset is rejected', async ({page, sadb}) => {
    const d = await resetDialog(page)
    await d.locator('#iv').fill('00000000000000000000000000000001')
    await d.locator('#ivLen').fill('16')
    await d.getByRole('button', {name: 'Continue'}).click()
    await expect(toast(page, 'HTTP 400:ivLen of 16 is invalid for the current ECS; AES_GCM accepts only ivLen 12'))
        .toBeVisible()
    expect(await sadb.get('TC', 100, 46)).toMatchObject({iv: '000000000000000000000001', ivLen: 12})
})

test('rekeys an SA', async ({page, sadb}) => {
    await page.getByRole('button', {name: '(Re)key'}).click()
    const d = dialog(page, '(Re)key SA')
    await d.locator('#ekid').fill('kmc/test/key999')
    await d.getByRole('button', {name: 'Continue'}).click()
    await expect(d).toBeHidden()
    await expect(toast(page, 'Updated keys on 100/46')).toBeVisible()
    await expect.poll(async () => (await sadb.get('TC', 100, 46)).ekid).toBe('kmc/test/key999')
})
