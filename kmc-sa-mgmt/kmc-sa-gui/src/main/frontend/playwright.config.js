import {defineConfig, devices} from '@playwright/test'

const port = process.env.E2E_PORT ?? '18447'
const base = `http://localhost:${port}/sa-mgmt/`

// Standalone browser suite for the SA management GUI, run by developers before a PR that touches the frontend.
// Not part of the Maven build or CI. See README.md.
export default defineConfig({
    testDir: './e2e',
    // the tests share one in-memory SADB and reset it before each test
    fullyParallel: false,
    workers: 1,
    reporter: [['list'], ['html', {open: 'never'}]],
    use: {
        ...devices['Desktop Chrome'],
        // E2E_BROWSER_CHANNEL=chrome uses the installed Google Chrome instead of Playwright's Chromium
        channel: process.env.E2E_BROWSER_CHANNEL || undefined,
        baseURL: base,
        viewport: {width: 1600, height: 1000},
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure',
    },
    webServer: {
        command: 'bash e2e/start-server.sh',
        url: `${base}api/status`,
        env: {E2E_PORT: port},
        reuseExistingServer: true,
        timeout: 300_000,
        stdout: 'ignore',
        stderr: 'pipe',
    },
})
