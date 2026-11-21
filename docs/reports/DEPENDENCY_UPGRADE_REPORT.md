# Dependency Upgrade Report

Date: 2026-11-19 (upgrades performed 2026-11-17; re-verified 2026-11-19)

## Staged changes

The upgrades were made as three separate, identifiable groups. Each group was followed by dependency installation, audit, production build and backend/browser regression checks before the next group.

| Group | Package changes | Landed version |
|---|---|---|
| A. Capacitor | `@capacitor/cli`, `@capacitor/core`, `@capacitor/android` 6.2.3 → 8.6.2 together | 8.6.2 (all three) |
| B. React Router | `react-router-dom` 6.30.6 → 7.18.5 (pulls `react-router` 7.18.5) | 7.18.5 |
| C. Vite toolchain | `vite` 5.4.21 → 8.3.8; `@vitejs/plugin-react` 4.7.0 → 6.3.0 | 8.3.8 / 6.3.0 |

Unchanged application dependencies: `react` 18.3.1, `react-dom` 18.3.1, `axios` (range `^1.7.9`, resolved 1.21.1), `playwright-core` 1.65.1. No unrelated packages were intentionally upgraded.

## Native Android changes (part of group A)

Capacitor 8 required a native project migration, performed with the official migration guide:

- Android Gradle Plugin 8.2.1 → 8.13.0.
- Gradle wrapper 8.2.1 → 8.14.3.
- `minSdkVersion` 22 → 24; compile/target SDK 34 → 36.
- AndroidX/Cordova dependency versions updated to the Capacitor 8 baseline in `frontend/android/variables.gradle`.
- Manifest and `capacitor.build.gradle` updated to the Capacitor 8 template; existing application ID, app name, cleartext/LAN configuration and Android project structure preserved.
- `frontend/scripts/android-build.mjs` selects JDK 21 + API 36 for Capacitor 8 (and retains the Cap 6/7 JDK 17 + API 34/35 selection logic). It does not modify machine-wide Java or SDK settings.

## Build/runtime compatibility changes

- Vite 8 requires a newer Node runtime than the previous Docker builder supplied, so the frontend Dockerfile builder changed from Node 20 to `node:22-alpine`. The local environment already runs Node 24.15.0.
- Vite 8 no longer carries the vulnerable `esbuild` 0.21.5 dependency chain; `esbuild` is absent from the final dependency tree.
- React Router 7 is a runtime major upgrade. The app continues to use hash routing; all routes and browser history behavior were re-tested.

## Final dependency tree

`npm ls --depth=0` resolves:

```text
@capacitor/android@8.6.2
@capacitor/cli@8.6.2
@capacitor/core@8.6.2
@vitejs/plugin-react@6.3.0
axios@1.21.1
playwright-core@1.65.1
react-dom@18.3.1
react-router-dom@7.18.5
react@18.3.1
vite@8.3.8
```

Resolved transitive security-relevant packages: `tar` 7.5.23, `rimraf` 6.1.4, `glob` 14.0.6, `minimatch` 11.0.9. Final `npm audit`: **0 vulnerabilities** across 181 locked entries.

## Verification performed

- `npm ci` — pass, lockfile consistent, 0 vulnerabilities.
- `npm audit` — pass, 0 advisories.
- `npm run build` — pass (Vite 8.3.8 production bundle).
- `mvn -B clean test` — 63 tests, 0 failures/errors/skips.
- `docker compose build` — frontend and backend images built successfully.
- `docker compose up -d` — PostgreSQL, backend and frontend healthy; frontend HTTP 200; API health UP.
- Browser regression (Playwright/Chrome, 390×844): home dashboard, four navigation destinations, back/forward, article deep link, learning-topic deep link, unknown route, console/page errors — 12/12 checks passed.
- `npm run android:build` — Vite Android build, `npx cap sync android`, Gradle `assembleDebug` succeeded; APK package/min/target SDK verified.

## Migration references

- [Capacitor 8 migration guide](https://capacitorjs.com/docs/updating/8-0)
- [React Router v7 upgrade guide](https://reactrouter.com/upgrading/v7)
- [Vite migration guide](https://vite.dev/guide/migration)
- [@vitejs/plugin-react package requirements](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react/package.json)

## Blockers / not adopted

None of the three planned groups is blocked; all were validated locally. The remaining unverified area is native runtime on an Android emulator/device, because no emulator or device is available. See [PHASE_6_TEST_REPORT.md](PHASE_6_TEST_REPORT.md).
