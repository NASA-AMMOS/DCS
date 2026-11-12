import {afterEach, describe, expect, it, vi} from 'vitest'
import {cleanup, render, screen, waitFor} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import EditControlStack from './EditControlStack'
import {modifyArsn, modifyIv, rekeySa} from './api'

vi.mock('notistack', () => ({useSnackbar: () => ({enqueueSnackbar: vi.fn()})}))
vi.mock('./api', async (importOriginal) => ({
    ...(await importOriginal()),
    modifyIv: vi.fn(),
    modifyArsn: vi.fn(),
    rekeySa: vi.fn()
}))

const data = {
    id: {spi: 1, scid: 2}, iv: '00', ivLen: 1, arsn: '00', arsnLen: 1, arsnw: 0, ekid: '1', akid: '2'
}

const renderStack = () => render(<EditControlStack type="tm" data={data} id={data.id} saState={1}
                                                   defaultValues={data} refresh={() => {}}
                                                   setModalOpen={() => {}}/>)

describe('EditControlStack', () => {
    afterEach(cleanup)

    it('passes the frame type as the first argument to modifyIv and modifyArsn', async () => {
        const user = userEvent.setup()
        renderStack()
        await user.click(screen.getByRole('button', {name: /IV\/ARSN/}))
        await user.type(screen.getByLabelText('IV'), '11')
        await user.type(screen.getByLabelText('ARSN'), '11')
        await user.click(screen.getByRole('button', {name: 'Continue'}))

        await waitFor(() => expect(modifyIv).toHaveBeenCalledTimes(1))
        expect(modifyIv.mock.calls[0][0]).toBe('tm')
        expect(modifyArsn).toHaveBeenCalledTimes(1)
        expect(modifyArsn.mock.calls[0][0]).toBe('tm')
    })
    it('passes the frame type as the first argument to rekeySa', async () => {
        const user = userEvent.setup()
        renderStack()
        await user.click(screen.getByRole('button', {name: /\(Re\)key/}))
        await user.type(screen.getByLabelText('EKID'), '9')
        await user.click(screen.getByRole('button', {name: 'Continue'}))

        await waitFor(() => expect(rekeySa).toHaveBeenCalledTimes(1))
        expect(rekeySa.mock.calls[0][0]).toBe('tm')
    })
    it('does not throw from error handlers on a network error without a response', async () => {
        const user = userEvent.setup()
        modifyIv.mockImplementationOnce((type, d, ok, err) => err(new Error('Network Error')))
        renderStack()
        await user.click(screen.getByRole('button', {name: /IV\/ARSN/}))
        await user.type(screen.getByLabelText('IV'), '11')
        await user.click(screen.getByRole('button', {name: 'Continue'}))
        await waitFor(() => expect(modifyIv).toHaveBeenCalled())
    })
})
