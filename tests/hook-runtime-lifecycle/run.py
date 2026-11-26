#!/usr/bin/env python3
"""Run production context/executor/rules/backup code with deterministic host adapters."""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
PROD = ROOT / 'library/libhook/src/main/java/com/sevtinge/hypercraft/libhook'
KOTLIN = Path(os.environ.get('KOTLIN_HOME', '/opt/android-studio/plugins/Kotlin/kotlinc'))
JAVA = Path(os.environ.get('JAVA_HOME', Path(shutil.which('javac')).resolve().parents[1])) / 'bin'
JSON_SOURCES = Path(os.environ.get('HYPERCRAFT_JSON_SOURCE',
    str(Path(os.environ.get('ANDROID_HOME', Path.home() / 'Android/Sdk')) / 'sources/android-37.0/org/json')))
STUBS = {
'android/compat/annotation/UnsupportedAppUsage.java': 'package android.compat.annotation; public @interface UnsupportedAppUsage {}',
'android/annotation/SystemApi.java': 'package android.annotation; public @interface SystemApi { Client client(); enum Client { MODULE_LIBRARIES } }',
'libcore/util/NonNull.java': 'package libcore.util; @java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE_USE,java.lang.annotation.ElementType.PARAMETER,java.lang.annotation.ElementType.METHOD,java.lang.annotation.ElementType.FIELD}) public @interface NonNull {}',
'libcore/util/Nullable.java': 'package libcore.util; @java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE_USE,java.lang.annotation.ElementType.PARAMETER,java.lang.annotation.ElementType.METHOD,java.lang.annotation.ElementType.FIELD}) public @interface Nullable {}',
'android/annotation/SuppressLint.java': 'package android.annotation; public @interface SuppressLint { String[] value(); }',
'androidx/annotation/Nullable.java': 'package androidx.annotation; public @interface Nullable {}',
'androidx/annotation/IntDef.java': 'package androidx.annotation; public @interface IntDef { long[] value(); }',
'android/os/Looper.java': 'package android.os; public class Looper { public static Looper getMainLooper(){return new Looper();} }',
'android/os/Handler.java': '''package android.os; public class Handler {
 public static final java.util.Queue<Runnable> pending=new java.util.concurrent.ConcurrentLinkedQueue<>();
 public Handler(Looper l){} public boolean post(Runnable r){pending.add(r);return true;}
 public void removeCallbacksAndMessages(Object x){pending.clear();}
 public static void drain(){for(Runnable r;(r=pending.poll())!=null;)r.run();}
}''',
'android/content/Context.java': '''package android.content; public class Context {
 public final ContentResolver resolver=new ContentResolver(); public ContentResolver getContentResolver(){return resolver;}
 public ClassLoader getClassLoader(){return getClass().getClassLoader();}
}''',
'android/content/ContentResolver.java': '''package android.content; public class ContentResolver {
 public java.io.InputStream input; public java.io.OutputStream output;
 public java.io.InputStream openInputStream(android.net.Uri u){return input;}
 public java.io.OutputStream openOutputStream(android.net.Uri u){return output;}
}''',
'android/content/Intent.java': '''package android.content; public class Intent {
 public static final String ACTION_CREATE_DOCUMENT="create",ACTION_OPEN_DOCUMENT="open",CATEGORY_OPENABLE="openable",EXTRA_TITLE="title";
 public Intent(String a){} public void addCategory(String a){} public void setType(String a){} public void putExtra(String k,String v){}
}''',
'android/content/pm/ApplicationInfo.java': 'package android.content.pm; public class ApplicationInfo { public String packageName; public ApplicationInfo(String n){packageName=n;} }',
'android/app/Application.java': 'package android.app; public class Application extends android.content.Context {}',
'android/app/Activity.java': 'package android.app; public class Activity extends android.content.Context {}',
'android/app/ActivityThread.java': '''package android.app; public class ActivityThread {
 public static volatile Application application; public static volatile android.content.Context system,ui;
 public static final java.util.concurrent.atomic.AtomicInteger attempts=new java.util.concurrent.atomic.AtomicInteger();
 public static Application currentApplication(){attempts.incrementAndGet();return application;}
 public static ActivityThread currentActivityThread(){return new ActivityThread();}
 public android.content.Context getSystemContext(){return system;} public android.content.Context getSystemUiContext(){return ui;}
}''',
'android/net/Uri.java': 'package android.net; public class Uri {}',
'android/os/SystemProperties.java': '''package android.os; public class SystemProperties {
 public static String get(String k){return "";} public static String get(String k,String d){return d;}
 public static boolean getBoolean(String k,boolean d){return d;} public static int getInt(String k,int d){return d;} public static long getLong(String k,long d){return d;}
}''',
'com/sevtinge/hypercraft/common/log/AndroidLog.java': 'package com.sevtinge.hypercraft.common.log; public class AndroidLog { public static void e(Object... a){} public static void d(Object... a){} }',
'com/sevtinge/hypercraft/common/log/XposedLog.java': 'package com.sevtinge.hypercraft.common.log; public class XposedLog { public static void d(Object... a){} }',
'com/sevtinge/hypercraft/common/utils/PrefsBridge.java': '''package com.sevtinge.hypercraft.common.utils; public class PrefsBridge {
 public static final java.util.Map<String,Object> values=new java.util.concurrent.ConcurrentHashMap<>();
 public static int getStringAsInt(String k,int d){return Integer.parseInt((String)values.getOrDefault(k,""+d));}
 public static int getInt(String k,int d){return (Integer)values.getOrDefault(k,d);}
 @SuppressWarnings("unchecked") public static java.util.Set<String> getStringSet(String k){return (java.util.Set<String>)values.getOrDefault(k,java.util.Set.of());}
 public static void putByApp(String k,Object v){values.put(k,v);} public static java.util.Map<String,?> getAll(){return values;}
}''',
'com/sevtinge/hypercraft/common/utils/ShellUtils.java': 'package com.sevtinge.hypercraft.common.utils; public class ShellUtils { public static int checkRootPermission(){return 0;} public static String rootExecCmd(String s){return "";} public static boolean getResultBoolean(String s,boolean b){return false;} public static String getResult(String s,boolean b){return "";} }',
'com/sevtinge/hypercraft/common/utils/shell/ShellExec.java': 'package com.sevtinge.hypercraft.common.utils.shell; public class ShellExec { public static String command; public ShellExec run(String s){command=s;return this;} public ShellExec sync(){return this;} public boolean isResult(){return true;} }',
'com/sevtinge/hypercraft/common/utils/shell/ShellInit.java': 'package com.sevtinge.hypercraft.common.utils.shell; public class ShellInit { public static int gets; public static ShellExec getShell(){gets++;return new ShellExec();} }',
'com/sevtinge/hypercraft/libhook/base/BaseHook.java': '''package com.sevtinge.hypercraft.libhook.base; public class BaseHook {
 public String TAG="fixture"; public static final java.util.List<Runnable> cleanups=new java.util.ArrayList<>();
 public static final java.util.Map<String,io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook> hooks=new java.util.HashMap<>();
 public static boolean classMissing;
 public void init(){} public static void registerHotReloadCleanup(Runnable r){cleanups.add(r);}
 public static void cleanup(){var copy=java.util.List.copyOf(cleanups);cleanups.clear();copy.forEach(Runnable::run);}
 public Class<?> findClassIfExists(String s){return classMissing?null:Object.class;}
 public static Class<?> findClass(String s,ClassLoader l){return Object.class;}
 public static Object callStaticMethod(Object... a){return "";}
 public void findAndHookMethod(Object owner,String name,Object... args){hooks.put(name,(io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook)args[args.length-1]);}
}''',
'io/github/lingqiqi5211/ezhooktool/xposed/java/IMethodHook.java': '''package io.github.lingqiqi5211.ezhooktool.xposed.java; public interface IMethodHook {
 default void before(io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam p){}
 default void after(io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam p){}
}''',
'io/github/lingqiqi5211/ezhooktool/xposed/common/HookParam.java': '''package io.github.lingqiqi5211.ezhooktool.xposed.common; public class HookParam {
 public final Object[] args; public Object result; public HookParam(Object... a){args=a;} public Object[] getArgs(){return args;} public void setResult(Object r){result=r;}
}''',

}
with tempfile.TemporaryDirectory(prefix='hc-hook-runtime-') as temp:
    work = Path(temp)
    sources = []
    for name, text in STUBS.items():
        path = work / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
        sources.append(path)
    sources += [PROD / name for name in ['utils/api/ContextUtils.java', 'utils/api/ThreadPoolManager.java',
                 'utils/api/BackupUtils.java', 'utils/api/PropUtils.java',
                 'rules/systemframework/freeform/AllowAutoStart.java', 'rules/systemframework/volume/VolumeDefaultStream.java']]
    json_sources = [JSON_SOURCES / (name + '.java') for name in
                    ['JSON', 'JSONObject', 'JSONArray', 'JSONStringer', 'JSONTokener', 'JSONException']]
    if not all(path.exists() for path in json_sources):
        raise SystemExit('Android SDK37 JSON sources required; set HYPERCRAFT_JSON_SOURCE to their org/json directory')
    sources += json_sources + [HERE / 'LifecycleTest.java']
    subprocess.run([str(JAVA / 'javac'), '--release', '21', '-d', str(work), *map(str, sources)], check=True)
    subprocess.run([str(JAVA / 'java'), '-ea', '-cp', str(work), 'LifecycleTest'], check=True, timeout=20)
    stdlib = KOTLIN / 'lib/kotlin-stdlib.jar'
    subprocess.run([str(JAVA / 'java'), '-cp', str(KOTLIN / 'lib/kotlin-compiler.jar'),
        'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-kotlin-home', str(KOTLIN), '-jvm-target', '21',
        '-classpath', str(work), '-d', str(work / 'kotlin'), str(PROD / 'utils/api/HostExecutor.kt'),
        str(PROD / 'rules/systemui/other/MonetThemeOverlay.kt'), str(HERE / 'HookDsl.kt'), str(HERE / 'ExecutorTest.kt')], check=True)
    subprocess.run([str(JAVA / 'java'), '-ea', '-cp', os.pathsep.join(map(str, [work, work/'kotlin', stdlib])), 'ExecutorTestKt'], check=True, timeout=20)
