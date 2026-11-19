import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {cleanup, render, screen, waitFor, within} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import SaTable from './SaTable'
import {listSa} from './api'

const enqueueSnackbar = vi.hoisted(() => vi.fn())
vi.mock('notistack', () => ({useSnackbar: () => ({enqueueSnackbar})}))
vi.mock('./api', async (importOriginal) => ({
    ...(await importOriginal()),
    listSa: vi.fn()
}))

const sa = (spi, overrides = {}) => ({
    id: {spi, scid: 46}, spi, scid: 46, vcid: 2, tfvn: 0, mapid: 0, saState: 1, lpid: null,
    serviceType: 'AUTHENTICATED_ENCRYPTION', est: 1, ast: 1, ekid: 'kmc/test/key128', akid: null,
    ecsLen: 1, ecs: '01', acsLen: 1, acs: '00', ivLen: 12, iv: '000000000000000000000001',
    shivfLen: 12, shsnfLen: 0, shplfLen: 0, stmacfLen: 16, abmLen: 19, abm: 'ffffffffffffff000000000000000000000000',
    arsnLen: 0, arsn: '', arsnw: 5, ...overrides
})

// a fresh array per call, as a real fetch returns
const serve = (...sas) => listSa.mockImplementation((type, ok) => ok({status: 200, data: sas.map(s => ({...s}))}))

const rowFor = (spi) => {
    // body rows carry role="checkbox" (row selection), so find them structurally
    const row = [...document.querySelectorAll('tbody tr')].find(r =>
        [...r.querySelectorAll('td')].some(c => c.textContent === String(spi)))
    if (!row) {
        throw new Error(`no row for SPI ${spi}`)
    }
    return row
}

