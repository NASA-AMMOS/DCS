import {afterEach, describe, expect, it, vi} from 'vitest'
import axios from 'axios'
import {act, cleanup, render, screen, waitFor, within} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import SaTableAppBar from './SaTableAppBar'
import {bulkCreate, downloadCsv} from './api'
import {expectNoStrayAttributes} from '../test/mui'

vi.mock('./api', async (importOriginal) => ({
    ...(await importOriginal()),
    bulkCreate: vi.fn(),
    downloadCsv: vi.fn()
}))

describe('SaTableAppBar bulk upload dialog', () => {
    afterEach(cleanup)

    it('clears the selected file when Cancel is clicked', async () => {
        const user = userEvent.setup()
        render(<SaTableAppBar allColumns={[]} selectedFlatRows={[]} type="tc"
                              resetDefaultCols={() => {}} hideColumn={() => {}} refreshTable={() => {}}
                              enqueueSnackbar={() => {}} handleRowOpen={() => {}}/>)

        await user.click(screen.getByRole('button', {name: /bulk/i}))
        await user.upload(document.getElementById('contained-button-file'),
            new File(['a,b'], 'sas.csv', {type: 'text/csv'}))
        expect(screen.getByText('sas.csv')).toBeTruthy()

        await user.click(screen.getByRole('button', {name: 'Cancel'}))
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
        await user.click(screen.getByRole('button', {name: /bulk/i}))

        expect(screen.queryByText('sas.csv')).toBeNull()
        expect(document.getElementById('contained-button-file').files.length).toBe(0)
    })
    it('keeps the selected file after a failed upload so it can be retried', async () => {
        bulkCreate.mockImplementation((types, data, setProgress, respCallback, errCallback) =>
            errCallback({response: {status: 400, data: {messages: ['bad csv']}}}))
        const user = userEvent.setup()
        render(<SaTableAppBar allColumns={[]} selectedFlatRows={[]} type="tc"
                              resetDefaultCols={() => {}} hideColumn={() => {}} refreshTable={() => {}}
                              enqueueSnackbar={() => {}} handleRowOpen={() => {}}/>)

        await user.click(screen.getByRole('button', {name: /bulk/i}))
        await user.upload(document.getElementById('contained-button-file'),
            new File(['a,b'], 'sas.csv', {type: 'text/csv'}))
        await user.click(screen.getByRole('button', {name: 'Continue'}))

        expect(bulkCreate).toHaveBeenCalledTimes(1)
        expect(screen.getByText('sas.csv')).toBeTruthy()
        expect(screen.getByRole('button', {name: 'Continue'}).disabled).toBe(false)
    })

    it('restricts the file picker to CSV and renders no stray MUI props', async () => {
        const user = userEvent.setup()
        render(<SaTableAppBar allColumns={[]} selectedFlatRows={[]} type="tc"
                              resetDefaultCols={() => {}} hideColumn={() => {}} refreshTable={() => {}}
                              enqueueSnackbar={() => {}} handleRowOpen={() => {}}/>)
        await user.click(screen.getByRole('button', {name: /bulk/i}))
        const input = document.getElementById('contained-button-file')
        expect(input.getAttribute('accept')).toBe('.csv')
        expect(input.parentElement.hasAttribute('accept')).toBe(false)
        expectNoStrayAttributes(document.body)
    })
    describe('cancelling an upload', () => {
        const setup = async (props = {}) => {
            const user = userEvent.setup()
            const refreshTable = vi.fn()
            const enqueueSnackbar = vi.fn()
            render(<SaTableAppBar allColumns={[]} selectedFlatRows={[]} type="tc"
                                  resetDefaultCols={() => {}} hideColumn={() => {}} refreshTable={refreshTable}
                                  enqueueSnackbar={enqueueSnackbar} handleRowOpen={() => {}} {...props}/>)
            await user.click(screen.getByRole('button', {name: /bulk/i}))
            await user.upload(document.getElementById('contained-button-file'),
                new File(['a,b'], 'sas.csv', {type: 'text/csv'}))
            await user.click(screen.getByRole('button', {name: 'Continue'}))
            return {user, refreshTable, enqueueSnackbar}
        }
        // bulkCreate mock that stays pending until the test settles it
        const pending = () => {
            const call = {}
            bulkCreate.mockImplementation((types, data, setProgress, respCallback, errCallback, signal) =>
                Object.assign(call, {setProgress, respCallback, errCallback, signal}))
            return call
        }

        it('aborts the request and silently closes the dialog when canceled mid-transfer', async () => {
            bulkCreate.mockReset()
            const call = pending()
            const {user, refreshTable, enqueueSnackbar} = await setup()
            act(() => call.setProgress(40))

            await user.click(screen.getByRole('button', {name: 'Cancel'}))
            expect(call.signal.aborted).toBe(true)
            await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())

            // axios rejects the aborted request; this must be a no-op
            const cancelErr = new axios.CanceledError('canceled')
            act(() => call.errCallback(cancelErr))
            act(() => call.setProgress(60))
            expect(enqueueSnackbar).not.toHaveBeenCalled()
            expect(refreshTable).not.toHaveBeenCalled()
        })
        it('ignores Cancel and ESC once the upload has fully transferred', async () => {
            bulkCreate.mockReset()
            const call = pending()
            const {user, refreshTable} = await setup()
            act(() => call.setProgress(100))

            expect(screen.getByText('Processing…')).toBeTruthy()
            // text.secondary of MUI's default light palette
            expect(getComputedStyle(screen.getByText('Processing…')).color).toBe('rgba(0, 0, 0, 0.6)')
            expect(screen.getByRole('button', {name: 'Cancel'}).disabled).toBe(true)
            await user.keyboard('{Escape}')
            expect(screen.getByRole('dialog')).toBeTruthy()
            expect(call.signal.aborted).toBe(false)

            act(() => call.respCallback({status: 200}))
            await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
            expect(refreshTable).toHaveBeenCalledTimes(1)
        })
        it('completes a normal upload', async () => {
            bulkCreate.mockReset()
            const call = pending()
            const {enqueueSnackbar, refreshTable} = await setup()
            act(() => call.setProgress(100))
            act(() => call.respCallback({status: 200}))

            await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
            expect(enqueueSnackbar).toHaveBeenCalledWith('Uploaded CSV', {variant: 'success'})
            expect(refreshTable).toHaveBeenCalledTimes(1)
        })
    })
})

