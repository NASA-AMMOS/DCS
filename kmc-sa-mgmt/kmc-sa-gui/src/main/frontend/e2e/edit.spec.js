import {dialog, expect, openSa, sa, test, toast} from './fixtures'

// Covers the cases listed (commented out) in kmc-test's SA_Management_Browser_Update.robot.

test.beforeEach(async ({page, sadb}) => {
    await sadb.create(sa(100, 46))
    await page.goto('./')
    await openSa(page, 100)
})

test('updates an SA from the edit form', async ({page, sadb}) => {
    const form = dialog(page, 'Edit SA')
    await form.locator('#ekid').fill('kmc/test/KEY131')
    await form.locator('#shivfLen').fill('16')
    await form.locator('#abm').fill('ffffffffffffff0000000000000000000000ff')
    await form.getByLabel('Service Type').click()
    await page.getByRole('option', {name: 'ENCRYPTION', exact: true}).click()
    await form.getByRole('button', {name: 'Update', exact: true}).click()
    await expect(toast(page, 'Updated SA 100/46')).toBeVisible()
    await expect.poll(() => sadb.get('TC', 100, 46)).toMatchObject({
        ekid: 'kmc/test/KEY131', shivfLen: 16, abm: 'ffffffffffffff0000000000000000000000ff',
        serviceType: 'ENCRYPTION', est: 1, ast: 0})
})

// a state change in the form is applied as start/stop/expire, so the server's transition rules apply
test('rejects an invalid state change with the server reason and keeps the SA unchanged', async ({page, sadb}) => {
    const form = dialog(page, 'Edit SA')
    await form.locator('#ekid').fill('kmc/test/KEY131')
    await form.getByLabel('State').click()
    await page.getByRole('option', {name: 'Keyed', exact: true}).click()
    await form.getByRole('button', {name: 'Update', exact: true}).click()
    await expect(toast(page, 'HTTP 400:SA TC [100, 46] is not operational, cannot stop')).toBeVisible()
    expect(await sadb.get('TC', 100, 46)).toMatchObject({ekid: 'kmc/test/key128', saState: 1})
})

test('skips an update when nothing changed', async ({page, sadb}) => {
    await dialog(page, 'Edit SA').getByRole('button', {name: 'Update', exact: true}).click()
    await expect(toast(page, 'Submission is unchanged, skipping update')).toBeVisible()
})

test('deletes an SA from the edit dialog', async ({page, sadb}) => {
    await page.getByRole('button', {name: 'Delete', exact: true}).click()
    const confirm = dialog(page, 'Confirm Deletion')
    await expect(confirm).toContainText('This action will permanently delete SA 100/46')
    await confirm.getByRole('button', {name: 'Continue'}).click()
    await expect(toast(page, 'SADB 100/46 has been deleted')).toBeVisible()
    await expect(page.getByRole('cell', {name: '100', exact: true})).toHaveCount(0)
    expect(await sadb.get('TC', 100, 46)).toBeNull()
})

// AKMC-211: after the Reset IV and/or ARSN dialog, the Edit SA window showed corrupted IV/ARSN values
test('shows the new IV and ARSN in the edit form after a reset', async ({page}) => {
    await page.getByRole('button', {name: 'IV/ARSN'}).click()
    const reset = dialog(page, 'Reset IV and/or ARSN')
    await reset.locator('#iv').fill('0000000000000000000000ab')
    await reset.locator('#ivLen').fill('12')
    await reset.locator('#arsn').fill('0102')
    await reset.locator('#arsnLen').fill('2')
    await reset.getByRole('button', {name: 'Continue'}).click()
    await expect(toast(page, 'Updated ARSN on 100/46')).toBeVisible()
    const form = dialog(page, 'Edit SA')
    await expect(form.locator('#iv')).toHaveValue('0000000000000000000000ab')
    await expect(form.locator('#arsn')).toHaveValue('0102')
    await expect(form.locator('#arsnLen')).toHaveValue('2')
})
