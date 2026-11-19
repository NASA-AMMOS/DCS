import {dialog, expect, openSa, sa, test, toast} from './fixtures'

const confirm = async (page, title) => {
    const d = dialog(page, title)
    await d.getByRole('button', {name: 'Continue'}).click()
    await expect(d).toBeHidden()
}

test('starts, stops and expires an SA with a non-hex key ID', async ({page, sadb}) => {
    await sadb.create(sa(100, 46))
    await page.goto('./')
    await openSa(page, 100)
    const state = async () => (await sadb.get('TC', 100, 46)).saState

    await page.getByRole('button', {name: 'Start', exact: true}).click()
    await confirm(page, 'Start SA')
    await expect(toast(page, 'Started SA 100/46')).toBeVisible()
    await expect.poll(state).toBe(3)

    await page.getByRole('button', {name: 'Stop', exact: true}).click()
    await confirm(page, 'Stop SA')
    await expect(toast(page, 'Stopped SA 100/46')).toBeVisible()
    await expect.poll(state).toBe(2)

    await page.getByRole('button', {name: 'Start', exact: true}).click()
    await confirm(page, 'Start SA')
    await expect.poll(state).toBe(3)
    await page.getByRole('button', {name: 'Expire', exact: true}).click()
    await confirm(page, 'Expire SA')
    await expect(toast(page, 'Expired SA 100/46')).toBeVisible()
    await expect.poll(state).toBe(1)
})

test('refuses to start an encrypted SA without an EKID', async ({page, sadb}) => {
    await sadb.create(sa(100, 46, {ekid: null}))
    await page.goto('./')
    await openSa(page, 100)
    await page.getByRole('button', {name: 'Start', exact: true}).click()
    await expect(toast(page, 'Unable to start 100/46, service type is AUTHENTICATED_ENCRYPTION but EKID or ECS are not set.'))
        .toBeVisible()
    await expect(dialog(page, 'Start SA').getByRole('button', {name: 'Continue'})).toBeHidden()
    expect((await sadb.get('TC', 100, 46)).saState).toBe(1)
})
