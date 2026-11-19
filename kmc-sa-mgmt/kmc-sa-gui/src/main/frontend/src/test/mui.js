import {expect} from 'vitest'

// MUI's default (light) palette warning.main, used when a component renders without a ThemeProvider
export const WARNING_MAIN = 'rgb(237, 108, 2)'

/** The leaf elements rendering a "0x" hex prefix inside a container. */
export const hexPrefixes = (container) =>
    [...container.querySelectorAll('p,span')].filter(e => e.textContent === '0x' && e.children.length === 0)

/** The 0x prefixes render bold, in Roboto Mono and in the warning color (MUI 9 ignores these as Typography props). */
export const expectHexPrefixesStyled = (container, count) => {
    const styles = hexPrefixes(container).map(e => {
        const c = getComputedStyle(e)
        return {weight: c.fontWeight, family: c.fontFamily, color: c.color}
    })
    expect(styles).toEqual(Array(count).fill({weight: '700', family: '"Roboto Mono"', color: WARNING_MAIN}))
}

// props that MUI no longer accepts (or never accepted) and that end up as plain DOM attributes when passed anyway
const STRAY = ['edge', 'font-weight', 'font-family', 'fontweight', 'fontfamily', 'inputprops', 'selectprops']

/** No element carries a removed MUI prop as a DOM attribute. */
export const expectNoStrayAttributes = (container) => {
    const found = [...container.querySelectorAll('*')].flatMap(e =>
        STRAY.filter(a => e.hasAttribute(a)).map(a => `<${e.tagName.toLowerCase()} ${a}>`))
    expect(found).toEqual([])
}
