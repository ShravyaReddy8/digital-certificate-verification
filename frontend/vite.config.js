import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
    allowedHosts: process.env.VITE_EXTRA_ALLOWED_HOST
      ? [process.env.VITE_EXTRA_ALLOWED_HOST]
      : undefined,
  },
});
