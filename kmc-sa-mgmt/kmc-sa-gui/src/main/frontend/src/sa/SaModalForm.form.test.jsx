import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {cleanup, render, screen, waitFor, within} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import SaModalForm from './SaModalForm'
import {createSa, updateSa} from './api'
import {expectHexPrefixesStyled, expectNoStrayAttributes} from '../test/mui'

const enqueueSnackbar = vi.hoisted(() => vi.fn())
vi.mock('notistack', () => ({useSnackbar: () => ({enqueueSnackbar})}))
vi.mock('./api', async (importOriginal) => ({
    ...(await importOriginal()),
    createSa: vi.fn(),
    updateSa: vi.fn()
}))

const existing = {
    id: {spi: 5, scid: 46}, spi: 5, scid: 46, vcid: 2, tfvn: 0, mapid: 0, saState: 2, lpid: null,
    serviceType: 'AUTHENTICATED_ENCRYPTION', est: 1, ast: 1, ekid: 'kmc/test/key128', akid: null,
    ecsLen: 1, ecs: '01', acsLen: 1, acs: '00', ivLen: 12, iv: '000000000000000000000001',
    shivfLen: 12, shsnfLen: 0, shplfLen: 0, stmacfLen: 16, abmLen: 19, abm: 'ffffffffffffff000000000000000000000000',
    arsnLen: 0, arsn: '', arsnw: 5
}

const renderForm = (props = {}) => render(<SaModalForm type="TC" rowOpen={true} isUpdate={false} refresh={() => {}}
                                                       onClose={() => {}} {...props}/>)

const choose = async (user, label, option) => {
    await user.click(screen.getByRole('combobox', {name: label}))
    await user.click(within(screen.getByRole('listbox')).getByRole('option', {name: option, exact: true}))
}

const fill = async (user, id, value) => {
    await user.clear(document.getElementById(id))
    await user.type(document.getElementById(id), value)
}

const submit = (user, name = 'Create') => user.click(screen.getByRole('button', {name, exact: true}))