describe('SaTable', () => {
    beforeEach(() => {
        localStorage.clear()
        vi.spyOn(console, 'log').mockImplementation(() => {})
    })
    afterEach(() => {
        cleanup()
        vi.restoreAllMocks()
        vi.clearAllMocks()
    })

    it('lists the SAs of its frame type with readable state and cipher suites', async () => {
        serve(sa(100), sa(101, {ecs: '02', saState: 3, serviceType: 'ENCRYPTION', ast: 0}))
        render(<SaTable type="TM"/>)
        expect(listSa.mock.calls[0][0]).toBe('TM')
        const first = await waitFor(() => rowFor(100))
        expect(within(first).getByText('Unkeyed | Expired')).toBeTruthy()
        expect(within(first).getByText('AES256-GCM')).toBeTruthy()
        expect(within(first).getByText('None')).toBeTruthy()
        expect(within(first).getByText('kmc/test/key128')).toBeTruthy()
        const second = rowFor(101)
        expect(within(second).getByText('Operational')).toBeTruthy()
        expect(within(second).getByText('AES256-CBC')).toBeTruthy()
    })

    it('hides the low-level columns by default', async () => {
        serve(sa(100))
        render(<SaTable type="TC"/>)
        await waitFor(() => rowFor(100))
        const headers = screen.getAllByRole('columnheader').map(h => h.textContent)
        for (const shown of ['SPI', 'SCID', 'VCID', 'State', 'Service Type', 'EKID', 'AKID', 'ECS', 'ACS']) {
            expect(headers).toContain(shown)
        }
        for (const hidden of ['IV', 'IV Length', 'ABM', 'ARSN', 'ARSNW']) {
            expect(headers).not.toContain(hidden)
        }
    })

    it('opens the clicked SA for editing', async () => {
        const user = userEvent.setup()
        serve(sa(100), sa(101, {ekid: 'kmc/test/key256'}))
        render(<SaTable type="TC"/>)
        await user.click(within(await waitFor(() => rowFor(101))).getByText('kmc/test/key256'))
        expect(await screen.findByText('Edit SA')).toBeTruthy()
        expect(document.getElementById('spi').value).toBe('101')
        expect(document.getElementById('ekid').value).toBe('kmc/test/key256')
    })

    it('enables DELETE once a row is selected', async () => {
        const user = userEvent.setup()
        serve(sa(100), sa(101))
        render(<SaTable type="TC"/>)
        const row = await waitFor(() => rowFor(101))
        const del = screen.getByRole('button', {name: 'DELETE'})
        expect(del.disabled).toBe(true)
        await user.click(row.querySelector('input[type=checkbox]'))
        expect(del.disabled).toBe(false)
        expect(screen.queryByText('Edit SA')).toBeNull()
    })

    it('reports a failed listing in a toast', async () => {
        listSa.mockImplementation((type, ok, err) => err({response: {status: 500, data: {messages: ['database down']}}}))
        render(<SaTable type="TC"/>)
        await waitFor(() => expect(enqueueSnackbar).toHaveBeenCalledWith('Error retrieving from the SADB: database down',
            {variant: 'error', preventDuplicate: true}))
    })

    const filter = async (user, header, value) => {
        const th = screen.getAllByRole('columnheader').find(h => h.textContent.startsWith(header))
        await user.click(within(th).getAllByRole('button')[0])
        await user.type(screen.getAllByRole('textbox').at(-1), value)
        await user.keyboard('{Escape}')
    }
    const spis = () => [...document.querySelectorAll('tbody tr')].map(r => r.querySelectorAll('td')[1].textContent)

    it('filters by SPI, VCID and AKID', async () => {
        const user = userEvent.setup()
        serve(sa(22), sa(23, {vcid: 20}), sa(24, {akid: 'kmc/test/KEY130'}), sa(25, {akid: 'kmc/test/NIST_CMAC_90'}))
        render(<SaTable type="TC"/>)
        await waitFor(() => expect(spis()).toEqual(['22', '23', '24', '25']))
        await filter(user, 'SPI', '22')
        expect(spis()).toEqual(['22'])
        await user.click(screen.getByRole('button', {name: 'Reload'}))
        await waitFor(() => expect(spis()).toHaveLength(4))
        await filter(user, 'VCID', '20')
        expect(spis()).toEqual(['23'])
        await user.click(screen.getByRole('button', {name: 'Reload'}))
        await waitFor(() => expect(spis()).toHaveLength(4))
        await filter(user, 'AKID', 'KEY130')
        expect(spis()).toEqual(['24'])
    })

    it('Reload refetches and clears the filters', async () => {
        const user = userEvent.setup()
        serve(sa(22), sa(23))
        render(<SaTable type="TC"/>)
        await waitFor(() => expect(spis()).toHaveLength(2))
        await filter(user, 'SPI', '22')
        expect(spis()).toEqual(['22'])
        serve(sa(22), sa(23), sa(24))
        await user.click(screen.getByRole('button', {name: 'Reload'}))
        await waitFor(() => expect(spis()).toEqual(['22', '23', '24']))
        expect(listSa).toHaveBeenCalledTimes(2)
    })

    it('shows a column chosen in the Columns chooser, remembers it, and Reset hides it again', async () => {
        const user = userEvent.setup()
        serve(sa(22))
        const {unmount} = render(<SaTable type="TC"/>)
        await waitFor(() => rowFor(22))
        const headers = () => screen.getAllByRole('columnheader').map(h => h.textContent)
        expect(headers()).not.toContain('IV Length')
        await user.click(screen.getByRole('button', {name: 'Columns'}))
        await user.click(within(document.getElementById('colSel')).getByLabelText('IV Length'))
        await user.keyboard('{Escape}')
        expect(headers()).toContain('IV Length')
        expect(JSON.parse(localStorage.getItem('hiddenCols'))).not.toContain('ivLen')

        unmount()
        render(<SaTable type="TC"/>)
        await waitFor(() => rowFor(22))
        expect(headers()).toContain('IV Length')
        await user.click(screen.getByRole('button', {name: 'Columns'}))
        await user.click(within(document.getElementById('colSel')).getByRole('button', {name: 'Reset'}))
        await user.keyboard('{Escape}')
        expect(headers()).not.toContain('IV Length')
        expect(JSON.parse(localStorage.getItem('hiddenCols'))).toContain('ivLen')
    })

    const selectFilter = async (user, header) => {
        const th = screen.getAllByRole('columnheader').find(h => h.textContent.startsWith(header))
        await user.click(within(th).getAllByRole('button')[0])
        await user.click(screen.getAllByRole('combobox').at(-1))
        return within(screen.getByRole('listbox'))
    }

    it.each([
        ['State', ['All', 'Unkeyed | Expired', 'Keyed', 'Operational'], 'Keyed', ['23']],
        // ordered by service type number
        ['Service Type', ['All', 'PLAINTEXT', 'ENCRYPTION', 'AUTHENTICATED_ENCRYPTION'], 'ENCRYPTION', ['23']],
        ['ECS', ['All', 'None', 'AES256-GCM', 'AES256-CBC'], 'AES256-CBC', ['23']],
        ['ACS', ['All', 'None', 'HMAC-SHA256'], 'HMAC-SHA256', ['24']],
    ])('the %s filter lists readable options and filters on the chosen one', async (header, options, choice, expected) => {
        const user = userEvent.setup()
        serve(sa(22, {saState: 1}),
            sa(23, {saState: 2, serviceType: 'ENCRYPTION', ast: 0, ecs: '02', ivLen: 16}),
            sa(24, {saState: 3, serviceType: 'PLAINTEXT', est: 0, ast: 0, ekid: null, ecs: '00', acs: '02'}))
        render(<SaTable type="TC"/>)
        await waitFor(() => expect(spis()).toHaveLength(3))
        const listbox = await selectFilter(user, header)
        expect(listbox.getAllByRole('option').map(o => o.textContent)).toEqual(options)
        await user.click(listbox.getByRole('option', {name: choice, exact: true}))
        await user.keyboard('{Escape}')
        expect(spis()).toEqual(expected)
    })
})
