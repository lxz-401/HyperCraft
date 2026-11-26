#!/usr/bin/env python3
"""Compile the production progress converter and run its JVM regression cases."""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/utils/hookapi/tool/VolumeProgress.java'
with tempfile.TemporaryDirectory(prefix='hypercraft-volume-progress-') as directory:
    subprocess.run(['javac', '-d', directory, str(SOURCE), str(Path(__file__).with_name('VolumeProgressTest.java'))], check=True)
    subprocess.run(['java', '-cp', directory, 'VolumeProgressTest'], check=True)
