import {describe, expect, it} from 'vitest'
import {render, screen, waitFor} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import SaTableAppBar from './SaTableAppBar'

describe('SaTableAppBar bulk upload dialog', () => {
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
})
