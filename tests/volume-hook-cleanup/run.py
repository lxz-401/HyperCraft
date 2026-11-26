#!/usr/bin/env python3
"""Exercise the production cleanup dispatcher with a deterministic Android main queue."""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
compiler = os.environ.get('KOTLINC') or shutil.which('kotlinc')
if compiler is None:
    bundled = Path('/opt/android-studio/plugins/Kotlin/kotlinc/bin/kotlinc')
    if bundled.exists():
        compiler = str(bundled)
if compiler is None:
    raise SystemExit('Set KOTLINC or install a Kotlin compiler')
source = ROOT / 'library/libhook/src/main/java/com/sevtinge/hypercraft/libhook/rules/systemui/plugin/systemui/VolumeHookCleanup.kt'
with tempfile.TemporaryDirectory(prefix='hypercraft-volume-cleanup-') as directory:
    jar = str(Path(directory) / 'test.jar')
    subprocess.run([compiler, str(source), str(HERE / 'AndroidMainStub.kt'), str(HERE / 'CleanupTest.kt'), '-include-runtime', '-d', jar], check=True)
    subprocess.run(['java', '-jar', jar], check=True)
