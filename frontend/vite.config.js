import { defineConfig } from "vite";

export default defineConfig({
  esbuild: {
    jsxInject: 'import React from "react"',
  },
  server: {
    port: 5173,
  },
});

