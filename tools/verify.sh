#!/usr/bin/env bash
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${TMPDIR:-/tmp}/zploy-logic-test-classes"
rm -rf "$OUT" && mkdir -p "$OUT"

javac -d "$OUT" \
  "$ROOT/tools/logic-test/stubs/android/view/KeyEvent.java" \
  "$ROOT/app/src/main/java/com/zploy/app/MappingMath.java" \
  "$ROOT/app/src/main/java/com/zploy/app/ControllerState.java" \
  "$ROOT/tools/logic-test/MappingMathLogicTest.java" \
  "$ROOT/tools/logic-test/ControllerStateLogicTest.java"

java -cp "$OUT" com.zploy.app.MappingMathLogicTest
java -cp "$OUT" com.zploy.app.ControllerStateLogicTest

python3 - "$ROOT" <<'PY'
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
root=Path(sys.argv[1])
res=root/'app/src/main/res'
files=list(res.rglob('*.xml'))+[root/'app/src/main/AndroidManifest.xml']
for f in files: ET.parse(f)
print(f'XML parse: PASS ({len(files)} files)')

def keys(path):
    r=ET.parse(path).getroot()
    return {x.attrib['name'] for x in r.findall('string')}
zh=keys(res/'values/strings.xml')
en=keys(res/'values-en/strings.xml')
if zh != en:
    raise SystemExit(f'String mismatch: zh-only={sorted(zh-en)} en-only={sorted(en-zh)}')
print(f'String parity: PASS ({len(zh)} keys)')
PY

LOG="${TMPDIR:-/tmp}/zploy-javac-syntax.log"
rm -rf "${TMPDIR:-/tmp}/zploy-syntax-out"
set +e
javac -proc:none -Xmaxerrs 500 -d "${TMPDIR:-/tmp}/zploy-syntax-out" $(find "$ROOT/app/src/main/java" -name '*.java' -print) >"$LOG" 2>&1
CODE=$?
set -e
if grep -E "';' expected|illegal start of|reached end of file while parsing|class, interface, enum, or record expected|not a statement|')' expected|'\}' expected|identifier expected|unclosed" "$LOG"; then
  echo "Java parse sanity: FAIL"
  exit 1
fi
echo "Java parse sanity: PASS (unresolved Android/Shizuku types are expected without SDK classpath; javac=$CODE)"
echo "Java files: $(find "$ROOT/app/src/main/java" -name '*.java' | wc -l | tr -d ' ')"
