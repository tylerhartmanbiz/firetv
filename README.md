# StreamFlow for Fire TV

Install on Fire TV with the Downloader app. Enter this link (it always points to the newest build):

```
github.com/tylerhartmanbiz/firetv/raw/main/streamflow.apk
```

A full-screen WebView app (`src/`) that runs the StreamFlow UI in `assets/index.html`.
Movies come from the Internet Archive's public-domain feature film collection.

Remote controls:
- **Browse:** arrows move between posters, Select plays, Up from the top row goes to search, Back returns to search.
- **Search:** Select opens the on-screen keyboard; press Go/Enter to search, Down to go to the results.
- **Player:** Select plays or pauses, Left/Right skip 10s, Up/Down show the buttons (rewind, play, forward, next source, close), Back closes the player. The play/pause, fast-forward and rewind keys also work.

Rebuild (needs Android build-tools 35.0.0 and platform android-30): `ANDROID_SDK=/path/to/sdk bash build.sh`.
`test.apk` is the earlier D-pad test app.
