# JoltBenchmarks: libGDX vs. Godot 4.5

<img width="1640" height="957" alt="Jolt" src="https://github.com/user-attachments/assets/f60df838-cc4f-4e5a-8d30-b5bda3e60f1d" />

YouTube video: https://youtu.be/1vwJIjNa1ik

### This repo contains a libGDX project and Godot 4.5.beta3 compiled Windows binaries, and WASM single thread web build.

The Godot windows binaries are in the `Godot_Export` directory.  Use the console version so you get the performance data.

To run the Godot HTML build, go into the `Godot_Export\HTML` directory and run `server.bat` (needs Python installed) from the command line.  If you do not have Python installed then use Itellij/Android Studio to run the `index.html` file.

To run the libGDX HTML build, go into the `libGDX_Export\HTML` directory and run `server.bat` (needs Python installed) from the command line.  If you do not have Python installed then use Itellij/Android Studio to run the `index.html` file.

If you are familair with Gradle libGDX projects just import this repo as a gradle project.  I used Java 17.  You then can run the libGDX desktop version and the teavm version using Gradle tasks.

Or you can run the JAR file in `libGDX_Exports` from the command line.  This is the prefered method if you want to set the thread count. 

The default number of threads is set to 11. You should set the number of threads to less than the number of cores your CPU has. 
To change pass the number of threads as a parameter to the jar file:

For 7 threads:
```
java -jar JoltBenchmark-1.0.0.jar 7
```

The complete report can be viewed for more information: [JoltPhysics-Godot-vs-libGDX.pdf](https://github.com/antzGames/Jolt-libGDX-vs-Godot/blob/master/JoltPhysics-Godot-vs-libGDX.pdf)

The test is based on **xpenatan**'s `gdx-jolt` sample test called [BoxSpawnTest.java](https://github.com/xpenatan/gdx-jolt/blob/master/examples/samples/core/src/main/java/jolt/example/samples/app/tests/playground/box/BoxSpawnTest.java)

His original samples can be viewed [here](https://xpenatan.github.io/gdx-jolt/examples/samples/).

## Summary of results

![jolt1](https://github.com/user-attachments/assets/77a2065d-43dc-4373-bd66-37b189961809)

![jolt2](https://github.com/user-attachments/assets/c6bec383-e32a-4fe4-a9b7-1aa13f579971)

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.
- `android`: Android mobile platform. Needs Android SDK.
- `teavm`: Web backend that supports most JVM languages.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `android:lint`: performs Android project validation.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `teavm:build`: builds the JavaScript application into the build/dist/webapp folder.
- `teavm:run`: serves the JavaScript application at http://localhost:8080 via a local Jetty server.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
