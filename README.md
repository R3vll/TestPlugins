**⚠️ This is currently under development, dont use it yet if you're not comfortable with constantly merging new changes**

# `Cloudstream3 Plugin Repo Template`

Template for a [Cloudstream3](https://github.com/recloudstream) plugin repo

**⚠️ Make sure you check "Include all branches" when using this template**

 
## Getting started with writing your first plugin

This template includes 1 example plugin.

1. Open the root build.gradle.kts, read the comments and replace all the placeholders
2. Familiarize yourself with the project structure. Most files are commented
3. Build or deploy your first plugin using:
   - Windows: `.\gradlew.bat ExampleProvider:make` or `.\gradlew.bat ExampleProvider:deployWithAdb`
   - Linux & Mac: `./gradlew ExampleProvider:make` or `./gradlew ExampleProvider:deployWithAdb`


## Granting All Files Access on Newer Android Devices

For local plugin testing, you need to grant the app "All Files Access" on newer Android devices (Android 11 and above). Here’s how to do it:

### Using ADB

* `adb shell appops set --uid PACKAGE_NAME MANAGE_EXTERNAL_STORAGE allow`
* Replace `PACKAGE_NAME` with the name of the package for the Cloudstream3 version you are using:
   - debug: `com.lagradost.cloudstream3.prerelease.debug`
   - prerelease: `com.lagradost.cloudstream3.prerelease`
   - stable: `com.lagradost.cloudstream3`

### Manually

1. **Open Settings**: Go to your device’s Settings menu.

2. **Navigate to Special Access**:
   - Tap on "Apps & notifications" or "Apps".
   - Select "Special app access" or "Special access".

3. **Select All Files Access**:
   - Tap on "All files access".
   - It may be under the three vertical dots menu towards the top of the screen.

4. **Grant Access to the App**: Find the app in the list and tap on it to toggle it, if it is not already enabled.

6. **Restart the App**: Close and reopen the app to apply the changes.


## License

Everything in this repo is released into the public domain. You may use it however you want with no conditions whatsoever


## Attribution

This template as well as the gradle plugin and the whole plugin system is **heavily** based on [Aliucord](https://github.com/Aliucord).
*Go use it, it's a great mobile discord client mod!*

## Vega provider port framework

This fork is the build home for native CloudStream ports of authorized Vega providers.

Framework only: all entries in `providers.yaml` are currently `blocked`; no ports exist yet. `ported` is a policy label, not enforced by the build.

- `providers.yaml` is the scope registry. A provider must be marked `ported` before a Gradle module is created for it.
- `core/src/main/kotlin/com/r3vll/vegaport` contains Vega `Stream` → CloudStream `ExtractorLink` / `SubtitleFile` mapping helpers. These sources are injected into every plugin module.
- Future ports are each their own Gradle module with a `MainAPI` subclass. The MVP implements `search()`, `load()`, and `loadLinks()`.
- CI runs `:core:test`, builds all modules, and publishes `.cs3` files plus `plugins.json` to the `builds` branch.
- Add this repository to CloudStream using the raw GitHub URL of `repository.json` on `main`.