describe('SaModalForm', () => {
    beforeEach(() => vi.spyOn(console, 'log').mockImplementation(() => {}))
    afterEach(() => {
        cleanup()
        vi.restoreAllMocks()
        vi.clearAllMocks()
    })

    it.each([
        ['PLAINTEXT', {est: 0, ast: 0}, {}],
        ['ENCRYPTION', {est: 1, ast: 0}, {ekid: 'kmc/test/key256'}],
        ['AUTHENTICATED_ENCRYPTION', {est: 1, ast: 1}, {ekid: 'kmc/test/key256'}],
    ])('creates a %s SA with matching est/ast', async (serviceType, flags, keys) => {
        const user = userEvent.setup()
        renderForm()
        await fill(user, 'spi', '7')
        await fill(user, 'scid', '46')
        await choose(user, 'Service Type', serviceType)
        if (keys.ekid) {
            await fill(user, 'ekid', keys.ekid)
        }
        await submit(user)
        await waitFor(() => expect(createSa).toHaveBeenCalledTimes(1))
        const [type, values] = createSa.mock.calls[0]
        expect(type).toBe('TC')
        expect(values).toMatchObject({serviceType, ...flags, ...keys, id: {spi: '7', scid: '46'}})
    })

    it('creates an AUTHENTICATION SA once an ACS is chosen', async () => {
        const user = userEvent.setup()
        renderForm()
        await choose(user, 'Service Type', 'AUTHENTICATION')
        await submit(user)
        expect(await screen.findByText('When Service Type is AUTHENTICATION, ACS cannot be None')).toBeTruthy()
        expect(createSa).not.toHaveBeenCalled()

        await choose(user, 'ACS', 'HMAC-SHA256')
        await submit(user)
        await waitFor(() => expect(createSa).toHaveBeenCalledTimes(1))
        expect(createSa.mock.calls[0][1]).toMatchObject({serviceType: 'AUTHENTICATION', est: 0, ast: 1, acs: '02'})
    })

    it('requires an EKID for a keyed encrypted SA', async () => {
        const user = userEvent.setup()
        renderForm()
        await choose(user, 'Service Type', 'ENCRYPTION')
        await choose(user, 'State', 'Keyed')
        await submit(user)
        expect(await screen.findByText('EKID is required when Service Type is ENCRYPTION or AUTHENTICATED_ENCRYPTION'))
            .toBeTruthy()
        expect(createSa).not.toHaveBeenCalled()
    })

    it('rejects an ECS of None for an encrypted SA', async () => {
        const user = userEvent.setup()
        renderForm()
        await choose(user, 'Service Type', 'ENCRYPTION')
        await choose(user, 'ECS', 'None')
        await submit(user)
        expect(await screen.findByText('When Service Type is ENCRYPTION or AUTHENTICATED_ENCRYPTION, ECS cannot be None'))
            .toBeTruthy()
        expect(createSa).not.toHaveBeenCalled()
    })

    it('rejects non-hex and odd-length hex values', async () => {
        const user = userEvent.setup()
        renderForm()
        await fill(user, 'iv', 'zz')
        await fill(user, 'abm', '000')
        await submit(user)
        expect(await screen.findAllByText('Must be a hex string that contains an even number of hex digits'))
            .toHaveLength(2)
        expect(createSa).not.toHaveBeenCalled()
    })

    it('enforces the frame type VCID width', async () => {
        const user = userEvent.setup()
        renderForm({type: 'TM'})
        await fill(user, 'vcid', '8')
        await submit(user)
        expect(await screen.findByText('VCID must be at most 7 for TM frames')).toBeTruthy()
        expect(createSa).not.toHaveBeenCalled()
    })

    it('skips an unchanged update', async () => {
        const user = userEvent.setup()
        const onClose = vi.fn()
        renderForm({isUpdate: true, data: existing, onClose})
        await submit(user, 'Update')
        await waitFor(() => expect(enqueueSnackbar)
            .toHaveBeenCalledWith('Submission is unchanged, skipping update', {variant: 'warning'}))
        expect(updateSa).not.toHaveBeenCalled()
        expect(onClose).toHaveBeenCalled()
    })

    it('updates a changed SA for its frame type', async () => {
        const user = userEvent.setup()
        renderForm({type: 'AOS', isUpdate: true, data: existing})
        await fill(user, 'ekid', 'kmc/test/key999')
        await submit(user, 'Update')
        await waitFor(() => expect(updateSa).toHaveBeenCalledTimes(1))
        expect(updateSa.mock.calls[0][0]).toBe('AOS')
        expect(updateSa.mock.calls[0][1]).toMatchObject({ekid: 'kmc/test/key999', id: {spi: 5, scid: 46}})
    })

    it('shows and keeps the ECS of an existing AES256-CBC SA', async () => {
        const user = userEvent.setup()
        renderForm({isUpdate: true, data: {...existing, ecs: '02', ivLen: 16, iv: '00000000000000000000000000000001'}})
        expect(screen.getByRole('combobox', {name: 'ECS'}).textContent).toBe('AES256-CBC')
        await fill(user, 'ekid', 'kmc/test/key999')
        await submit(user, 'Update')
        await waitFor(() => expect(updateSa).toHaveBeenCalledTimes(1))
        expect(updateSa.mock.calls[0][1]).toMatchObject({ecs: '02'})
    })

    it('creates an AES256-CBC SA', async () => {
        const user = userEvent.setup()
        renderForm()
        await choose(user, 'Service Type', 'ENCRYPTION')
        await choose(user, 'ECS', 'AES256-CBC')
        await submit(user)
        await waitFor(() => expect(createSa).toHaveBeenCalledTimes(1))
        expect(createSa.mock.calls[0][1]).toMatchObject({serviceType: 'ENCRYPTION', ecs: '02'})
    })

    it('labels the cipher suites by name in the helper text', () => {
        renderForm()
        expect(screen.getByText(/Encryption Cipher Suite:/).textContent)
            .toMatch(/None = 0,\s+AES256-GCM = 1,\s+AES256-CBC = 2/)
        expect(screen.getByText(/Authentication Cipher Suite:/).textContent)
            .toMatch(/None = 0,\s+AES256-CMAC = 1,\s+HMAC-SHA256 = 2,\s+HMAC-SHA512 = 3/)
    })

    it('styles the hex fields: bold warning-colored 0x prefixes and Roboto Mono inputs', () => {
        renderForm()
        const dialog = screen.getByRole('dialog')
        expectHexPrefixesStyled(dialog, 3)
        for (const id of ['iv', 'abm', 'arsn', 'ekid']) {
            expect(getComputedStyle(document.getElementById(id)).fontFamily).toBe('"Roboto Mono"')
        }
        expectNoStrayAttributes(dialog)
    })
})
