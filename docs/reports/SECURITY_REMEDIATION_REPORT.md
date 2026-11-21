# Security Remediation Report

Date: 2026-11-17

## Current audit: REVIEW REQUIRED

The existing frontend dependency tree reports **9 advisories: 1 critical, 2 high, 6 moderate**. No dependency versions in the application were changed. The audit covers npm dependencies, not container base images or the full operating system.

| Area | Current dependency | Candidate path | Status / compatibility risk |
|---|---|---|---|
| Capacitor CLI and `tar` | CLI 6.2.3, `tar` 6.2.1 | Trialled Capacitor CLI/Core/Android 7.6.10 in a temporary isolated copy; candidate audit showed 0 vulnerabilities and resolved `tar` 7.5.23. | **BLOCKED for adoption.** Existing Android project uses Capacitor 6, JDK 17, compile/target API 34, AGP 8.2.1, Gradle 8.2.1. Capacitor 7 migration expects JDK 21 and newer Android build settings. Local Android build is blocked because SDK platform 34 is missing; JDK 21 is also unavailable. Keep the coordinated Capacitor upgrade separate until native validation is possible. |
| Vite | 5.4.21 | Vite 6.4.6 trial build passed with current React plugin; candidate audit showed 0 vulnerabilities. | Major-version change; trial build passed. Check plugin and dev-server behavior as a separate adoption change. Vite 8 is the npm audit suggested path, but the Vite 6 trial is not a full resolution recommendation for future audit state. |
| React Router | 6.30.6 | React Router 7.18.5 trial. | Major runtime change. Temporary browser E2E passed across five navigation destinations plus back/forward. Adopt separately after full route, error-boundary, and native integration regression. |

## Isolated upgrade trial

A temporary copy tested `@capacitor/cli`, `@capacitor/core`, `@capacitor/android` 7.6.10, Vite 6.4.6, and React Router 7.18.5. `npm install`, candidate `npm audit` (0 advisories), Vite production build, and browser navigation/back-forward checks passed. The Android build did not pass: the required Android SDK platform 34 was absent. The candidate was not copied into the working application. npm also warned that the esbuild install script was blocked by npm’s allow-scripts policy; the production build nevertheless passed.

## Recommended staged remediation

1. Provision a compatible Android toolchain (JDK 21 and the target SDK/build tools required by the chosen Capacitor major) and confirm the existing Android project can build first.
2. Upgrade Capacitor CLI/Core/Android together in a dedicated change; run install, audit, production build, browser E2E, Android build and native navigation checks before adoption.
3. Upgrade React Router separately and run all navigation, deep-link, browser history, and Android navigation checks.
4. Upgrade Vite and its React plugin as a separate build-tool change; repeat install, audit, production build and browser tests.
5. Re-run audit and review transitive dependency changes. Do not apply `npm audit fix --force` blindly.

Capacitor’s official [6-to-7 migration guide](https://capacitorjs.com/docs/updating/7-0) documents Android toolchain requirements. Vite’s official [v6 migration guide](https://v6.vite.dev/guide/migration.html) and React Router’s [v7 upgrade guide](https://reactrouter.com/upgrading/v7) are the starting points for separate adoption work.

**No dependency upgrade was applied to the main app; Capacitor functionality remains intact.**
