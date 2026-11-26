import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.VolumeProgress;

public class VolumeProgressTest {
    private static void equal(int expected, int actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    public static void main(String[] args) {
        equal(0, VolumeProgress.percentage(10, 0));
        equal(0, VolumeProgress.percentage(10, -1));
        equal(0, VolumeProgress.percentage(-1, 10));
        equal(50, VolumeProgress.percentage(5, 10));
        equal(200, VolumeProgress.percentage(20, 10));
        equal(100, VolumeProgress.percentage(Integer.MAX_VALUE, Integer.MAX_VALUE));
        equal(Integer.MAX_VALUE, VolumeProgress.percentage(Integer.MAX_VALUE, 1));
        equal(0, VolumeProgress.volumePercentage(10, 0));
        equal(0, VolumeProgress.volumePercentage(10, -1));
        equal(50, VolumeProgress.volumePercentage(5, 10));
        equal(100, VolumeProgress.volumePercentage(999, 999));
        equal(100, VolumeProgress.volumePercentage(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Preserve the OEM scaled level rounding used by the existing overlay.
        for (int max : new int[] {1000, 2000, 7000, 15000, 30000}) {
            int previous = -1;
            for (int progress = 0; progress <= max; progress++) {
                int maxLevel = max / 1000;
                int level = progress == 0 ? 0 : progress == max ? maxLevel
                    : progress * (maxLevel - 1) / max + 1;
                int percent = VolumeProgress.volumePercentage(progress, max);
                equal(level * 100 / maxLevel, percent);
                if (percent < previous || percent < 0 || percent > 100) {
                    throw new AssertionError("Invalid scaled progress at " + progress + "/" + max);
                }
                previous = percent;
            }
            equal(0, VolumeProgress.volumePercentage(-1, max));
            equal(100, VolumeProgress.volumePercentage(max + 1, max));
        }
        System.out.println("Volume progress regression checks passed");
    }
}
