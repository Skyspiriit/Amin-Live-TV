# In-app update permission on the Xiaomi box (3.3)

Checked on 18 November 2026. The box is a Xiaomi MiTV-AFKR0 on Android 11. All box checks were read-only ADB, run while the TV was in use, without touching the screen.

## Diagnosis on the box

| | User 0 (Owner, the profile Mom uses) | User 10 (`new_user`, managed work profile, quiet mode, no profile owner, created about 958 days ago) |
|---|---|---|
| `com.enigma.aminlivetv` | installed, uid 10091, has data, in use | installed, uid 1010091, **never launched, no data** (`stopped=true notLaunched=true ceDataInode=0`) |
| Install unknown apps (`REQUEST_INSTALL_PACKAGES`) | **deny** (rejected about 09:01) | **allow** |

The work profile also contains `com.ansub.tvhome` (unrelated, left alone). Amin Player is no longer installed.

Event log at 09:02: the in-app update reached Android's installer. The installer opened TV Settings' **Install unknown apps** page (`ExternalSourcesActivity`), came back 5 seconds later, and the install was abandoned.

## Cause, reproduced on the emulator

On an Android TV emulator (Android 11), I recreated the same setup: a managed profile, the app installed with a plain `adb install`, permission denied in user 0 and allowed in user 10.

1. **The duplicate comes from ADB.** `adb install` without `--user` installs into every user, so the app also landed in the work profile.
2. **TV Settings hides the right switch.** With the same package in both profiles, **Install unknown apps** lists only the work-profile copy, marked with a small briefcase and already "Allowed". The main-profile copy isn't listed, so it can't be turned on from the TV. Opening the page for this specific app (`package:` URI) doesn't help, because TV Settings shows the same list.
3. **Removing only the work-profile copy fixes it.** After `pm uninstall --user 10`, the list shows the main copy ("Not allowed"). Turning it on lets the update install. The main copy's data was untouched (same checksums).

So this is the same kind of problem as Amin Player had. The duplicate doesn't block the install directly: it hides the only switch that would allow it.

## App changes (3.3)

- **Check before installing.** The app checks `canRequestPackageInstalls()` first. If installs aren't allowed, it opens `ACTION_MANAGE_UNKNOWN_APP_SOURCES` for its own package instead of going through Android's generic prompt.
- **Resume after Settings.** It remembers the verified APK, so the update continues when the viewer returns. Android kills the app when this permission changes, so the request survives a restart. If the switch is now on, the install confirmation appears straight away. If not, a Persian message says to turn on Amin Live TV, and that a briefcase icon means another copy that needs family help.
- **Version tags.** Versions may be `X.Y` or `X.Y.Z`. The app is now 3.3 (versionCode 9), so it still upgrades 0.3.3 (versionCode 8). The release is tagged `v3.3.0`, because 0.3.3 only recognises three-part tags.
- **Unchanged:** package ID, signing key, the APK verification (same package, newer version, same certificate), catalogue and user data.

## Emulator verification

The test was update 3.3 → 3.4, debug-signed QA builds with the same certificate, local release feed.

- **Detection:** 3.3 detected `v3.4` and downloaded and verified it.
- **With the duplicate:** OK on the update button opened Install unknown apps, which showed only the work-profile copy. Back led to the "still off" message, and nothing was installed.
- **Without the duplicate:** after removing the work-profile copy, the list showed the main copy. I turned it on and pressed Back. Android restarted the app, the app opened the install confirmation by itself, and Right then OK installed 3.4.
- **Data kept:** `viewing.xml` and `channel-health.xml` were byte-identical before and after, and «ادامهٔ تماشا · شبکه یک» was still shown.
- **Checks:** 10 unit tests pass (including two-part version tags), and lint has no errors.

## Action that needs your approval (box)

Remove only the unused work-profile copy. This doesn't touch the main-profile app, its data, or any other app:

```
adb connect 192.168.1.152:5555
adb -s 192.168.1.152:5555 shell pm uninstall --user 10 com.enigma.aminlivetv
```

Check afterwards (expect it in user 0 only):

```
adb -s 192.168.1.152:5555 shell pm list packages --user 0 com.enigma.aminlivetv
adb -s 192.168.1.152:5555 shell pm list packages --user 10 com.enigma.aminlivetv
```

After that, the next in-app update shows the correct switch. Mom (or you) turns on **Amin Live TV** once and presses Back.

For any future install by ADB, always use `adb install -r --user 0 …`, or the duplicate comes back.
