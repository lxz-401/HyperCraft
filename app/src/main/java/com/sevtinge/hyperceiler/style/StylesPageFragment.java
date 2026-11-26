/*
 * This file is part of HyperCraft.
 *
 * Licensed under the GNU General Public License v3.0
 */
package com.sevtinge.hyperceiler.style;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.hooker.systemui.VolumeStyleSettings;
import com.sevtinge.hyperceiler.utils.DialogHelper;

import fan.appcompat.app.Fragment;

public class StylesPageFragment extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeRes(R.style.NavigatorContentTheme);
        setHasOptionsMenu(true);
    }

    @Override
    public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_styles, container, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.sevtinge.hyperceiler.core.R.menu.settings_sub_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == com.sevtinge.hyperceiler.core.R.id.quick_restart) {
            DialogHelper.showRestartDialog(requireActivity(), "com.android.systemui");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onViewInflated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewInflated(view, savedInstanceState);
        if (getActionBar() != null) getActionBar().setTitle(R.string.navigation_styles_title);
        if (getChildFragmentManager().findFragmentById(R.id.styles_prefs_container) == null) {
            getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.styles_prefs_container, new VolumeStyleSettings())
                .commit();
        }
    }
}
