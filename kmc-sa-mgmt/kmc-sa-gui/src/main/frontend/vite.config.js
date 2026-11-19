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
        // e2e/ is the Playwright suite, run with `yarn e2e`
        exclude: [...configDefaults.exclude, 'e2e/**'],
        onConsoleLog: (log, type) => type === 'stderr',
    },
    server: {
        port: 3001,
        proxy: {
            '/api': 'http://127.0.0.1:8447/sa-mgmt',
        }
    }
})
