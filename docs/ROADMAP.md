# Amin TV roadmap

Review from iPhone; normal development/builds run remotely. Production stays 3.9.2 until a separately approved release.

## Checkpoint 1 — cloud setup and Persian episode interface

Audit source/docs/issues/unfinished branches; version a repeatable Linux/Java 17/SDK 35 setup; produce a separate installable preview and reports in GitHub Actions; improve compact numbered RTL episode selection, unavailable gaps and focus while preserving exact resume/Continue Watching. Test, update continuation documents, prepare a PR and **stop**. No merge, production tag/release or device installation without owner approval.

## Checkpoint 2 — catalogue additions, after interface acceptance

Research and document لحظه گرگ و میش، مدار صفر درجه، در چشم باد، مسافران، ساختمان پزشکان، شب‌های برره، قهوه تلخ، پاورچین and نقطه‌چین now; implement later. Establish authorization, full-episode identity/order/counts, stable IDs, per-video availability and actual embedded playback. Public metadata/duration alone is insufficient. Preserve missing numbers; unknown end counts need evidence. Current source inventory contains older, weaker acceptance rules. Do not change `channels.json` in checkpoint 1.

Plan explicit unavailable records/known season bounds only after checking older-reader compatibility. Do not invent links or replace working sources silently.

## Checkpoint 3 — device acceptance and approved release

With new permission, test a separate preview on Xiaomi: viewing-distance readability, D-pad focus/scroll, missing entries, pause, Back/reopen, exact resume, process restart, captions off, next episode and official YouTube fallback. Retest live TV, documentaries, loading/error recovery and startup.

After owner review, merge, choose the next version/code, securely provision the **existing** signing key into protected Actions secrets with specific approval, approve a production release, compare the certificate with the previous public APK, and publish only that signed artifact. Never generate a replacement production key.

## Later

Source maintenance and measured reliability improvements. New VPN providers, networking rewrites, paid services and Amin Player changes are outside this checkpoint.
