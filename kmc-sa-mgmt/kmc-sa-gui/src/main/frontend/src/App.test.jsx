import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {act, cleanup, render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from './App'
import {listSa, status} from './sa/api'

vi.mock('./sa/api', async (importOriginal) => ({
    ...(await importOriginal()),
    status: vi.fn(),
    listSa: vi.fn((type, ok) => ok({status: 200, data: []}))
}))

const dot = () => document.querySelector('[data-testid="CircleIcon"]')

describe('App', () => {
    beforeEach(() => localStorage.clear())
    afterEach(() => {
        cleanup()
        vi.useRealTimers()
        vi.clearAllMocks()
    })

    it('shows the database status and polls it every 5 seconds', async () => {
        vi.useFakeTimers({shouldAdvanceTime: true})
        status.mockImplementation((ok) => ok({status: 200}))
        render(<App/>)
        expect(dot().getAttribute('class')).toMatch(/colorSuccess/)
        expect(status).toHaveBeenCalledTimes(1)

        status.mockImplementation((ok, err) => err(new Error('down')))
        await act(() => vi.advanceTimersByTimeAsync(5000))
        expect(status).toHaveBeenCalledTimes(2)
        expect(dot().getAttribute('class')).toMatch(/colorError/)
    })

    it('lists TC SAs on load', () => {
        status.mockImplementation((ok) => ok({status: 200}))
        render(<App/>)
        expect(listSa.mock.calls[0][0]).toBe('TC')
        expect(screen.getByRole('tab', {name: 'TC', selected: true})).toBeTruthy()
    })

    it('starts in the dark theme and remembers a switch to light', async () => {
        const user = userEvent.setup()
        status.mockImplementation((ok) => ok({status: 200}))
        const {unmount} = render(<App/>)
        const background = () => getComputedStyle(document.body).backgroundColor
        expect(background()).toBe('rgb(18, 18, 18)')
        await user.click(screen.getByTestId('Brightness7Icon').closest('button'))
        expect(background()).toBe('rgb(255, 255, 255)')
        expect(localStorage.getItem('theme')).toBe('false')
        unmount()
        render(<App/>)
        expect(background()).toBe('rgb(255, 255, 255)')
    })
})
