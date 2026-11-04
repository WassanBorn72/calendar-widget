"""Validate a checked-in distribution APK before GitHub Release publication."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
version = sys.argv[1] if len(sys.argv) > 1 else 'v1.0.6'
if not re.fullmatch(r'v\d+\.\d+\.\d+', version):
    raise SystemExit('Expected a semantic-version tag, such as v1.0.6')
assets = root / 'release-assets' / version
manifest = json.loads((assets / 'calendar-widget_release.json').read_text())
apk = assets / f'calendar-widget-{version}.apk'
actual = hashlib.sha256(apk.read_bytes()).hexdigest()
if actual != manifest['apkSha256']:
    raise SystemExit('APK checksum mismatch')
expected_sum = f'{actual}  {apk.name}'
if (assets / 'calendar-widget_SHA256SUMS').read_text().strip() != expected_sum:
    raise SystemExit('SHA256SUMS mismatch')
tools = Path(os.environ['ANDROID_HOME']) / 'build-tools' / '35.0.0'
signature = subprocess.check_output([str(tools / 'apksigner'), 'verify', '--verbose', '--print-certs', str(apk)], text=True)
match = re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]+)', signature)
cert_file = (assets / 'calendar-widget_PUBLIC_CERTIFICATE_SHA256.txt').read_text().strip().replace(':', '').lower()
if not match or match.group(1) != manifest['certificateSha256'] or match.group(1) != cert_file:
    raise SystemExit('Signing certificate mismatch')
if 'Number of signers: 1' not in signature or 'Verified using v2 scheme (APK Signature Scheme v2): true' not in signature:
    raise SystemExit('Expected one APK v2 signer')
badging = subprocess.check_output([str(tools / 'aapt'), 'dump', 'badging', str(apk)], text=True)
for field, value in [('name', 'io.github.wassanborn72.calendarwidget'), ('versionCode', str(manifest['versionCode'])), ('versionName', version[1:])]:
    if f"{field}='{value}'" not in badging.splitlines()[0]:
        raise SystemExit(f'APK {field} mismatch')
if "sdkVersion:'26'" not in badging or 'application-debuggable' in badging:
    raise SystemExit('Wrong minimum SDK or debuggable APK')
print(f'PASS: {version}, package, version, minimum SDK, non-debuggable, SHA-256 and release signature')