// ports of kmc-test SA_Management_Browser.robot checks that do not need a browser
describe('SaTableAppBar toolbar', () => {
    afterEach(() => {
        cleanup()
        vi.restoreAllMocks()
    })

    const columns = [{id: 'selection'}, {id: 'spi', Header: 'SPI'}, {id: 'ivLen', Header: 'IV Length'},
        {id: 'abmLen', Header: 'ABM Length'}].map(c => ({...c, getToggleHiddenProps: () => ({checked: c.id === 'spi', onChange: () => {}})}))
    const renderBar = (props = {}) => {
        const handlers = {resetDefaultCols: vi.fn(), hideColumn: vi.fn(), refreshTable: vi.fn(), enqueueSnackbar: vi.fn(),
            handleRowOpen: vi.fn()}
        render(<SaTableAppBar allColumns={columns} selectedFlatRows={[]} type="TC" {...handlers} {...props}/>)
        return handlers
    }

    it('shows the table controls, with DELETE disabled until rows are selected', () => {
        renderBar()
        for (const name of ['Columns', 'DELETE', 'Create', 'Bulk', 'Export', 'Reload']) {
            expect(screen.getByRole('button', {name})).toBeTruthy()
        }
        expect(screen.getByRole('button', {name: 'DELETE'}).disabled).toBe(true)
    })

    it('lists every column except selection in the Columns chooser, with Reset', async () => {
        const user = userEvent.setup()
        const {resetDefaultCols, hideColumn} = renderBar()
        await user.click(screen.getByRole('button', {name: 'Columns'}))
        const chooser = document.getElementById('colSel')
        expect(within(chooser).getAllByRole('checkbox').map(c => c.parentElement.textContent.trim()))
            .toEqual(['SPI', 'IV Length', 'ABM Length'])
        await user.click(within(chooser).getByLabelText('IV Length'))
        expect(hideColumn).toHaveBeenCalledWith('ivLen', true)
        await user.click(within(chooser).getByRole('button', {name: 'Reset'}))
        expect(resetDefaultCols).toHaveBeenCalledTimes(1)
    })

    it('reloads the table', async () => {
        const user = userEvent.setup()
        const {refreshTable} = renderBar()
        await user.click(screen.getByRole('button', {name: 'Reload'}))
        expect(refreshTable).toHaveBeenCalledTimes(1)
    })

    it('downloads the CSV under the server-suggested name and confirms it', async () => {
        const user = userEvent.setup()
        downloadCsv.mockImplementation((ok) => ok({data: 'a,b', headers: {'x-suggested-filename': 'SADB_x.csv'}}))
        window.URL.createObjectURL = vi.fn(() => 'blob:sadb')
        const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function () {
            this.dataset.clicked = this.getAttribute('download')
        })
        const {enqueueSnackbar} = renderBar()
        await user.click(screen.getByRole('button', {name: 'Export'}))
        expect(click).toHaveBeenCalledTimes(1)
        expect(click.mock.contexts[0].getAttribute('download')).toBe('SADB_x.csv')
        expect(click.mock.contexts[0].href).toBe('blob:sadb')
        expect(enqueueSnackbar).toHaveBeenCalledWith('Downloaded CSV', {variant: 'success'})
    })

    it('reports a failed CSV download', async () => {
        const user = userEvent.setup()
        downloadCsv.mockImplementation((ok, err) => err(new Error('boom')))
        const {enqueueSnackbar} = renderBar()
        await user.click(screen.getByRole('button', {name: 'Export'}))
        expect(enqueueSnackbar).toHaveBeenCalledWith('Error downloading CSV', {variant: 'error'})
    })

    it('closes the Bulk dialog when clicking outside it', async () => {
        const user = userEvent.setup()
        renderBar()
        await user.click(screen.getByRole('button', {name: 'Bulk'}))
        expect(screen.getByText(/Bulk creation of SAs by uploading a CSV file/)).toBeTruthy()
        await user.click(document.querySelector('.MuiBackdrop-root'))
        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    })
})
