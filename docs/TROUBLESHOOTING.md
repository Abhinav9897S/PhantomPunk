# Troubleshooting (Eclipse and Gradle)

### "Unsupported class file major version" / "invalid source release: 17"
The project needs **JDK 17 or newer**. Check with `java -version`.
* Command line: set `JAVA_HOME` to a JDK 17+ folder.
* Eclipse: *Window > Preferences > Java > Installed JREs* > add a JDK 17+ and tick it. Then
  *Window > Preferences > Gradle* > set *Java home* to the same JDK, and right-click the
  project > *Gradle > Refresh Gradle Project*.

### Eclipse does not show a "Gradle" import option
Install **Buildship**: *Help > Eclipse Marketplace* > search "Buildship Gradle Integration".
It is already included in "Eclipse IDE for Java Developers".

### Import worked but there are red errors everywhere / "The import com.badlogic cannot be resolved"
The Gradle sync did not finish (often because the first download was interrupted).
Right-click the root project > *Gradle > Refresh Gradle Project*. Make sure you imported with
*Import > Gradle > Existing Gradle Project*, **not** *General > Existing Projects*.

### "Missing source folder" or tests not visible in Eclipse
Refresh the Gradle project (see above). Source folders are `core/src/main/java`,
`core/src/test/java` and `desktop/src/main/java`; Buildship creates them from the Gradle files.
Do not edit `.classpath` by hand.

### "Asset not found: rooms/world.json" or "font not found"
The game looks for `assets/` first in the working directory and then on the classpath.
* `./gradlew desktop:run` sets the working directory to `assets/` automatically.
* In Eclipse, the desktop project's classpath already contains the `assets` folder. If you
  created your own run configuration, either keep the default classpath or set
  *Run Configurations > Arguments > Working directory* to `${workspace_loc}/PhantomPunk/assets`
  (adjust to your folder name).
* Check that the `assets` folder still sits next to `core` and `desktop`.

### The window opens and closes at once on macOS / "GLFW may only be used on the main thread"
macOS needs the JVM flag `-XstartOnFirstThread`. `DesktopLauncher` relaunches itself with that
flag automatically (`StartupHelper`). If you run from Eclipse and it still fails, add
`-XstartOnFirstThread` under *Run Configurations > Arguments > VM arguments*.

### Gradle fails with "Could not resolve com.badlogicgames.gdx..." / HTTP 429
The first build downloads libraries from Maven Central. Check your internet connection or
proxy; rerun the build (a temporary "429 Too Many Requests" goes away on retry). After one
successful build everything is cached in `~/.gradle`.

### "Permission denied: ./gradlew" (Linux/macOS)
`chmod +x gradlew`

### Black screen or "OpenGL 2.0 required"
Update the graphics driver. On virtual machines, enable 3D acceleration.

### No sound
Sound is optional. If the audio device cannot be opened, the game logs "Audio disabled" and
continues silently. Check the volume in *Settings*.

### My save is gone after an update
If a save file is unreadable or from an incompatible version, the game shows a message, starts
fresh and keeps the old file as `~/.phantom-descent/save.json.bad`.

### Tests fail with "assets folder not found"
Run tests from the project root (`./gradlew test`) or, in Eclipse, run them in the `core`
project; the tests also look for `../assets` relative to the working directory.
