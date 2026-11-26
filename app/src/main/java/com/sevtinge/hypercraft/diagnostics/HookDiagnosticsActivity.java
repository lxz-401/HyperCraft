/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hypercraft.diagnostics;
import com.sevtinge.hypercraft.R;
import com.sevtinge.hypercraft.common.base.BaseActivity;

/** The host keeps the existing MIUIX activity chrome; content is a MIUIX preference fragment. */
public class HookDiagnosticsActivity extends BaseActivity {
    @Override protected int getContentLayoutId(){return R.layout.activity_hook_diagnostics;}
    @Override protected void onCreate(){
        super.onCreate();setTitle(R.string.hook_diagnostics_title);
        if(getSupportFragmentManager().findFragmentById(R.id.diagnostics_container)==null)
            getSupportFragmentManager().beginTransaction().replace(R.id.diagnostics_container,new HookDiagnosticsFragment()).commit();
    }
}
