import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveApiBaseUrl } from '../src/api/baseUrl.js';

test('production without VITE_API_BASE_URL uses same-origin /api paths', () => {
  assert.equal(resolveApiBaseUrl(undefined), '');
  assert.equal(resolveApiBaseUrl(''), '');
  assert.equal(resolveApiBaseUrl('   '), '');
});

test('production never falls back to an absolute localhost URL', () => {
  const resolved = resolveApiBaseUrl(undefined);
  assert.ok(!resolved.includes('localhost'), `unexpected base URL: ${resolved}`);
  assert.ok(!/^https?:\/\//.test(resolved), `expected a relative base URL: ${resolved}`);
});

test('development keeps relative URLs so the Vite proxy handles /api', () => {
  // vite.config.js proxies /api to http://localhost:8080 in dev mode.
  assert.equal(resolveApiBaseUrl(undefined), '');
});

test('explicit base URLs are preserved for Android and split-origin builds', () => {
  assert.equal(resolveApiBaseUrl('https://api.example.com'), 'https://api.example.com');
  assert.equal(resolveApiBaseUrl('  http://10.0.2.2:8080  '), 'http://10.0.2.2:8080');
});

test('trailing slashes are removed to avoid double-slash API paths', () => {
  assert.equal(resolveApiBaseUrl('https://api.example.com/'), 'https://api.example.com');
  assert.equal(resolveApiBaseUrl('https://api.example.com///'), 'https://api.example.com');
});
