import {afterEach, expect, it, vi} from 'vitest'
import {cleanup, render, screen} from '@testing-library/react'
import ErrorBoundary from './ErrorBoundary'

afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
})

const Boom = () => {
    throw new Error('boom')
}

it('renders its children when they do not throw', () => {
    render(<ErrorBoundary><p>fine</p></ErrorBoundary>)
    expect(screen.getByText('fine')).toBeTruthy()
})

it('replaces a crashing child with a message instead of blanking the page', () => {
    // React reports the caught error through console.error; that is expected here
    vi.spyOn(console, 'error').mockImplementation(() => {})
    vi.spyOn(console, 'log').mockImplementation(() => {})
    render(<div><ErrorBoundary><Boom/></ErrorBoundary><p>rest of the page</p></div>)
    expect(screen.getByText('Something went wrong')).toBeTruthy()
    expect(screen.getByText('rest of the page')).toBeTruthy()
})
