#!/usr/bin/env bash
# Builds test.apk with only the Android SDK (no Gradle). Usage: ANDROID_SDK=/path/to/sdk bash build.sh
set -euo pipefail
cd "$(dirname "$0")"
SDK=${ANDROID_SDK:?set ANDROID_SDK}; BT=$SDK/build-tools/35.0.0; JAR=$SDK/platforms/android-30/android.jar
rm -rf build && mkdir -p build/classes build/dex build/assets && cp index.html build/assets/
$BT/aapt2 compile --dir res -o build/res.zip
$BT/aapt2 link -o build/base.apk -I $JAR --manifest AndroidManifest.xml -A build/assets build/res.zip
javac -nowarn -source 8 -target 8 -bootclasspath $JAR -d build/classes $(find src -name '*.java') 2>&1 | grep -v 'warning' || true
(cd build/classes && jar cf ../classes.jar .)
$BT/d8 --min-api 21 --lib $JAR --output build/dex build/classes.jar
cp build/base.apk build/unsigned.apk && (cd build/dex && zip -q ../unsigned.apk classes.dex)
$BT/zipalign -f -p 4 build/unsigned.apk build/aligned.apk
[ -f test.keystore ] || keytool -genkeypair -keystore test.keystore -storepass testtest -keypass testtest \
  -alias test -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Test" >/dev/null 2>&1
$BT/apksigner sign --ks test.keystore --ks-pass pass:testtest --out test.apk build/aligned.apk
$BT/apksigner verify --verbose test.apk | head -5
grep -o 'versionCode="[0-9]*"' AndroidManifest.xml | grep -o '[0-9]*' > version.txt
echo "version.txt = $(cat version.txt)"
