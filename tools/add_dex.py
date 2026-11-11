"""aapt2 가 만든 base.apk 에 classes.dex 를 더해 새 압축본을 쓴다(머리 정보를 처음부터 다시 써서 zipalign 경고를 없앰)."""
import sys
import zipfile

src, dex, dst = sys.argv[1:4]
STAMP = (2026, 1, 1, 0, 0, 0)  # 재현 가능한 빌드를 위해 시각 고정
with zipfile.ZipFile(src) as zi, zipfile.ZipFile(dst, 'w') as zo:
    for it in zi.infolist():
        n = zipfile.ZipInfo(it.filename, date_time=STAMP)
        n.compress_type = it.compress_type
        n.external_attr = it.external_attr
        zo.writestr(n, zi.read(it.filename))
    n = zipfile.ZipInfo('classes.dex', date_time=STAMP)
    n.compress_type = zipfile.ZIP_DEFLATED
    with open(dex, 'rb') as f:
        zo.writestr(n, f.read())
