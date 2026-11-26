/*
 * This file is part of HyperCraft.
 *
 * Licensed under the GNU General Public License v3.0
 */
package com.sevtinge.hyperceiler.hooker.systemui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.core.R;

public class VolumeStylePreviewView extends FrameLayout {

    private TextView mTvBadge;
    private TextView mTvLevel;
    private TextView mTvInfo;
    private FrameLayout mFlTrack;
    private View mVProgress;
    private ImageView mIvIcon;
    private Button mBtnTest;

    private String mPresetId = "oneui";
    private int mRadiusDp = 28;
    private int mWidthDp = 52;
    private int mProgressColor = 0xFF3478F6;
    private int mBgColor = 0x33000000;
    private int mProgressPercent = 68;

    public VolumeStylePreviewView(@NonNull Context context) {
        this(context, null);
    }

    public VolumeStylePreviewView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_volume_style_preview_content, this, true);
        mTvBadge = findViewById(R.id.tv_preview_badge);
        mTvLevel = findViewById(R.id.tv_preview_level);
        mTvInfo = findViewById(R.id.tv_preview_info);
        mFlTrack = findViewById(R.id.fl_preview_track);
        mVProgress = findViewById(R.id.v_preview_progress);
        mIvIcon = findViewById(R.id.iv_preview_icon);
        mBtnTest = findViewById(R.id.btn_test_system_ui);

        if (mFlTrack != null) {
            mFlTrack.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        float height = v.getHeight();
                        if (height > 0) {
                            float y = event.getY();
                            float ratio = 1.0f - (y / height);
                            ratio = Math.max(0.0f, Math.min(1.0f, ratio));
                            mProgressPercent = Math.round(ratio * 100);
                            updateProgressHeight();
                            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                        }
                        return true;
                }
                return false;
            });
        }

        if (mBtnTest != null) {
            mBtnTest.setOnClickListener(v -> {
                try {
                    AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    if (am != null) {
                        am.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
                    }
                } catch (Exception ignored) {}
            });
        }
    }

    public void updateConfig(String presetId, int radiusDp, int widthDp, int progressColor, int bgColor) {
        mPresetId = presetId != null ? presetId : "oneui";
        mRadiusDp = radiusDp;
        mWidthDp = widthDp;
        mProgressColor = progressColor;
        mBgColor = bgColor;
        applyVisuals();
    }

    private void applyVisuals() {
        float density = getResources().getDisplayMetrics().density;
        int widthPx = (int) (mWidthDp * density);
        int radiusPx = (int) (mRadiusDp * density);

        // 1. Track width and radius
        if (mFlTrack != null) {
            ViewGroup.LayoutParams lp = mFlTrack.getLayoutParams();
            if (lp != null && lp.width != widthPx) {
                lp.width = widthPx;
                mFlTrack.setLayoutParams(lp);
            }

            GradientDrawable trackDrawable = new GradientDrawable();
            trackDrawable.setShape(GradientDrawable.RECTANGLE);
            trackDrawable.setCornerRadius(radiusPx);
            trackDrawable.setColor(mBgColor);
            mFlTrack.setBackground(trackDrawable);

            mFlTrack.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radiusPx);
                }
            });
            mFlTrack.setClipToOutline(true);
        }

        // 2. Progress fill color and radius
        if (mVProgress != null) {
            GradientDrawable progressDrawable = new GradientDrawable();
            progressDrawable.setShape(GradientDrawable.RECTANGLE);
            progressDrawable.setCornerRadius(radiusPx);
            progressDrawable.setColor(mProgressColor);
            mVProgress.setBackground(progressDrawable);

            mVProgress.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radiusPx);
                }
            });
            mVProgress.setClipToOutline(true);
            updateProgressHeight();
        }

        // 3. Icon tint (Contrast optimization for white / iOS progress)
        if (mIvIcon != null) {
            int iconColor = "ios".equalsIgnoreCase(mPresetId) ? 0xDD2A2A2A : 0xFFFFFFFF;
            mIvIcon.setColorFilter(iconColor);
        }

        // 4. Badge text and color
        if (mTvBadge != null) {
            String badgeText;
            int badgeColor;
            switch (mPresetId.toLowerCase()) {
                case "ios":
                    badgeText = "Apple iOS";
                    badgeColor = 0xFFFFFFFF;
                    break;
                case "pixel":
                    badgeText = "Google Pixel";
                    badgeColor = 0xFF6750A4;
                    break;
                case "hyperos":
                    badgeText = "Xiaomi HyperOS";
                    badgeColor = 0xFF1875F0;
                    break;
                case "custom":
                    badgeText = "Maxsus (Custom)";
                    badgeColor = mProgressColor;
                    break;
                case "oneui":
                default:
                    badgeText = "Samsung One UI";
                    badgeColor = 0xFF3478F6;
                    break;
            }
            mTvBadge.setText(badgeText);
            mTvBadge.setTextColor(badgeColor);

            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius((int) (8 * density));
            badgeBg.setColor(Color.argb(40, Color.red(badgeColor), Color.green(badgeColor), Color.blue(badgeColor)));
            mTvBadge.setBackground(badgeBg);
        }

        // 5. Info summary
        if (mTvInfo != null) {
            mTvInfo.setText("Radius: " + mRadiusDp + "dp • Kenglik: " + mWidthDp + "dp");
        }
    }

    private void updateProgressHeight() {
        if (mVProgress != null && mFlTrack != null) {
            int totalHeight = mFlTrack.getHeight();
            if (totalHeight <= 0) {
                totalHeight = (int) (160 * getResources().getDisplayMetrics().density);
            }
            int progressHeight = (int) (totalHeight * (mProgressPercent / 100.0f));
            ViewGroup.LayoutParams lp = mVProgress.getLayoutParams();
            if (lp != null) {
                lp.height = progressHeight;
                mVProgress.setLayoutParams(lp);
            }
        }
        if (mTvLevel != null) {
            mTvLevel.setText(mProgressPercent + "%");
        }
    }
}
