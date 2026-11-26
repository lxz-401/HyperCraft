/* SPDX-License-Identifier: AGPL-3.0-or-later */
import android.app.Activity;
import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.common.utils.shell.ShellExec;
import com.sevtinge.hyperceiler.common.utils.shell.ShellInit;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform.AllowAutoStart;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.volume.VolumeDefaultStream;
import com.sevtinge.hyperceiler.libhook.utils.api.*;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public final class LifecycleTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void await(CountDownLatch latch)throws Exception{check(latch.await(3,TimeUnit.SECONDS),"callback timed out");}
    static final class Input extends ByteArrayInputStream {
        boolean closed; Input(String value){super(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        public void close(){closed=true;}
    }
    static final class Output extends ByteArrayOutputStream {
        boolean closed; public void close(){closed=true;}
    }
    static void context()throws Exception {
        ExecutorService pool=ThreadPoolManager.getInstance();
        CountDownLatch delivered=new CountDownLatch(1);
        ContextUtils.getWaitContext(c->{check(c==ActivityThread.application,"delivered application");delivered.countDown();},false);
        check(ThreadPoolManager.getInstance()==pool,"context polling must not retire the shared pool");
        Thread.sleep(150);
        check(ActivityThread.attempts.get()<15,"context polling must sleep between lookups");
        ActivityThread.application=new Application();await(delivered);
        ActivityThread.system=null;ActivityThread.ui=new Context();
        check(ContextUtils.getContext(ContextUtils.FlAG_ONLY_ANDROID)==ActivityThread.ui,"system UI context fallback");
        ActivityThread.application=null;
        CountDownLatch cancelledDelivery=new CountDownLatch(1);
        ContextUtils.getWaitContext(c->cancelledDelivery.countDown(),false);
        Thread.sleep(60);pool.shutdownNow();check(pool.awaitTermination(1,TimeUnit.SECONDS),"poll must stop when interrupted");
        check(cancelledDelivery.getCount()==1,"cancelled old-generation poll must not deliver context");
        ThreadPoolManager.shutdown();
        System.out.println("PASS context wait/fallback/interruption/shared-pool lifetime");
    }
    static void backup()throws Exception {
        String legacy = new JSONObject()
            .put("brackets","Text [with spaces] and, commas")
            .put("emptyBrackets","[]")
            .put("fraction",1.25d)
            .put("large",1234567890123L)
            .put("integer",7)
            .put("apps",new JSONArray(List.of("one.two","three.four")))
            .put("prefs_key_allow_hook",false)
            .put("prefs_key_framework_check_name","untrusted backup state").toString(2);
        for(boolean useActivity:new boolean[]{false,true}) {
            PrefsBridge.values.clear();
            Context context=useActivity?new Activity():new Context();
            Input input=new Input(legacy);context.resolver.input=input;
            if(useActivity)BackupUtils.handleReadDocument((Activity)context,new Uri());
            else BackupUtils.handleReadDocument(context,new Uri());
            check(input.closed,"restore closes input");
            check(PrefsBridge.values.get("brackets").equals("Text [with spaces] and, commas"),"bracket strings retain type and whitespace");
            check(PrefsBridge.values.get("emptyBrackets").equals("[]"),"empty bracket string stays a string");
            check(PrefsBridge.values.get("fraction").equals(1.25f),"JSON fractional numbers restore as SharedPreferences floats");
            check(PrefsBridge.values.get("large").equals(1234567890123L),"long integer preserved");
            check(PrefsBridge.values.get("apps").equals(Set.of("one.two","three.four")),"array restores string set");
            check(!PrefsBridge.values.containsKey("prefs_key_allow_hook"),"backup cannot overwrite live hook gate");
            check(!PrefsBridge.values.containsKey("prefs_key_framework_check_name"),"backup cannot overwrite framework identity");
            Input bad=new Input("malformed");context.resolver.input=bad;
            try { if(useActivity)BackupUtils.handleReadDocument((Activity)context,new Uri());else BackupUtils.handleReadDocument(context,new Uri());throw new AssertionError("parse failure must propagate"); }
            catch(JSONException expected){}
            check(bad.closed,"input closed after JSON parse failure");
            Output output=new Output();context.resolver.output=output;
            if(useActivity)BackupUtils.handleCreateDocument((Activity)context,new Uri());else BackupUtils.handleCreateDocument(context,new Uri());
            check(output.closed,"backup closes output");
        }
        Map<String,Object> original = new LinkedHashMap<>();
        original.put("floatOne",1.0f);original.put("floatZero",0.0f);
        original.put("floatNegativeZero",-0.0f);original.put("floatFraction",1.25f);
        original.put("floatMin",Float.MIN_VALUE);original.put("floatMax",Float.MAX_VALUE);
        original.put("longZero",0L);original.put("longOne",1L);
        original.put("longMax",Long.MAX_VALUE);original.put("longMin",Long.MIN_VALUE);
        original.put("integer",7);original.put("string","[brackets, spaces]");
        original.put("bool",true);original.put("set",Set.of("one", "two"));
        for(boolean useActivity:new boolean[]{false,true}) {
            Context context=useActivity?new Activity():new Context();
            PrefsBridge.values.clear();PrefsBridge.values.putAll(original);
            Output output=new Output();context.resolver.output=output;
            if(useActivity)BackupUtils.handleCreateDocument((Activity)context,new Uri());else BackupUtils.handleCreateDocument(context,new Uri());
            check(output.closed,"round-trip output closed");
            String serialized=output.toString(java.nio.charset.StandardCharsets.UTF_8);
            PrefsBridge.values.clear();
            Input input=new Input(serialized);context.resolver.input=input;
            if(useActivity)BackupUtils.handleReadDocument((Activity)context,new Uri());else BackupUtils.handleReadDocument(context,new Uri());
            check(input.closed,"round-trip input closed");
            for(var entry:original.entrySet()) {
                Object restored=PrefsBridge.values.get(entry.getKey());
                check(entry.getValue().equals(restored), "JSON round trip must preserve value and type for "
                    +entry.getKey()+": expected="+entry.getValue().getClass()+", actual="+(restored==null?null:restored.getClass()));
            }
            // These casts model SharedPreferences typed getters, which reject a restored Integer.
            check(((Float)PrefsBridge.values.get("floatOne"))==1.0f,"float typed getter after restore");
            check(((Long)PrefsBridge.values.get("longOne"))==1L,"long typed getter after restore");
            check(Float.floatToRawIntBits((Float)PrefsBridge.values.get("floatNegativeZero"))
                ==Float.floatToRawIntBits(-0.0f),"negative float zero preserves its sign bit");
        }
        Context missing=new Context();
        try{BackupUtils.handleReadDocument(missing,new Uri());throw new AssertionError("null input");}catch(IOException expected){}
        try{BackupUtils.handleCreateDocument(missing,new Uri());throw new AssertionError("null output");}catch(IOException expected){}
        System.out.println("PASS real SDK37 JSON backup round trips/types/legacy numbers/overloads/stream closure/runtime gate filtering");
    }
    static void volume() {
        new VolumeDefaultStream().init();
        var hook=BaseHook.hooks.get("getActiveStreamType");
        for(int i=0;i<1000;i++) {
            PrefsBridge.values.put("system_framework_default_volume_stream",""+(i%5));
            HookParam p=new HookParam(0);hook.before(p);
            check(i%5==0 ? p.result==null : p.result.equals(i%5),"live preference updates, including restoring the default stream");
        }
        System.out.println("PASS volume live preferences across 1000 hot-path calls without observers");
    }
    static void autoStart()throws Exception {
        new AllowAutoStart().init();
        var checkStart=BaseHook.hooks.get("getCheckStartActivityIntent");
        var restrict=BaseHook.hooks.get("restrictForChain");
        PrefsBridge.values.put("system_framework_auto_start_apps",Set.of("allowed"));
        HookParam allowed=new HookParam(null,new ApplicationInfo("allowed"));
        HookParam denied=new HookParam(null,new ApplicationInfo("denied"));
        checkStart.before(allowed);checkStart.before(denied);
        HookParam inner=new HookParam((Object)null);restrict.before(inner);check(inner.result==null,"nested denied callee must not inherit outer exemption");
        checkStart.after(denied);
        HookParam outer=new HookParam((Object)null);restrict.before(outer);check(Boolean.FALSE.equals(outer.result),"nested scope restores outer exemption even with null caller");
        CountDownLatch entered=new CountDownLatch(1),leave=new CountDownLatch(1);
        Thread other=new Thread(()->{checkStart.before(denied);entered.countDown();try{leave.await();}catch(InterruptedException e){throw new AssertionError(e);}finally{checkStart.after(denied);}});
        other.start();await(entered);
        HookParam isolated=new HookParam(new ApplicationInfo("caller"));restrict.before(isolated);check(Boolean.FALSE.equals(isolated.result),"other thread must not overwrite callee correlation");
        leave.countDown();other.join();
        checkStart.after(allowed);
        HookParam stale=new HookParam((Object)null);restrict.before(stale);check(stale.result==null,"completed request leaves no stale callee exemption");
        HookParam noCallee=new HookParam(null,null);checkStart.before(noCallee);restrict.before(stale);checkStart.after(noCallee);
        PrefsBridge.values.put("system_framework_auto_start_apps",Set.of());
        checkStart.before(allowed);HookParam changed=new HookParam((Object)null);restrict.before(changed);checkStart.after(allowed);
        check(changed.result==null,"auto-start selection follows live preference changes");
        System.out.println("PASS auto-start nested/thread-local/null/stale/live-preference handling");
    }
    static void properties()throws Exception {
        String value="spaces 'quoted' $(printf substituted) `whoami` $HOME \\\"";
        ShellInit.gets=0;check(PropUtils.setProp("persist.fixture",value),"property command executes");
        check(ShellInit.gets==1,"one shell instance per command");
        var quote=PropUtils.class.getDeclaredMethod("shellQuote",Object.class);quote.setAccessible(true);
        String quoted=(String)quote.invoke(null,value);
        Process process=new ProcessBuilder("/bin/sh","-c","printf '%s' "+quoted).start();
        String decoded=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        check(process.waitFor()==0&&decoded.equals(value),"shell quote preserves metacharacters as literal text");
        check(ShellExec.command.startsWith("setprop 'persist.fixture' '"),"property name and value are both quoted");
        System.out.println("PASS single shell acquisition and literal POSIX property quoting");
    }
    public static void main(String[] args)throws Exception {
        try{context();backup();volume();autoStart();properties();}finally{ThreadPoolManager.shutdown();}
    }
}
