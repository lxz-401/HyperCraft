package com.sevtinge.hypercraft.ui;

import static android.os.Process.killProcess;
import static android.os.Process.myPid;
import static com.sevtinge.hypercraft.libhook.utils.api.DeviceHelper.System.SUPPORT_FULL;
import static com.sevtinge.hypercraft.libhook.utils.api.DeviceHelper.System.SUPPORT_PARTIAL;
import static com.sevtinge.hypercraft.libhook.utils.api.DeviceHelper.System.getAndroidVersion;
import static com.sevtinge.hypercraft.libhook.utils.api.DeviceHelper.System.getHyperOSVersion;
import static com.sevtinge.hypercraft.libhook.utils.api.DeviceHelper.System.getSmallVersion;
import static com.sevtinge.hypercraft.libhook.utils.api.DeviceHelper.System.getVersionListText;
import static com.sevtinge.hypercraft.libhook.utils.api.DeviceHelper.System.isVersionListed;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sevtinge.hypercraft.R;
import com.sevtinge.hypercraft.style.StylesPageFragment;
import com.sevtinge.hypercraft.about.AboutPageFragment;
import com.sevtinge.hypercraft.about.AboutSettingsFragment;
import com.sevtinge.hypercraft.common.log.AndroidLog;
import com.sevtinge.hypercraft.common.utils.AppSettingsStore;
import com.sevtinge.hypercraft.common.utils.PrefsBridge;
import com.sevtinge.hypercraft.common.utils.shell.IResult;
import com.sevtinge.hypercraft.dashboard.base.ActivityCallback;
import com.sevtinge.hypercraft.home.HomePageFragment;
import com.sevtinge.hypercraft.home.IconTitleLoader;
import com.sevtinge.hypercraft.home.adapter.HomeContentAdapter;
import com.sevtinge.hypercraft.home.manager.PageDecorator;
import com.sevtinge.hypercraft.home.task.AppInitializer;
import com.sevtinge.hypercraft.log.LogManager;
import com.sevtinge.hypercraft.log.XposedLogLoader;
import com.sevtinge.hypercraft.home.widget.NavigationStyle;
import com.sevtinge.hypercraft.home.widget.SwitchManager;
import com.sevtinge.hypercraft.home.widget.SwitchMediator;
import com.sevtinge.hypercraft.provision.utils.NoticeProvider;
import com.sevtinge.hypercraft.provision.utils.ProvisionManager;
import com.sevtinge.hypercraft.settings.SettingsFragment;
import com.sevtinge.hypercraft.settings.SettingsPageFragment;
import com.sevtinge.hypercraft.utils.NoticeProcessor;
import com.sevtinge.hypercraft.utils.PersistConfig;
import com.sevtinge.hypercraft.utils.XposedActivateHelper;

import java.util.ArrayList;
import java.util.List;

import fan.appcompat.app.AlertDialog;
import fan.appcompat.app.AppCompatActivity;
import fan.preference.PreferenceFragment;
import fan.provider.Settings;
import fan.provision.OobeUtils;
import fan.viewpager.widget.ViewPager;

