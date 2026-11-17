import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  // The Android bundle has no dev proxy, so the API URL must be baked in at build
  // time. Fail the build instead of shipping a bundle that silently calls localhost.
  if (mode === 'android') {
    const env = { ...loadEnv(mode, process.cwd(), 'VITE_'), ...process.env };
    const apiUrl = env.VITE_API_BASE_URL;
    if (!apiUrl) {
      throw new Error(
        'VITE_API_BASE_URL is required for Android builds. Copy .env.android.example to ' +
          '.env.android and set the backend URL reachable from the phone.',
      );
    }
    if (/^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?(\/|$)/i.test(apiUrl)) {
      throw new Error(
        `VITE_API_BASE_URL=${apiUrl} is not reachable from a phone. Use the backend's ` +
          'LAN address (http://<LAN-IP>:8080), the emulator alias http://10.0.2.2:8080, ' +
          'or a deployed HTTPS URL.',
      );
    }
  }

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
    },
  };
});
