#!/usr/bin/env python3
"""Exercise real host shells without Android or root privileges."""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'library/common/src/main/java/com/sevtinge/hyperceiler'

with tempfile.TemporaryDirectory(prefix='hc-shell-test-') as temporary:
    work = Path(temporary)
    fixtures = {
        'androidx/annotation/Nullable.java': 'package androidx.annotation; public @interface Nullable {}',
        'androidx/annotation/NonNull.java': 'package androidx.annotation; public @interface NonNull {}',
        'com/sevtinge/hyperceiler/common/log/AndroidLog.java': '''package com.sevtinge.hyperceiler.common.log;
public class AndroidLog {
    public static void e(String tag, String message, Throwable error) { error.printStackTrace(); }
    public static void i(String tag, String message) {}
    public static void w(String tag, String message) {}
}''',
    }
    for path, content in fixtures.items():
        destination = work / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(content)
    # ShellInit requests su; substitute a local unprivileged shell only for this test.
    executable = work / 'bin/su'
    executable.parent.mkdir()
    executable.write_text('#!/bin/sh\nexec sh "$@"\n')
    executable.chmod(0o755)
    java_home = os.environ.get('JAVA_HOME')
    javac = str(Path(java_home) / 'bin/javac') if java_home else shutil.which('javac')
    java = str(Path(java_home) / 'bin/java') if java_home else shutil.which('java')
    sources = list(work.rglob('*.java'))
    sources += [SOURCE / 'common/utils/shell' / name for name in
                ('IResult.java', 'ShellExec.java', 'ShellInit.java')]
    sources += [SOURCE / 'common/utils/ShellUtils.java', Path(__file__).with_name('ShellRuntimeTest.java')]
    subprocess.run([javac, '-encoding', 'UTF-8', '-d', str(work), *map(str, sources)], check=True)
    environment = dict(os.environ, PATH=str(executable.parent) + os.pathsep + os.environ['PATH'])
    subprocess.run([java, '-ea', '-cp', str(work), 'ShellRuntimeTest'], env=environment,
                   check=True, timeout=30)
