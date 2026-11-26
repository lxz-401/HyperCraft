import com.sevtinge.hypercraft.common.utils.ShellUtils;
import com.sevtinge.hypercraft.common.utils.shell.IResult;
import com.sevtinge.hypercraft.common.utils.shell.ShellExec;
import com.sevtinge.hypercraft.common.utils.shell.ShellInit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ShellRuntimeTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        String largeOutput = "i=0; while [ $i -lt 12000 ]; do echo stdout-line; echo stderr-line >&2; i=$((i+1)); done";
        ShellUtils.CommandResult captured = ShellUtils.execCommand(largeOutput, false, true);
        check(captured.result == 0, "large output must complete");
        check(captured.successMsg.length() == 12000 * "stdout-line".length(), "capture all stdout");
        check(captured.errorMsg.length() == 12000 * "stderr-line".length(), "capture all stderr");
        ShellUtils.CommandResult discarded = ShellUtils.execCommand(largeOutput, false, false);
        check(discarded.result == 0 && discarded.successMsg == null && discarded.errorMsg == null,
            "discarding results must still drain both pipes");

        List<String> first = new CopyOnWriteArrayList<>();
        List<String> second = new CopyOnWriteArrayList<>();
        CountDownLatch firstCompletion = new CountDownLatch(1);
        ShellExec one = new ShellExec(false, true, new IResult() {
            @Override public void result(String command, int status) {
                first.add(command + ":" + status);
                firstCompletion.countDown();
            }
        });
        ShellExec two = new ShellExec(false, true, new IResult() {
            @Override public void result(String command, int status) { second.add(command + ":" + status); }
        });
        try {
            one.run("true");
            // Complete before sync starts: a notification alone is insufficient.
            check(firstCompletion.await(2, TimeUnit.SECONDS), "first command must finish before sync");
            one.sync();
            one.sync();
            two.run("false").sync();
            check(one.getResult() == 0 && two.getResult() == 1, "independent exit statuses");
            check(first.equals(List.of("true:0")), "first callback must remain on first shell: " + first);
            check(second.equals(List.of("false:1")), "second callback must remain on second shell: " + second);
            one.run("false").sync();
            two.run("true").sync();
            check(first.equals(List.of("true:0", "false:1")), "subsequent commands must retain callback ownership");
            check(second.equals(List.of("false:1", "true:0")), "second callback ownership");
        } finally {
            one.close();
            two.close();
        }
        ShellExec shared = ShellInit.getShell();
        try {
            check(shared == ShellInit.getShell(), "lazy ShellInit must retain its shell");
            shared.run("true").sync();
            check(shared.isResult(), "shared shell executes");
        } finally {
            ShellInit.destroy();
        }
        System.out.println("Shell runtime regressions: PASS");
    }
}
