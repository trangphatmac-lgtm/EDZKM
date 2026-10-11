import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import {
  obfuscationOptions,
  trimOptions,
  saveOptions,
  ruleKeywords,
} from "./src/options";
export default defineConfig({
  plugins: [
    react(),
    {
      name: "zkm-option-catalog",
      generateBundle() {
        this.emitFile({
          type: "asset",
          fileName: "options.json",
          source: JSON.stringify({
            obfuscationOptions,
            trimOptions,
            saveOptions,
            ruleKeywords,
          }),
        });
      },
    },
  ],
  build: { chunkSizeWarningLimit: 1800 },
  server: { proxy: { "/api": "http://127.0.0.1:8270" } },
});
