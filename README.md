# Fire TV test app

Install on Fire TV with the Downloader app. Enter this link (it always points to the newest build):

```
github.com/tylerhartmanbiz/firetv/raw/main/test.apk
```

## Live updates (no reinstall)

The app is a thin shell. Its whole UI is `index.html` in this repo. Every time the app opens it
downloads the latest `index.html` from the `main` branch, so any change pushed to `main` shows up
the next time you open the app (or press the Menu ☰ button on the remote to reload right away).
GitHub can cache the file for up to ~5 minutes.

If the TV is offline, it uses the last copy it downloaded, or the copy bundled in the APK.

Remote: Back closes the player, or exits when the player is closed.

You only need a new APK if `src/`, `AndroidManifest.xml` or `res/` change.
Rebuild: `ANDROID_SDK=/path/to/sdk bash build.sh` (needs build-tools 35.0.0 and platform android-30).
