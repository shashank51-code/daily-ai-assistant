# Dependency Security Report

Audit date: 2026-11-17. Source: `npm audit --json` in `frontend`, plus `npm ls` dependency tree inspection. No dependency updates were applied.

## Current audit summary

9 vulnerable package entries: 1 critical, 2 high, 6 moderate, 0 low. npm reports available fixes only through major upgrades for the affected direct packages.

| Package | Installed | Direct/transitive path | Severity | Finding / safe path |
|---|---:|---|---|---|
| `@capacitor/cli` | 6.2.3 | Direct dev dependency | High (includes critical transitive `tar`) | `tar` archive path/overwrite and denial-of-service advisories; npm proposes CLI 8.6.2, a major Capacitor upgrade. Coordinate CLI with Android/Core plugins and run Android build checks. |
| `tar` | 6.2.1 | `@capacitor/cli` → `tar` | Critical advisory present | Vulnerable archive extraction behavior; update the owning CLI/toolchain or evaluate a tested package override. A broad override could break the CLI. |
| `rimraf` | 4.4.1 | `@capacitor/cli` → `rimraf` | Moderate | Transitively affected through `glob`/`minimatch`; resolved by proposed CLI major upgrade. |
| `glob` | 9.3.5 | `@capacitor/cli` → `rimraf` → `glob` | Moderate | Abort/cancellation traversal advisory and affected minimatch dependency; upgrade owning CLI chain. |
| `minimatch` | 8.0.7 | `@capacitor/cli` → `rimraf` → `glob` → `minimatch` | Moderate | Denial-of-service advisory; upgrade owning CLI chain. |
| `vite` | 5.4.21 | Direct dev dependency; also used by `@vitejs/plugin-react` | High (with moderate/low advisories) | Dev-server/path/editor vulnerabilities and affected `esbuild`; npm proposes Vite 8.3.8, a major upgrade. Test plugin compatibility and production build. |
| `esbuild` | 0.21.5 | `vite` → `esbuild` | Moderate | Dev-server cross-origin request issue; update through a compatible Vite upgrade. |
| `react-router-dom` | 6.30.6 | Direct runtime dependency | Moderate | Open redirect and SSR hydration advisories in `react-router`; npm proposes Router 7.18.5, a major upgrade. Test route behavior before adoption. |
| `react-router` | 6.30.6 | `react-router-dom` → `react-router` | Moderate | Same upstream advisories as the direct DOM package. |

## Recommendation

Treat the critical `tar` issue and high-severity items as upgrade work before distributing developer tooling or relying on the affected dev server. Plan a coordinated Capacitor 8, Vite 8, and React Router 7 upgrade on a separate change with Android and route regression testing. A narrower `tar` override may be evaluated first, but should be validated against Capacitor CLI commands. Do not use `npm audit fix --force` as a blind change.

## Why no upgrade was made

All npm-suggested direct fixes cross major versions, and the Capacitor CLI upgrade can affect the existing Android setup. This phase’s instruction says to report risky upgrades for approval; no user approval for those breaking changes was given. The audit is of frontend dependencies only, not a full container/base-image security scan.
