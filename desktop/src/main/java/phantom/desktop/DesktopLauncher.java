package phantom.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import phantom.PhantomGame;
import phantom.save.SaveManager;
import phantom.util.Constants;

/**
 * Desktop entry point (LWJGL3). Run with {@code ./gradlew desktop:run} or as a Java
 * application from Eclipse.
 * <p>OOP concept: the only platform-specific class; the game itself lives in the core module.</p>
 */
public final class DesktopLauncher {
    private DesktopLauncher() { }

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // macOS needs -XstartOnFirstThread
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(Constants.TITLE);
        config.setWindowedMode(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        config.setWindowSizeLimits(640, 360, -1, -1);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setIdleFPS(30);
        if (args.length >= 2 && args[0].equals("--tour")) {
            // developer screenshot tour; uses a throwaway save folder so real saves are untouched
            java.nio.file.Path temp = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "phantom-tour-save");
            new Lwjgl3Application(new ScreenshotTour(new PhantomGame(temp), args[1]), config);
            return;
        }
        new Lwjgl3Application(new PhantomGame(SaveManager.defaultDirectory()), config);
    }
}