public class HomePageActivity extends AppCompatActivity
    implements ActivityCallback, IResult,
    PreferenceFragment.OnPreferenceStartFragmentCallback {

    private static final String STATE_CURRENT_PAGE = "home_current_page";

    public ViewPager mViewPager;
    public HomeContentAdapter mContentAdapter;

    public SwitchManager mSwitchManager;
    private boolean mIsUnsupportedVersionExiting;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(com.sevtinge.hypercraft.utils.LanguageHelper.wrapContext(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PersistConfig.isAprilFoolsThemeView) setTheme(R.style.HomePageAprilFoolsTheme);
        if (!OobeUtils.isProvisioned(this) && !OobeUtils.isDebugOobeMode(this)) {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
            return;
        }
        if (!isVersionListed()) {
            showUnsupportedVersionDialog();
            return;
        }
        // Log health/import belongs to an explicit UI session, not Provider startup.
        LogManager.init(this, null, () -> XposedLogLoader.syncLogsToDatabaseSync(this));
        // Activity 启动阶段，绑定 UI 任务（如签名校验弹窗、公告展示）
        AppInitializer.initOnActivityCreate(this, this);
        setContentView(R.layout.activity_home);
        setupNavigation();
        restoreCurrentPage(savedInstanceState);

        ProvisionManager.setProvider(context -> {
            NoticeProcessor.NoticeResult result = NoticeProcessor.process(context);
            List<Integer> list = new ArrayList<>();
            if (result != null) {
                list.add(result.protocolVersion());
                list.add(result.privacyVersion());
            }
            return list;
        });
    }

    private void setupNavigation() {
        mSwitchManager = new SwitchManager(findViewById(R.id.container));

        boolean isFloating = AppSettingsStore.isFloatNavEnabled(this);

        NavigationStyle initialStyle = isFloating ? NavigationStyle.CAPSULE_ICON : NavigationStyle.BOTTOM_LABEL;
        mSwitchManager.addSwitchView(R.menu.bottom_nav_menu, initialStyle);

        // 后续变化通过 LiveData 监听
        LiveData<Boolean> isFloatNavEnabled = Settings.Global.getBooleanLiveData(
            this,
            AppSettingsStore.KEY_FLOAT_NAV,
            false
        );
        isFloatNavEnabled.observe(this, isEnabled -> {
            // Hook/备份链路仍依赖 prefs，保持镜像同步。
            PrefsBridge.putByApp(AppSettingsStore.PREF_FLOAT_NAV, isEnabled);
            mSwitchManager.setFloatingStyle(isEnabled);
        });

        mViewPager = findViewById(R.id.vp_fragments);
        com.sevtinge.hypercraft.home.widget.HomePageContentInsets.register(this);
        rebuildContentPages();
        new SwitchMediator(mSwitchManager, mViewPager, true).attach();
    }

    private void rebuildContentPages() {
        // Retire the old position-only tags before restoring pages with stable class tags.
        androidx.fragment.app.FragmentTransaction legacyPages = getSupportFragmentManager().beginTransaction();
        boolean hasLegacyPages = false;
        for (androidx.fragment.app.Fragment fragment : getSupportFragmentManager().getFragments()) {
            String tag = fragment.getTag();
            if (tag != null && tag.matches("f[0-9]+")) {
                legacyPages.remove(fragment);
                hasLegacyPages = true;
            }
        }
        if (hasLegacyPages) legacyPages.commitNow();
        mContentAdapter = new HomeContentAdapter(this);
        mContentAdapter.addFragment(new HomePageFragment());
        mContentAdapter.addFragment(new StylesPageFragment());
        mContentAdapter.addFragment(new SettingsPageFragment());
        mContentAdapter.addFragment(new AboutPageFragment());

        mViewPager.setAdapter(mContentAdapter);
        mViewPager.setOffscreenPageLimit(4);
    }

    public void reloadPagesForLanguageChange() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        // 首页应用名和页面上下文都依赖当前 locale，直接重建能避免旧 Context 残留。
        IconTitleLoader.clearLabelCache();
        recreate();
    }

    private void restoreCurrentPage(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null || mViewPager == null) {
            return;
        }
        int currentItem = Math.max(0, Math.min(savedInstanceState.getInt(STATE_CURRENT_PAGE, 0),
            mContentAdapter.getItemCount() - 1));
        mViewPager.setCurrentItem(currentItem, false);
        if (mSwitchManager != null) {
            mSwitchManager.setSelectedPosition(currentItem, false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    public SwitchManager getSwitchManager() {
        return mSwitchManager;
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        if (caller instanceof SettingsFragment || caller instanceof AboutSettingsFragment
            || caller instanceof com.sevtinge.hypercraft.hooker.systemui.VolumeStyleSettings) {
            onStartSubSettingsForArguments(this, pref, false);
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        PageDecorator.onResume();
        if (mContentAdapter != null) XposedActivateHelper.init(this);
    }

    @Override
    protected void onPause() {
        XposedActivateHelper.clear(this);
        super.onPause();
        PageDecorator.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mViewPager != null) {
            outState.putInt(STATE_CURRENT_PAGE, mViewPager.getCurrentItem());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        XposedActivateHelper.clear(this);
        super.onDestroy();
    }

    private void showUnsupportedVersionDialog() {
        String versionText = getString(
            R.string.homepage_unsupported_version_current,
            getAndroidVersion(),
            formatHyperOsVersion()
        );
        String supportedVersionText = getVersionListText(SUPPORT_FULL);
        String partialSupportedVersionText = getVersionListText(SUPPORT_PARTIAL);
        if (!partialSupportedVersionText.isEmpty()) {
            supportedVersionText = supportedVersionText.isEmpty()
                ? partialSupportedVersionText
                : supportedVersionText + "\n" + partialSupportedVersionText;
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle(R.string.warn)
            .setMessage(getString(R.string.homepage_unsupported_version_message, versionText, supportedVersionText))
            .setPositiveButton(R.string.exit, (d, which) -> exitForUnsupportedVersion())
            .create();

        dialog.show();

        final var button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (button == null) {
            exitForUnsupportedVersion();
            return;
        }

        button.setText(getString(R.string.exit) + " (30)");

        new CountDownTimer(30_000L, 1_000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                button.setText(getString(R.string.exit) + " (" + (millisUntilFinished / 1000L) + ")");
            }

            @Override
            public void onFinish() {
                if (!isFinishing() && !isDestroyed()) {
                    dialog.dismiss();
                }
                exitForUnsupportedVersion();
            }
        }.start();
    }

    private String formatHyperOsVersion() {
        float smallVersion = getSmallVersion();
        if (smallVersion > 0f) {
            return String.valueOf(smallVersion);
        }
        return String.valueOf(getHyperOSVersion());
    }

    private void exitForUnsupportedVersion() {
        if (mIsUnsupportedVersionExiting) {
            return;
        }
        mIsUnsupportedVersionExiting = true;
        finishAffinity();
        killProcess(myPid());
    }
}
