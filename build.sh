#!/usr/bin/env bash
# ONDA release APK 빌드 (Gradle 없이 Android SDK 명령줄 도구만 사용)
# 필요: JDK 17 이상, Android SDK(platforms;android-36, build-tools;36.x), Python 3
# 서명: ONDA_KEYSTORE(키 저장소 경로), ONDA_KEYSTORE_PASS(암호) 환경변수. 별칭은 onda.
set -euo pipefail
cd "$(dirname "$0")"

: "${ANDROID_SDK_ROOT:?ANDROID_SDK_ROOT 를 Android SDK 경로로 지정하세요}"
PLATFORM="${ONDA_PLATFORM:-android-36}"
BT="${ONDA_BUILD_TOOLS:-$(ls "$ANDROID_SDK_ROOT/build-tools" | sort -V | tail -1)}"
bt="$ANDROID_SDK_ROOT/build-tools/$BT"
jar="$ANDROID_SDK_ROOT/platforms/$PLATFORM/android.jar"
ext=""; [ -f "$bt/d8.bat" ] && ext=".bat"
PY="$(command -v python3 || command -v python)"
VERSION_CODE=1
VERSION_NAME=1.0.0
OUT="apk/${ONDA_APK_NAME:-ONDA_release.apk}"

test -f "$jar"
rm -rf build && mkdir -p build/classes build/dex apk

# 1) 리소스(아이콘) 컴파일 + 매니페스트·화면 파일 묶기
"$bt/aapt2" compile --dir app/src/main/res -o build/res.zip
"$bt/aapt2" link -I "$jar" --manifest app/src/main/AndroidManifest.xml \
  -A app/src/main/assets -o build/base.apk build/res.zip \
  --min-sdk-version 28 --target-sdk-version 35 \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME"

# 2) 자바 → DEX
javac --release 11 -encoding UTF-8 -classpath "$jar" -d build/classes \
  $(find app/src/main/java -name '*.java')
"$bt/d8$ext" --release --lib "$jar" --min-api 28 --output build/dex \
  $(find build/classes -name '*.class')

# 3) DEX 넣어 새 압축본 만들기 → 정렬 → 서명 → 검증
"$PY" tools/add_dex.py build/base.apk build/dex/classes.dex build/unsigned.apk
"$bt/zipalign" -p -f 4 build/unsigned.apk build/aligned.apk
: "${ONDA_KEYSTORE:?서명 키 저장소 경로 ONDA_KEYSTORE 를 지정하세요}"
: "${ONDA_KEYSTORE_PASS:?서명 암호 ONDA_KEYSTORE_PASS 를 지정하세요}"
"$bt/apksigner$ext" sign --ks "$ONDA_KEYSTORE" --ks-key-alias onda \
  --ks-pass env:ONDA_KEYSTORE_PASS --key-pass env:ONDA_KEYSTORE_PASS \
  --v2-signing-enabled true --v3-signing-enabled true --v4-signing-enabled false \
  --out "$OUT" build/aligned.apk
"$bt/apksigner$ext" verify --verbose "$OUT" | grep -E "Verified using|signers"
"$bt/zipalign" -c -P 16 4 "$OUT" && echo "정렬 확인"
echo "완료: $OUT"
