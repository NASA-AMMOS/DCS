import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import {configDefaults} from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
    plugins: [react()],
    base: "./",
    test: {
        environment: 'jsdom',
        setupFiles: ['./src/test/setup.js'],
        // the form and dialog tests re-render large MUI trees; CI runs them with coverage on shared runners
        testTimeout: 30000,
        // e2e/ is the Playwright suite, run with `yarn e2e`
        exclude: [...configDefaults.exclude, 'e2e/**'],
        coverage: {
            include: ['src/**/*.{js,jsx}'],
            exclude: ['src/**/*.test.*', 'src/test/**', 'src/main.jsx'],
            // lcov is imported by the Sonar scan; its paths must be relative to the kmc-sa-gui module directory
            reporter: ['text', 'html', ['lcov', {projectRoot: '../../..'}]],
        },
        onConsoleLog: (log, type) => type === 'stderr',
    },
    server: {
        port: 3001,
        proxy: {
            '/api': 'http://127.0.0.1:8447/sa-mgmt',
        }
    }
})
