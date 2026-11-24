// Release regression check: existing data must survive the Sports-only expansion unchanged.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const root = path.join(__dirname, '..');
const catalogue = JSON.parse(fs.readFileSync(path.join(root, 'channels.json'), 'utf8'));
const baseline = require('./fixtures/sports-baseline-3.9.1.json');
const evidence = JSON.parse(fs.readFileSync(path.join(root, 'docs/sports-verification-2026-11-24.json'), 'utf8'));
const canonical = value => Array.isArray(value) ? value.map(canonical) : value && typeof value === 'object'
  ? Object.fromEntries(Object.keys(value).sort().map(key => [key, canonical(value[key])])) : value;
const hash = value => crypto.createHash('sha256').update(JSON.stringify(canonical(value))).digest('hex');
assert.equal(catalogue.schemaVersion, 1);
assert.ok(catalogue.revision > baseline.revision);
assert.ok(fs.statSync(path.join(root, 'channels.json')).size < 2 * 1024 * 1024);
const allIds = [];
const newVideos = [];
let retained = 0;
for (const [collection, oldEntries] of Object.entries(baseline.collections)) {
  const entries = catalogue[collection];
  allIds.push(...entries.map(entry => entry.id));
  const oldIds = new Set(oldEntries.map(entry => entry.id));
  assert.deepEqual(entries.filter(entry => oldIds.has(entry.id)).map(entry => entry.id), oldEntries.map(entry => entry.id), `${collection}: preserve existing relative order`);
  for (const old of oldEntries) {
    const entry = entries.find(value => value.id === old.id);
    assert.ok(entry, `Retain ${old.id}`);
    const comparable = { ...entry };
    if (entry.id === 'persian-chess') {
      assert.equal(entry.group, 'chess', 'Older clients must still recognise the original group');
      assert.equal(entry.displayGroup, 'sports');
      delete comparable.displayGroup;
    }
    assert.equal(hash(comparable), old.sha256, `Preserve ${old.id} source, metadata and stable ID`);
    retained++;
  }
  for (const entry of entries.filter(value => !oldIds.has(value.id))) {
    assert.equal(collection, 'programmes2', 'New groups remain safe for legacy catalogue readers');
    assert.equal(entry.group, 'sports');
    assert.ok(entry.sourceAttribution.length >= 2);
    assert.ok(Array.isArray(entry.youtubeVideos) && entry.youtubeVideos.length > 0 && entry.youtubeVideos.length <= 25);
    assert.equal(new Set(entry.youtubeVideos).size, entry.youtubeVideos.length);
    assert.equal(entry.youtubeChannel, undefined);
    assert.equal(entry.youtubePlaylist, undefined);
    for (const id of entry.youtubeVideos) {
      newVideos.push(id);
      assert.match(id, /^[\w-]{11}$/);
      const verified = evidence.videos.find(video => video.id === id);
      assert.ok(verified?.metadata.verified && verified.playback.ok, `${id}: require metadata AND actual in-app decoded frames`);
      assert.equal(verified.playback.decoded.region, 'PT');
      assert.ok(verified.playback.decoded.frames >= 10 && verified.playback.decoded.width > 0 && !verified.playback.decoded.ad);
      assert.ok(entry.sourceAttribution.includes(`https://www.youtube.com/channel/${verified.metadata.owner}`), `${id}: official publisher matches`);
    }
  }
}
assert.equal(allIds.length, new Set(allIds).size, 'All channel/programme IDs remain unique');
assert.equal(newVideos.length, new Set(newVideos).size, 'New collections do not duplicate videos');
assert.equal(evidence.result.newVideos, newVideos.length);
assert.ok(evidence.chess.playback.ok && evidence.chess.backToSportsGrid, 'The existing chess playlist plays after moving');
assert.equal(evidence.result.portugalDecoded, evidence.native.filter(entry => entry.played).length + evidence.accepted.length + 1);
for (const [key, digest] of Object.entries(baseline.unchanged)) assert.equal(hash(catalogue[key]), digest, `${key} unchanged`);
console.log(`Sports catalogue validated: ${retained} existing channel/programme records preserved; both series libraries unchanged; every new video decoded in Portugal.`);
