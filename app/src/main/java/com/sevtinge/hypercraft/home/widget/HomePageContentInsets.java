package com.sevtinge.hypercraft.home.widget;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hypercraft.hooker.systemui.VolumeStyleSettings;
import com.sevtinge.hypercraft.settings.SettingsFragment;
import com.sevtinge.hypercraft.ui.HomePageActivity;

import fan.preference.PreferenceFragment;

/** Keeps home preferences scrollable above both navigation styles. */
public final class HomePageContentInsets {
    private HomePageContentInsets() { }

    public static void register(HomePageActivity activity) {
        activity.getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewCreated(@NonNull FragmentManager manager, @NonNull Fragment fragment,
                                              @NonNull View view, @Nullable Bundle savedInstanceState) {
                if (!(fragment instanceof SettingsFragment || fragment instanceof VolumeStyleSettings)) return;
                RecyclerView list = ((PreferenceFragment) fragment).getListView();
                SwitchManager switchManager = activity.getSwitchManager();
                if (list == null || switchManager == null || switchManager.getSwitchView() == null) return;
                View navigation = switchManager.getSwitchView();
                int originalBottom = list.getPaddingBottom();
                list.setClipToPadding(false);
                ViewTreeObserver.OnGlobalLayoutListener listener = () -> {
                    if (!list.isAttachedToWindow() || !navigation.isAttachedToWindow()) return;
                    int[] listLocation = new int[2];
                    int[] navigationLocation = new int[2];
                    list.getLocationOnScreen(listLocation);
                    navigation.getLocationOnScreen(navigationLocation);
                    int overlap = navigation.getVisibility() == View.VISIBLE
                        ? Math.max(0, listLocation[1] + list.getHeight() - navigationLocation[1]) : 0;
                    int clearance = (int) (8 * list.getResources().getDisplayMetrics().density);
                    int bottom = Math.max(originalBottom, overlap > 0 ? overlap + clearance : 0);
                    if (list.getPaddingBottom() != bottom) {
                        list.setPaddingRelative(list.getPaddingStart(), list.getPaddingTop(), list.getPaddingEnd(), bottom);
                    }
                };
                list.getViewTreeObserver().addOnGlobalLayoutListener(listener);
                fragment.getViewLifecycleOwner().getLifecycle().addObserver(new DefaultLifecycleObserver() {
                    @Override
                    public void onDestroy(@NonNull LifecycleOwner owner) {
                        if (list.getViewTreeObserver().isAlive()) {
                            list.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
                        }
                    }
                });
            }
        }, true);
    }
}
