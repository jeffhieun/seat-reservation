import { defineConfig } from "vite";

export default defineConfig({
  esbuild: {
    jsxInject: 'import React from "react"',
  },
  server: {
    port: 5173,
  },
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/setupTests.js",
    globals: true,
  },
});
