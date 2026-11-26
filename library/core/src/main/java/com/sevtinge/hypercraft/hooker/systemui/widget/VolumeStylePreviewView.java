/*
 * This file is part of HyperCraft.
 *
 * Licensed under the GNU General Public License v3.0
 */
package com.sevtinge.hypercraft.hooker.systemui.widget;

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
import androidx.core.graphics.ColorUtils;

import com.sevtinge.hypercraft.core.R;

import java.util.Locale;

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

    private final ViewOutlineProvider mRoundedOutline = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            int width = view.getWidth();
            int height = view.getHeight();
            if (width <= 0 || height <= 0) {
                outline.setEmpty();
                return;
            }
            float radius = mRadiusDp * getResources().getDisplayMetrics().density;
            outline.setRoundRect(0, 0, width, height, Math.min(radius, Math.min(width, height) / 2f));
        }
    };

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
            mFlTrack.setOutlineProvider(mRoundedOutline);
            mFlTrack.setClipToOutline(true);
            mFlTrack.setOnClickListener(v -> { });
            mFlTrack.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                    updateProgressHeight();
                }
            });
            mFlTrack.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (v.getParent() != null) v.getParent().requestDisallowInterceptTouchEvent(true);
                        updateProgressFromTouch(v, event);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        updateProgressFromTouch(v, event);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (v.getParent() != null) v.getParent().requestDisallowInterceptTouchEvent(false);
                        v.performClick();
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        if (v.getParent() != null) v.getParent().requestDisallowInterceptTouchEvent(false);
                        return true;
                    default:
                        return false;
                }
            });
        }
        if (mVProgress != null) {
            mVProgress.setOutlineProvider(mRoundedOutline);
            mVProgress.setClipToOutline(true);
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

    private void updateProgressFromTouch(View view, MotionEvent event) {
        if (view.getHeight() <= 0) return;
        float ratio = Math.max(0f, Math.min(1f, 1f - event.getY() / view.getHeight()));
        int percent = Math.round(ratio * 100);
        if (percent != mProgressPercent) {
            mProgressPercent = percent;
            updateProgressHeight();
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
    }

    public void updateConfig(String presetId, int radiusDp, int widthDp, int progressColor, int bgColor) {
        mPresetId = presetId != null ? presetId : "oneui";
        mRadiusDp = Math.max(8, Math.min(50, radiusDp));
        mWidthDp = Math.max(36, Math.min(85, widthDp));
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

            mFlTrack.invalidateOutline();
        }

        // 2. Progress fill color and radius
        if (mVProgress != null) {
            GradientDrawable progressDrawable = new GradientDrawable();
            progressDrawable.setShape(GradientDrawable.RECTANGLE);
            progressDrawable.setCornerRadius(radiusPx);
            progressDrawable.setColor(mProgressColor);
            mVProgress.setBackground(progressDrawable);

            mVProgress.invalidateOutline();
            updateProgressHeight();
        }

        // Badge text and color
        if (mTvBadge != null) {
            String badgeText;
            int badgeColor;
            switch (mPresetId.toLowerCase(Locale.ROOT)) {
                case "ios":
                    badgeText = "Apple iOS";
                    badgeColor = getResources().getColor(R.color.textview_black_title, getContext().getTheme());
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
                    badgeText = getResources().getString(R.string.volume_style_preview_custom);
                    badgeColor = mProgressColor;
                    break;
                case "oneui":
                default:
                    badgeText = "Samsung One UI";
                    badgeColor = 0xFF3478F6;
                    break;
            }
            mTvBadge.setText(badgeText);
            int surface = getResources().getColor(R.color.volume_preview_card, getContext().getTheme());
            int badgeSurface = ColorUtils.compositeColors(ColorUtils.setAlphaComponent(badgeColor, 40), surface);
            if (ColorUtils.calculateContrast(ColorUtils.setAlphaComponent(badgeColor, 255), badgeSurface) < 4.5) {
                badgeColor = contrastColor(badgeSurface);
            }
            mTvBadge.setTextColor(badgeColor);

            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius((int) (8 * density));
            badgeBg.setColor(Color.argb(40, Color.red(badgeColor), Color.green(badgeColor), Color.blue(badgeColor)));
            mTvBadge.setBackground(badgeBg);
        }

        // Info summary
        if (mTvInfo != null) {
            mTvInfo.setText(getResources().getString(R.string.volume_style_preview_dimensions, mRadiusDp, mWidthDp));
        }
        updateIconContrast();
    }

    private void updateProgressHeight() {
        if (mVProgress != null && mFlTrack != null) {
            int totalHeight = mFlTrack.getHeight();
            int progressHeight = (int) (totalHeight * (mProgressPercent / 100.0f));
            ViewGroup.LayoutParams lp = mVProgress.getLayoutParams();
            if (totalHeight > 0 && lp != null && lp.height != progressHeight) {
                lp.height = progressHeight;
                mVProgress.setLayoutParams(lp);
            }
        }
        if (mTvLevel != null) {
            mTvLevel.setText(getResources().getString(R.string.volume_style_preview_percent, mProgressPercent));
        }
        updateIconContrast();
    }

    private void updateIconContrast() {
        if (mIvIcon == null || mFlTrack == null) return;
        int surface = getResources().getColor(R.color.volume_preview_stage_end, getContext().getTheme());
        int track = ColorUtils.compositeColors(mBgColor, surface);
        float density = getResources().getDisplayMetrics().density;
        boolean overProgress = mFlTrack.getHeight() * (mProgressPercent / 100f) >= 27 * density;
        int background = overProgress ? ColorUtils.compositeColors(mProgressColor, track) : track;
        mIvIcon.setColorFilter(contrastColor(background));
    }

    private static int contrastColor(int background) {
        return ColorUtils.calculateContrast(Color.WHITE, background) >= ColorUtils.calculateContrast(Color.BLACK, background)
            ? Color.WHITE : Color.BLACK;
    }
}
