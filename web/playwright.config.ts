import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./tests",
  timeout: 120000,
  fullyParallel: false,
  workers: 1,
  use: {
    viewport: { width: 1440, height: 1000 },
    locale: "en-US",
    colorScheme: "dark",
    headless: true,
    actionTimeout: 15000,
  },
  reporter: "list",
});
