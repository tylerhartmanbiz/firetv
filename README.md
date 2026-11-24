# Fire TV test app

Install on Fire TV with the Downloader app. Enter this link (it always points to the newest build):

```
github.com/tylerhartmanbiz/firetv/raw/main/test.apk
```

A full-screen WebView app (`src/`) that runs the UI in `index.html` (a movie browser for the Internet Archive's public-domain films).

Remote controls:
- **Browse:** arrows move between posters, Select plays, Up from the top row goes to search, Back returns to search.
- **Search:** Select opens the on-screen keyboard; press Go/Enter to search, Down to go to the results.
- **Player:** Select plays or pauses, Left/Right skip 10s, Up/Down show the buttons, Back closes the player.
- **Menu (☰):** reloads the UI from GitHub.

## Updating without reinstalling

Install `test.apk` once. After that:
- **UI changes** (`index.html`): push to `main`. The app downloads the latest `index.html` every time it opens; press Menu to reload right away. GitHub can take up to ~5 minutes to serve a new file. Offline, the app uses the last downloaded copy.
- **App changes** (Java/manifest): bump `versionCode` in `AndroidManifest.xml`, run `build.sh`, and push `test.apk` and `version.txt` to `main`. The app offers to install the update when it opens. The first time, Fire TV asks you to allow Test to install apps.

Every build must be signed with the same `test.keystore`, or Fire TV refuses the update. The keystore is not committed, so keep a copy of it.

Rebuild (needs Android build-tools 35.0.0 and platform android-30): `ANDROID_SDK=/path/to/sdk bash build.sh`.
