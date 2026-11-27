const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { execFileSync } = require('node:child_process');
const assert = require('node:assert/strict');
const root = path.join(__dirname, '..');
const sdk = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT;
assert.ok(sdk, 'Set ANDROID_HOME');
const apk = path.join(root, 'app/build/outputs/apk/debug/app-debug.apk');
const aapt = path.join(sdk, 'build-tools/35.0.0', process.platform === 'win32' ? 'aapt.exe' : 'aapt');
const metadata = execFileSync(aapt, ['dump', 'badging', apk], { encoding: 'utf8' });
assert.match(metadata, /package: name='com\.enigma\.aminlivetv\.qa'/, 'Preview must not replace production');
assert.match(metadata, /application-debuggable/);
assert.match(metadata, /application-label:'Amin TV Preview'/);
const output = path.join(root, 'app/build/outputs/preview');
fs.mkdirSync(output, { recursive: true });
const bytes = fs.readFileSync(apk);
fs.writeFileSync(path.join(output, 'Amin-TV-Preview.apk'), bytes);
const verification = {
  package: 'com.enigma.aminlivetv.qa', productionPackage: 'com.enigma.aminlivetv',
  version: metadata.match(/versionName='([^']+)'/)?.[1],
  commit: execFileSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim(),
  bytes: bytes.length, sha256: crypto.createHash('sha256').update(bytes).digest('hex'),
  productionSigningKeyRequired: false, emulatorResultsArePhysicalDeviceResults: false
};
fs.writeFileSync(path.join(output, 'verification.json'), JSON.stringify(verification, null, 2) + '\n');
console.log(JSON.stringify(verification));
