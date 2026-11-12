import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
    plugins: [react()],
    base: "./",
    test: {
        environment: 'jsdom',
    },
    server: {
        port: 3001,
        proxy: {
            '/api': 'http://127.0.0.1:8447/sa-mgmt',
        }
    }
})
