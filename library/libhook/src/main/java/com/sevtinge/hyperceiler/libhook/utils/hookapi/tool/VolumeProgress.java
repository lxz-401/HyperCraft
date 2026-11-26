/*
 * This file is part of HyperCraft.
 * Licensed under the GNU General Public License v3.0
 */
package com.sevtinge.hyperceiler.libhook.utils.hookapi.tool;

/** Progress conversion shared by the volume and brightness overlays. */
public final class VolumeProgress {
    private VolumeProgress() {}

    public static int percentage(int progress, int max) {
        if (max <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) progress * 100 / max));
    }

    public static int volumePercentage(int progress, int max) {
        if (max <= 0) return 0;
        int clamped = Math.max(0, Math.min(progress, max));
        int maxLevel = max / 1000;
        // Older/plugin variants may expose unscaled progress instead of 1000 units per level.
        if (maxLevel == 0) return percentage(clamped, max);
        int level = clamped == 0 ? 0 : clamped == max ? maxLevel
            : (int) ((long) clamped * (maxLevel - 1) / max) + 1;
        return percentage(level, maxLevel);
    }
}
