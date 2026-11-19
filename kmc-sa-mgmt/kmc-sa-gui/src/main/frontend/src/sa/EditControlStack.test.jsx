import {afterEach, describe, expect, it, vi} from 'vitest'
import {cleanup, render, screen, waitFor} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import EditControlStack from './EditControlStack'
import {modifyArsn, modifyIv, rekeySa} from './api'

const enqueueSnackbar = vi.hoisted(() => vi.fn())
vi.mock('notistack', () => ({useSnackbar: () => ({enqueueSnackbar})}))
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
    afterEach(() => {
        cleanup()
        vi.clearAllMocks()
    })

    it('passes the frame type as the first argument to modifyIv and modifyArsn', async () => {
        const user = userEvent.setup()
        renderStack()
        await user.click(screen.getByRole('button', {name: /IV\/ARSN/}))
        await user.type(screen.getByLabelText('IV'), '11')
        await user.type(screen.getByLabelText('ARSN'), '11')
        await user.click(screen.getByRole('button', {name: 'Continue'}))

        await waitFor(() => expect(modifyIv).toHaveBeenCalledTimes(1))
        expect(modifyIv.mock.calls[0][0]).toBe('tm')
        modifyIv.mock.calls[0][2]({})
        await waitFor(() => expect(modifyArsn).toHaveBeenCalledTimes(1))
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
    it('calls modifyArsn only after modifyIv succeeds', async () => {
        const user = userEvent.setup()
        renderStack()
        await user.click(screen.getByRole('button', {name: /IV\/ARSN/}))
        await user.type(screen.getByLabelText('IV'), '11')
        await user.type(screen.getByLabelText('ARSN'), '11')
        await user.click(screen.getByRole('button', {name: 'Continue'}))
        await waitFor(() => expect(modifyIv).toHaveBeenCalledTimes(1))
        expect(modifyArsn).not.toHaveBeenCalled()
        modifyIv.mock.calls[0][2]({})
        await waitFor(() => expect(modifyArsn).toHaveBeenCalledTimes(1))
    })
    it('does not call modifyArsn when modifyIv fails', async () => {
        const user = userEvent.setup()
        modifyIv.mockImplementationOnce((type, d, ok, err) => err(new Error('boom')))
        renderStack()
        await user.click(screen.getByRole('button', {name: /IV\/ARSN/}))
        await user.type(screen.getByLabelText('IV'), '11')
        await user.type(screen.getByLabelText('ARSN'), '11')
        await user.click(screen.getByRole('button', {name: 'Continue'}))
        await waitFor(() => expect(modifyIv).toHaveBeenCalledTimes(1))
        expect(modifyArsn).not.toHaveBeenCalled()
    })
    it('enqueues "Updated keys on" after a successful rekey', async () => {
        const user = userEvent.setup()
        rekeySa.mockImplementationOnce((type, d, ok) => ok({}))
        renderStack()
        await user.click(screen.getByRole('button', {name: /\(Re\)key/}))
        await user.type(screen.getByLabelText('EKID'), '9')
        await user.click(screen.getByRole('button', {name: 'Continue'}))
        await waitFor(() => expect(enqueueSnackbar).toHaveBeenCalledWith('Updated keys on 1/2', {variant: 'success'}))
        expect(enqueueSnackbar).not.toHaveBeenCalledWith(expect.stringMatching(/Cannot read/), expect.anything())
    })
    it('does not open the start dialog when EKID is not set for an encrypted SA', async () => {
        const user = userEvent.setup()
        const d = {...data, spi: 1, scid: 2, serviceType: 'ENCRYPTION', ekid: '', ecs: '01'}
        render(<EditControlStack type="tm" data={d} id={d.id} saState={1} defaultValues={d}
                                 refresh={() => {}} setModalOpen={() => {}}/>)
        await user.click(screen.getByRole('button', {name: 'Start'}))
        expect(enqueueSnackbar).toHaveBeenCalledWith(expect.stringMatching(/EKID or ECS are not set/))
        expect(screen.queryByText('Start SA')).toBeNull()
    })
    it.each(['00', '0x00', '0000', '0X00', 0, null])('does not start an encrypted SA when ecs is %j', async (ecs) => {
        const user = userEvent.setup()
        const d = {...data, spi: 1, scid: 2, serviceType: 'ENCRYPTION', ekid: '1', ecs}
        render(<EditControlStack type="tm" data={d} id={d.id} saState={1} defaultValues={d}
                                 refresh={() => {}} setModalOpen={() => {}}/>)
        await user.click(screen.getByRole('button', {name: 'Start'}))
        expect(enqueueSnackbar).toHaveBeenCalledWith(expect.stringMatching(/EKID or ECS are not set/))
        expect(screen.queryByText('Start SA')).toBeNull()
    })
    it.each(['00', '0x00', '0000', 0, null])('does not start an authenticated SA when acs is %j', async (acs) => {
        const user = userEvent.setup()
        const d = {...data, spi: 1, scid: 2, serviceType: 'AUTHENTICATION', akid: '1', acs}
        render(<EditControlStack type="tm" data={d} id={d.id} saState={1} defaultValues={d}
                                 refresh={() => {}} setModalOpen={() => {}}/>)
        await user.click(screen.getByRole('button', {name: 'Start'}))
        expect(enqueueSnackbar).toHaveBeenCalledWith(expect.stringMatching(/AKID or ACS are not set/))
        expect(screen.queryByText('Start SA')).toBeNull()
    })
    it('opens the start dialog when keys and suites are set', async () => {
        const user = userEvent.setup()
        const d = {...data, spi: 1, scid: 2, serviceType: 'ENCRYPTION', ekid: '1', ecs: '01'}
        render(<EditControlStack type="tm" data={d} id={d.id} saState={1} defaultValues={d}
                                 refresh={() => {}} setModalOpen={() => {}}/>)
        await user.click(screen.getByRole('button', {name: 'Start'}))
        expect(await screen.findByText('Start SA')).toBeTruthy()
    })
    it.each([
        ['ENCRYPTION', {ekid: 'kmc/test/key128', ecs: '01'}],
        ['AUTHENTICATION', {akid: 'kmc/test/hmacsha256', acs: '02'}],
        ['AUTHENTICATED_ENCRYPTION', {ekid: 'kmc/test/key128', ecs: '01', akid: null, acs: '00'}]
    ])('opens the start dialog for a %s SA with non-hex key ids', async (serviceType, keys) => {
        const user = userEvent.setup()
        const d = {...data, spi: 1, scid: 2, serviceType, ...keys}
        render(<EditControlStack type="tm" data={d} id={d.id} saState={1} defaultValues={d}
                                 refresh={() => {}} setModalOpen={() => {}}/>)
        await user.click(screen.getByRole('button', {name: 'Start'}))
        expect(enqueueSnackbar).not.toHaveBeenCalled()
        expect(await screen.findByText('Start SA')).toBeTruthy()
    })
})
