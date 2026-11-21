# Phase 6 Security Report

Date: 2026-11-19 (remediation performed 2026-11-17; re-verified 2026-11-19)

## Outcome

**Frontend npm dependency audit: PASS — 0 advisories.**

A fresh `npm audit` against the current lockfile reports 0 info, low, moderate, high and critical advisories. The original audit contained 9 vulnerable package entries: 1 critical, 2 high, 6 moderate. All 9 were resolved by the coordinated dependency upgrades recorded in [DEPENDENCY_UPGRADE_REPORT.md](DEPENDENCY_UPGRADE_REPORT.md); no `npm audit fix --force` was used and no unrelated package was upgraded.

```text
$ cd frontend && npm audit
found 0 vulnerabilities
```

The lockfile contains 181 locked package entries (133 production, 48 development).

## Resolved advisories

| Package | Original severity | Installed then | Resolved version | Resolution |
|---|---|---:|---:|---|
| `tar` | Critical | 6.2.1 | 7.5.23 | Reached through Capacitor CLI 8.6.2 |
| `@capacitor/cli` | High | 6.2.3 | 8.6.2 | Coordinated Capacitor CLI/Core/Android upgrade |
| `vite` | High | 5.4.21 | 8.3.8 | Vite major upgrade with compatible React plugin |
| `esbuild` | Moderate | 0.21.5 | not present | Removed with the Vite 8 toolchain change |
| `react-router-dom` | Moderate | 6.30.6 | 7.18.5 | React Router major upgrade |
| `react-router` | Moderate | 6.30.6 | 7.18.5 | Upgraded transitively with `react-router-dom` |
| `rimraf` | Moderate | 4.4.1 | 6.1.4 | Reached through Capacitor CLI 8.6.2 |
| `glob` | Moderate | 9.3.5 | 14.0.6 | Reached through Capacitor CLI 8.6.2 |
| `minimatch` | Moderate | 8.0.7 | 11.0.9 | Reached through Capacitor CLI 8.6.2 |

## Remaining advisories

None reported by `npm audit` for the frontend dependency graph. There are therefore no package/fixed-version rows for unresolved npm advisories.

## Scope and limits — not a security certification

A zero-advisory npm audit only means npm's advisory database has no matching entry for this dependency tree. It is **not** a claim that the application is secure:

- The audit covers npm packages only. It does not cover Maven/backend dependencies.
- Docker base images, the host OS and installed JDKs/Android SDK were not vulnerability-scanned.
- No SAST, DAST, penetration test or application-level review was performed in this phase.
- The Android APK was assembled but not installed/run on a device or emulator, so native runtime behavior was not security-tested.
- `AI_ENABLED=false`; no real AI provider was exercised.
- Feed content, CORS configuration, the optional admin key and the absence of user authentication remain application design considerations unrelated to npm advisory counts.

## Verification evidence

- `npm ci` completed and reported `found 0 vulnerabilities`.
- `npm audit` reported 0 advisories.
- `npm run build` (Vite 8.3.8) completed successfully.
- Backend `mvn -B clean test`: 63 tests, 0 failures/errors/skips.
- Docker frontend and backend images built; Compose stack started healthy.
- Browser navigation, back/forward, article/topic deep links and unknown-route handling passed with no console or uncaught errors.

No deployment, GitHub push, database reset or secret disclosure occurred. Full details are in [PHASE_6_TEST_REPORT.md](PHASE_6_TEST_REPORT.md) and [ANDROID_TOOLCHAIN_REPORT.md](ANDROID_TOOLCHAIN_REPORT.md).
