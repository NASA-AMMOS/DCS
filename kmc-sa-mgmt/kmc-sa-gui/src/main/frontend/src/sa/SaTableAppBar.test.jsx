import {afterEach, describe, expect, it, vi} from 'vitest'
import {cleanup, render, screen, waitFor} from '@testing-library/react'
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
})
