package phantom.desktop;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * On macOS, GLFW must run on the main thread, which requires the JVM flag
 * {@code -XstartOnFirstThread}. If it is missing (typical when launching from an IDE),
 * this helper relaunches the game in a new JVM with the flag. Other systems are unaffected.
 * <p>OOP concept: a small platform utility kept out of the game code.</p>
 */
final class StartupHelper {
    private static final String RESTARTED = "phantom.restarted";

    private StartupHelper() { }

    static boolean startNewJvmIfRequired() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("mac") || System.getProperty(RESTARTED) != null) return false;
        String pid = String.valueOf(ProcessHandle.current().pid());
        if ("1".equals(System.getenv("JAVA_STARTED_ON_FIRST_THREAD_" + pid))) return false;
        try {
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            List<String> cmd = new ArrayList<>();
            cmd.add(javaBin);
            cmd.add("-XstartOnFirstThread");
            cmd.add("-D" + RESTARTED + "=true");
            cmd.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
            cmd.add("-cp");
            cmd.add(System.getProperty("java.class.path"));
            cmd.add(DesktopLauncher.class.getName());
            Process p = new ProcessBuilder(cmd).inheritIO().start();
            p.waitFor();
            return true;
        } catch (Exception e) {
            System.err.println("Could not relaunch with -XstartOnFirstThread: " + e.getMessage());
            return false;
        }
    }
}
