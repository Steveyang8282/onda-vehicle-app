# ONDA — 동승자별 승차감 개인화 앱

> 동승자마다 원하는 배려 정도(1~10단계)를 기억하고, 도착 후 한 번의 응답(높이기·유지·낮추기)으로 다음 이동의 설정을 조정하는 차량용 앱 시제품입니다.

현대자동차그룹 차량용 앱 개발 공모전 출품작입니다. 현재는 **모의 차량 데이터**로 동작하는 시제품이며, 실제 차량 신호·Pleos Connect SDK 연동·서스펜션 등 하드웨어 제어는 아직 하지 않습니다.

## APK 위치

| 파일 | 내용 |
|---|---|
| [`apk/양호열_ONDA_release.apk`](apk/양호열_ONDA_release.apk) | release 서명본 · 버전 1.0.0 (versionCode 1) · 패키지 `app.onda.comfort` · 최소 Android 9 (API 28) |

SHA-256: `aa38d6c8eb21cc10761d2eb8558dd3147bfbd095ea058e95c00a8a5810ea2733`

## 실행 방법

1. Android 에뮬레이터 또는 기기(Android 9 이상)를 켭니다.
2. APK 를 설치합니다.
   ```bash
   adb install apk/양호열_ONDA_release.apk
   ```
3. 앱 목록에서 **ONDA** 를 실행합니다. 로그인·인터넷 연결·권한 허용이 필요 없습니다.

### 시연 순서 (약 1분)

1. **01 승차감 설정** — 「동승자 A」를 누르면 저장된 8단계가 나옵니다. 오른쪽에 단계별 동작(감지 기준 1.56 m/s² 등)이 바뀝니다.
2. **02 주행 체험** — 「1.6 m/s² 변화 재생」을 누르면 8단계 기준(1.56)을 넘어 안내가 나옵니다. 6단계(기준 1.80)에서는 같은 변화에 안내하지 않습니다.
3. **도착 및 주차** → **03 도착 후 피드백** — 「9 · 배려를 더 높이기」와 이유(예: 출발·제동)를 고르고 「다음 주행에 9단계 적용」.
4. **01 승차감 설정** 으로 돌아가면 동승자 A 만 9단계로 바뀌어 있고, 다른 동승자는 그대로입니다. 앱을 완전히 껐다 켜도 유지됩니다.
5. 피드백 화면의 「방금 변경 취소」로 직전 변경을 되돌릴 수 있습니다. 10단계에서 높여도 10, 1단계에서 낮춰도 1을 유지합니다.
6. **04 하드웨어 확장** — 제조사 협업 시 승인된 상위 모드로 연결하는 구상입니다(현재 제어 기능 없음).
7. 오른쪽 위 「데모 초기화」로 처음 상태로 돌아갑니다.

## 테스트 환경

| 항목 | 값 |
|---|---|
| 에뮬레이터 | Android Emulator 37.2.13.0 |
| 시스템 이미지 | Android 16 (API 36) · `system-images;android-36;default;x86_64` (rev 2) |
| 화면 규격 | 1920 × 1080 가로, 240 dpi (차량 중앙 화면 비율을 가정) |
| WebView | Android System WebView 133.0.6943.137 |
| 필요한 설정 | 없음 (권한 0개, 인터넷 불필요) |
| Pleos Connect 에뮬레이터 | 아직 확인하지 않음 — Pleos Connect SDK 연동 단계에서 확인 예정 |

확인한 동작: 설치·실행, 프로필 선택, 단계 변경, 앱 강제 종료 후 저장값 유지, 같은 서명으로 덮어 설치 후 저장값 유지, 단계별 안내 기준(8·9·10단계), 도착 후 +1/0/−1 적용, 1·10 경계 유지, 중복 적용 방지, 직전 변경 취소, 다른 프로필 값 보존, 상태 표시줄·내비게이션 막대에 화면이 가리지 않음.

## 직접 빌드하기

Gradle 없이 Android SDK 명령줄 도구로 빌드합니다.

- 필요: JDK 17 이상, Android SDK `platforms;android-36`·`build-tools;36.x`, Python 3, Node.js(시험용)
- 서명 키는 저장소에 넣지 않습니다. 직접 만든 키 저장소를 환경변수로 지정하세요(별칭 `onda`).

```bash
export ANDROID_SDK_ROOT=/path/to/android-sdk
export ONDA_KEYSTORE=/path/to/your-release.jks
export ONDA_KEYSTORE_PASS=********
bash build.sh            # apk/ONDA_release.apk 생성·서명·검증
node test/core.test.cjs  # 단계 경계·피드백·단계별 정책 시험
```

## 구조

```
app/src/main/AndroidManifest.xml          권한 없음, 백업 끔, 평문 통신 끔
app/src/main/java/app/onda/comfort/        MainActivity — 앱에 묶은 화면만 여는 WebView
app/src/main/assets/web/                   화면(HTML·CSS·JS)과 단계 계산 모듈 core.js
app/src/main/assets/web/fonts/             Pretendard 글꼴(SIL OFL 1.1)
app/src/main/res/mipmap-*/                 앱 아이콘
build.sh, tools/add_dex.py                 빌드 스크립트
test/core.test.cjs                         단계 계산 시험
apk/                                       release APK
```

## 단계 계산 규칙 (시연 가설)

- 다음 단계 = min(10, max(1, 현재 단계 + 응답)) — 응답은 높이기 +1, 유지 0, 낮추기 −1
- 감지 기준 A(L) = 2.40 − 0.12 × (L − 1) m/s²
- 휴식 제안 기준 R(L) = 120 − 5 × (L − 1) 분
- 수치는 검증 전 시연용 가설입니다. 안전 기준·멀미 기준·서스펜션 제어값이 아닙니다.

## 보안·개인정보

- 권한을 하나도 요청하지 않습니다(인터넷·위치·저장소 없음).
- 앱에 묶은 화면 파일(`file:///android_asset/web/`) 외의 주소는 열지 않고 요청도 막습니다.
- 기기 파일 접근, 다른 출처 접근, 새 창 열기, 원격 디버깅을 끕니다.
- 프로필·단계·피드백 기록은 기기 안(WebView 저장소)에만 저장하며 밖으로 보내지 않습니다. 앱 백업도 끕니다.

## 다음 단계

1. Pleos Connect SDK 의 Vehicle SDK 로 속도·정차 상태를 받아 모의 데이터를 대체하고, Pleos Connect 에뮬레이터에서 확인
2. TextToSpeech SDK 로 짧은 음성 안내
3. 제조사 협업 시 승인된 상위 주행 모드 요청으로 확장(바퀴별 실시간 제어는 차량 제어기가 담당)

## 글꼴 라이선스

화면 글꼴 Pretendard 는 SIL Open Font License 1.1 을 따릅니다. 원문: `app/src/main/assets/web/fonts/Pretendard-LICENSE.txt`
