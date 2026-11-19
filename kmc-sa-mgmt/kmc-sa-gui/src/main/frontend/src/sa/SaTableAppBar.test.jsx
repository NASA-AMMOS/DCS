import {afterEach, describe, expect, it, vi} from 'vitest'
import axios from 'axios'
import {act, cleanup, render, screen, waitFor} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import SaTableAppBar from './SaTableAppBar'
import {bulkCreate} from './api'

vi.mock('./api', async (importOriginal) => ({
    ...(await importOriginal()),
    bulkCreate: vi.fn()
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
