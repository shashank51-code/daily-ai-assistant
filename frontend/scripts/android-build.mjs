#!/usr/bin/env node
/**
 * Builds the Android debug APK with a project-local toolchain.
 *
 * - Uses JDK 17 (required by Android Gradle Plugin 8.2) without touching the
 *   machine-wide JAVA_HOME, which may point at a newer JDK.
 * - Finds the Android SDK from ANDROID_HOME/ANDROID_SDK_ROOT, the project-local
 *   .tools/android-sdk, or the default per-user install.
 * - Writes android/local.properties (gitignored) so Gradle sees the SDK.
 * - Runs: vite build --mode android -> cap sync android -> gradlew assembleDebug.
 */
import { spawnSync } from 'node:child_process';
import { existsSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { homedir, platform } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const frontendDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const projectRoot = resolve(frontendDir, '..');
const isWindows = platform() === 'win32';

function fail(message) {
  console.error(`\nAndroid build cannot start: ${message}\n`);
  process.exit(1);
}

function javaMajor(home) {
  const release = join(home, 'release');
  if (!existsSync(release)) return null;
  const match = /JAVA_VERSION="(\d+)/.exec(readFileSync(release, 'utf8'));
  return match ? Number(match[1]) : null;
}

function findJdk17() {
  const candidates = [];
  if (process.env.JAVA_HOME) candidates.push(process.env.JAVA_HOME);
  const jdksDir = join(homedir(), '.jdks');
  if (existsSync(jdksDir)) {
    for (const name of readdirSync(jdksDir)) candidates.push(join(jdksDir, name));
  }
  for (const dir of candidates) {
    if (!existsSync(join(dir, 'bin', isWindows ? 'java.exe' : 'java'))) continue;
    if (javaMajor(dir) === 17) return dir;
  }
  return null;
}

function findAndroidSdk() {
  const candidates = [
    process.env.ANDROID_HOME,
    process.env.ANDROID_SDK_ROOT,
    join(projectRoot, '.tools', 'android-sdk'),
    join(homedir(), 'AppData', 'Local', 'Android', 'Sdk'),
    join(homedir(), 'Library', 'Android', 'sdk'),
    join(homedir(), 'Android', 'Sdk'),
  ].filter(Boolean);

  for (const dir of candidates) {
    if (existsSync(join(dir, 'platforms', 'android-34'))) return dir;
  }
  return null;
}

const jdk17 = findJdk17();
if (!jdk17) {
  fail(
    'JDK 17 was not found. Install one (for example via IntelliJ) or set JAVA_HOME to a JDK 17 ' +
      'installation. Android Gradle Plugin 8.2 does not support newer JDKs.',
  );
}

const androidSdk = findAndroidSdk();
if (!androidSdk) {
  fail(
    'Android SDK with platform 34 was not found. Set ANDROID_HOME or install the SDK ' +
      'command-line tools and run: sdkmanager "platforms;android-34" "build-tools;34.0.0" ' +
      '"platform-tools"',
  );
}

const localProperties = join(frontendDir, 'android', 'local.properties');
if (!existsSync(localProperties)) {
  const escaped = androidSdk.replace(/\\/g, '\\\\').replace(/:/g, '\\:');
  writeFileSync(localProperties, `sdk.dir=${escaped}\n`);
  console.log(`Wrote ${localProperties}`);
}

const env = {
  ...process.env,
  JAVA_HOME: jdk17,
  ANDROID_HOME: androidSdk,
  ANDROID_SDK_ROOT: androidSdk,
  PATH: `${join(jdk17, 'bin')}${isWindows ? ';' : ':'}${join(androidSdk, 'platform-tools')}${isWindows ? ';' : ':'}${process.env.PATH}`,
};

console.log(`JDK:  ${jdk17}`);
console.log(`SDK:  ${androidSdk}`);

function run(label, command, args, cwd = frontendDir) {
  console.log(`\n==> ${label}`);
  const result = spawnSync(command, args, { cwd, env, stdio: 'inherit', shell: isWindows });
  if (result.status !== 0) {
    fail(`${label} failed with exit code ${result.status ?? 'unknown'}.`);
  }
}

run('Building web assets for Android', 'npx', ['vite', 'build', '--mode', 'android']);
run('Syncing Capacitor', 'npx', ['cap', 'sync', 'android']);

const gradle = isWindows ? 'gradlew.bat' : './gradlew';
run('Assembling debug APK', gradle, ['assembleDebug'], join(frontendDir, 'android'));

const apk = join(frontendDir, 'android', 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
console.log(existsSync(apk) ? `\nAPK: ${apk}` : '\nGradle finished but the APK was not found.');
