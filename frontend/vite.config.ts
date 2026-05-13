import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  base: '/',
  server: {
    proxy: {
      '/api': 'http://127.0.0.1:9527',
      '/actuator': 'http://127.0.0.1:9527'
    }
  },
  build: {
    outDir: '../src/main/resources/dist-next',
    emptyOutDir: true,
    sourcemap: false
  }
});
