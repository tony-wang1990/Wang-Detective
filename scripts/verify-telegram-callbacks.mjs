#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const telegramDir = path.join(root, 'src/main/java/com/tony/kingdetective/telegram');

function rel(file) {
  return path.relative(root, file).replaceAll(path.sep, '/');
}

function walk(dir) {
  if (!fs.existsSync(dir)) return [];
  const output = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) output.push(...walk(full));
    else if (full.endsWith('.java')) output.push(full);
  }
  return output;
}

function add(map, key, source) {
  if (!key) return;
  if (!map.has(key)) map.set(key, []);
  map.get(key).push(source);
}

const files = walk(telegramDir);
const callbacks = new Map();
const handlerPatterns = new Set();
const explicitCallbacks = new Set();

for (const file of files) {
  const source = fs.readFileSync(file, 'utf8');
  const sourceName = rel(file);

  for (const match of source.matchAll(/\.callbackData\("([^"]+)"\)/g)) {
    add(callbacks, match[1], sourceName);
  }
  for (const match of source.matchAll(/KeyboardBuilder\.button\("[^"]+",\s*"([^"]+)"\)/g)) {
    add(callbacks, match[1], sourceName);
  }
  for (const match of source.matchAll(/buildConfirmationKeyboard\("([^"]+)",\s*"([^"]+)"\)/g)) {
    add(callbacks, match[1], sourceName);
    add(callbacks, match[2], sourceName);
  }

  for (const match of source.matchAll(/getCallbackPattern\s*\(\)\s*\{[\s\S]*?return\s+"([^"]+)"/g)) {
    handlerPatterns.add(match[1]);
  }
  for (const match of source.matchAll(/"([^"]+)"\.equals\(callbackData\)/g)) {
    explicitCallbacks.add(match[1]);
  }
  for (const match of source.matchAll(/callbackData\.equals\("([^"]+)"\)/g)) {
    explicitCallbacks.add(match[1]);
  }
}

function isCovered(callback) {
  return explicitCallbacks.has(callback) || [...handlerPatterns].some((pattern) => callback.startsWith(pattern));
}

const missing = [...callbacks.entries()]
  .filter(([callback]) => !isCovered(callback))
  .sort(([left], [right]) => left.localeCompare(right));

console.log(`Telegram callbacks: ${callbacks.size}`);
console.log(`Telegram handler patterns: ${handlerPatterns.size}`);
console.log(`Telegram explicit callbacks: ${explicitCallbacks.size}`);

if (missing.length) {
  console.error('\nMissing Telegram callback handlers:');
  for (const [callback, sources] of missing) {
    console.error(`- ${callback} (${[...new Set(sources)].join(', ')})`);
  }
  process.exit(1);
}

console.log('Telegram callback mapping check passed.');
