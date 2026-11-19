# React + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react/README.md) uses [Babel](https://babeljs.io/) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

## Expanding the ESLint configuration

If you are developing a production application, we recommend using TypeScript and enable type-aware lint rules. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) to integrate TypeScript and [`typescript-eslint`](https://typescript-eslint.io) in your project.

## Tests

There are two test suites. Run both before opening a PR that changes the frontend, and update them with the change.

### Unit tests (vitest)

```
yarn test
```

Component and helper tests in `src/**/*.test.{js,jsx}`, run in jsdom. These also run in the Maven build.

- A test fails if anything logs through `console.error` or `console.warn` (`src/test/setup.js`). That is how React
  and MUI report unknown or removed props, out-of-range select values and spread keys, which otherwise render
  silently wrong. Fix the warning rather than silencing it.
- `api.requests.test.js` checks every `api.js` call against the routes declared in `SaController.java`, so update
  both sides together.
- `src/test/mui.js` has helpers for the styling MUI does not check for you (hex prefixes, stray DOM attributes).
- Prefer realistic data in tests, e.g. key IDs like `kmc/test/key128`; `'1'` hid a bug because it is valid hex.

### Browser tests (Playwright)

```
yarn playwright install chromium   # once, downloads the browser (about 100 MB)
yarn e2e
```

`yarn e2e` builds the frontend, starts the real SA management backend on an empty in-memory H2 SADB
(`e2e/start-server.sh`, port 18447), and drives the built GUI in headless Chromium. Each test resets the SADB and
seeds what it needs through the REST API. The suite is not part of the Maven build or CI.

- Needs Maven and JDK 17, as for the backend build. The first start installs the current `kmc-sa-db-api` and
  `kmc-sa-db-lib` into `~/.m2` so the backend matches the working tree.
- To iterate faster, keep the backend running in another terminal with `yarn e2e:server`; `yarn e2e` then reuses it.
  Set `E2E_SKIP_INSTALL=1` to skip the `~/.m2` install when only frontend code changed. The server serves `dist/`,
  so rerun `yarn build` (or `yarn e2e`, which builds) after a change.
- `E2E_BROWSER_CHANNEL=chrome` uses an installed Google Chrome instead of the downloaded Chromium.
- Failures leave a screenshot and a trace in `test-results/`; open a trace with `yarn playwright show-trace <trace.zip>`,
  or the HTML report with `yarn playwright show-report`.
- `yarn playwright test --headed` or `--ui` shows the browser while it runs.
