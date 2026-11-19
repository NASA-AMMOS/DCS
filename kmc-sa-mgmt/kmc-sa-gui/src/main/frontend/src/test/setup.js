import {format} from 'node:util'
import {afterEach, beforeEach} from 'vitest'

// Node 25+ has its own global localStorage, undefined unless node runs with --localstorage-file, and its presence keeps
// vitest from installing jsdom's. The app reads localStorage directly, so use jsdom's.
if (globalThis.localStorage === undefined && globalThis.jsdom) {
    Object.defineProperty(globalThis, 'localStorage', {value: globalThis.jsdom.window.localStorage, configurable: true})
}

// Fail any test that logs through console.error or console.warn. React and MUI report problems such as unknown
// or removed props this way (for example "React does not recognize the `inputProps` prop"), and nothing else in the
// app uses these, so a warning in a test is a bug or a test that needs updating. The message is still printed.
const original = {error: console.error, warn: console.warn}
let logged = []

beforeEach(() => {
    logged = []
    for (const level of ['error', 'warn']) {
        console[level] = (...args) => {
            logged.push(`console.${level}: ${format(...args)}`)
            original[level](...args)
        }
    }
})

afterEach(() => {
    console.error = original.error
    console.warn = original.warn
    if (logged.length) {
        throw new Error(`Unexpected console output during the test:\n${logged.join('\n').slice(0, 2000)}`)
    }
})
